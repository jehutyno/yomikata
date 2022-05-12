package com.jehutyno.yomikata.screens.content

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.viewpager.widget.ViewPager
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import android.view.MenuItem
import android.view.View.GONE
import android.view.View.VISIBLE
import com.github.salomonbrys.kodein.Kodein
import com.github.salomonbrys.kodein.KodeinInjector
import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance
import com.github.salomonbrys.kodein.provider
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.model.Quiz
import com.jehutyno.yomikata.model.StatAction
import com.jehutyno.yomikata.model.StatResult
import com.jehutyno.yomikata.repository.QuizRepository
import com.jehutyno.yomikata.repository.local.StatsSource
import com.jehutyno.yomikata.screens.quiz.QuizActivity
import com.jehutyno.yomikata.util.*
import com.jehutyno.yomikata.util.Extras.EXTRA_LEVEL
import com.jehutyno.yomikata.util.Extras.EXTRA_QUIZ_IDS
import com.jehutyno.yomikata.util.Extras.EXTRA_QUIZ_TITLE
import io.github.inflationx.viewpump.ViewPumpContextWrapper
import kotlinx.android.synthetic.main.activity_content.*
import mu.KLogging
import org.jetbrains.anko.defaultSharedPreferences
import org.jetbrains.anko.support.v4.withArguments
import java.util.*

class ContentActivity : AppCompatActivity() {

    companion object : KLogging()

    private var quizIds = longArrayOf()
    private lateinit var selectedTypes: IntArray

    private var category: Int = -1
    private var level: Int = -1

    private lateinit var statsRepository: StatsSource

    private var contentLevelFragment: ContentFragment? = null
    private val injector = KodeinInjector()
    private val contentPresetnter: ContentContract.Presenter by injector.instance()

