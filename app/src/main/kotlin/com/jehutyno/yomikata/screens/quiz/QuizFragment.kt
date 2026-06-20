package com.jehutyno.yomikata.screens.quiz

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Parcelable
import android.speech.tts.TextToSpeech
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.audio.VoicesManager
import com.jehutyno.yomikata.model.Answer
import com.jehutyno.yomikata.model.Sentence
import com.jehutyno.yomikata.model.Word
import com.jehutyno.yomikata.screens.answers.AnswersActivity
import com.jehutyno.yomikata.screens.content.word.WordDetailDialogFragment
import com.jehutyno.yomikata.ui.quiz.AnswerButtonState
import com.jehutyno.yomikata.ui.quiz.AnswerMode
import com.jehutyno.yomikata.ui.quiz.QcmOption
import com.jehutyno.yomikata.ui.quiz.QuizScreen
import com.jehutyno.yomikata.ui.quiz.QuizUiState
import com.jehutyno.yomikata.ui.quiz.currentWord
import com.jehutyno.yomikata.ui.quiz.currentQuizType
import com.jehutyno.yomikata.ui.quiz.SegmentState
import com.jehutyno.yomikata.ui.theme.AccentOrange
import com.jehutyno.yomikata.ui.theme.Correct
import com.jehutyno.yomikata.ui.theme.YomikataTheme
import com.jehutyno.yomikata.util.Extras
import com.jehutyno.yomikata.util.Prefs
import com.jehutyno.yomikata.util.SPEECH_NOT_INITALIZED
import com.jehutyno.yomikata.util.SpeechAvailability
import com.jehutyno.yomikata.util.backup.LocalPersistence
import com.jehutyno.yomikata.util.checkSpeechAvailability
import com.jehutyno.yomikata.util.createNewSelectionDialog
import com.jehutyno.yomikata.util.hideSoftKeyboard
import com.jehutyno.yomikata.util.onTTSinit
import com.jehutyno.yomikata.util.quiz.QuizType
import com.jehutyno.yomikata.util.quiz.getCategoryLevel
import com.jehutyno.yomikata.util.reportError
import com.jehutyno.yomikata.util.speechNotSupportedAlert
import kotlinx.coroutines.launch
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.instance
import org.kodein.di.lazy
import org.kodein.di.singleton
import splitties.alertdialog.appcompat.*


class QuizFragment(private val di: DI) : Fragment(), QuizContract.View, TextToSpeech.OnInitListener {

    // Kodein
    private val subDI = DI.lazy {
        extend(di)
        bind<VoicesManager>() with singleton { VoicesManager(requireActivity()) }
    }
    @Suppress("unused")
    private val voicesManager: VoicesManager by subDI.instance()
    private val presenter: QuizContract.Presenter by subDI.instance(arg = this@QuizFragment)
    private val prefs: SharedPreferences by subDI.instance()

    // TTS
    private var tts: TextToSpeech? = null
    private var ttsSupported = SPEECH_NOT_INITALIZED

    // Compose state
    private var uiState by mutableStateOf(QuizUiState())

    private lateinit var dialogFlowController: DialogFlowController

    // Track if answer input is busy (prevent double-submit)
    private var holdOn = false

    override fun onInit(status: Int) {
        ttsSupported = onTTSinit(activity, status, tts)
        presenter.setTTSSupported(ttsSupported)
        val currentWord = uiState.currentWord
        if (currentWord != null) {
            val noPlayStart = prefs.getBoolean(Prefs.PLAY_START.pref, false)
            if (uiState.currentQuizType == QuizType.TYPE_AUDIO || noPlayStart) {
                voicesManager.speakWord(currentWord, ttsSupported, tts)
            }
        }
    }

    override fun speakWord(word: Word) {
        if (ttsSupported != SPEECH_NOT_INITALIZED)
            voicesManager.speakWord(word, ttsSupported, tts)
    }

