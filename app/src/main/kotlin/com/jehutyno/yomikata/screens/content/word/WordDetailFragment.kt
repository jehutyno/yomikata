package com.jehutyno.yomikata.screens.content.word

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.audio.VoicesManager
import com.jehutyno.yomikata.model.KanjiSoloRadical
import com.jehutyno.yomikata.model.Sentence
import com.jehutyno.yomikata.model.Word
import com.jehutyno.yomikata.ui.word.WordDetailScreen
import com.jehutyno.yomikata.ui.word.WordDetailUiState
import com.jehutyno.yomikata.util.Extras
import com.jehutyno.yomikata.util.getSerializableHelper
import com.jehutyno.yomikata.util.onTTSinit
import com.jehutyno.yomikata.util.quiz.Level
import com.jehutyno.yomikata.util.quiz.QuizType
import com.jehutyno.yomikata.util.quiz.getLevelFromPoints
import com.jehutyno.yomikata.util.quiz.levelDown
import com.jehutyno.yomikata.util.quiz.levelUp
import com.jehutyno.yomikata.util.createNewSelectionDialog
import com.jehutyno.yomikata.util.reportError
import kotlinx.coroutines.launch
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.instance
import org.kodein.di.provider
import org.kodein.di.singleton
import splitties.alertdialog.appcompat.*


class WordDetailFragment(private val di: DI) : Fragment(), WordContract.View, TextToSpeech.OnInitListener {

    // ── DI ──────────────────────────────────────────────────────────────────
    private val subDI = DI.lazy {
        extend(di)
        bind<WordContract.Presenter>() with provider {
            WordPresenter(
                instance(), instance(), instance(),
                instance(arg = lifecycleScope), instance(),
                quizIds, level, searchString,
            )
        }
        bind<VoicesManager>() with singleton { VoicesManager(requireActivity()) }
    }
    @Suppress("unused")
    private val presenter: WordContract.Presenter by subDI.instance()
    private val voices: VoicesManager by subDI.instance()

    // ── Args ─────────────────────────────────────────────────────────────────
    private var wordId: Long = -1
    private var quizIds: LongArray? = null
    private var quizType: QuizType? = null
    private var wordPosition: Int = 0
    private var searchString: String = ""
    private var quizTitle: String = ""
    private var level: Level? = null

    // ── TTS ──────────────────────────────────────────────────────────────────
    private var tts: TextToSpeech? = null
    private var ttsSupported: Int = TextToSpeech.LANG_NOT_SUPPORTED

    override fun onInit(status: Int) {
        if (activity != null)
            ttsSupported = onTTSinit(activity, status, tts)
    }

    // ── Compose state ─────────────────────────────────────────────────────────
    private var composeState by mutableStateOf(WordDetailUiState())

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(activity, this)

        arguments?.let { args ->
            wordId = args.getLong(Extras.EXTRA_WORD_ID, -1L)
            quizType = args.getSerializableHelper(Extras.EXTRA_QUIZ_TYPE, QuizType::class.java)
            quizIds = args.getLongArray(Extras.EXTRA_QUIZ_IDS)
            quizTitle = args.getString(Extras.EXTRA_QUIZ_TITLE) ?: ""
            wordPosition = args.getInt(Extras.EXTRA_WORD_POSITION)
            searchString = args.getString(Extras.EXTRA_SEARCH_STRING) ?: ""
            level = args.getSerializableHelper(Extras.EXTRA_LEVEL, Level::class.java)
        }

