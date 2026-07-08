package com.jehutyno.yomikata.screens.content

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View.GONE
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.viewpager2.widget.ViewPager2
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.databinding.ActivityContentBinding
import com.jehutyno.yomikata.ui.components.FABBar
import com.jehutyno.yomikata.ui.components.FABBarState
import com.jehutyno.yomikata.ui.study.LaunchOptionsSheet
import com.jehutyno.yomikata.ui.theme.YomikataTheme
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
import com.jehutyno.yomikata.util.quiz.Level
import com.jehutyno.yomikata.util.Prefs
import com.jehutyno.yomikata.util.quiz.QuizStrategy
import com.jehutyno.yomikata.util.quiz.QuizType
import com.jehutyno.yomikata.util.quiz.QuizTypePrefs
import com.jehutyno.yomikata.util.DiFragmentFactory
import com.jehutyno.yomikata.util.addOrReplaceFragment
import com.jehutyno.yomikata.util.getParcelableArrayListExtraHelper
import com.jehutyno.yomikata.util.getSerializableExtraHelper
import io.github.inflationx.viewpump.ViewPumpContextWrapper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import mu.KLogging
import org.kodein.di.DIAware
import org.kodein.di.android.closestDI
import org.kodein.di.direct
import org.kodein.di.instance
import org.kodein.di.newInstance
import org.kodein.di.DI
import java.util.Calendar


class ContentActivity : AppCompatActivity(), DIAware {

    companion object : KLogging()

    private var quizIds = longArrayOf()
    private lateinit var quizzes: List<Quiz>

    // Launch-bar Compose state (harmonisé avec l'écran Study)
    private var launchTypes by mutableStateOf<List<QuizType>>(emptyList())
    private var lastMode by mutableStateOf<QuizStrategy?>(null)

    private var category: Int = -1
    private var level: Level? = null

    private var contentLevelFragment: ContentFragment? = null

    // kodein
    override val di: DI by closestDI()

    private lateinit var statsRepository: StatsSource

    private var contentPagerAdapter: ContentPagerAdapter? = null

    // View Binding
    private lateinit var binding: ActivityContentBinding


    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(ViewPumpContextWrapper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Doit être posé AVANT super.onCreate : le FragmentManager y restaure ContentFragment
        // et WordDetailFragment (constructeurs à DI, pas de no-arg) → sans factory,
        // NoSuchMethodException au démarrage.
        supportFragmentManager.fragmentFactory = DiFragmentFactory(di)
        super.onCreate(savedInstanceState)

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
        launchTypes = intent.getParcelableArrayListExtraHelper(EXTRA_QUIZ_TYPES, QuizType::class.java) ?: arrayListOf()
        lastMode = loadLastMode()

        lifecycleScope.launch {
            getQuizzes()
            launchFragment(savedInstanceState, quizPosition)
        }

        // Launch bar Compose : bouton « Lancer le quiz » + bottom sheet d'options (comme Study)
        binding.launchBar.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                YomikataTheme {
                    ContentLaunchBar(
                        selectedTypes = launchTypes,
                        lastMode = lastMode,
                        // Progressif sans objet sur un sous-ensemble par niveau (revue rouge/orange/…)
                        showProgressive = level == null,
                        onQuizTypeToggle = { type ->
                            val prefs = PreferenceManager.getDefaultSharedPreferences(this@ContentActivity)
                            launchTypes = QuizTypePrefs.toggle(prefs, ArrayList(launchTypes), type)
                        },
                        onModeSelected = { strategy -> launchQuiz(strategy) },
                    )
                }
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
        if (quizzes.isEmpty()) {
            // Aucune sélection à afficher (catégorie vide ou données obsolètes) :
            // fermer proprement plutôt que crasher sur un accès indexé (quizzes[-1]).
            finish()
            return
        }
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
                val newFragment = ContentFragment(di)
                newFragment.arguments = bundle
                contentLevelFragment = newFragment
                quizIds = ids.toLongArray()
            }
            addOrReplaceFragment(R.id.fragment_container, requireNotNull(contentLevelFragment) { "contentLevelFragment not initialized" })

        } else {
            // La position peut arriver à -1 (extra absent) ou hors bornes : la ramener
            // dans l'intervalle valide pour éviter un accès indexé illégal.
            val safePosition = quizPosition.coerceIn(0, quizzes.lastIndex)
            contentPagerAdapter = ContentPagerAdapter(this@ContentActivity, quizzes, di)
            binding.pagerContent.adapter = contentPagerAdapter
            val quizTitle = quizzes[safePosition].getName().split("%")[0]
            quizIds = longArrayOf(quizzes[safePosition].id)
            title = quizTitle
            binding.pagerContent.currentItem = safePosition
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
        pref.edit().putString(Prefs.LAST_LAUNCH_MODE.pref, strategy.name).apply()
        lastMode = strategy

        val intent = Intent(this, QuizActivity::class.java).apply {
            putExtra(EXTRA_QUIZ_IDS, quizIds)
            putExtra(EXTRA_QUIZ_TITLE, title)
            putExtra(EXTRA_QUIZ_STRATEGY, strategy)
            putExtra(EXTRA_LEVEL, level)
            putExtra(EXTRA_QUIZ_TYPES, ArrayList(launchTypes))
        }
        startActivity(intent)
    }

    private fun loadLastMode(): QuizStrategy? {
        val pref = PreferenceManager.getDefaultSharedPreferences(this)
        val name = pref.getString(Prefs.LAST_LAUNCH_MODE.pref, null) ?: return null
        return runCatching { QuizStrategy.valueOf(name) }.getOrNull()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        //Save the fragment's instance
        contentLevelFragment?.let {
            supportFragmentManager.putFragment(outState, "contentLevelFragment", it)
        }
    }

}

/**
 * Floating launch button anchored at the bottom of the Content screen, opening the shared
 * [LaunchOptionsSheet] (quiz types + launch mode). Mirrors the launch UX of the Study screen.
 */
@Composable
private fun ContentLaunchBar(
    selectedTypes: List<QuizType>,
    lastMode: QuizStrategy?,
    showProgressive: Boolean,
    onQuizTypeToggle: (QuizType) -> Unit,
    onModeSelected: (QuizStrategy) -> Unit,
) {
    var showSheet by remember { mutableStateOf(false) }

    FABBar(
        state = FABBarState.Launch,
        onClick = { showSheet = true },
        modifier = Modifier
            .navigationBarsPadding()
            .padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 16.dp),
    )

    if (showSheet) {
        LaunchOptionsSheet(
            selectedTypes = selectedTypes,
            lastMode = lastMode,
            showProgressive = showProgressive,
            onQuizTypeToggle = onQuizTypeToggle,
            onModeSelected = { strategy ->
                showSheet = false
                onModeSelected(strategy)
            },
            onDismiss = { showSheet = false },
        )
    }
}