    private var contentPagerAdapter: ContentPagerAdapter? = null

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(ViewPumpContextWrapper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(defaultSharedPreferences.getInt(Prefs.DAY_NIGHT_MODE.pref, AppCompatDelegate.MODE_NIGHT_YES))
        setContentView(R.layout.activity_content)

        statsRepository = StatsSource(appKodein.invoke().instance())

        if (resources.getBoolean(R.bool.portrait_only)) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        category = intent.getIntExtra(Extras.EXTRA_CATEGORY, -1)
        level = intent.getIntExtra(Extras.EXTRA_LEVEL, -1)
        var quizPosition = intent.getIntExtra(Extras.EXTRA_QUIZ_POSITION, -1)
        selectedTypes = intent.getIntArrayExtra(Extras.EXTRA_QUIZ_TYPES) ?: intArrayOf()

        findViewById<Toolbar>(R.id.toolbar).let {
            setSupportActionBar(it)
        }

        supportActionBar?.apply {
            setHomeAsUpIndicator(R.drawable.ic_arrow_back_orange_24dp)
            setDisplayHomeAsUpEnabled(true)
        }

        var quizSource: QuizRepository = appKodein.invoke().instance()


        quizSource.getQuiz(category, object : QuizRepository.LoadQuizCallback {
            override fun onQuizLoaded(quizzes: List<Quiz>) {
                if (level > -1) {
                    title = when (level) {
                       0 -> getString(R.string.red_review)
                       1 -> getString(R.string.orange_review)
                       2 -> getString(R.string.yellow_review)
                       else -> getString(R.string.green_review)
                    }
                    pager_content.visibility = GONE
                    if (savedInstanceState != null) {
                        //Restore the fragment's instance
                        contentLevelFragment = supportFragmentManager.getFragment(savedInstanceState, "contentLevelFragment") as ContentFragment
                    } else {
                        val ids = mutableListOf<Long>()
                        quizzes.forEach { ids.add(it.id) }
                        contentLevelFragment = ContentFragment().withArguments(EXTRA_QUIZ_IDS to ids.toLongArray(), EXTRA_QUIZ_TITLE to title, EXTRA_LEVEL to level)
                        quizIds = ids.toLongArray()
                    }
                    addOrReplaceFragment(R.id.fragment_container, contentLevelFragment!!)
                    injector.inject(Kodein {
                        extend(appKodein())
                        import(contentPresenterModule(contentLevelFragment!!))
                        bind<ContentContract.Presenter>() with provider {
                            ContentPresenter(instance(), instance(), instance())
                        }
                    })
                } else {
                    contentPagerAdapter = ContentPagerAdapter(this@ContentActivity, supportFragmentManager, quizzes)
                    pager_content.adapter = contentPagerAdapter
                    val quizTitle = quizzes[quizPosition].getName().split("%")[0]
                    quizIds = longArrayOf(quizzes[quizPosition].id)
                    title = quizTitle
                    pager_content.currentItem = quizPosition
                    pager_content.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
                        override fun onPageScrollStateChanged(state: Int) {

                        }

                        override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {

                        }

                        override fun onPageSelected(position: Int) {
                            val quizTitle = quizzes[position].getName().split("%")[0]
                            title = quizTitle
                            quizIds = longArrayOf(quizzes[position].id)
                        }

                    })
                }
            }

            override fun onDataNotAvailable() {

            }

        })

        progressive_play.visibility = if (level > -1) GONE else VISIBLE

        progressive_play.setOnClickListener {
            launchQuiz(QuizStrategy.PROGRESSIVE)
        }
        normal_play.setOnClickListener {
            when (level) {
                0 -> launchQuiz(QuizStrategy.LOW_STRAIGHT)
                1 -> launchQuiz(QuizStrategy.MEDIUM_STRAIGHT)
                2 -> launchQuiz(QuizStrategy.HIGH_STRAIGHT)
                3 -> launchQuiz(QuizStrategy.MASTER_STRAIGHT)
                else -> launchQuiz(QuizStrategy.STRAIGHT)
            }
        }
        shuffle_play.setOnClickListener {
            when (level) {
                0 -> launchQuiz(QuizStrategy.LOW_SHUFFLE)
                1 -> launchQuiz(QuizStrategy.MEDIUM_SHUFFLE)
                2 -> launchQuiz(QuizStrategy.HIGH_SHUFFLE)
                3 -> launchQuiz(QuizStrategy.MASTER_SHUFFLE)
                else -> launchQuiz(QuizStrategy.SHUFFLE)
            }
        }
    }

    fun launchQuiz(strategy: QuizStrategy) {
        statsRepository.addStatEntry(StatAction.LAUNCH_QUIZ_FROM_CATEGORY, category.toLong(), Calendar.getInstance().timeInMillis, StatResult.OTHER)
        val cat1 = defaultSharedPreferences.getInt(Prefs.LATEST_CATEGORY_1.pref, -1)

        if (category != cat1) {
            defaultSharedPreferences.edit().putInt(Prefs.LATEST_CATEGORY_2.pref, cat1).apply()
            defaultSharedPreferences.edit().putInt(Prefs.LATEST_CATEGORY_1.pref, category).apply()
        }

        val intent = Intent(this, QuizActivity::class.java).apply {
            putExtra(Extras.EXTRA_QUIZ_IDS, quizIds)
            putExtra(Extras.EXTRA_QUIZ_TITLE, title)
            putExtra(Extras.EXTRA_QUIZ_STRATEGY, strategy)
            putExtra(Extras.EXTRA_QUIZ_TYPES, selectedTypes)
        }
        startActivity(intent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        //Save the fragment's instance
        if (contentLevelFragment != null)
            supportFragmentManager.putFragment(outState, "contentLevelFragment", contentLevelFragment!!)
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

    override fun onBackPressed() {
        if (multiple_actions.isExpanded)
            multiple_actions.collapse()
        else
            super.onBackPressed()
    }

    fun unlockFullVersion() {
        finish()
    }

}
