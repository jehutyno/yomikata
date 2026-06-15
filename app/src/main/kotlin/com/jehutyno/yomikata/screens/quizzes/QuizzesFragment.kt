package com.jehutyno.yomikata.screens.quizzes

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
import com.jehutyno.yomikata.ui.study.StudyScreen
import com.jehutyno.yomikata.ui.study.StudyUiState
import com.jehutyno.yomikata.ui.study.categoryName
import com.jehutyno.yomikata.ui.theme.YomikataTheme
import com.jehutyno.yomikata.util.Extras
import com.jehutyno.yomikata.util.Prefs
import com.jehutyno.yomikata.util.quiz.Categories
import com.jehutyno.yomikata.util.quiz.QuizStrategy
import com.jehutyno.yomikata.util.quiz.QuizType
import com.jehutyno.yomikata.util.quiz.toQuizType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance
import java.util.Calendar
import java.util.StringTokenizer


class QuizzesFragment(private val diArg: DI) : Fragment(), DIAware {

    override val di: DI = diArg

    private val prefs: SharedPreferences by instance()
    private val quizRepository: QuizRepository by instance()
    private val statsRepository: StatsRepository by instance()

    private val _categoryFlow = MutableStateFlow(Categories.CATEGORY_HIRAGANA)

    // Compose state
    private var uiState by mutableStateOf(StudyUiState())

    // Individual count components — summed into uiState.goodCount / wrongCount
    private var cntHigh = 0
    private var cntMaster = 0
    private var cntLow = 0
    private var cntMedium = 0

    // MARK: — Lifecycle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val saved = prefs.getInt(Prefs.LAST_SELECTED_LEVEL.pref, Categories.CATEGORY_HIRAGANA)
        _categoryFlow.value = saved
        uiState = uiState.copy(selectedCategory = saved)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        // Dynamic quiz IDs flow drives WordCountPresenter
        val quizIdsFlow = _categoryFlow.flatMapLatest { cat ->
            quizRepository.getQuiz(cat).map { list -> list.map { it.id }.toLongArray() }
        }
        val wordCount = WordCountPresenter(quizRepository, quizIdsFlow)

        // Quiz list LiveData from the dynamic category
        val quizListLiveData = _categoryFlow
            .flatMapLatest { cat -> quizRepository.getQuiz(cat) }
            .asLiveData()
            .distinctUntilChanged()

        quizListLiveData.observe(viewLifecycleOwner) { quizzes ->
            uiState = uiState.copy(
                quizzes = quizzes ?: emptyList(),
                selectedCategory = _categoryFlow.value,
            )
        }
        wordCount.quizCount.observe(viewLifecycleOwner) { c ->
            uiState = uiState.copy(quizCount = c ?: 0)
        }
        wordCount.highCount.observe(viewLifecycleOwner) { c ->
            cntHigh = c ?: 0; syncGoodWrong()
        }
        wordCount.masterCount.observe(viewLifecycleOwner) { c ->
            cntMaster = c ?: 0; syncGoodWrong()
        }
        wordCount.lowCount.observe(viewLifecycleOwner) { c ->
            cntLow = c ?: 0; syncGoodWrong()
        }
        wordCount.mediumCount.observe(viewLifecycleOwner) { c ->
            cntMedium = c ?: 0; syncGoodWrong()
        }

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                YomikataTheme {
                    StudyScreen(
                        state = uiState,
                        onCategorySelected = { cat -> selectCategory(cat) },
                        onQuizChecked = { quizId, checked ->
                            lifecycleScope.launch { quizRepository.updateQuizSelected(quizId, checked) }
                        },
                        onQuizClick = { quiz -> openContent(quiz) },
                        onLaunchQuiz = { launchQuiz() },
                    )
                }
            }
        }
    }

    // MARK: — Public API (called from Activity Drawer navigation)

    fun setCategory(category: Int) = selectCategory(category)

    // MARK: — Private helpers

    private fun syncGoodWrong() {
        uiState = uiState.copy(
            goodCount = cntHigh + cntMaster,
            wrongCount = cntLow + cntMedium,
        )
    }

    private fun selectCategory(category: Int) {
        _categoryFlow.value = category
        prefs.edit().putInt(Prefs.LAST_SELECTED_LEVEL.pref, category).apply()
        uiState = uiState.copy(selectedCategory = category)
    }

    private fun openContent(quiz: Quiz) {
        val intent = Intent(context, ContentActivity::class.java).apply {
            putExtra(Extras.EXTRA_CATEGORY, uiState.selectedCategory)
            putExtra(Extras.EXTRA_QUIZ_POSITION, quiz.id.toInt())
            putExtra(Extras.EXTRA_QUIZ_TYPES, getSelectedTypes())
            putExtra(Extras.EXTRA_LEVEL, null as java.io.Serializable?)
        }
        startActivity(intent)
    }

    private fun launchQuiz() {
        val selectedIds = uiState.quizzes.filter { it.isSelected }.map { it.id }
        val ids = if (selectedIds.isEmpty()) uiState.quizzes.map { it.id } else selectedIds

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

            val category = _categoryFlow.value
            statsRepository.addStatEntry(
                StatAction.LAUNCH_QUIZ_FROM_CATEGORY,
                category.toLong(),
                Calendar.getInstance().timeInMillis,
                StatResult.OTHER,
            )
            val cat1 = prefs.getInt(Prefs.LATEST_CATEGORY_1.pref, -1)
            if (category != cat1) {
                prefs.edit()
                    .putInt(Prefs.LATEST_CATEGORY_2.pref, cat1)
                    .putInt(Prefs.LATEST_CATEGORY_1.pref, category)
                    .apply()
            }

            startActivity(Intent(activity, QuizActivity::class.java).apply {
                putExtra(Extras.EXTRA_QUIZ_IDS, ids.toLongArray())
                putExtra(Extras.EXTRA_QUIZ_TITLE, categoryName(category))
                putExtra(Extras.EXTRA_QUIZ_STRATEGY, QuizStrategy.STRAIGHT)
                putExtra(Extras.EXTRA_LEVEL, null as java.io.Serializable?)
                putExtra(Extras.EXTRA_QUIZ_TYPES, getSelectedTypes())
            })
        }
    }

    private fun getSelectedTypes(): ArrayList<QuizType> {
        val savedString = prefs.getString(Prefs.SELECTED_QUIZ_TYPES.pref, "") ?: ""
        if (savedString.isEmpty()) return arrayListOf(QuizType.TYPE_AUTO)
        val st = StringTokenizer(savedString, ",")
        val result = ArrayList<QuizType>()
        repeat(st.countTokens()) { result.add(Integer.parseInt(st.nextToken()).toQuizType()) }
        return result.ifEmpty { arrayListOf(QuizType.TYPE_AUTO) }
    }
}
