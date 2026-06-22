package com.jehutyno.yomikata.screens.selections

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.lifecycle.asLiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.lifecycleScope
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.model.Quiz
import com.jehutyno.yomikata.model.StatAction
import com.jehutyno.yomikata.model.StatResult
import com.jehutyno.yomikata.presenters.impl.WordCountPresenter
import com.jehutyno.yomikata.repository.QuizRepository
import com.jehutyno.yomikata.repository.StatsRepository
import com.jehutyno.yomikata.screens.content.ContentActivity
import com.jehutyno.yomikata.screens.quiz.QuizActivity
import com.jehutyno.yomikata.ui.selections.SelectionsScreen
import com.jehutyno.yomikata.ui.selections.SelectionsUiState
import com.jehutyno.yomikata.ui.theme.YomikataTheme
import com.jehutyno.yomikata.util.Extras
import com.jehutyno.yomikata.util.Prefs
import com.jehutyno.yomikata.util.analytics.Analytics
import com.jehutyno.yomikata.util.createNewSelectionDialog
import com.jehutyno.yomikata.util.quiz.Categories
import com.jehutyno.yomikata.util.quiz.QuizStrategy
import com.jehutyno.yomikata.util.quiz.QuizType
import com.jehutyno.yomikata.util.quiz.QuizTypePrefs
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance
import java.util.Calendar


class SelectionsFragment(private val diArg: DI) : Fragment(), DIAware {

    override val di: DI = diArg

    private val prefs: SharedPreferences by instance()
    private val quizRepository: QuizRepository by instance()
    private val statsRepository: StatsRepository by instance()

    // Compose state
    private var uiState by mutableStateOf(SelectionsUiState())

    // Individual mastery components — summed into uiState.goodCount
    private var cntHigh = 0
    private var cntMaster = 0