        savedInstanceState?.let { state ->
            wordPosition = state.getInt("position", wordPosition)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                WordDetailScreen(
                    state = composeState,
                    title = quizTitle,
                    onBack = { parentFragmentManager.popBackStack() },
                    onPrev = { navigateTo(composeState.currentIndex - 1) },
                    onNext = { navigateTo(composeState.currentIndex + 1) },
                    onWordTtsClick = { speakCurrentWord() },
                    onSentenceTtsClick = { speakCurrentSentence() },
                    onFavoriteClick = { showSelectionPopup(requireView()) },
                    onCopyClick = { copyCurrentWord() },
                    onReportClick = { reportCurrentWord() },
                    onLevelUp = { doLevelUp() },
                    onLevelDown = { doLevelDown() },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        presenter.start()
        presenter.words?.let { liveData ->
            liveData.observe(viewLifecycleOwner) { words ->
                lifecycleScope.launch {
                    displayWords(presenter.getWordKanjiSoloRadicalSentenceList(words))
                }
            }
        } ?: lifecycleScope.launch {
            val oneWord = listOf(presenter.getWordById(wordId))
            displayWords(presenter.getWordKanjiSoloRadicalSentenceList(oneWord))
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("position", composeState.currentIndex)
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        voices.releasePlayer()
        super.onDestroy()
    }

    // ── WordContract.View ─────────────────────────────────────────────────────

    @Synchronized
    override fun displayWords(words: List<Triple<Word, List<KanjiSoloRadical?>, Sentence>>) {
        val idx = wordPosition.coerceIn(0, (words.size - 1).coerceAtLeast(0))
        composeState = WordDetailUiState(words = words, currentIndex = idx)
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    private fun navigateTo(index: Int) {
        val clamped = index.coerceIn(0, composeState.words.size - 1)
        wordPosition = clamped
        composeState = composeState.copy(currentIndex = clamped)
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    private fun speakCurrentWord() {
        val word = currentWord() ?: return
        voices.speakWord(word, ttsSupported, tts)
    }

    private fun speakCurrentSentence() {
        val sentence = currentSentence() ?: return
        voices.speakSentence(sentence, ttsSupported, tts)
    }

    private fun copyCurrentWord() {
        val word = currentWord() ?: return
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(getString(R.string.copy_word), word.japanese)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(requireContext(), getString(R.string.word_copied), Toast.LENGTH_SHORT).show()
    }

    private fun reportCurrentWord() {
        val word = currentWord() ?: return
        val sentence = currentSentence() ?: return
        reportError(requireActivity(), word, sentence)
    }

    private fun showSelectionPopup(anchor: View) {
        lifecycleScope.launch {
            val word = currentWord() ?: return@launch
            val selections = presenter.getSelections()
            val popup = PopupMenu(requireActivity(), anchor)
            popup.menuInflater.inflate(R.menu.popup_selections, popup.menu)
            for ((i, selection) in selections.withIndex()) {
                popup.menu.add(1, i, i, selection.getName())
                    .isChecked = presenter.isWordInQuiz(word.id, selection.id)
                popup.menu.setGroupCheckable(1, true, false)
            }
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.add_selection -> addSelection(word.id)
                    else -> lifecycleScope.launch {
                        if (!item.isChecked)
                            presenter.addWordToSelection(word.id, selections[item.itemId].id)
                        else
                            presenter.deleteWordFromSelection(word.id, selections[item.itemId].id)
                        item.isChecked = !item.isChecked
                    }
                }
                true
            }
            popup.show()
        }
    }

    private fun addSelection(wordId: Long) {
        requireActivity().createNewSelectionDialog("", { selectionName ->
            lifecycleScope.launch {
                val selectionId = presenter.createSelection(selectionName)
                presenter.addWordToSelection(wordId, selectionId)
            }
        }, null)
    }

    private fun doLevelUp() {
        val word = currentWord() ?: return
        lifecycleScope.launch {
            presenter.levelUp(word.id, word.points)
            val newPoints = levelUp(word.points)
            word.points = newPoints
            word.level = getLevelFromPoints(newPoints)
            composeState = composeState.copy(updateCounter = composeState.updateCounter + 1)
        }
    }

    private fun doLevelDown() {
        val word = currentWord() ?: return
        lifecycleScope.launch {
            presenter.levelDown(word.id, word.points)
            val newPoints = levelDown(word.points)
            word.points = newPoints
            word.level = getLevelFromPoints(newPoints)
            composeState = composeState.copy(updateCounter = composeState.updateCounter + 1)
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun currentWord(): Word? =
        composeState.words.getOrNull(composeState.currentIndex)?.first

    private fun currentSentence(): Sentence? =
        composeState.words.getOrNull(composeState.currentIndex)?.third
}
