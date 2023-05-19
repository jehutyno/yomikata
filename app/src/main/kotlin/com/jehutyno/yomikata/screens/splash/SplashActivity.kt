package com.jehutyno.yomikata.screens.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.databinding.ActivitySplashBinding
import com.jehutyno.yomikata.repository.database.YomikataDatabase
import com.jehutyno.yomikata.screens.quizzes.QuizzesActivity
import com.jehutyno.yomikata.util.Prefs
import com.jehutyno.yomikata.util.RestartDialogMessage
import com.jehutyno.yomikata.util.UpdateProgressDialog
import com.jehutyno.yomikata.util.backupProgress
import com.jehutyno.yomikata.util.contactDiscord
import com.jehutyno.yomikata.util.getBackupLauncher
import com.jehutyno.yomikata.util.getRestartDialog
import com.jehutyno.yomikata.util.getRestoreLauncher
import com.jehutyno.yomikata.util.restoreProgress
import com.jehutyno.yomikata.util.triggerRebirth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import splitties.alertdialog.appcompat.alertDialog
import splitties.alertdialog.appcompat.cancelButton
import splitties.alertdialog.appcompat.message
import splitties.alertdialog.appcompat.messageResource
import splitties.alertdialog.appcompat.negativeButton
import splitties.alertdialog.appcompat.neutralButton
import splitties.alertdialog.appcompat.okButton
import splitties.alertdialog.appcompat.positiveButton
import splitties.alertdialog.appcompat.titleResource


// TODO: migrate to splashScreen https://developer.android.com/develop/ui/views/launch/splash-screen/migrate
class SplashActivity : AppCompatActivity() {

    // View Binding
    private lateinit var binding: ActivitySplashBinding

    private lateinit var backupLauncher : ActivityResultLauncher<Intent>
    private lateinit var restoreLauncher : ActivityResultLauncher<Intent>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySplashBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)

        val updateProgressDialogMigrate = UpdateProgressDialog(this)
        updateProgressDialogMigrate.prepare(getString(R.string.migrating), getString(R.string.may_take_a_while))

        backupLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult())
                                                        { result -> getBackupLauncher(result) }
        // don't create a local backup from the current database, since it may be corrupt
        restoreLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult())
                                                        { result -> getRestoreLauncher(result, false) }

        val recoveryDialog = alertDialog {
            titleResource = R.string.recovery

            positiveButton(R.string.choose_file_short) {
                restoreProgress(restoreLauncher)
            }
            neutralButton(R.string.prefs_reinit) {
                alertDialog {
                    messageResource = R.string.prefs_reinit_sure
                    okButton {
                        YomikataDatabase.resetDatabase(this@SplashActivity)
                        YomikataDatabase.forceLoadDatabase(this@SplashActivity)
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
        // override onClickListener to never dismiss AlertDialog
        errorDialog.setOnShowListener {
            val buttonPositive = errorDialog.getButton(AlertDialog.BUTTON_POSITIVE)
            buttonPositive.setOnClickListener {
                contactDiscord(this@SplashActivity)
            }
            val buttonNeutral = errorDialog.getButton(AlertDialog.BUTTON_NEUTRAL)
            buttonNeutral.setOnClickListener {
                backupProgress(backupLauncher)
            }
            val buttonNegative = errorDialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            buttonNegative.setOnClickListener {
                recoveryDialog.show()
            }
        }

        var success = false
        val job = CoroutineScope(Dispatchers.Main).launch {
            // do migration
            YomikataDatabase.setUpdateProgressDialog(updateProgressDialogMigrate)
            success = withContext(Dispatchers.IO) {
                try {
                    YomikataDatabase.forceLoadDatabase(this@SplashActivity)
                    return@withContext true
                } catch (e: Exception) {
                    return@withContext false
                }
            }
        }

        binding.pathView.useNaturalColors()
        binding.pathView.pathAnimator
            .delay(100)
            .duration(500)
            .listenerEnd {
                binding.logo.visibility = View.VISIBLE
            }
            .start()

        val handler = Handler(Looper.getMainLooper())
        // delay showing the progress dialog by a short time to prevent showing it when no migration is needed
        handler.postDelayed({
            if (job.isActive) {
                updateProgressDialogMigrate.show()
            }
        }, 250)
        handler.postDelayed(
            {
                // TODO: handle old yomikata database type?
//                if (!pref.getBoolean("migrationYomiDone", false)) {
//                    importYomikata("????")
//                    pref.edit().putBoolean("migrationYomiDone", true).apply()
//                }
                CoroutineScope(Dispatchers.Main).launch {
                    // finish the migration & destroy progress dialog (if it was shown at all)
                    job.join()
                    updateProgressDialogMigrate.destroy()
                    YomikataDatabase.setUpdateProgressDialog(null)

                    if (!success) { // failure -> show error dialog and don't load main activity
                        // if the error was caused due to loading a bad database from PrefsActivity,
                        // automatically restore the local backup and continue the app
                        var restoredSuccessfully = false
                        if (prefs.getBoolean(Prefs.DB_RESTORE_ONGOING.pref, false)) {
                            restoredSuccessfully =
                                YomikataDatabase.restoreLocalBackup(this@SplashActivity)
                        }
                        prefs.edit().putBoolean(Prefs.DB_RESTORE_ONGOING.pref, false).apply()
                        if (!restoredSuccessfully) {
                            errorDialog.show()
                        } else {
                            alertDialog {
                                titleResource = R.string.restore_error
                                messageResource = R.string.app_closed_data_recovered
                                okButton {
                                    this@SplashActivity.triggerRebirth()
                                }
                                setCancelable(false)
                            }.show()
                        }
                        return@launch
                    }

                    prefs.edit().putBoolean(Prefs.DB_RESTORE_ONGOING.pref, false).apply()
                    val intent = Intent(this@SplashActivity, QuizzesActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    this@SplashActivity.finish()
                }
            }, 900)

    }

}
