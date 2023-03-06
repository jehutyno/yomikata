package com.jehutyno.yomikata.screens.quizzes

import android.R.id.home
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ProgressDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.navigation.NavigationView
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.viewpager.widget.ViewPager
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import com.flaviofaria.kenburnsview.KenBurnsView
import com.getbase.floatingactionbutton.FloatingActionButton
import com.getbase.floatingactionbutton.FloatingActionsMenu
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.databinding.ActivityQuizzesBinding
import com.jehutyno.yomikata.screens.PrefsActivity
import com.jehutyno.yomikata.screens.content.QuizzesPagerAdapter
import com.jehutyno.yomikata.screens.home.HomeFragment
import com.jehutyno.yomikata.screens.search.SearchResultActivity
import com.jehutyno.yomikata.util.*
import com.jehutyno.yomikata.util.Extras.REQUEST_PREFS
import com.jehutyno.yomikata.view.AppBarStateChangeListener
import com.wooplr.spotlight.utils.SpotlightListener
import mu.KLogging
import org.jetbrains.anko.*
import java.util.*


class QuizzesActivity : AppCompatActivity() {

    companion object : KLogging() {
        val UPDATE_INTENT = "update_intent"
        val UPDATE_COUNT = "update_count"
        val UPDATE_PROGRESS = "update_progress"
        val UPDATE_FINISHED = "update_finished"
    }

    private var selectedCategory: Int = 0
    private lateinit var toolbar: Toolbar
    lateinit var fabMenu: FloatingActionsMenu
    lateinit var progressive: FloatingActionButton
    private var recreate = false
    lateinit var quizzesAdapter: QuizzesPagerAdapter
    private lateinit var kenburns: KenBurnsView
    private var menu: Menu? = null
    var progressDialog: ProgressDialog? = null
    val handler = Handler()
    val runnable = object : Runnable {
        override fun run() {
            setImageRandom()
            handler.postDelayed(this, 7000)
        }
    }

    val homeImages = intArrayOf(R.drawable.pic_04, R.drawable.pic_05, R.drawable.pic_06,
        R.drawable.pic_07, R.drawable.pic_08, R.drawable.pic_21, R.drawable.pic_22,
        R.drawable.pic_23, R.drawable.pic_24, R.drawable.pic_25)

