package com.jehutyno.yomikata.screens.answers

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.WindowCompat
import androidx.preference.PreferenceManager
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.databinding.ActivityAnswersBinding
import com.jehutyno.yomikata.util.Prefs
import com.jehutyno.yomikata.util.addOrReplaceFragment
import mu.KLogging
import org.kodein.di.DIAware
import org.kodein.di.android.closestDI
import org.kodein.di.DI


/**
 * Created by valentin on 25/10/2016.
 */
class AnswersActivity : AppCompatActivity(), DIAware {

    companion object : KLogging()

    // kodein
    override val di: DI by closestDI()

    private lateinit var binding: ActivityAnswersBinding
    private lateinit var answersFragment: AnswersFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pref = PreferenceManager.getDefaultSharedPreferences(this)
        AppCompatDelegate.setDefaultNightMode(pref.getInt(Prefs.DAY_NIGHT_MODE.pref, AppCompatDelegate.MODE_NIGHT_YES))
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityAnswersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if(resources.getBoolean(R.bool.portrait_only)){
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        answersFragment = if (savedInstanceState != null) {
            //Restore the fragment's instance
            supportFragmentManager.getFragment(savedInstanceState, "answersFragment") as AnswersFragment
        } else {
            AnswersFragment(di)
        }
        addOrReplaceFragment(R.id.container_content, answersFragment)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        //Save the fragment's instance
        supportFragmentManager.putFragment(outState, "answersFragment", answersFragment)
    }

}
