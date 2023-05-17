package com.jehutyno.yomikata.screens.quiz

import android.animation.Animator
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.os.Parcelable
import android.speech.tts.TextToSpeech
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.text.method.ScrollingMovementMethod
import android.view.*
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.viewpager.widget.ViewPager
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.databinding.FragmentQuizBinding
import com.jehutyno.yomikata.furigana.FuriganaView
import com.jehutyno.yomikata.managers.VoicesManager
import com.jehutyno.yomikata.model.*
import com.jehutyno.yomikata.screens.answers.AnswersActivity
import com.jehutyno.yomikata.screens.content.word.WordDetailDialogFragment
import com.jehutyno.yomikata.util.*
import com.jehutyno.yomikata.view.SwipeDirection
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.kodein.di.*
import splitties.alertdialog.appcompat.*


/**
 * Created by valentin on 18/10/2016.
 */
class QuizFragment(private val di: DI) : Fragment(), QuizContract.View, QuizItemPagerAdapter.Callback, TextToSpeech.OnInitListener {

    // kodein
    private val subDI = DI.lazy {
        extend(di)
        bind<VoicesManager>() with singleton { VoicesManager(requireActivity()) }
    }
    @Suppress("unused")
    private val voicesManager: VoicesManager by subDI.instance()

    private lateinit var presenter: QuizContract.Presenter
    private var adapter: QuizItemPagerAdapter? = null
    private var tts: TextToSpeech? = null
    private var ttsSupported = SPEECH_NOT_INITALIZED
    private var currentEditColor: Int = R.color.lighter_gray
    private lateinit var errorsMenu: MenuItem
    private lateinit var ttsSettingsMenu: MenuItem
    private var holdOn = false
    private var isSettingsOpen = false

    // View Binding
    private var _binding: FragmentQuizBinding? = null
    private val binding get() = _binding!!
    // for keyboard entry edit and multiple choice
    private val editBinding get() = binding.quizAnswersKeyboardEntry
    private val qcmBinding get() = binding.quizAnswersMultipleChoice

    /** List of QCM tvs for convenience */
    private val QCMtvs get() = qcmBinding.let {
        listOf(it.option1Tv, it.option2Tv, it.option3Tv, it.option4Tv)
    }
    /** List of QCM furigana for convenience */
    private val QCMfuri get() = qcmBinding.let {
        listOf(it.option1Furi, it.option2Furi, it.option3Furi, it.option4Furi)
    }