    override fun launchSpeakSentence(sentence: Sentence) {
        voicesManager.speakSentence(sentence, ttsSupported, tts)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        presenter.start()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("edit", uiState.editText)
        presenter.onSaveInstanceState(outState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val quizTitle = requireActivity().intent.getStringExtra(Extras.EXTRA_QUIZ_TITLE) ?: ""
        uiState = uiState.copy(title = quizTitle)

        val composeView = ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                YomikataTheme {
                    QuizScreen(
                        uiState = uiState,
                        onClose = { showQuitDialog() },
                        onTtsSettings = { showTtsSettingsToast() },
                        onDisplayAnswers = { presenter.onDisplayAnswersClick() },
                        onOptionClick = { index ->
                            if (!holdOn) {
                                holdOn = true
                                lifecycleScope.launch {
                                    presenter.onOptionClick(index + 1)
                                }
                            }
                        },
                        onNextWord = {
                            lifecycleScope.launch { presenter.onNextWord() }
                        },
                        onFuriToggle = { isSelected ->
                            prefs.edit().putBoolean(Prefs.FURI_DISPLAYED.pref, isSelected).apply()
                            uiState = uiState.copy(showFurigana = isSelected)
                            lifecycleScope.launch { presenter.setIsFuriDisplayed(isSelected) }
                        },
                        onTradToggle = {
                            val newVal = !uiState.showTranslation
                            prefs.edit().putBoolean(Prefs.TRAD_DISPLAYED.pref, newVal).apply()
                            uiState = uiState.copy(showTranslation = newVal)
                        },

                        onItemClick = {
                            val word = uiState.currentWord ?: return@QuizScreen
                            val dialog = WordDetailDialogFragment(di)
                            val bundle = Bundle().apply {
                                putLong(Extras.EXTRA_WORD_ID, word.id)
                                putSerializable(
                                    Extras.EXTRA_QUIZ_TYPE,
                                    if (presenter.previousAnswerWrong()) null else uiState.currentQuizType,
                                )
                                putString(Extras.EXTRA_SEARCH_STRING, "")
                            }
                            dialog.arguments = bundle
                            dialog.isCancelable = true
                            dialog.show(childFragmentManager, "")
                        },
                        onSelectionClick = { showSelectionMenu() },
                        onReportClick = {
                            presenter.onReportClick(uiState.currentIndex)
                        },
                        onSentenceTts = { presenter.onSpeakSentence() },
                        onSoundClick = { presenter.onSpeakWordTTS() },
                        onEditTextChange = { newText ->
                            uiState = uiState.copy(editText = newText)
                        },
                        onEditBeforeTextChange = {
                            uiState = uiState.copy(editTextColorInt = Color.White.toArgb())
                        },
                        onEditSubmit = { text ->
                            val quizType = uiState.currentQuizType
                            if (quizType == QuizType.TYPE_PRONUNCIATION && !holdOn) {
                                holdOn = true
                                val cleaned = text
                                    .replace("n", if ((uiState.currentWord?.isKana ?: 0) >= 1) "n" else "ん")
                                    .trim()
                                    .replace(" ", "")
                                    .replace("　", "")
                                    .replace("\n", "")
                                lifecycleScope.launch {
                                    presenter.onAnswerGiven(cleaned)
                                }
                            }
                        },
                        onEditAction = {
                            if (!holdOn) {
                                holdOn = true
                                presenter.onEditActionClick()
                                holdOn = false
                            }
                        },
                    )
                }
            }
        }

        dialogFlowController = DialogFlowController(this, prefs, presenter)
        return composeView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tts?.shutdown()
        tts = TextToSpeech(activity, this)

        uiState = uiState.copy(
            showFurigana = prefs.getBoolean(Prefs.FURI_DISPLAYED.pref, true),
            showTranslation = prefs.getBoolean(Prefs.TRAD_DISPLAYED.pref, true),
        )

        if (savedInstanceState != null) {
            uiState = uiState.copy(editText = savedInstanceState.getString("edit") ?: "")
            presenter.onRestoreInstanceState(savedInstanceState)
        } else {
            lifecycleScope.launch { presenter.initQuiz() }
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        voicesManager.releasePlayer()
        super.onDestroy()
    }

    override fun onPause() {
        super.onPause()
        requireActivity().hideSoftKeyboard()
    }

    private fun showQuitDialog() {
        requireContext().alertDialog(getString(R.string.quit_quiz)) {
            okButton { requireActivity().finish() }
            cancelButton()
        }.show()
    }

