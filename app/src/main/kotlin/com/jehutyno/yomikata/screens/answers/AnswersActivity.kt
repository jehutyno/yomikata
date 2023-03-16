package com.jehutyno.yomikata.screens.answers

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate 
import androidx.appcompat.widget.Toolbar
import android.view.MenuItem
import androidx.preference.PreferenceManager
import com.github.salomonbrys.kodein.Kodein
import com.github.salomonbrys.kodein.KodeinInjector
import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance
import com.github.salomonbrys.kodein.provider
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.util.Prefs
import com.jehutyno.yomikata.util.addOrReplaceFragment
import mu.KLogging


/**
 * Created by valentin on 25/10/2016.
 */
class AnswersActivity : AppCompatActivity() {

    companion object : KLogging()

    private val injector = KodeinInjector()
    @Suppress("unused")
    private val answersPresenter: AnswersContract.Presenter by injector.instance()
    private lateinit var answersFragment: AnswersFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pref = PreferenceManager.getDefaultSharedPreferences(this)
        AppCompatDelegate.setDefaultNightMode(pref.getInt(Prefs.DAY_NIGHT_MODE.pref, AppCompatDelegate.MODE_NIGHT_YES))
        setContentView(R.layout.activity_answers)

        if(resources.getBoolean(R.bool.portrait_only)){
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        findViewById<Toolbar>(R.id.toolbar).let {
            setSupportActionBar(it)
            title = getString(R.string.answer_title)
        }

        supportActionBar?.apply {
            setHomeAsUpIndicator(R.drawable.ic_arrow_back_orange_24dp)
            setDisplayHomeAsUpEnabled(true)
        }

        answersFragment = if (savedInstanceState != null) {
            //Restore the fragment's instance
            supportFragmentManager.getFragment(savedInstanceState, "answersFragment") as AnswersFragment
        } else {
            AnswersFragment()
        }
        addOrReplaceFragment(R.id.container_content, answersFragment)

        injector.inject(Kodein {
            extend(appKodein())
            import(answersPresenterModule(answersFragment))
            bind<AnswersContract.Presenter>() with provider { AnswersPresenter(instance(), instance(), instance(), instance()) }
        })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        //Save the fragment's instance
        supportFragmentManager.putFragment(outState, "answersFragment", answersFragment)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                // Open the navigation drawer when the home icon is selected from the toolbar.
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun unlockFullVersion() {
        answersFragment.unlockFullVersion()
    }

}