    override fun onInit(status: Int) {
        ttsSupported = onTTSinit(activity, status, tts)
        presenter.setTTSSupported(ttsSupported)
        if (adapter != null && adapter!!.words.isNotEmpty()) {
            val pref = PreferenceManager.getDefaultSharedPreferences(requireContext())
            val noPlayStart = pref.getBoolean("play_start", false)
            if (adapter!!.words[binding.pager.currentItem].second == QuizType.TYPE_AUDIO || noPlayStart) {
                voicesManager.speakWord(adapter!!.words[binding.pager.currentItem].first, ttsSupported, tts)
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

    override fun setPresenter(presenter: QuizContract.Presenter) {
        this.presenter = presenter
    }

    /**
     * Activity Methods
     */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onResume() {
        super.onResume()
        val pref = PreferenceManager.getDefaultSharedPreferences(requireContext())
        editBinding.hiraganaEdit.inputType = if (pref.getBoolean("input_change", false))
            InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        else
            InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        presenter.start()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("edit", editBinding.hiraganaEdit.text.toString())
        outState.putInt("edit_color", currentEditColor)
        presenter.onSaveInstanceState(outState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentQuizBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tts = TextToSpeech(activity, this)

        initUI()

        if (savedInstanceState != null) {
            editBinding.hiraganaEdit.setText(savedInstanceState.getString("edit"))
            savedInstanceState.getString("edit")?.let { editBinding.hiraganaEdit.setSelection(it.length) }
            editBinding.hiraganaEdit.setTextColor(ContextCompat.getColor(requireActivity(), currentEditColor))
            presenter.onRestoreInstanceState(savedInstanceState)
        } else {
            lifecycleScope.launch {
                presenter.initQuiz()
            }
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        voicesManager.releasePlayer()
        super.onDestroy()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onPause() {
        super.onPause()
        requireActivity().hideSoftKeyboard()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_quiz, menu)
        super.onCreateOptionsMenu(menu, inflater)
        this.errorsMenu = menu.findItem(R.id.errors)
        if (context != null) {
            val errorsImage = ImageView(context)
            errorsImage.setImageResource(R.drawable.ic_tooltip_edit)
            val pad = DimensionHelper.getPixelFromDip(activity, 12)
            errorsImage.setPadding(pad, pad, pad, pad)
            errorsImage.setOnClickListener {
                presenter.onDisplayAnswersClick()
            }
            this.errorsMenu.actionView = errorsImage
        }
        this.ttsSettingsMenu = menu.findItem(R.id.tts_settings)

        if (context != null) {
            val ttsErrorsImage = ImageView(context)
            ttsErrorsImage.setImageResource(R.drawable.ic_tts_settings)
            val pad = DimensionHelper.getPixelFromDip(activity, 12)
            ttsErrorsImage.setPadding(pad, pad, pad, pad)
            ttsErrorsImage.setOnClickListener {
                val category = adapter!!.words[binding.pager.currentItem].first.baseCategory
                when (val speechAvailability = checkSpeechAvailability(requireActivity(), ttsSupported, getCategoryLevel(category))) {
                    SpeechAvailability.NOT_AVAILABLE -> {
                        speechNotSupportedAlert(requireActivity(), getCategoryLevel(category)) {}
                    }
                    else -> {
                        if (isSettingsOpen) {
                            closeTTSSettings()
                        } else {
                            if (speechAvailability == SpeechAvailability.VOICES_AVAILABLE) {
                                binding.settingsSpeed.visibility = GONE
                                binding.seekSpeed.visibility = GONE
                            } else {
                                binding.settingsSpeed.visibility = VISIBLE
                                binding.seekSpeed.visibility = VISIBLE
                            }
                            binding.settingsContainer.animate().setDuration(300).translationY(0f).withStartAction {
                                binding.settingsContainer.visibility = VISIBLE
                                isSettingsOpen = true
                            }.start()
                        }
                    }
                }
            }
            this.ttsSettingsMenu.actionView = ttsErrorsImage
        }
    }

    /**
     *  UI Initialization
     */

    private fun initUI() {
        initPager()
        initEditText()
        setUpAudioManager()
        initAnswersButtons()
    }

    private fun initPager() {
        adapter = QuizItemPagerAdapter(requireContext(), this)
        binding.pager.adapter = adapter
        binding.pager.setAllowedSwipeDirection(SwipeDirection.none)
        binding.pager.offscreenPageLimit = 0
        binding.pager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {
            }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
            }

            override fun onPageSelected(position: Int) {
                editBinding.hiraganaEdit.isEnableConversion = adapter!!.words[position].first.isKana == 0
                holdOn = false
            }
        })
    }

    private fun initEditText() {
        editBinding.hiraganaEdit.setOnEditorActionListener { _, i, keyEvent -> runBlocking {
            if (isSettingsOpen) closeTTSSettings()
            if (adapter!!.words[binding.pager.currentItem].second == QuizType.TYPE_PRONUNCIATION
                && (i == EditorInfo.IME_ACTION_DONE || keyEvent?.keyCode == KeyEvent.KEYCODE_ENTER) && !holdOn) {
                // Validate Action
                holdOn = true
                editBinding.hiraganaEdit.setText(editBinding.hiraganaEdit.text.toString().replace("n", if (adapter!!.words[binding.pager.currentItem].first.isKana >= 1) "n" else "ん"))
                presenter.onAnswerGiven(editBinding.hiraganaEdit.text.toString().trim().replace(" ", "").replace("　", "").replace("\n", "")) // TODO add function clean utils
            }
            true
        }}

        editBinding.hiraganaEdit.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                if (isSettingsOpen) closeTTSSettings()
                // Return to normal color when typing again (because it becomes Red or Green when
                // you validate
                currentEditColor = R.color.lighter_gray
                editBinding.hiraganaEdit.setTextColor(ContextCompat.getColor(activity!!, currentEditColor))
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

        })

        editBinding.editAction.setOnClickListener {
            if (isSettingsOpen) closeTTSSettings()
            // hold is used to wait so only one answer is validated even with multiple press
            if (!holdOn) {
                holdOn = true
                presenter.onEditActionClick()
                holdOn = false
            }
        }
    }

