package com.jehutyno.yomikata.screens.answers

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.jehutyno.yomikata.audio.VoicesManager
import com.jehutyno.yomikata.model.Answer
import com.jehutyno.yomikata.ui.answers.AnswerReviewScreen
import com.jehutyno.yomikata.ui.answers.AnswerReviewUiState
import com.jehutyno.yomikata.ui.answers.SelectionEntry
import com.jehutyno.yomikata.ui.answers.SelectionSheetState
import com.jehutyno.yomikata.ui.theme.YomikataTheme
import com.jehutyno.yomikata.util.*
import com.jehutyno.yomikata.util.backup.LocalPersistence
import kotlinx.coroutines.launch
import org.kodein.di.*
import java.util.*


/**
 * Created by valentin on 25/10/2016.
 */
class AnswersFragment(private val di: DI) : Fragment(), AnswersContract.View, TextToSpeech.OnInitListener {

    // kodein
    private val subDI by DI.lazy {
        extend(di)
        bind<AnswersContract.Presenter>() with provider {
            AnswersPresenter(instance(arg = lifecycleScope), instance(), instance())
        }
        bind<VoicesManager>() with singleton { VoicesManager(requireActivity()) }
    }
    @Suppress("unused")
    private val voicesManager: VoicesManager by subDI.instance()
    private val presenter: AnswersContract.Presenter by subDI.instance()

    private var tts: TextToSpeech? = null
    private var ttsSupported: Int = TextToSpeech.LANG_NOT_SUPPORTED

    // Compose state
    private var uiState by mutableStateOf(AnswerReviewUiState())

    override fun onInit(status: Int) {
        ttsSupported = onTTSinit(activity, status, tts)
    }

    private lateinit var answers: List<Answer>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(activity, this)

        val answersListRaw = LocalPersistence.readObjectFromFile(context, "answers")
        val answersList = answersListRaw as ArrayList<*>
        answers = answersListRaw.filterIsInstance<Answer>()
        if (answers.size != answersList.size) {
            Log.e("Failed cast", "Some items in the read list of answers were not of the type Answer")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                YomikataTheme {
                    AnswerReviewScreen(
                        state = uiState,
                        title = getString(com.jehutyno.yomikata.R.string.answer_title),
                        onBack = { requireActivity().finish() },
                        onSelectionClick = { index -> openSelectionSheet(index) },
                        onReportClick = { index ->
                            val item = uiState.items.getOrNull(index) ?: return@AnswerReviewScreen
                            reportError(requireActivity(), item.second, item.third)
                        },
                        onWordTtsClick = { index ->
                            val item = uiState.items.getOrNull(index) ?: return@AnswerReviewScreen
                            voicesManager.speakWord(item.second, ttsSupported, tts)
                        },
                        onSentenceTtsClick = { index ->
                            val item = uiState.items.getOrNull(index) ?: return@AnswerReviewScreen
                            voicesManager.speakSentence(item.third, ttsSupported, tts)
                        },
                        onSelectionToggle = { quizId, checked -> toggleSelection(quizId, checked) },
                        onCreateSelection = { createSelectionForCurrentWord() },
                        onDismissSelectionSheet = { uiState = uiState.copy(selectionSheet = null) },
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycleScope.launch {
            uiState = uiState.copy(items = presenter.getAnswersWordsSentences(answers))
        }
    }

    override fun onResume() {
        super.onResume()
        presenter.start()
    }

    override fun displayAnswers() {

    }

    /** Loads the selections + their checked state for the word at [index] and opens the sheet. */
    private fun openSelectionSheet(index: Int) {
        val word = uiState.items.getOrNull(index)?.second ?: return
        lifecycleScope.launch {
            uiState = uiState.copy(
                selectionSheet = SelectionSheetState(index, buildSelectionEntries(word.id)),
            )
        }
    }

    private suspend fun buildSelectionEntries(wordId: Long): List<SelectionEntry> {
        return presenter.getSelections().map { selection ->
            SelectionEntry(selection, presenter.isWordInQuiz(wordId, selection.id))
        }
    }

    private fun toggleSelection(quizId: Long, checked: Boolean) {
        val sheet = uiState.selectionSheet ?: return
        val word = uiState.items.getOrNull(sheet.wordIndex)?.second ?: return
        lifecycleScope.launch {
            if (checked)
                presenter.addWordToSelection(word.id, quizId)
            else
                presenter.deleteWordFromSelection(word.id, quizId)
            // Refresh sheet so the checkmarks update
            uiState = uiState.copy(
                selectionSheet = sheet.copy(entries = buildSelectionEntries(word.id)),
            )
        }
    }

    private fun createSelectionForCurrentWord() {
        val sheet = uiState.selectionSheet ?: return
        val word = uiState.items.getOrNull(sheet.wordIndex)?.second ?: return
        requireActivity().createNewSelectionDialog("", { selectionName ->
            lifecycleScope.launch {
                val selectionId = presenter.createSelection(selectionName)
                presenter.addWordToSelection(word.id, selectionId)
                uiState = uiState.copy(
                    selectionSheet = sheet.copy(entries = buildSelectionEntries(word.id)),
                )
            }
        }, null)
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        voicesManager.releasePlayer()
        super.onDestroy()
    }

}
