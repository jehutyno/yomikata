package com.jehutyno.yomikata.screens.quizzes

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.window.OnBackInvokedDispatcher
import androidx.activity.addCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.databinding.ActivityQuizzesBinding
import com.jehutyno.yomikata.repository.database.YomikataDatabase
import com.jehutyno.yomikata.screens.home.HomeFragment
import com.jehutyno.yomikata.screens.selections.SelectionsFragment
import com.jehutyno.yomikata.screens.settings.SettingsFragment
import com.jehutyno.yomikata.ui.components.BottomNavDestination
import com.jehutyno.yomikata.ui.components.YomikataBottomBar
import com.jehutyno.yomikata.ui.theme.YomikataTheme
import com.jehutyno.yomikata.util.*
import com.jehutyno.yomikata.util.backup.RestartDialogMessage
import com.jehutyno.yomikata.util.backup.backupProgress
import com.jehutyno.yomikata.util.backup.getBackupLauncher
import com.jehutyno.yomikata.util.backup.getRestartDialog
import com.jehutyno.yomikata.util.backup.getRestoreLauncher
import com.jehutyno.yomikata.util.backup.restoreProgress
import com.jehutyno.yomikata.util.backup.triggerRebirth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.android.closestDI
import splitties.alertdialog.appcompat.*


class QuizzesActivity : AppCompatActivity(), DIAware {

    override val di: DI by closestDI()

    @Volatile private var dbReady = false

    private lateinit var backupLauncher: ActivityResultLauncher<Intent>
    private lateinit var restoreLauncher: ActivityResultLauncher<Intent>

    private lateinit var binding: ActivityQuizzesBinding

    private var currentDestination by mutableStateOf(BottomNavDestination.STUDY)

    @SuppressLint("MissingSuperCall")
    override fun onSaveInstanceState(outState: Bundle) {
        // prevent ViewPager2/Fragment state restoration conflicts during migration
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        val pref = PreferenceManager.getDefaultSharedPreferences(this)
        AppCompatDelegate.setDefaultNightMode(pref.getInt(Prefs.DAY_NIGHT_MODE.pref, AppCompatDelegate.MODE_NIGHT_YES))

        backupLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult())
            { result -> getBackupLauncher(result) }
        restoreLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult())
            { result -> getRestoreLauncher(result, false) }

        splashScreen.setKeepOnScreenCondition { !dbReady }

        lifecycleScope.launch {
            val success = withContext(Dispatchers.IO) {
                try { YomikataDatabase.forceLoadDatabase(this@QuizzesActivity); true }
                catch (e: Exception) { false }
            }
            dbReady = true
            if (!success) handleDatabaseError(pref)
        }

        binding = ActivityQuizzesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (resources.getBoolean(R.bool.portrait_only)) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        // Initialize BottomNav
        binding.bottomNavCompose.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                YomikataTheme {
                    YomikataBottomBar(
                        selected = currentDestination,
                        onDestinationSelected = { navigateTo(it) }
                    )
                }
            }
        }

        // Show the initial fragment
        if (savedInstanceState == null) {
            navigateTo(BottomNavDestination.STUDY)
        }

        binding.anchor.postDelayed({ tutos() }, 500)

        fun collapseOrQuit() {
            if (currentDestination != BottomNavDestination.STUDY) {
                navigateTo(BottomNavDestination.STUDY)
                return
            }
            alertDialog {
                titleResource = R.string.app_quit
                okButton { finishAffinity() }
                cancelButton()
                setOnKeyListener { _, keyCode, _ ->
                    if (keyCode == KeyEvent.KEYCODE_BACK) finishAffinity()
                    true
                }
            }.show()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT
            ) { collapseOrQuit() }
        } else {
            onBackPressedDispatcher.addCallback(this) { collapseOrQuit() }
        }
    }

    fun navigateTo(destination: BottomNavDestination) {
        val fm = supportFragmentManager
        val tag = destination.name
        val fragment = fm.findFragmentByTag(tag) ?: when (destination) {
            BottomNavDestination.HOME -> HomeFragment(di)
            BottomNavDestination.STUDY -> QuizzesFragment(di)
            BottomNavDestination.SELECTIONS -> SelectionsFragment()
            BottomNavDestination.SETTINGS -> SettingsFragment()
        }
        fm.beginTransaction()
            .replace(R.id.study_fragment_container, fragment, tag)
            .commitNow()
        currentDestination = destination
    }

    private fun studyFragment(): QuizzesFragment? =
        supportFragmentManager.findFragmentByTag(BottomNavDestination.STUDY.name) as? QuizzesFragment

    fun navigateToCategory(category: Int) {
        navigateTo(BottomNavDestination.STUDY)
        studyFragment()?.setCategory(category)
    }

    fun tutos() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        showTutoAlways(this, binding.anchor, getString(R.string.tuto_yomikataz), getString(R.string.tuto_welcome)) {
            showTutoOnce(prefs, TutoId.CATEGORIES, this, null,
                getString(R.string.tuto_categories), getString(R.string.tuto_categories_message)) {}
        }
    }

    private fun handleDatabaseError(prefs: android.content.SharedPreferences) {
        val recoveryDialog = alertDialog {
            titleResource = R.string.recovery
            positiveButton(R.string.choose_file_short) { restoreProgress(restoreLauncher) }
            neutralButton(R.string.prefs_reinit) {
                alertDialog {
                    messageResource = R.string.prefs_reinit_sure
                    okButton {
                        YomikataDatabase.resetDatabase(this@QuizzesActivity)
                        YomikataDatabase.forceLoadDatabase(this@QuizzesActivity)
                        getRestartDialog(RestartDialogMessage.RESET, null).show()
                    }
                    cancelButton()
                }.show()
            }
            cancelButton()
            setCancelable(false)
        }

        val errorDialog = alertDialog {
            titleResource = R.string.migration_error
            message = getString(R.string.contact_devs_for_help) + "\n" +
                      getString(R.string.create_backup_is_recommended)
            positiveButton(R.string.contact) {}
            neutralButton(R.string.create_backup) {}
            negativeButton(R.string.recovery) {}
            setCancelable(false)
        }
        errorDialog.setOnShowListener {
            errorDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener { contactDiscord(this) }
            errorDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener { backupProgress(backupLauncher) }
            errorDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener { recoveryDialog.show() }
        }

        var restoredSuccessfully = false
        if (prefs.getBoolean(Prefs.DB_RESTORE_ONGOING.pref, false)) {
            restoredSuccessfully = YomikataDatabase.restoreLocalBackup(this)
        }
        prefs.edit().putBoolean(Prefs.DB_RESTORE_ONGOING.pref, false).apply()

        if (restoredSuccessfully) {
            alertDialog {
                titleResource = R.string.restore_error
                messageResource = R.string.app_closed_data_recovered
                okButton { triggerRebirth() }
                setCancelable(false)
            }.show()
        } else {
            errorDialog.show()
        }
    }

    companion object
}
