package com.jehutyno.yomikata.screens.content

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View.GONE
import android.view.View.VISIBLE
import android.window.OnBackInvokedDispatcher
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
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
import com.jehutyno.yomikata.util.Extras
import com.jehutyno.yomikata.util.Extras.EXTRA_LEVEL
import com.jehutyno.yomikata.util.Extras.EXTRA_QUIZ_IDS
import com.jehutyno.yomikata.util.Extras.EXTRA_QUIZ_POSITION
import com.jehutyno.yomikata.util.Extras.EXTRA_QUIZ_STRATEGY
import com.jehutyno.yomikata.util.Extras.EXTRA_QUIZ_TITLE
import com.jehutyno.yomikata.util.Extras.EXTRA_QUIZ_TYPES
import com.jehutyno.yomikata.util.Level
import com.jehutyno.yomikata.util.Prefs
import com.jehutyno.yomikata.util.QuizStrategy
import com.jehutyno.yomikata.util.QuizType
import com.jehutyno.yomikata.util.addOrReplaceFragment
import com.jehutyno.yomikata.util.getParcelableArrayListExtraHelper
import com.jehutyno.yomikata.util.getSerializableExtraHelper
import io.github.inflationx.viewpump.ViewPumpContextWrapper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import mu.KLogging
import org.kodein.di.DIAware
import org.kodein.di.android.di
import org.kodein.di.direct
import org.kodein.di.instance
import org.kodein.di.newInstance
import java.util.Calendar


class ContentActivity : AppCompatActivity(), DIAware {

    companion object : KLogging()

    private var quizIds = longArrayOf()
    private lateinit var selectedTypes: ArrayList<QuizType>
    private lateinit var quizzes: List<Quiz>

    private var category: Int = -1
    private var level: Level? = null

    private var contentLevelFragment: ContentFragment? = null

    // kodein
    override val di by di()

    private lateinit var statsRepository: StatsSource

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
        level = intent.getSerializableExtraHelper(EXTRA_LEVEL, Level::class.java)

        val quizPosition = intent.getIntExtra(EXTRA_QUIZ_POSITION, -1)
        selectedTypes = intent.getParcelableArrayListExtraHelper(EXTRA_QUIZ_TYPES, QuizType::class.java) ?: arrayListOf()

        setSupportActionBar(findViewById(R.id.toolbar))

        supportActionBar?.apply {
            setHomeAsUpIndicator(R.drawable.ic_arrow_back_orange_24dp)
            setDisplayHomeAsUpEnabled(true)
        }

        lifecycleScope.launch {
            getQuizzes()
            launchFragment(savedInstanceState, quizPosition)
        }


        binding.progressivePlay.visibility = if (level != null) GONE else VISIBLE

        binding.progressivePlay.setOnClickListener {
            launchQuiz(QuizStrategy.PROGRESSIVE)
        }
        binding.normalPlay.setOnClickListener {
            launchQuiz(QuizStrategy.STRAIGHT)
        }
        binding.shufflePlay.setOnClickListener {
            launchQuiz(QuizStrategy.SHUFFLE)
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

    private suspend fun getQuizzes() {
        val quizSource = di.direct.instance<QuizRepository>()
        quizzes = quizSource.getQuiz(category).first()
    }

    /**
     * Launch fragment
     *
     * Launches a fragment depending on the level and category that is set.
     *
     * There are two types of fragments that can be loaded:
     * - level fragment (contentLevelFragment) which corresponds to clicking on one of the
     * per-level sorted selections (through the little arrow buttons in the statsDisplay)
     * - normal fragment which corresponds to simply clicking on a selection. This type
     * of fragment is loaded through a viewPager to allow scrolling through the different selections.
     *
     * @param savedInstanceState Used to retrieve a previously stored (level) fragment
     * @param quizPosition The position of the quiz selections in the list in case a normal
     * selection is loaded (non-level selection)
     */
    private fun launchFragment(savedInstanceState: Bundle?, quizPosition: Int) {
        if (level != null) {
            title = when (level) {
                Level.LOW -> getString(R.string.red_review)
                Level.MEDIUM -> getString(R.string.orange_review)
                Level.HIGH -> getString(R.string.yellow_review)
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
                bundle.putSerializable(EXTRA_LEVEL, level)
                contentLevelFragment = ContentFragment(di)
                contentLevelFragment!!.arguments = bundle
                quizIds = ids.toLongArray()
            }
            addOrReplaceFragment(R.id.fragment_container, contentLevelFragment!!)

        } else {
            contentPagerAdapter = ContentPagerAdapter(this@ContentActivity, quizzes, di)
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

    private fun launchQuiz(strategy: QuizStrategy) {
        lifecycleScope.launch {
            statsRepository.addStatEntry(
                StatAction.LAUNCH_QUIZ_FROM_CATEGORY,
                category.toLong(),
                Calendar.getInstance().timeInMillis,
                StatResult.OTHER)
        }
        val pref = PreferenceManager.getDefaultSharedPreferences(this)
        val cat1 = pref.getInt(Prefs.LATEST_CATEGORY_1.pref, -1)

        if (category != cat1) {
            pref.edit().putInt(Prefs.LATEST_CATEGORY_2.pref, cat1).apply()
            pref.edit().putInt(Prefs.LATEST_CATEGORY_1.pref, category).apply()
        }

        val intent = Intent(this, QuizActivity::class.java).apply {
            putExtra(EXTRA_QUIZ_IDS, quizIds)
            putExtra(EXTRA_QUIZ_TITLE, title)
            putExtra(EXTRA_QUIZ_STRATEGY, strategy)
            putExtra(EXTRA_LEVEL, level)
            putExtra(EXTRA_QUIZ_TYPES, selectedTypes)
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