    override fun clearEdit() {
        editBinding.hiraganaEdit.setText("")
    }

    override fun displayEditAnswer(answer: String) {
        editBinding.hiraganaEdit.setText(answer)
        currentEditColor = R.color.level_master_4
        editBinding.hiraganaEdit.setTextColor(ContextCompat.getColor(requireActivity(), R.color.level_master_4))
        editBinding.hiraganaEdit.setSelection(editBinding.hiraganaEdit.text.length)
    }

    private fun setUpAudioManager() {
        val audioManager = requireActivity().getSystemService(Context.AUDIO_SERVICE) as AudioManager
        binding.seekVolume.max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        binding.seekVolume.progress = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        binding.seekVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, p1, 0)
                presenter.onSpeakSentence()
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
            }

        })

        binding.seekSpeed.max = 250
        val pref = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val rate = pref.getInt(Prefs.TTS_RATE.pref, 50)
        binding.seekSpeed.progress = rate
        tts?.setSpeechRate((rate + 50).toFloat() / 100)
        binding.seekSpeed.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                pref.edit().putInt(Prefs.TTS_RATE.pref, p1).apply()
                tts?.setSpeechRate((p1 + 50).toFloat() / 100)
                presenter.onSpeakSentence()
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
            }

        })
        binding.settingsContainer.translationY = -400f
        binding.settingsClose.setOnClickListener {
            closeTTSSettings()
        }
    }

    private fun initAnswersButtons() {
        binding.quizContainer.setOnClickListener { if (isSettingsOpen) closeTTSSettings() }
        binding.answerContainer.setOnClickListener { if (isSettingsOpen) closeTTSSettings() }
        binding.quizAnswerType.visibility = GONE
        binding.tapToReveal.setOnClickListener { it.visibility = GONE }
        binding.tapToReveal.visibility = GONE

        // produces the function for the OnClickListener of a button with a number = 1,2,3,4
        fun clickerFactory(num: Int) : (View) -> Unit {
            return {
                if (isSettingsOpen) closeTTSSettings()
                if (!holdOn) {
                    holdOn = true
                    runBlocking {
                        presenter.onOptionClick(num)
                    }
                }
            }
        }
        qcmBinding.option1Container.setOnClickListener(clickerFactory(1))
        qcmBinding.option2Container.setOnClickListener(clickerFactory(2))
        qcmBinding.option3Container.setOnClickListener(clickerFactory(3))
        qcmBinding.option4Container.setOnClickListener(clickerFactory(4))

        QCMtvs.forEachIndexed { i, tv ->
            tv.setOnClickListener(clickerFactory(i + 1))
            tv.movementMethod = ScrollingMovementMethod()
        }
    }

    override fun reInitUI() {
        editBinding.hiraganaEdit.setText("")
        editBinding.editAction.setImageResource(R.drawable.ic_cancel_black_24dp)
        editBinding.editAction.setColorFilter(ContextCompat.getColor(requireActivity(), R.color.lighter_gray))
        QCMtvs.forEach { tv ->
            tv.setTextColor(ContextCompat.getColor(requireActivity(), android.R.color.white))
        }
        QCMfuri.forEach { furi ->
            furi.setTextColor(ContextCompat.getColor(requireActivity(), android.R.color.white))
        }
    }

    /**
     * Graphical methods
     */

    override fun displayWords(quizWordsPair: List<Pair<Word, QuizType>>) {
        holdOn = false
        adapter!!.replaceData(quizWordsPair)
        // TODO do something with that
//        pager.post { tutos() }
    }

    override fun noWords() {
        qcmBinding.root.visibility = GONE
        binding.answerContainer.visibility = GONE

        requireContext().alertDialog {
            messageResource = R.string.quiz_empty
            okButton { requireActivity().finish() }
            setOnCancelListener { requireActivity().finish() }
        }.show()
    }

    override fun setPagerPosition(position: Int) {
        binding.pager.currentItem = position
    }

    override fun setSentence(sentence: Sentence) {
        adapter!!.replaceSentence(sentence)
        adapter!!.notifyDataSetChanged()
    }

    override fun setEditTextColor(color: Int) {
        editBinding.hiraganaEdit.setTextColor(ContextCompat.getColor(requireActivity(), color))
        editBinding.hiraganaEdit.setSelection(editBinding.hiraganaEdit.text.length)
    }

    override fun animateCheck(result: Boolean) {
        if (result) {
            binding.check.setImageResource(R.drawable.ic_check_black_48dp)
            binding.check.setColorFilter(ContextCompat.getColor(requireActivity(), R.color.level_master_4))
        } else {
            binding.check.setImageResource(R.drawable.ic_clear_black_48dp)
            binding.check.setColorFilter(ContextCompat.getColor(requireActivity(), R.color.level_low_1))
        }
        binding.check.animate().alpha(1f).setDuration(200).setStartDelay(0).setListener(object : Animator.AnimatorListener {
            override fun onAnimationRepeat(animation: Animator) {
            }

            override fun onAnimationEnd(animation: Animator) {
                binding.check.animate().alpha(0f).setDuration(300).setStartDelay(300).setListener(object : Animator.AnimatorListener {
                    override fun onAnimationRepeat(animation: Animator) {
                    }

                    override fun onAnimationEnd(animation: Animator) {
                        binding.check.visibility = GONE
                        if (result) {
                            runBlocking {
                                presenter.onNextWord()
                            }
                        } else {
                            holdOn = false
                            displayEditDisplayAnswerButton()
                        }
                    }

                    override fun onAnimationCancel(animation: Animator) {
                    }

                    override fun onAnimationStart(animation: Animator) {
                    }

                }).start()
            }

            override fun onAnimationCancel(animation: Animator) {
            }

            override fun onAnimationStart(animation: Animator) {
                binding.check.visibility = VISIBLE
            }

        }).start()

    }

    override fun displayEditDisplayAnswerButton() {
        editBinding.editAction.setImageResource(R.drawable.ic_visibility_black_24dp)
        editBinding.editAction.setColorFilter(ContextCompat.getColor(requireActivity(), R.color.level_master_4))
    }

    /**
     * Display QCM mode
     *
     * Hides the keyboard entry and shows multiple choice options.
     *
     * @param hintText Text to show if tap_to_reveal = true in user preferences
     */
    override fun displayQCMMode(hintText: String?) {
        qcmBinding.root.visibility = VISIBLE
        editBinding.root.visibility = GONE
        val pref = PreferenceManager.getDefaultSharedPreferences(requireContext())
        if (pref.getBoolean("tap_to_reveal", false)) {
            binding.tapToReveal.visibility = VISIBLE
            binding.quizAnswerType.visibility = VISIBLE
            // say what type of answer should be found
            if (hintText != null)
                binding.quizAnswerType.text = hintText
        } else {
            binding.tapToReveal.visibility = GONE
            binding.quizAnswerType.visibility = GONE
        }
    }

    override fun displayEditMode() {
        binding.quizAnswerType.visibility = GONE
        qcmBinding.root.visibility = GONE
        editBinding.root.visibility = VISIBLE
    }

    override fun displayQCMNormalTextViews() {
        val pref = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val fontSize = pref.getString("font_size", "23")!!.toFloat()
        QCMtvs.forEach { tv ->
            tv.textSize = fontSize
            tv.visibility = VISIBLE
        }
        QCMfuri.forEach{ furi ->
            furi.visibility = GONE
        }
    }

    /**
     * Display QCM furi text views
     *
     * Makes all normal option texts GONE and makes furi text VISIBLE
     */
    override fun displayQCMFuriTextViews() {
        QCMtvs.forEach { tv ->
            tv.visibility = GONE
        }
        QCMfuri.forEach { furi ->
            furi.visibility = VISIBLE
        }
    }

    /**
     * Display QCM tv
     *
     * Sets the text and color of a specified QCM tv.
     *
     * @param tvNum Index of the tv: 1,2,3,4
     * @param option Text to show in tv
     * @param colorId Color of the text
     */
    override fun displayQCMTv(tvNum: Int, option: String, colorId: Int) {
        QCMtvs[tvNum - 1].also { tv ->
            tv.text = option
            tv.setTextColor(ContextCompat.getColor(requireContext(), colorId))
        }
    }

    /**
     * Display QCM tv
     *
     * Sets the text and color of all QCM tvs to the values in options and colors.
     *
     * @param options List of 4 strings
     * @param colorIds List of 4 color ids (int)
     */
    override fun displayQCMTv(options: List<String>, colorIds: List<Int>) {
        QCMtvs.forEachIndexed { i, tv ->
            tv.text = options[i]
            tv.setTextColor(ContextCompat.getColor(requireContext(), colorIds[i]))
        }
    }

    /**
     * Display QCM furi
     *
     * @param furiNum Index of the furigana text: 1,2,3,4
     * @param optionFuri Text to display
     * @param start Start (int)
     * @param end End (int)
     * @param colorId Id of the color (int)
     */
    override fun displayQCMFuri(furiNum: Int, optionFuri: String, start: Int, end: Int, colorId: Int) {
        val color = ContextCompat.getColor(requireContext(), colorId)
        QCMfuri[furiNum - 1].text_set(optionFuri, start, end, color)
    }

    override fun displayQCMFuri(options: List<String>, starts: List<Int>, ends: List<Int>, colorIds: List<Int>) {
        QCMfuri.forEachIndexed { i, furi ->
            furi.text_set(options[i], starts[i], ends[i], ContextCompat.getColor(requireContext(), colorIds[i]))
        }
    }

    fun setOptionsFontSize(fontSize: Float) {
        QCMtvs.forEach { tv ->
            tv.textSize = fontSize
        }
    }

    fun closeTTSSettings() {
        binding.settingsContainer.animate().setDuration(300).translationY(-400f).withEndAction {
            isSettingsOpen = false
            binding.settingsContainer.visibility = GONE
        }.start()
    }

    override fun showKeyboard() {
        val inputMethodManager = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.showSoftInput(editBinding.hiraganaEdit, InputMethodManager.SHOW_IMPLICIT)
        editBinding.hiraganaEdit.requestFocus()
    }

    override fun hideKeyboard() {
        val inputMethodManager = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(editBinding.hiraganaEdit.windowToken, 0)
    }

    override fun animateColor(position: Int, word: Word, sentence: Sentence, quizType: QuizType, fromPoints: Int, toPoints: Int) {
        val view = binding.pager.findViewWithTag<View>("pos_$position")
        val btnFuri = view.findViewById<View>(R.id.btn_furi)
        val furiSentence = view.findViewById<FuriganaView>(R.id.furi_sentence)
        val tradSentence = view.findViewById<TextView>(R.id.trad_sentence)
        val sound = view.findViewById<ImageButton>(R.id.sound)
        val sentenceNoFuri = sentenceNoFuri(sentence)
        val colorAnimation = ValueAnimator.ofObject(ArgbEvaluator(),
            getWordColor(requireContext(), fromPoints),
            getWordColor(requireContext(), toPoints))
        colorAnimation.addUpdateListener {
            animator ->
            run {
                when (quizType) {
                    QuizType.TYPE_PRONUNCIATION, QuizType.TYPE_PRONUNCIATION_QCM, QuizType.TYPE_JAP_EN -> {
                        val colorEntireWord = word.isKana == 2 && quizType == QuizType.TYPE_JAP_EN
                        val wordTruePosition = if (colorEntireWord) 0 else getWordPositionInFuriSentence(sentence.jap, word)
                        if (btnFuri.isSelected) {
                            if (!colorEntireWord) wordTruePosition.let {
                                furiSentence.text_set(
                                    sentenceNoAnswerFuri(sentence, word), it,
                             wordTruePosition + word.japanese.length,
                                    animator.animatedValue as Int)
                            }
                        } else {
                            furiSentence.text_set(
                                if (colorEntireWord) sentence.jap else sentenceNoFuri.replace("%", word.japanese),
                                (if (colorEntireWord) 0 else wordTruePosition),
                                if (colorEntireWord) sentence.jap.length else wordTruePosition + word.japanese.length,
                                animator.animatedValue as Int)
                        }
                    }
                    QuizType.TYPE_EN_JAP -> {
                        tradSentence.setTextColor(animator.animatedValue as Int)
                    }
                    QuizType.TYPE_AUDIO -> {
                        sound.setColorFilter(animator.animatedValue as Int)
                    }
                    else -> {
                    }
                }
            }
        }
        colorAnimation.start()
    }

    override fun showAlertProgressiveSessionEnd(proposeErrors: Boolean) {
        val sessionLength = adapter!!.words.size

        requireContext().alertDialog {
            message = getString(R.string.alert_session_finished, sessionLength)
            positiveButton(R.string.alert_continue) {
                runBlocking {
                    presenter.onLaunchNextProgressiveSession()
                }
            }
            neutralButton(R.string.alert_quit) { finishQuiz() }
            setCancelable(false)    // avoid accidental click out of session
        }.show()
    }

    override fun showAlertErrorSessionEnd(quizEnded: Boolean) {
        requireContext().alertDialog {
            messageResource = R.string.alert_error_review_finished

            if (!quizEnded) {
                messageResource = R.string.alert_error_review_session_message
                positiveButton(R.string.alert_continue_quiz) {
                    runBlocking {
                        presenter.onContinueQuizAfterErrorSession()
                    }
                }
            } else {
                messageResource = R.string.alert_error_review_quiz_message
                positiveButton(R.string.alert_restart) {
                    runBlocking {
                        presenter.onRestartQuiz()
                    }
                }
            }
            neutralButton(R.string.alert_quit) {
                finishQuiz()
            }
            setCancelable(false)    // avoid accidental click out of session
        }.show()
    }

    override fun showAlertNonProgressiveSessionEnd(proposeErrors: Boolean) {
        val pref = PreferenceManager.getDefaultSharedPreferences(requireContext())

        requireContext().alertDialog {
            message = getString(R.string.alert_session_finished, pref.getString("length", "-1")?.toInt())
            positiveButton(R.string.alert_continue) {
                runBlocking {
                    presenter.onContinueAfterNonProgressiveSessionEnd()
                }
            }
            if (proposeErrors) {
                negativeButton(R.string.alert_review_session_errors) {
                    // TODO handle shuffle ?
                    runBlocking {
                        presenter.onLaunchErrorSession()
                    }
                }
            }
            neutralButton(R.string.alert_quit) {
                presenter.onFinishQuiz()
            }
            setCancelable(false)    // avoid accidental click out of session
        }.show()
    }

    override fun showAlertQuizEnd(proposeErrors: Boolean) {
        requireContext().alertDialog {
            messageResource = R.string.alert_quiz_finished
            positiveButton(R.string.alert_restart) {
                runBlocking {
                    presenter.onRestartQuiz()
                }
            }
            if (proposeErrors) {
                negativeButton(R.string.alert_review_quiz_errors) {
                    runBlocking {
                        presenter.onLaunchErrorSession()
                    }
                }
            }
            neutralButton(R.string.alert_quit) {
                finishQuiz()
            }
            setCancelable(false)    // avoid accidental click out of session
        }.show()
    }

    // TODO move to presenter ?
    /**
     * Actions
     */

    override fun setHiraganaConversion(enabled: Boolean) {
        editBinding.hiraganaEdit.isEnableConversion = enabled
    }

    override fun finishQuiz() {
        requireActivity().finish()
    }

    override fun openAnswersScreen(answers: ArrayList<Answer>) {
        LocalPersistence.witeObjectToFile(activity, answers, "answers")
        val intent = Intent(activity, AnswersActivity::class.java)
        startActivity(intent)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.errors -> {
                presenter.onDisplayAnswersClick()
            }
            R.id.tts_settings -> {
                val category = adapter!!.words[binding.pager.currentItem].first.baseCategory
                when (val speechAvailability = checkSpeechAvailability(requireActivity(), ttsSupported, getCategoryLevel(category))) {
                    SpeechAvailability.NOT_AVAILABLE -> {
                        speechNotSupportedAlert(requireActivity(), getCategoryLevel(category)) {}
                    }
                    else -> {
                        if (isSettingsOpen) {
                            closeTTSSettings()
                        } else {
                            if (speechAvailability == SpeechAvailability.VOICES_AVAILABLE) {
                                binding.settingsSpeed.visibility = GONE
                                binding.seekSpeed.visibility = GONE
                            } else {
                                binding.settingsSpeed.visibility = VISIBLE
                                binding.seekSpeed.visibility = VISIBLE
                            }
                            binding.settingsContainer.animate().setDuration(300).translationY(0f).withStartAction {
                                binding.settingsContainer.visibility = VISIBLE
                                isSettingsOpen = true
                            }.start()
                        }
                    }
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onItemClick(position: Int) {
        Intent().putExtra(Extras.EXTRA_QUIZ_TYPE, adapter!!.words[position].second as Parcelable)
        val dialog = WordDetailDialogFragment(di)
        val bundle = Bundle()
        bundle.putLong(Extras.EXTRA_WORD_ID, adapter!!.words[position].first.id)
        bundle.putSerializable(Extras.EXTRA_QUIZ_TYPE,
            if (presenter.previousAnswerWrong()) null else adapter!!.words[position].second)
        bundle.putString(Extras.EXTRA_SEARCH_STRING, "")
        dialog.arguments = bundle
        dialog.show(childFragmentManager, "")
        dialog.isCancelable = true
    }

    override fun onSoundClick(button: ImageButton, position: Int) {
        presenter.onSpeakWordTTS()
    }

    override fun onSelectionClick(view: View, position: Int) = runBlocking {
        val selections = presenter.getSelections()
        val popup = PopupMenu(activity, view)
        val word = adapter!!.words[position].first
        popup.menuInflater.inflate(R.menu.popup_selections, popup.menu)
        for ((i, selection) in selections.withIndex()) {
            popup.menu.add(1, i, i, selection.getName()).isChecked = presenter.isWordInQuiz(word.id, selection.id)
            popup.menu.setGroupCheckable(1, true, false)
        }
        popup.setOnMenuItemClickListener { it -> runBlocking {
            when (it.itemId) {
                R.id.add_selection -> addSelection(word.id)
                else -> {
                    if (!it.isChecked)
                        presenter.addWordToSelection(word.id, selections[it.itemId].id)
                    else {
                        presenter.deleteWordFromSelection(word.id, selections[it.itemId].id)
                    }
                    it.isChecked = !it.isChecked
                }
            }
            true
        }}
        popup.show()
    }

    override fun onReportClick(position: Int) {
        presenter.onReportClick(position)
    }

    override fun reportError(word: Word, sentence: Sentence) {
        reportError(requireActivity(), word, sentence)
    }

    override fun onFuriClick(position: Int, isSelected: Boolean) = lifecycleScope.launch {
        presenter.setIsFuriDisplayed(isSelected)
    }

    override fun onSentenceTTSClick(position: Int) {
        presenter.onSpeakSentence()
    }

    override fun onTradClick(position: Int) {

    }

    private fun addSelection(wordId: Long) {
        val input = EditText(activity)
        input.setSingleLine()
        input.hint = getString(R.string.selection_name)

        val container = FrameLayout(requireActivity())
        val params = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        params.leftMargin = DimensionHelper.getPixelFromDip(activity, 20)
        params.rightMargin = DimensionHelper.getPixelFromDip(activity, 20)
        input.layoutParams = params
        container.addView(input)

        requireContext().alertDialog {
            titleResource = R.string.new_selection
            setView(container)
            okButton {
                lifecycleScope.launch {
                    val selectionId = presenter.createSelection(input.text.toString())
                    presenter.addWordToSelection(wordId, selectionId)
                }
            }
            cancelButton()
        }.show()
    }

    override fun incrementInfiniteCount() {
        adapter!!.isInfiniteSize = (adapter!!.isInfiniteSize?: 0) + 1
    }

}