    private fun showTtsSettingsToast() {
        val word = uiState.currentWord
        if (word != null) {
            when (checkSpeechAvailability(requireActivity(), ttsSupported, getCategoryLevel(word.baseCategory))) {
                SpeechAvailability.NOT_AVAILABLE ->
                    speechNotSupportedAlert(requireActivity(), getCategoryLevel(word.baseCategory)) {}
                else ->
                    Toast.makeText(context, R.string.tts_settings_title, Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ─── QuizContract.View ────────────────────────────────────────────────────

    override fun displayWords(quizWordsPair: List<Pair<Word, QuizType>>) {
        holdOn = false
        uiState = uiState.copy(
            words = quizWordsPair,
            segments = List(quizWordsPair.size) { SegmentState.Pending },
        )
    }

    override fun noWords() {
        requireContext().alertDialog {
            messageResource = R.string.quiz_empty
            okButton { requireActivity().finish() }
            setOnCancelListener { requireActivity().finish() }
        }.show()
    }


    override fun setPagerPosition(position: Int) {
        // P5 : marquer le segment courant en Current (orange)
        val newSegments = uiState.segments.toMutableList()
        if (position in newSegments.indices && newSegments[position] == SegmentState.Pending) {
            newSegments[position] = SegmentState.Current
        }
        uiState = uiState.copy(currentIndex = position, segments = newSegments)
    }

    override fun setSentence(sentence: Sentence) {
        // P4 : orange signature pour le mot dans la phrase (pas la couleur de maîtrise)
        uiState = uiState.copy(sentence = sentence, wordHighlightColor = AccentOrange.toArgb())
    }

    override fun reInitUI() {
        holdOn = false
        uiState = uiState.copy(
            isRevealed = false,
            editText = "",
            editTextColorInt = Color.White.toArgb(),
            editShowDisplayAnswer = false,
            qcmOptions = List(4) { QcmOption("") },
            answerMode = AnswerMode.None,
        )
    }

    override fun displayQCMMode(hintText: String?) {
        hideKeyboard()
        uiState = uiState.copy(
            answerMode = AnswerMode.QCM,
            hintText = hintText,
            isRevealed = false,
        )
    }

    override fun displayEditMode() {
        uiState = uiState.copy(answerMode = AnswerMode.Edit, isRevealed = false)
        showKeyboard()
    }

    override fun displayQCMNormalTextViews() {
        uiState = uiState.copy(qcmShowFuri = false)
    }

    override fun displayQCMFuriTextViews() {
        uiState = uiState.copy(qcmShowFuri = true)
    }

    override fun displayQCMTv(tvNum: Int, option: String, colorId: Int) {
        val newOptions = uiState.qcmOptions.toMutableList()
        val idx = tvNum - 1
        if (idx in newOptions.indices) {
            newOptions[idx] = newOptions[idx].copy(
                label = option,
                buttonState = colorIdToButtonState(colorId),
                isFuri = false,
            )
        }
        uiState = uiState.copy(qcmOptions = newOptions)
    }

    override fun displayQCMTv(options: List<String>, colorIds: List<Int>) {
        uiState = uiState.copy(
            qcmOptions = options.mapIndexed { i, label ->
                QcmOption(
                    label = label,
                    buttonState = colorIdToButtonState(colorIds.getOrElse(i) { android.R.color.white }),
                    isFuri = false,
                )
            },
        )
        // After answer: set isRevealed if any option has Correct or Wrong state
        updateRevealedFromOptions()
    }

    override fun displayQCMFuri(furiNum: Int, optionFuri: String, start: Int, end: Int, colorId: Int) {
        val newOptions = uiState.qcmOptions.toMutableList()
        val idx = furiNum - 1
        if (idx in newOptions.indices) {
            newOptions[idx] = QcmOption(
                label = optionFuri,
                furiStart = start,
                furiEnd = end,
                buttonState = colorIdToButtonState(colorId),
                isFuri = true,
            )
        }
        uiState = uiState.copy(qcmOptions = newOptions)
    }

    override fun displayQCMFuri(options: List<String>, starts: List<Int>, ends: List<Int>, colorIds: List<Int>) {
        uiState = uiState.copy(
            qcmOptions = options.mapIndexed { i, label ->
                QcmOption(
                    label = label,
                    furiStart = starts.getOrElse(i) { 0 },
                    furiEnd = ends.getOrElse(i) { label.length },
                    buttonState = colorIdToButtonState(colorIds.getOrElse(i) { android.R.color.white }),
                    isFuri = true,
                )
            },
        )
        updateRevealedFromOptions()
    }

    override fun showKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        view?.let { imm.showSoftInput(it, InputMethodManager.SHOW_IMPLICIT) }
    }

    override fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        view?.let { imm.hideSoftInputFromWindow(it.windowToken, 0) }
    }

    override fun animateCheck(result: Boolean) {
        val newSegments = uiState.segments.toMutableList()
        val idx = uiState.currentIndex
        if (idx in newSegments.indices) {
            newSegments[idx] = if (result) SegmentState.Correct else SegmentState.Wrong
        }
        // P4 : mot dans la phrase passe en vert après bonne réponse, reste orange après mauvaise
        val newHighlight = if (result) Correct.toArgb() else AccentOrange.toArgb()
        uiState = uiState.copy(
            segments = newSegments,
            isRevealed = true,
            wordHighlightColor = newHighlight,
            editShowDisplayAnswer = !result && uiState.answerMode == AnswerMode.Edit,
        )
        holdOn = false
    }

    override fun setEditTextColor(color: Int) {
        val argb = ContextCompat.getColor(requireContext(), color)
        uiState = uiState.copy(editTextColorInt = argb)
    }

    override fun clearEdit() {
        uiState = uiState.copy(editText = "")
    }

    override fun displayEditAnswer(answer: String) {
        val green = ContextCompat.getColor(requireContext(), R.color.level_master_4)
        uiState = uiState.copy(editText = answer, editTextColorInt = green)
    }

    override fun displayEditDisplayAnswerButton() {
        uiState = uiState.copy(editShowDisplayAnswer = true)
    }

    override fun setHiraganaConversion(enabled: Boolean) {
        uiState = uiState.copy(editIsEnableConversion = enabled)
    }

    override fun animateColor(
        position: Int,
        word: Word,
        sentence: Sentence,
        quizType: QuizType,
        fromPoints: Int,
        toPoints: Int,
    ) {
        // P4 : couleur déjà gérée par animateCheck — ne pas écraser avec la couleur de maîtrise
    }

    override fun showAlertSessionEnd(wordCount: Int, isProgressive: Boolean, proposeErrors: Boolean) {
        dialogFlowController.showAlertSessionEnd(wordCount, isProgressive, proposeErrors)
    }

    override fun showAlertErrorSessionEnd(quizEnded: Boolean, isProgressive: Boolean) {
        dialogFlowController.showAlertErrorSessionEnd(quizEnded, isProgressive)
    }

    override fun showAlertQuizEnd(proposeErrors: Boolean) {
        dialogFlowController.showAlertQuizEnd(proposeErrors)
    }

    override fun finishQuiz() {
        requireActivity().finish()
    }

    override fun openAnswersScreen(answers: ArrayList<Answer>) {
        LocalPersistence.witeObjectToFile(activity, answers, "answers")
        val intent = Intent(activity, AnswersActivity::class.java)
        startActivity(intent)
    }

    override fun reportError(word: Word, sentence: Sentence) {
        reportError(requireActivity(), word, sentence)
    }

    override fun incrementInfiniteCount() {
        uiState = uiState.copy(infiniteCount = (uiState.infiniteCount ?: 0) + 1)
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private fun colorIdToButtonState(colorId: Int): AnswerButtonState = when (colorId) {
        R.color.level_master_4 -> AnswerButtonState.Correct
        R.color.level_low_1 -> AnswerButtonState.Wrong
        else -> AnswerButtonState.Default
    }

    private fun updateRevealedFromOptions() {
        val hasAnswer = uiState.qcmOptions.any {
            it.buttonState == AnswerButtonState.Correct || it.buttonState == AnswerButtonState.Wrong
        }
        if (hasAnswer) {
            uiState = uiState.copy(isRevealed = true)
        }
    }

    private fun showSelectionMenu() {
        val word = uiState.currentWord ?: return
        lifecycleScope.launch {
            val selections = presenter.getSelections()
            val items = selections.map { it.getName() }.toTypedArray()
            requireContext().alertDialog {
                setTitle(R.string.add_to_selections)
                setItems(items) { _, index ->
                    lifecycleScope.launch {
                        if (presenter.isWordInQuiz(word.id, selections[index].id))
                            presenter.deleteWordFromSelection(word.id, selections[index].id)
                        else
                            presenter.addWordToSelection(word.id, selections[index].id)
                    }
                }
                negativeButton(R.string.new_selection) { addSelection(word.id) }
            }.show()
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
}
