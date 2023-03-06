package com.jehutyno.yomikata.screens.search

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import android.view.MenuItem
import com.github.salomonbrys.kodein.Kodein
import com.github.salomonbrys.kodein.KodeinInjector
import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance
import com.github.salomonbrys.kodein.provider
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.util.Prefs
import com.jehutyno.yomikata.util.addOrReplaceFragment
import org.jetbrains.anko.defaultSharedPreferences

class SearchResultActivity : AppCompatActivity() {

    private val injector = KodeinInjector()
    private val searchResultPresenter: SearchResultContract.Presenter by injector.instance()
    private lateinit var searchResultFramgent : SearchResultFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(defaultSharedPreferences.getInt(Prefs.DAY_NIGHT_MODE.pref, AppCompatDelegate.MODE_NIGHT_YES))
        setContentView(R.layout.activity_search)

        if(resources.getBoolean(R.bool.portrait_only)){
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        setSupportActionBar(findViewById(R.id.toolbar))

        supportActionBar?.apply {
            setHomeAsUpIndicator(R.drawable.ic_arrow_back_orange_24dp)
            setDisplayHomeAsUpEnabled(true)
            title = getString(R.string.search_title)
        }

        if (savedInstanceState != null) {
            //Restore the fragment's instance
            searchResultFramgent = supportFragmentManager.getFragment(savedInstanceState, "searchResultFramgent") as SearchResultFragment
        } else {
            searchResultFramgent = SearchResultFragment()
        }
        addOrReplaceFragment(R.id.container_content, searchResultFramgent)

        injector.inject(Kodein {
            extend(appKodein())
            import(searchResultPresenterModule(searchResultFramgent))
            bind<SearchResultContract.Presenter>() with provider { SearchResultPresenter(instance(),
                instance(), instance())}
        })

    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        //Save the fragment's instance
        supportFragmentManager.putFragment(outState, "searchResultFramgent", searchResultFramgent)
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


}