    private val selectionsFlow
        get() = quizRepository.getQuiz(Categories.CATEGORY_SELECTIONS)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        uiState = uiState.copy(
            selectedTypes = QuizTypePrefs.loadSelectedTypes(prefs),
            lastMode = loadLastMode(),
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        // Mastery bar over the checked selections (or all if none checked)
        val idsFlow = selectionsFlow.map { list ->
            val checked = list.filter { it.isSelected }.map { it.id }
            checked.ifEmpty { list.map { it.id } }.toLongArray()
        }
        val wordCount = WordCountPresenter(quizRepository, idsFlow)

        val selectionsLiveData = selectionsFlow
            .asLiveData()
            .distinctUntilChanged()

        selectionsLiveData.observe(viewLifecycleOwner) { selections ->
            val list = selections ?: emptyList()
            uiState = uiState.copy(selections = list)
            refreshWordCounts(list)
        }
        wordCount.quizCount.observe(viewLifecycleOwner) { c ->
            uiState = uiState.copy(quizCount = c ?: 0)
        }
        wordCount.highCount.observe(viewLifecycleOwner) { c ->
            cntHigh = c ?: 0; syncGood()
        }
        wordCount.masterCount.observe(viewLifecycleOwner) { c ->
            cntMaster = c ?: 0; syncGood()
        }

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                YomikataTheme {
                    SelectionsScreen(
                        state = uiState,
                        onSelectionChecked = { quizId, checked ->
                            lifecycleScope.launch { quizRepository.updateQuizSelected(quizId, checked) }
                        },
                        onSelectionClick = { quiz -> openContent(quiz) },
                        onSelectionEdit = { quiz -> editSelection(quiz) },
                        onCreate = { createSelection() },
                        onLaunchQuiz = { strategy -> launchQuiz(strategy) },
                        onQuizTypeToggle = { type -> toggleQuizType(type) },
                    )
                }
            }
        }
    }

    // MARK: — Private helpers

    private fun syncGood() {
        uiState = uiState.copy(goodCount = cntHigh + cntMaster)
    }

    private fun refreshWordCounts(list: List<Quiz>) {
        lifecycleScope.launch {
            val counts = mutableMapOf<Long, Int>()
            list.forEach { quiz ->
                counts[quiz.id] = quizRepository.countWordsForQuizzes(longArrayOf(quiz.id)).first()
            }
            uiState = uiState.copy(wordCounts = counts)
        }
    }

    private fun createSelection() {
        requireActivity().createNewSelectionDialog("", { name ->
            lifecycleScope.launch { quizRepository.saveQuiz(name, Categories.CATEGORY_SELECTIONS) }
            Analytics.logSelectionCreated()
        }, null)
    }

    private fun editSelection(quiz: Quiz) {
        val currentName = quiz.getName().split("%")[0]
        requireActivity().createNewSelectionDialog(
            currentName,
            { name -> lifecycleScope.launch { quizRepository.updateQuizName(quiz.id, name) } },
            {
                lifecycleScope.launch { quizRepository.deleteQuiz(quiz.id) }
                Analytics.logSelectionDeleted()
            },
        )
    }

    private fun openContent(quiz: Quiz) {
        val position = uiState.selections.indexOfFirst { it.id == quiz.id }.takeIf { it >= 0 } ?: return
        val intent = Intent(context, ContentActivity::class.java).apply {
            putExtra(Extras.EXTRA_CATEGORY, Categories.CATEGORY_SELECTIONS)
            putExtra(Extras.EXTRA_QUIZ_POSITION, position)
            putExtra(Extras.EXTRA_QUIZ_TYPES, ArrayList(uiState.selectedTypes))
            putExtra(Extras.EXTRA_LEVEL, null as java.io.Serializable?)
        }
        startActivity(intent)
    }

    private fun toggleQuizType(type: QuizType) {
        val next = QuizTypePrefs.toggle(prefs, ArrayList(uiState.selectedTypes), type)
        uiState = uiState.copy(selectedTypes = next)
    }

    private fun loadLastMode(): QuizStrategy? {
        val name = prefs.getString(Prefs.LAST_LAUNCH_MODE.pref, null) ?: return null
        return runCatching { QuizStrategy.valueOf(name) }.getOrNull()
    }

    private fun launchQuiz(strategy: QuizStrategy) {
        val selectedIds = uiState.selections.filter { it.isSelected }.map { it.id }
        val ids = if (selectedIds.isEmpty()) uiState.selections.map { it.id } else selectedIds

        if (ids.isEmpty()) {
            Toast.makeText(context, R.string.error_no_quiz_no_word, Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            val count = quizRepository.countWordsForQuizzes(ids.toLongArray()).first()
            if (count <= 0) {
                Toast.makeText(context, R.string.error_no_quiz_no_word, Toast.LENGTH_SHORT).show()
                return@launch
            }

            statsRepository.addStatEntry(
                StatAction.LAUNCH_QUIZ_FROM_CATEGORY,
                Categories.CATEGORY_SELECTIONS.toLong(),
                Calendar.getInstance().timeInMillis,
                StatResult.OTHER,
            )
            Analytics.logQuizLaunched(
                source = Analytics.SOURCE_SELECTION,
                category = Categories.CATEGORY_SELECTIONS,
                level = null,
                strategy = strategy,
                types = uiState.selectedTypes,
            )
            prefs.edit().putString(Prefs.LAST_LAUNCH_MODE.pref, strategy.name).apply()
            uiState = uiState.copy(lastMode = strategy)

            startActivity(Intent(activity, QuizActivity::class.java).apply {
                putExtra(Extras.EXTRA_QUIZ_IDS, ids.toLongArray())
                putExtra(Extras.EXTRA_QUIZ_TITLE, getString(R.string.drawer_your_selections))
                putExtra(Extras.EXTRA_QUIZ_STRATEGY, strategy)
                putExtra(Extras.EXTRA_LEVEL, null as java.io.Serializable?)
                putExtra(Extras.EXTRA_QUIZ_TYPES, ArrayList(uiState.selectedTypes))
            })
        }
    }
}
