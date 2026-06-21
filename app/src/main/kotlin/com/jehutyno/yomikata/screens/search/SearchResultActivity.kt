package com.jehutyno.yomikata.screens.search

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.databinding.ActivitySearchBinding
import com.jehutyno.yomikata.util.addOrReplaceFragment
import org.kodein.di.DIAware
import org.kodein.di.android.closestDI
import org.kodein.di.direct
import org.kodein.di.instance
import org.kodein.di.DI


class SearchResultActivity : AppCompatActivity(), DIAware {

    // kodein
    override val di: DI by closestDI()

    private lateinit var binding: ActivitySearchBinding
    private lateinit var searchResultFragment : SearchResultFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if(resources.getBoolean(R.bool.portrait_only)){
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        setSupportActionBar(binding.toolbar)

        supportActionBar?.apply {
            setHomeAsUpIndicator(R.drawable.ic_arrow_back_orange_24dp)
            setDisplayHomeAsUpEnabled(true)
            title = getString(R.string.search_title)
        }

        searchResultFragment = if (savedInstanceState != null) {
            //Restore the fragment's instance
            supportFragmentManager.getFragment(savedInstanceState, "searchResultFragment") as SearchResultFragment
        } else {
            SearchResultFragment(di)
        }
        addOrReplaceFragment(R.id.container_content, searchResultFragment)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        //Save the fragment's instance
        supportFragmentManager.putFragment(outState, "searchResultFragment", searchResultFragment)
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
