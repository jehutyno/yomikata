package com.jehutyno.yomikata.screens.quizzes

import android.R.id.home
import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.navigation.NavigationView
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
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
import android.widget.ProgressBar
import android.widget.TextView
import android.window.OnBackInvokedDispatcher
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.preference.PreferenceManager
import androidx.viewpager2.widget.ViewPager2
import com.flaviofaria.kenburnsview.KenBurnsView
import com.getbase.floatingactionbutton.FloatingActionButton
import com.getbase.floatingactionbutton.FloatingActionsMenu
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.databinding.ActivityQuizzesBinding
import com.jehutyno.yomikata.screens.PrefsActivity
import com.jehutyno.yomikata.screens.content.QuizzesPagerAdapter
import com.jehutyno.yomikata.screens.search.SearchResultActivity
import com.jehutyno.yomikata.util.*
import com.jehutyno.yomikata.util.Extras.REQUEST_PREFS
import com.jehutyno.yomikata.view.AppBarStateChangeListener
import com.wooplr.spotlight.utils.SpotlightListener
import mu.KLogging
import splitties.alertdialog.appcompat.*
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
    private var recreate = false
    lateinit var quizzesAdapter: QuizzesPagerAdapter
    private lateinit var kenburns: KenBurnsView
    private var menu: Menu? = null

    // alertDialog for progressBar
    private var progressAlertDialog: AlertDialog? = null
    private lateinit var progressBar: ProgressBar

    val handler = Handler(Looper.getMainLooper())
    val runnable = object : Runnable {
        override fun run() {
            setImageRandom()
            handler.postDelayed(this, 7000)
        }
    }

    val homeImages = intArrayOf(R.drawable.pic_04, R.drawable.pic_05, R.drawable.pic_06,
        R.drawable.pic_07, R.drawable.pic_08, R.drawable.pic_21, R.drawable.pic_22,
        R.drawable.pic_23, R.drawable.pic_24, R.drawable.pic_25)

    private val mHandler = Handler(Looper.getMainLooper())
    private var receiversRegistered = false
    private val updateReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == UPDATE_INTENT) {
                if (progressAlertDialog == null) {
                    progressBar.max = intent.getIntExtra(UPDATE_COUNT, 0)

                    progressAlertDialog = alertDialog {
                        titleResource = R.string.progress_bdd_update_title
                        messageResource = R.string.progress_bdd_update_message
                        setCancelable(false)
                        setView(progressBar)
                    }
                    progressAlertDialog!!.show()
                }
                progressBar.progress = intent.getIntExtra(UPDATE_PROGRESS, 0)
                if (intent.getBooleanExtra(UPDATE_FINISHED, false)) {
                    progressAlertDialog!!.dismiss()
                    progressAlertDialog = null
                    alertDialog {
                        titleResource = R.string.update_success_title
                        okButton { }
                        messageResource = R.string.update_success_message
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
        val pref = PreferenceManager.getDefaultSharedPreferences(this)
        AppCompatDelegate.setDefaultNightMode(pref.getInt(Prefs.DAY_NIGHT_MODE.pref, AppCompatDelegate.MODE_NIGHT_YES))

        binding = ActivityQuizzesBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        // progressBar for database update
        progressBar = ProgressBar(this, null, android.R.style.Widget_ProgressBar_Horizontal)

        // Register Sync Receivers
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
            override fun onStateChanged(appBarLayout: AppBarLayout, state: State) {
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

        quizzesAdapter = QuizzesPagerAdapter(this, supportFragmentManager, lifecycle)
        binding.pagerQuizzes.adapter = quizzesAdapter
        binding.pagerQuizzes.currentItem = quizzesAdapter.positionFromCategory(selectedCategory)
        binding.pagerQuizzes.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {

            override fun onPageSelected(position: Int) {
                selectedCategory = quizzesAdapter.categories[position]
                if (selectedCategory == Categories.HOME) {
                    binding.multipleActions.visibility = GONE
                } else {
                    binding.multipleActions.visibility = VISIBLE
                }
                pref.edit().putInt(Prefs.SELECTED_CATEGORY.pref, selectedCategory).apply()
                displayCategoryTitle(selectedCategory)
                binding.navView.setCheckedItem(quizzesAdapter.getMenuItemFromPosition(position))
            }

        })

        // set onClick for quiz strategies in floating action button
        fun FloatingActionButton.setLaunchQuizOnClickListener(quizStrategy: QuizStrategy) {
            this.setOnClickListener {
                // Hacky way of finding selected fragment:
                // Viewpager2 uses the tag: "f" + position to store its fragments
                val fragment = supportFragmentManager.findFragmentByTag("f${binding.pagerQuizzes.currentItem}")
                if (fragment is QuizzesFragment) {
                    fragment.launchQuizClick(quizStrategy, binding.textTitle.text.toString())
                    binding.multipleActions.collapseImmediately()
                }
            }
        }
        binding.progressivePlay.setLaunchQuizOnClickListener(QuizStrategy.PROGRESSIVE)
        binding.normalPlay.setLaunchQuizOnClickListener(QuizStrategy.STRAIGHT)
        binding.shufflePlay.setLaunchQuizOnClickListener(QuizStrategy.SHUFFLE)

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

        fun collapseOrQuit() {
            if (binding.multipleActions.isExpanded)
                binding.multipleActions.collapse()
            else
                alertDialog {
                    titleResource = R.string.app_quit
                    okButton { finishAffinity() }
                    cancelButton()
                    setOnKeyListener { _, keyCode, _ ->
                        if (keyCode == KeyEvent.KEYCODE_BACK)
                            finishAffinity()
                        true
                    }
                }.show()
        }

        // set back button to close floating actions menu or show alertDialog
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_DEFAULT
            ) {
                collapseOrQuit()
            }
        } else {
            onBackPressedDispatcher.addCallback(this) {
                collapseOrQuit()
            }
        }

    }

    private fun tutos() {
        spotlightWelcome(this, binding.anchor, getString(R.string.tuto_yomikataz), getString(R.string.tuto_welcome), SpotlightListener {
            spotlightTuto(this, getNavButtonView(toolbar), getString(R.string.tuto_categories),
                getString(R.string.tuto_categories_message), SpotlightListener {
            })
        })
    }

    private fun getNavButtonView(toolbar: Toolbar): View? {
        return (0 until toolbar.childCount)
            .firstOrNull { toolbar.getChildAt(it) is ImageButton }
            ?.let { toolbar.getChildAt(it) as ImageButton }
    }

    private fun setupDrawerContent(navigationView: NavigationView) {
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION") packageManager.getPackageInfo(packageName, 0)
        }
        navigationView.getHeaderView(0).findViewById<TextView>(R.id.version).text = getString(R.string.yomiakataz_drawer, packageInfo.versionName)
        navigationView.getHeaderView(0).findViewById<ImageView>(R.id.facebook).setOnClickListener { contactFacebook(this) }
        navigationView.getHeaderView(0).findViewById<ImageView>(R.id.discord).setOnClickListener { contactDiscord(this) }
        navigationView.getHeaderView(0).findViewById<ImageView>(R.id.play_store).setOnClickListener { contactPlayStore(this) }
        navigationView.getHeaderView(0).findViewById<ImageView>(R.id.share).setOnClickListener { shareApp(this) }

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
                    menuItem.actionView?.findViewById<SwitchCompat>(R.id.my_switch)?.toggle()
                }
                R.id.settings -> {
                    menuItem.isChecked = false
                    val intent = Intent(this, PrefsActivity::class.java)
                    startActivityForResult(intent, REQUEST_PREFS)
                }
                else -> {
                }
            }
            binding.drawerLayout.closeDrawers()
            true
        }

        navigationView.menu.findItem(R.id.day_night_item).actionView?.findViewById<SwitchCompat>(R.id.my_switch)?.isChecked = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES
        navigationView.menu.findItem(R.id.day_night_item).isChecked = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES
        navigationView.menu.findItem(R.id.day_night_item).actionView?.findViewById<SwitchCompat>(R.id.my_switch)?.setOnCheckedChangeListener {
            _, isChecked ->
            navigationView.menu.findItem(R.id.day_night_item).isChecked = isChecked
            val pref = PreferenceManager.getDefaultSharedPreferences(this)
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                pref.edit().putInt(Prefs.DAY_NIGHT_MODE.pref, AppCompatDelegate.MODE_NIGHT_YES).apply()
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                pref.edit().putInt(Prefs.DAY_NIGHT_MODE.pref, AppCompatDelegate.MODE_NIGHT_NO).apply()
            }
            binding.drawerLayout.closeDrawers()
            recreate = true
        }
    }

    private fun setImageRandom() {
        // change images randomly
        val ran = Random()
        val i = ran.nextInt(homeImages.size)
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
            R.id.search -> {
                val intent = Intent(this, SearchResultActivity::class.java)
                startActivity(intent)
            }
        }
        return super.onOptionsItemSelected(item)
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
