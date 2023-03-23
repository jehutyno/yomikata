package com.jehutyno.yomikata.screens.content

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import android.view.MenuItem
import android.view.View.GONE
import android.view.View.VISIBLE
import android.window.OnBackInvokedDispatcher
import androidx.activity.addCallback
import androidx.preference.PreferenceManager
import androidx.viewpager2.widget.ViewPager2
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.databinding.ActivityContentBinding
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
import mu.KLogging
import org.kodein.di.*
import org.kodein.di.android.di
import java.util.*


class ContentActivity : AppCompatActivity(), DIAware {

    companion object : KLogging()

    private var quizIds = longArrayOf()
    private lateinit var selectedTypes: IntArray

    private var category: Int = -1
    private var level: Int = -1

    private var contentLevelFragment: ContentFragment? = null

    // kodein
    override val di by di()
    private val subDI by DI.lazy {
        extend(di)
        import(contentPresenterModule(contentLevelFragment!!))
        bind<ContentContract.Presenter>() with provider {
            ContentPresenter(instance(), instance(), instance())
        }
    }
    private lateinit var statsRepository: StatsSource
    // use trigger because contentLevelFragment may not be set yet
    private val trigger = DITrigger()
    @Suppress("unused")
    private val contentPresenter: ContentContract.Presenter by subDI.on(trigger = trigger).instance()

    private var contentPagerAdapter: ContentPagerAdapter? = null

    // View Binding
    private lateinit var binding: ActivityContentBinding


    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(ViewPumpContextWrapper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pref = PreferenceManager.getDefaultSharedPreferences(this)
        AppCompatDelegate.setDefaultNightMode(pref.getInt(Prefs.DAY_NIGHT_MODE.pref, AppCompatDelegate.MODE_NIGHT_YES))

        binding = ActivityContentBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        statsRepository = di.direct.newInstance { StatsSource(instance()) }

        if (resources.getBoolean(R.bool.portrait_only)) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        category = intent.getIntExtra(Extras.EXTRA_CATEGORY, -1)
        level = intent.getIntExtra(Extras.EXTRA_LEVEL, -1)
        val quizPosition = intent.getIntExtra(Extras.EXTRA_QUIZ_POSITION, -1)
        selectedTypes = intent.getIntArrayExtra(Extras.EXTRA_QUIZ_TYPES) ?: intArrayOf()

        setSupportActionBar(findViewById(R.id.toolbar))

        supportActionBar?.apply {
            setHomeAsUpIndicator(R.drawable.ic_arrow_back_orange_24dp)
            setDisplayHomeAsUpEnabled(true)
        }

        val quizSource: QuizRepository by instance()

        quizSource.getQuiz(category, object : QuizRepository.LoadQuizCallback {
            override fun onQuizLoaded(quizzes: List<Quiz>) {
                if (level > -1) {
                    title = when (level) {
                       0 -> getString(R.string.red_review)
                       1 -> getString(R.string.orange_review)
                       2 -> getString(R.string.yellow_review)
                       else -> getString(R.string.green_review)
                    }
                    binding.pagerContent.visibility = GONE
                    if (savedInstanceState != null) {
                        //Restore the fragment's instance
                        contentLevelFragment = supportFragmentManager.getFragment(savedInstanceState, "contentLevelFragment") as ContentFragment
                    } else {
                        val ids = mutableListOf<Long>()
                        quizzes.forEach { ids.add(it.id) }
                        val bundle = Bundle()
                        bundle.putLongArray(EXTRA_QUIZ_IDS, ids.toLongArray())
                        bundle.putString(EXTRA_QUIZ_TITLE, title as String)
                        bundle.putInt(EXTRA_LEVEL, level)
                        contentLevelFragment = ContentFragment(di)
                        contentLevelFragment!!.arguments = bundle
                        quizIds = ids.toLongArray()
                    }
                    addOrReplaceFragment(R.id.fragment_container, contentLevelFragment!!)
                    // contentLevelFragment is set now, so safely pull trigger
                    trigger.trigger()

                } else {
                    contentPagerAdapter = ContentPagerAdapter(this@ContentActivity, supportFragmentManager, quizzes, lifecycle, di)
                    binding.pagerContent.adapter = contentPagerAdapter
                    val quizTitle = quizzes[quizPosition].getName().split("%")[0]
                    quizIds = longArrayOf(quizzes[quizPosition].id)
                    title = quizTitle
                    binding.pagerContent.currentItem = quizPosition
                    binding.pagerContent.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {

                        override fun onPageSelected(position: Int) {
                            val newQuizTitle = quizzes[position].getName().split("%")[0]
                            title = newQuizTitle
                            quizIds = longArrayOf(quizzes[position].id)
                        }

                    })
                }
            }

            override fun onDataNotAvailable() {

            }

        })

        binding.progressivePlay.visibility = if (level > -1) GONE else VISIBLE

        binding.progressivePlay.setOnClickListener {
            launchQuiz(QuizStrategy.PROGRESSIVE)
        }
        binding.normalPlay.setOnClickListener {
            when (level) {
                0 -> launchQuiz(QuizStrategy.LOW_STRAIGHT)
                1 -> launchQuiz(QuizStrategy.MEDIUM_STRAIGHT)
                2 -> launchQuiz(QuizStrategy.HIGH_STRAIGHT)
                3 -> launchQuiz(QuizStrategy.MASTER_STRAIGHT)
                else -> launchQuiz(QuizStrategy.STRAIGHT)
            }
        }
        binding.shufflePlay.setOnClickListener {
            when (level) {
                0 -> launchQuiz(QuizStrategy.LOW_SHUFFLE)
                1 -> launchQuiz(QuizStrategy.MEDIUM_SHUFFLE)
                2 -> launchQuiz(QuizStrategy.HIGH_SHUFFLE)
                3 -> launchQuiz(QuizStrategy.MASTER_SHUFFLE)
                else -> launchQuiz(QuizStrategy.SHUFFLE)
            }
        }

        /**
         * Collapse or quit
         * If action button is expanded -> collapse it
         * Otherwise -> finish this activity
         */
        fun collapseOrQuit() {
            if (binding.multipleActions.isExpanded)
                binding.multipleActions.collapse()
            else
                finish()
        }

        // set back button to close floating actions menu
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

    private fun launchQuiz(strategy: QuizStrategy) {
        statsRepository.addStatEntry(StatAction.LAUNCH_QUIZ_FROM_CATEGORY, category.toLong(), Calendar.getInstance().timeInMillis, StatResult.OTHER)
        val pref = PreferenceManager.getDefaultSharedPreferences(this)
        val cat1 = pref.getInt(Prefs.LATEST_CATEGORY_1.pref, -1)

        if (category != cat1) {
            pref.edit().putInt(Prefs.LATEST_CATEGORY_2.pref, cat1).apply()
            pref.edit().putInt(Prefs.LATEST_CATEGORY_1.pref, category).apply()
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

}
