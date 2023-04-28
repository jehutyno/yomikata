package com.jehutyno.yomikata.screens.quiz

import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.MenuItem
import android.window.OnBackInvokedDispatcher
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.util.Extras
import com.jehutyno.yomikata.util.Prefs
import com.jehutyno.yomikata.util.QuizStrategy
import com.jehutyno.yomikata.util.addOrReplaceFragment
import io.github.inflationx.viewpump.ViewPumpContextWrapper
import mu.KLogging
import org.kodein.di.*
import org.kodein.di.android.di
import splitties.alertdialog.appcompat.alertDialog
import splitties.alertdialog.appcompat.cancelButton
import splitties.alertdialog.appcompat.okButton
import splitties.alertdialog.appcompat.titleResource


class QuizActivity : AppCompatActivity(), DIAware {

    companion object : KLogging()

    // kodein
    override val di by di()
    private val subDI by DI.lazy {
        extend(di)
        import(quizPresenterModule(quizFragment))
        bind<QuizContract.Presenter>() with provider {
            QuizPresenter(instance(), instance(), instance(), instance(), instance(), instance(),
                            quizIds, quizStrategy, quizTypes, lifecycleScope)
        }
    }
    // trigger when quizFragment is set (see subDI)
    private val trigger = DITrigger()
    @Suppress("unused")
    private val quizPresenter: QuizContract.Presenter by subDI.on(trigger = trigger).instance()

    private lateinit var quizFragment: QuizFragment

    private lateinit var quizIds: LongArray
    private lateinit var quizStrategy: QuizStrategy
    private lateinit var quizTypes: IntArray

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(ViewPumpContextWrapper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pref = PreferenceManager.getDefaultSharedPreferences(this)
        AppCompatDelegate.setDefaultNightMode(pref.getInt(Prefs.DAY_NIGHT_MODE.pref, AppCompatDelegate.MODE_NIGHT_YES))
        setContentView(R.layout.activity_quiz)
        if(resources.getBoolean(R.bool.portrait_only)){
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.apply {
            setHomeAsUpIndicator(R.drawable.ic_clear_orange_24dp)
            setDisplayHomeAsUpEnabled(true)
            title = intent.getStringExtra(Extras.EXTRA_QUIZ_TITLE)
        }

        if (savedInstanceState != null) {
            //Restore the fragment's instance
            quizFragment = supportFragmentManager.getFragment(savedInstanceState, "quizFragment") as QuizFragment
            quizIds = savedInstanceState.getLongArray("quiz_ids")?: longArrayOf()

            quizStrategy = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                savedInstanceState.getSerializable("quiz_strategy", QuizStrategy::class.java)!!
            }
            else {
                @Suppress("DEPRECATION")
                savedInstanceState.getSerializable("quiz_strategy") as QuizStrategy
            }

            quizTypes = savedInstanceState.getIntArray("quiz_types")?: intArrayOf()
        } else {
            quizIds = intent.getLongArrayExtra(Extras.EXTRA_QUIZ_IDS) ?: longArrayOf()

            quizStrategy = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getSerializableExtra(Extras.EXTRA_QUIZ_STRATEGY, QuizStrategy::class.java)!!
            }
            else {
                @Suppress("DEPRECATION")
                intent.getSerializableExtra(Extras.EXTRA_QUIZ_STRATEGY) as QuizStrategy
            }

            quizTypes = intent.getIntArrayExtra(Extras.EXTRA_QUIZ_TYPES) ?: intArrayOf()

            val bundle = Bundle()
            bundle.putLongArray(Extras.EXTRA_QUIZ_IDS, quizIds)
            bundle.putSerializable(Extras.EXTRA_QUIZ_STRATEGY, quizStrategy)
            bundle.putIntArray(Extras.EXTRA_QUIZ_TYPES, quizTypes)

            quizFragment = QuizFragment(di)
            quizFragment.arguments = bundle
        }
        addOrReplaceFragment(R.id.container_content, quizFragment)

        // quizFragment has been set so trigger injection
        trigger.trigger()

        fun askToQuitSession() {
            alertDialog(getString(R.string.quit_quiz)) {
                okButton { finish() }
                cancelButton()
                setOnKeyListener { _, keyCode, _ ->
                    if (keyCode == KeyEvent.KEYCODE_BACK)
                        finish()
                    true
                }
            }.show()
        }

        // set back button: ask if user wants to quit out of session
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_DEFAULT
            ) {
                askToQuitSession()
            }
        } else {
            onBackPressedDispatcher.addCallback(this /* lifecycle owner */) {
                askToQuitSession()
            }
        }

    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        //Save the fragment's instance
        supportFragmentManager.putFragment(outState, "quizFragment", quizFragment)
        outState.putLongArray("quiz_ids", quizIds)
        outState.putSerializable("quiz_strategy", quizStrategy)
        outState.putIntArray("quiz_types", quizTypes)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                alertDialog {
                    titleResource = R.string.quit_quiz
                    okButton { finish() }
                    cancelButton()
                }.show()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

}
