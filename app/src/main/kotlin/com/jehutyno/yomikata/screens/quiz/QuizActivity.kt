package com.jehutyno.yomikata.screens.quiz

import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import android.view.KeyEvent
import android.view.MenuItem
import com.github.salomonbrys.kodein.Kodein
import com.github.salomonbrys.kodein.KodeinInjector
import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance
import com.github.salomonbrys.kodein.provider
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.util.Extras
import com.jehutyno.yomikata.util.Prefs
import com.jehutyno.yomikata.util.QuizStrategy
import com.jehutyno.yomikata.util.addOrReplaceFragment
import io.github.inflationx.viewpump.ViewPumpContextWrapper
import mu.KLogging
import org.jetbrains.anko.alert
import org.jetbrains.anko.defaultSharedPreferences
import org.jetbrains.anko.noButton
import org.jetbrains.anko.support.v4.withArguments
import org.jetbrains.anko.yesButton

class QuizActivity : AppCompatActivity() {

    companion object : KLogging()

    private val injector = KodeinInjector()
    private val quizPresenter: QuizContract.Presenter by injector.instance()
    private lateinit var quizFragment: QuizFragment

    private lateinit var quizIds: LongArray
    private lateinit var quizStrategy: QuizStrategy
    private lateinit var quiztypes: IntArray

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(ViewPumpContextWrapper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(defaultSharedPreferences.getInt(Prefs.DAY_NIGHT_MODE.pref, AppCompatDelegate.MODE_NIGHT_YES))
        setContentView(R.layout.activity_quiz)
        if(resources.getBoolean(R.bool.portrait_only)){
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        findViewById<Toolbar>(R.id.toolbar).let {
            setSupportActionBar(it)
        }
        supportActionBar?.apply {
            setHomeAsUpIndicator(R.drawable.ic_clear_orange_24dp)
            setDisplayHomeAsUpEnabled(true)
            title = intent.getStringExtra(Extras.EXTRA_QUIZ_TITLE)
        }

        if (savedInstanceState != null) {
            //Restore the fragment's instance
            quizFragment = supportFragmentManager.getFragment(savedInstanceState, "quizFragment") as QuizFragment
            quizIds = savedInstanceState.getLongArray("quiz_ids")?: longArrayOf()
            quizStrategy = savedInstanceState.getSerializable("quiz_strategy") as QuizStrategy
            quiztypes = savedInstanceState.getIntArray("quiz_types")?: intArrayOf()
        } else {
            quizIds = intent.getLongArrayExtra(Extras.EXTRA_QUIZ_IDS)
            quizStrategy = intent.getSerializableExtra(Extras.EXTRA_QUIZ_STRATEGY) as QuizStrategy
            quiztypes = intent.getIntArrayExtra(Extras.EXTRA_QUIZ_TYPES)

            quizFragment = QuizFragment().withArguments(
                Extras.EXTRA_QUIZ_IDS to quizIds,
                Extras.EXTRA_QUIZ_STRATEGY to quizStrategy,
                Extras.EXTRA_QUIZ_TYPES to quiztypes)
        }
        addOrReplaceFragment(R.id.container_content, quizFragment)

        injector.inject(Kodein {
            extend(appKodein())
            import(quizPresenterModule(quizFragment))
            bind<QuizContract.Presenter>() with provider {
                QuizPresenter(instance(), instance(), instance(), instance(), instance(), instance(), quizIds, quizStrategy, quiztypes)
            }
        })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        //Save the fragment's instance
        supportFragmentManager.putFragment(outState, "quizFragment", quizFragment)
        outState.putLongArray("quiz_ids", quizIds)
        outState.putSerializable("quiz_strategy", quizStrategy)
        outState.putIntArray("quiz_types", quiztypes)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                alert {
                    title = getString(R.string.quit_quiz)
                    yesButton { finish() }
                    noButton { }
                }.show()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        alert(R.string.quit_quiz) {
            yesButton { finish() }
            noButton { }
            onKeyPressed { _, keyCode, e ->
                if (keyCode == KeyEvent.KEYCODE_BACK)
                    finish()
                true
            }
        }.show()
    }

    fun unlockFullVersion() {
        quizFragment.unlockFullVersion()
    }
}