    private val mHandler = Handler()
    private var receiversRegistered = false
    private val updateReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == UPDATE_INTENT) {
                if (progressDialog == null) {
                    progressDialog = ProgressDialog(this@QuizzesActivity)
                    progressDialog!!.max = intent.getIntExtra(UPDATE_COUNT, 0)
                    progressDialog!!.setTitle(getString(R.string.progress_bdd_update_title))
                    progressDialog!!.setMessage(getString(R.string.progress_bdd_update_message))
                    progressDialog!!.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
                    progressDialog!!.setCancelable(false)
                    progressDialog!!.show()

                }
                progressDialog!!.progress = intent.getIntExtra(UPDATE_PROGRESS, 0)
                if (intent.getBooleanExtra(UPDATE_FINISHED, false)) {
                    progressDialog!!.dismiss()
                    progressDialog = null
                    alert {
                        title = getString(R.string.update_success_title)
                        okButton { }
                        message = getString(R.string.update_success_message)
                        binding.pagerQuizzes.adapter = null
                        binding.pagerQuizzes.adapter = quizzesAdapter
                        selectedCategory = Categories.HOME
                        displayCategoryTitle(selectedCategory)
                    }.show()
                }
            }
        }
    }

    // View Binding
    private lateinit var binding: ActivityQuizzesBinding


    fun voicesDownload(level: Int) {
        launchVoicesDownload(this, level) {quizzesAdapter.notifyDataSetChanged()}
    }

    @SuppressLint("MissingSuperCall")
    override fun onSaveInstanceState(outState: Bundle) {
        // super.onSaveInstanceState(outState);
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(defaultSharedPreferences.getInt(Prefs.DAY_NIGHT_MODE.pref, AppCompatDelegate.MODE_NIGHT_YES))

        binding = ActivityQuizzesBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        // Register Sync Recievers
        if (!receiversRegistered) {
            val intentToReceiveFilter = IntentFilter()
            intentToReceiveFilter.addAction(UPDATE_INTENT)
            registerReceiver(updateReceiver, intentToReceiveFilter, null, mHandler)
            receiversRegistered = true
        }

        kenburns = binding.imageSectionIcon

        if (resources.getBoolean(R.bool.portrait_only)) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setHomeAsUpIndicator(R.drawable.ic_menu)
            setDisplayHomeAsUpEnabled(true)
            title = ""
        }

        binding.appbar.addOnOffsetChangedListener(object : AppBarStateChangeListener() {
            override fun onStateChanged(appBarLayout: AppBarLayout, state: AppBarStateChangeListener.State) {
                when (state) {
                    State.COLLAPSED -> {
                        supportActionBar?.setBackgroundDrawable(ColorDrawable(ContextCompat.getColor(this@QuizzesActivity, R.color.toolbarColor)))
                    }
                    else -> {
                        supportActionBar?.setBackgroundDrawable(ColorDrawable(ContextCompat.getColor(this@QuizzesActivity, R.color.transparent)))
                    }
                }
            }
        })

        selectedCategory = Categories.HOME
        displayCategoryTitle(Categories.HOME)
        binding.multipleActions.visibility = GONE

        // Set up the navigation drawer.
        binding.drawerLayout.setStatusBarBackground(R.color.colorPrimaryDark)
        setupDrawerContent(binding.navView)

        quizzesAdapter = QuizzesPagerAdapter(this, supportFragmentManager)
        binding.pagerQuizzes.adapter = quizzesAdapter
        binding.pagerQuizzes.currentItem = quizzesAdapter.positionFromCategory(selectedCategory)
        progressive = binding.progressivePlay
        binding.pagerQuizzes.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {

            }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {

            }

            override fun onPageSelected(position: Int) {
                selectedCategory = quizzesAdapter.categoryFromPosition(position)
                if (selectedCategory == Categories.HOME) {
                    binding.multipleActions.visibility = GONE
                    if (quizzesAdapter.registered[position] != null)
                        (quizzesAdapter.registered[position] as HomeFragment).displayLatestCategories()
                } else if (selectedCategory == Categories.CATEGORY_SELECTIONS) {
                        binding.multipleActions.visibility = View.VISIBLE
                } else {
                    binding.multipleActions.visibility = VISIBLE
                    if (selectedCategory != Categories.CATEGORY_SELECTIONS)
                        (quizzesAdapter.registered[binding.pagerQuizzes.currentItem] as QuizzesFragment).tutos()
                }

                defaultSharedPreferences.edit().putInt(Prefs.SELECTED_CATEGORY.pref, selectedCategory).apply()
                displayCategoryTitle(selectedCategory)
                binding.navView.setCheckedItem(quizzesAdapter.getMenuItemFromPosition(position))
            }

        })

        progressive = binding.progressivePlay

        progressive.setOnClickListener {

            val fragment = quizzesAdapter.registered[binding.pagerQuizzes.currentItem]
            if (fragment is QuizzesFragment) {
                fragment.launchQuizClick(QuizStrategy.PROGRESSIVE, binding.textTitle.text.toString())
                binding.multipleActions.collapseImmediately()
            }
        }
        binding.normalPlay.setOnClickListener {
            val fragment = quizzesAdapter.registered[binding.pagerQuizzes.currentItem]
            if (fragment is QuizzesFragment) {
                fragment.launchQuizClick(QuizStrategy.STRAIGHT, binding.textTitle.text.toString())
                binding.multipleActions.collapseImmediately()
            }
        }
        binding.shufflePlay.setOnClickListener {
            val fragment = quizzesAdapter.registered[binding.pagerQuizzes.currentItem]
            if (fragment is QuizzesFragment) {
                fragment.launchQuizClick(QuizStrategy.SHUFFLE, binding.textTitle.text.toString())
                binding.multipleActions.collapse()
            }
        }
        fabMenu = binding.multipleActions


        binding.anchor.postDelayed({ tutos() }, 500)

        binding.drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerClosed(drawerView: View) {
                if (recreate) {
                    recreate()
                    recreate = false
                }
            }

            override fun onDrawerStateChanged(newState: Int) {

            }

            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {

            }

            override fun onDrawerOpened(drawerView: View) {

            }

        })

    }

    private fun tutos() {
        spotlightWelcome(this, binding.anchor, getString(R.string.tuto_yomikataz), getString(R.string.tuto_welcome), SpotlightListener {
            spotlightTuto(this, getNavButtonView(toolbar), getString(R.string.tuto_categories),
                getString(R.string.tuto_categories_message), SpotlightListener {
            })
        })
    }

    private fun getNavButtonView(toolbar: Toolbar): View? {
        return (0..toolbar.childCount - 1)
            .firstOrNull { toolbar.getChildAt(it) is ImageButton }
            ?.let { toolbar.getChildAt(it) as ImageButton }
    }

    private fun setupDrawerContent(navigationView: NavigationView) {
        navigationView.getHeaderView(0).find<TextView>(R.id.version).text = getString(R.string.yomiakataz_drawer, packageManager.getPackageInfo(packageName, 0).versionName)
        navigationView.getHeaderView(0).find<ImageView>(R.id.facebook).setOnClickListener { contactFacebook(this) }
        navigationView.getHeaderView(0).find<ImageView>(R.id.discord).setOnClickListener { contactDiscord(this) }
        navigationView.getHeaderView(0).find<ImageView>(R.id.play_store).setOnClickListener { contactPlayStore(this) }
        navigationView.getHeaderView(0).find<ImageView>(R.id.share).setOnClickListener { shareApp(this) }

        navigationView.setNavigationItemSelectedListener { menuItem ->
            binding.multipleActions.collapse()
            when (menuItem.itemId) {
                R.id.home -> {
                    menuItem.isChecked = true
                    binding.pagerQuizzes.currentItem = quizzesAdapter.positionFromCategory(Categories.HOME)
                }
                R.id.your_selections_item -> {
                    menuItem.isChecked = true
                    binding.pagerQuizzes.currentItem = quizzesAdapter.positionFromCategory(Categories.CATEGORY_SELECTIONS)
                }
                R.id.hiragana_item -> {
                    menuItem.isChecked = true
                    binding.pagerQuizzes.currentItem = quizzesAdapter.positionFromCategory(Categories.CATEGORY_HIRAGANA)
                }
                R.id.katakana_item -> {
                    menuItem.isChecked = true
                    binding.pagerQuizzes.currentItem = quizzesAdapter.positionFromCategory(Categories.CATEGORY_KATAKANA)
                }
                R.id.kanji_item -> {
                    menuItem.isChecked = true
                    binding.pagerQuizzes.currentItem = quizzesAdapter.positionFromCategory(Categories.CATEGORY_KANJI)
                }
                R.id.counters_item -> {
                    menuItem.isChecked = true
                    binding.pagerQuizzes.currentItem = quizzesAdapter.positionFromCategory(Categories.CATEGORY_COUNTERS)
                }
                R.id.jlpt1_item -> {
                    menuItem.isChecked = true
                    binding.pagerQuizzes.currentItem = quizzesAdapter.positionFromCategory(Categories.CATEGORY_JLPT_1)
                }
                R.id.jlpt2_item -> {
                    menuItem.isChecked = true
                    binding.pagerQuizzes.currentItem = quizzesAdapter.positionFromCategory(Categories.CATEGORY_JLPT_2)
                }
                R.id.jlpt3_item -> {
                    menuItem.isChecked = true
                    binding.pagerQuizzes.currentItem = quizzesAdapter.positionFromCategory(Categories.CATEGORY_JLPT_3)
                }
                R.id.jlpt4_item -> {
                    menuItem.isChecked = true
                    binding.pagerQuizzes.currentItem = quizzesAdapter.positionFromCategory(Categories.CATEGORY_JLPT_4)
                }
                R.id.jlpt5_item -> {
                    menuItem.isChecked = true
                    binding.pagerQuizzes.currentItem = quizzesAdapter.positionFromCategory(Categories.CATEGORY_JLPT_5)
                }
                R.id.day_night_item -> {
                    menuItem.isChecked = !menuItem.isChecked
                    menuItem.actionView.find<SwitchCompat>(R.id.my_switch).toggle()
                }
                R.id.settings -> {
                    menuItem.isChecked = false
                    startActivityForResult(intentFor<PrefsActivity>(), REQUEST_PREFS)
                }
                else -> {
                }
            }
            binding.drawerLayout.closeDrawers()
            true
        }

        navigationView.menu.findItem(R.id.day_night_item).actionView.find<SwitchCompat>(R.id.my_switch).isChecked = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES
        navigationView.menu.findItem(R.id.day_night_item).isChecked = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES
        navigationView.menu.findItem(R.id.day_night_item).actionView.find<SwitchCompat>(R.id.my_switch).setOnCheckedChangeListener {
            switch, isChecked ->
            navigationView.menu.findItem(R.id.day_night_item).isChecked = isChecked
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                defaultSharedPreferences.edit().putInt(Prefs.DAY_NIGHT_MODE.pref, AppCompatDelegate.MODE_NIGHT_YES).apply()
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                defaultSharedPreferences.edit().putInt(Prefs.DAY_NIGHT_MODE.pref, AppCompatDelegate.MODE_NIGHT_NO).apply()
            }
            binding.drawerLayout.closeDrawers()
            recreate = true
        }
    }

    private fun setImageRandom() {
        // change images randomly
        val ran = Random()
        var i = ran.nextInt(homeImages.size)
        binding.imageSectionIcon.setImageResource(homeImages[i])
    }

    fun displayCategoryTitle(category: Int) {
        when (category) {
            Categories.HOME -> {
                binding.logoImageview.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.yomi_logo_home))
                binding.textTitle.text = getString(R.string.home_title)
                binding.navView.setCheckedItem(R.id.home)
                binding.imageSectionIcon.setImageResource(R.drawable.pic_24)
                handler.postDelayed(runnable, 7000)
            }
            Categories.CATEGORY_HIRAGANA -> {
                handler.removeCallbacks(runnable)
                binding.logoImageview.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_hiragana_big))
                binding.textTitle.setText(R.string.drawer_hiragana)
                binding.navView.setCheckedItem(R.id.hiragana_item)
                binding.imageSectionIcon.setImageResource(R.drawable.pic_miyajima)
            }
            Categories.CATEGORY_KATAKANA -> {
                handler.removeCallbacks(runnable)
                binding.textTitle.setText(R.string.drawer_katakana)
                binding.logoImageview.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_katakana_big))
                binding.navView.setCheckedItem(R.id.katakana_item)
                binding.imageSectionIcon.setImageResource(R.drawable.pic_le_charme)
            }
            Categories.CATEGORY_KANJI -> {
                handler.removeCallbacks(runnable)
                binding.logoImageview.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_kanji_big))
                binding.textTitle.setText(R.string.drawer_kanji_beginner)
                binding.navView.setCheckedItem(R.id.kanji_item)
                binding.imageSectionIcon.setImageResource(R.drawable.pic_toit)
            }
            Categories.CATEGORY_COUNTERS -> {
                handler.removeCallbacks(runnable)
                binding.logoImageview.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_counters_big))
                binding.textTitle.setText(R.string.drawer_counters)
                binding.navView.setCheckedItem(R.id.counters_item)
                binding.imageSectionIcon.setImageResource(R.drawable.pic_fujiyoshida)
            }
            Categories.CATEGORY_JLPT_1 -> {
                handler.removeCallbacks(runnable)
                binding.logoImageview.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_jlpt1_big))
                binding.textTitle.setText(R.string.drawer_jlpt1)
                binding.navView.setCheckedItem(R.id.jlpt1_item)
                binding.imageSectionIcon.setImageResource(R.drawable.pic_fujisan)
            }
            Categories.CATEGORY_JLPT_2 -> {
                handler.removeCallbacks(runnable)
                binding.logoImageview.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_jlpt2_big))
                binding.textTitle.setText(R.string.drawer_jlpt2)
                binding.navView.setCheckedItem(R.id.jlpt2_item)
                binding.imageSectionIcon.setImageResource(R.drawable.pic_hokusai)
            }
            Categories.CATEGORY_JLPT_3 -> {
                handler.removeCallbacks(runnable)
                binding.logoImageview.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_jlpt3_big))
                binding.textTitle.setText(R.string.drawer_jlpt3)
                binding.navView.setCheckedItem(R.id.jlpt3_item)
                binding.imageSectionIcon.setImageResource(R.drawable.pic_geisha)
            }
            Categories.CATEGORY_JLPT_4 -> {
                handler.removeCallbacks(runnable)
                binding.logoImageview.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_jlpt4_big))
                binding.textTitle.setText(R.string.drawer_jlpt4)
                binding.navView.setCheckedItem(R.id.jlpt4_item)
                binding.imageSectionIcon.setImageResource(R.drawable.pic_monk)
            }
            Categories.CATEGORY_JLPT_5 -> {
                handler.removeCallbacks(runnable)
                binding.logoImageview.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_jlpt5_big))
                binding.textTitle.setText(R.string.drawer_jlpt5)
                binding.navView.setCheckedItem(R.id.jlpt5_item)
                binding.imageSectionIcon.setImageResource(R.drawable.pic_dragon)
            }
            Categories.CATEGORY_SELECTIONS -> {
                handler.removeCallbacks(runnable)
                binding.logoImageview.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_selections_big))
                binding.textTitle.setText(R.string.drawer_your_selections)
                binding.navView.setCheckedItem(R.id.your_selections_item)
                binding.imageSectionIcon.setImageResource(R.drawable.pic_hanami)
            }
        }

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        this.menu = menu
        menuInflater.inflate(R.menu.menu_home, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            home -> {
                // Open the navigation drawer when the home icon is selected from the toolbar.
                binding.drawerLayout.openDrawer(GravityCompat.START)
                return true
            }
            R.id.search -> startActivity(intentFor<SearchResultActivity>())
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        if (binding.multipleActions.isExpanded)
            binding.multipleActions.collapse()
        else
            alert {
                title = getString(R.string.app_quit)
                yesButton { finishAffinity() }
                noButton { }
                onKeyPressed { _, keyCode, _ ->
                    if (keyCode == KeyEvent.KEYCODE_BACK)
                        finishAffinity()
                    true
                }
            }.show()
    }

    fun gotoCategory(category: Int) {
        binding.pagerQuizzes.currentItem = quizzesAdapter.positionFromCategory(category)
    }

    override fun onResume() {
        super.onResume()
        displayCategoryTitle(selectedCategory)
        quizzesAdapter.notifyDataSetChanged()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_PREFS -> if (resultCode == Activity.RESULT_OK) tutos()
        }
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(runnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (receiversRegistered) {
            unregisterReceiver(updateReceiver)
            receiversRegistered = false
        }
    }
}
