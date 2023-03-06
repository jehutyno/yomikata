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
import androidx.fragment.app.Fragment
import androidx.core.content.ContextCompat
import androidx.viewpager.widget.ViewPager
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
import com.github.salomonbrys.kodein.Kodein
import com.github.salomonbrys.kodein.KodeinInjector
import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance
import com.github.salomonbrys.kodein.singleton
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.databinding.FragmentQuizBinding
import com.jehutyno.yomikata.furigana.FuriganaView
import com.jehutyno.yomikata.managers.VoicesManager
import com.jehutyno.yomikata.model.*
import com.jehutyno.yomikata.screens.answers.AnswersActivity
import com.jehutyno.yomikata.screens.content.word.WordDetailDialogFragment
import com.jehutyno.yomikata.util.*
import com.jehutyno.yomikata.view.SwipeDirection
import org.jetbrains.anko.cancelButton
import org.jetbrains.anko.okButton
import org.jetbrains.anko.padding
import org.jetbrains.anko.support.v4.alert
import org.jetbrains.anko.support.v4.defaultSharedPreferences
import org.jetbrains.anko.support.v4.withArguments
import org.jetbrains.anko.textColor


/**
 * Created by valentin on 18/10/2016.
 */
class QuizFragment : Fragment(), QuizContract.View, QuizItemPagerAdapter.Callback, TextToSpeech.OnInitListener {

    private val injector = KodeinInjector()
    private val voicesManager: VoicesManager by injector.instance()
    private lateinit var presenter: QuizContract.Presenter
    private var adapter: QuizItemPagerAdapter? = null
    private lateinit var selections: List<Quiz>
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


    override fun onInit(status: Int) {
        if (adapter != null && adapter!!.words.isNotEmpty()) {
            ttsSupported = onTTSinit(activity, status, tts)
            presenter.setTTSSupported(ttsSupported)
            if (adapter!!.words[binding.pager.currentItem].second == QuizType.TYPE_AUDIO ||
                defaultSharedPreferences.getBoolean("play_start", false)) {
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
        binding.hiraganaEdit.inputType = if (defaultSharedPreferences.getBoolean("input_change", false))
            InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        else
            InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        presenter.start()
        presenter.loadSelections()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("edit", binding.hiraganaEdit.text.toString())
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
        injector.inject(Kodein {
            extend(appKodein())
            bind<VoicesManager>() with singleton { VoicesManager(activity!!) }
        })

        initUI()

        if (savedInstanceState != null) {
            binding.hiraganaEdit.setText(savedInstanceState.getString("edit"))
            savedInstanceState.getString("edit")?.let { binding.hiraganaEdit.setSelection(it.length) }
            binding.hiraganaEdit.setTextColor(ContextCompat.getColor(activity!!, currentEditColor))
            presenter.onRestoreInstanceState(savedInstanceState)
        } else
            presenter.initQuiz()
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
        activity!!.hideSoftKeyboard()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_quiz, menu)
        super.onCreateOptionsMenu(menu, inflater)
        this.errorsMenu = menu.findItem(R.id.errors)
        if (context != null) {
            val errorsImage = ImageView(context)
            errorsImage.setImageResource(R.drawable.ic_tooltip_edit)
            errorsImage.padding = DimensionHelper.getPixelFromDip(activity, 12)
            errorsImage.setOnClickListener {
                presenter.onDisplayAnswersClick()
            }
            this.errorsMenu.actionView = errorsImage
        }
        this.ttsSettingsMenu = menu.findItem(R.id.tts_settings)

        if (context != null) {
            val ttsErrorsImage = ImageView(context)
            ttsErrorsImage.setImageResource(R.drawable.ic_tts_settings)
            ttsErrorsImage.padding = DimensionHelper.getPixelFromDip(activity, 12)
            ttsErrorsImage.setOnClickListener {
                val category = adapter!!.words[binding.pager.currentItem].first.baseCategory
                val speechAvailability = checkSpeechAvailability(activity!!, ttsSupported, getCategoryLevel(category))
                when (speechAvailability) {
                    SpeechAvailability.NOT_AVAILABLE -> {
                        speechNotSupportedAlert(activity!!, getCategoryLevel(category), {})
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
        adapter = QuizItemPagerAdapter(context!!, this)
        binding.pager.adapter = adapter
        binding.pager.setAllowedSwipeDirection(SwipeDirection.none)
        binding.pager.offscreenPageLimit = 0
        binding.pager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {
            }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
            }

            override fun onPageSelected(position: Int) {
                binding.hiraganaEdit.isEnableConversion = adapter!!.words[position].first.isKana == 0
                holdOn = false
            }
        })
    }

    private fun initEditText() {
        binding.hiraganaEdit.setOnEditorActionListener { _, i, keyEvent ->
            if (isSettingsOpen) closeTTSSettings()
            if (adapter!!.words[binding.pager.currentItem].second == QuizType.TYPE_PRONUNCIATION
                && (i == EditorInfo.IME_ACTION_DONE || keyEvent?.keyCode == KeyEvent.KEYCODE_ENTER) && !holdOn) {
                // Validate Action
                holdOn = true
                binding.hiraganaEdit.setText(binding.hiraganaEdit.text.toString().replace("n", if (adapter!!.words[binding.pager.currentItem].first.isKana >= 1) "n" else "ん"))
                presenter.onAnswerGiven(binding.hiraganaEdit.text.toString().trim().replace(" ", "").replace("　", "").replace("\n", "")) // TODO add function clean utils
            }
            true
        }

        binding.hiraganaEdit.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                if (isSettingsOpen) closeTTSSettings()
                // Return to nuraml color when typing again (because it becomes Red or Green when
                // you validate
                currentEditColor = R.color.lighter_gray
                binding.hiraganaEdit.setTextColor(ContextCompat.getColor(activity!!, currentEditColor))
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

        })

        binding.editAction.setOnClickListener {
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
        binding.hiraganaEdit.setText("")
    }

    override fun displayEditAnswer(answer: String) {
        binding.hiraganaEdit.setText(answer)
        currentEditColor = R.color.level_master_4
        binding.hiraganaEdit.setTextColor(ContextCompat.getColor(activity!!, R.color.level_master_4))
        binding.hiraganaEdit.setSelection(binding.hiraganaEdit.text.length)
    }

    private fun setUpAudioManager() {
        val audioManager = activity!!.getSystemService(Context.AUDIO_SERVICE) as AudioManager
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
        val rate = defaultSharedPreferences.getInt(Prefs.TTS_RATE.pref, 50)
        binding.seekSpeed.progress = rate
        tts?.setSpeechRate((rate + 50).toFloat() / 100)
        binding.seekSpeed.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                defaultSharedPreferences.edit().putInt(Prefs.TTS_RATE.pref, p1).apply()
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
        binding.option1Container.setOnClickListener {
            if (isSettingsOpen) closeTTSSettings()
            if (!holdOn) {
                holdOn = true
                presenter.onOption1Click()

            }
        }
        binding.option2Container.setOnClickListener {
            if (isSettingsOpen) closeTTSSettings()
            if (!holdOn) {
                holdOn = true
                presenter.onOption2Click()

            }
        }
        binding.option3Container.setOnClickListener {
            if (isSettingsOpen) closeTTSSettings()
            if (!holdOn) {
                holdOn = true
                presenter.onOption3Click()

            }
        }
        binding.option4Container.setOnClickListener {
            if (isSettingsOpen) closeTTSSettings()
            if (!holdOn) {
                holdOn = true
                presenter.onOption4Click()

            }
        }
        binding.option1Tv.setOnClickListener {
            if (isSettingsOpen) closeTTSSettings()
            if (!holdOn) {
                holdOn = true
                presenter.onOption1Click()

            }
        }
        binding.option2Tv.setOnClickListener {
            if (isSettingsOpen) closeTTSSettings()
            if (!holdOn) {
                holdOn = true
                presenter.onOption2Click()

            }
        }
        binding.option3Tv.setOnClickListener {
            if (isSettingsOpen) closeTTSSettings()
            if (!holdOn) {
                holdOn = true
                presenter.onOption3Click()

            }
        }
        binding.option4Tv.setOnClickListener {
            if (isSettingsOpen) closeTTSSettings()
            if (!holdOn) {
                holdOn = true
                presenter.onOption4Click()

            }
        }
        binding.option1Tv.movementMethod = ScrollingMovementMethod()
        binding.option2Tv.movementMethod = ScrollingMovementMethod()
        binding.option3Tv.movementMethod = ScrollingMovementMethod()
        binding.option4Tv.movementMethod = ScrollingMovementMethod()
    }

    override fun reInitUI() {
        binding.hiraganaEdit.setText("")
        binding.editAction.setImageResource(R.drawable.ic_cancel_black_24dp)
        binding.editAction.setColorFilter(ContextCompat.getColor(activity!!, R.color.lighter_gray))
        binding.option1Tv.setTextColor(ContextCompat.getColor(activity!!, android.R.color.white))
        binding.option2Tv.setTextColor(ContextCompat.getColor(activity!!, android.R.color.white))
        binding.option3Tv.setTextColor(ContextCompat.getColor(activity!!, android.R.color.white))
        binding.option4Tv.setTextColor(ContextCompat.getColor(activity!!, android.R.color.white))
        binding.option1Furi.setTextColor(ContextCompat.getColor(activity!!, android.R.color.white))
        binding.option2Furi.setTextColor(ContextCompat.getColor(activity!!, android.R.color.white))
        binding.option3Furi.setTextColor(ContextCompat.getColor(activity!!, android.R.color.white))
        binding.option4Furi.setTextColor(ContextCompat.getColor(activity!!, android.R.color.white))
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
        binding.qcmContainer.visibility = GONE
        binding.answerContainer.visibility = GONE
        alert {
            message = getString(R.string.quiz_empty)
            okButton { activity!!.finish() }
            onCancelled { activity!!.finish() }
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
        binding.hiraganaEdit.setTextColor(ContextCompat.getColor(activity!!, color))
        binding.hiraganaEdit.setSelection(binding.hiraganaEdit.text.length)
    }

    override fun animateCheck(result: Boolean) {
        if (result) {
            binding.check.setImageResource(R.drawable.ic_check_black_48dp)
            binding.check.setColorFilter(ContextCompat.getColor(activity!!, R.color.level_master_4))
        } else {
            binding.check.setImageResource(R.drawable.ic_clear_black_48dp)
            binding.check.setColorFilter(ContextCompat.getColor(activity!!, R.color.level_low_1))
        }
        binding.check.animate().alpha(1f).setDuration(200).setStartDelay(0).setListener(object : Animator.AnimatorListener {
            override fun onAnimationRepeat(animation: Animator?) {
            }

            override fun onAnimationEnd(animation: Animator?) {
                binding.check.animate().alpha(0f).setDuration(300).setStartDelay(300).setListener(object : Animator.AnimatorListener {
                    override fun onAnimationRepeat(animation: Animator?) {
                    }

                    override fun onAnimationEnd(animation: Animator?) {
                        binding.check.visibility = View.GONE
                        if (result) {
                            presenter.onNextWord()
                        } else {
                            holdOn = false
                            displayEditDisplayAnswerButton()
                        }
                    }

                    override fun onAnimationCancel(animation: Animator?) {
                    }

                    override fun onAnimationStart(animation: Animator?) {
                    }

                }).start()
            }

            override fun onAnimationCancel(animation: Animator?) {
            }

            override fun onAnimationStart(animation: Animator?) {
                binding.check.visibility = View.VISIBLE
            }

        }).start()

    }

    override fun displayEditDisplayAnswerButton() {
        binding.editAction.setImageResource(R.drawable.ic_visibility_black_24dp)
        binding.editAction.setColorFilter(ContextCompat.getColor(activity!!, R.color.level_master_4))
    }

    override fun displayQCMMode() {
        binding.qcmContainer.visibility = View.VISIBLE
        binding.editContainer.visibility = View.GONE
    }

    override fun displayEditMode() {
        binding.qcmContainer.visibility = View.GONE
        binding.editContainer.visibility = View.VISIBLE
    }

    override fun displayQCMNormalTextViews() {
        binding.option1Tv.textSize = defaultSharedPreferences.getString("font_size", "18")!!.toFloat()
        binding.option2Tv.textSize = defaultSharedPreferences.getString("font_size", "18")!!.toFloat()
        binding.option3Tv.textSize = defaultSharedPreferences.getString("font_size", "18")!!.toFloat()
        binding.option4Tv.textSize = defaultSharedPreferences.getString("font_size", "18")!!.toFloat()
        binding.option1Tv.visibility = View.VISIBLE
        binding.option2Tv.visibility = View.VISIBLE
        binding.option3Tv.visibility = View.VISIBLE
        binding.option4Tv.visibility = View.VISIBLE
        binding.option1Furi.visibility = View.GONE
        binding.option2Furi.visibility = View.GONE
        binding.option3Furi.visibility = View.GONE
        binding.option4Furi.visibility = View.GONE
    }

    override fun displayQCMFuriTextViews() {
        binding.option1Tv.visibility = View.GONE
        binding.option2Tv.visibility = View.GONE
        binding.option3Tv.visibility = View.GONE
        binding.option4Tv.visibility = View.GONE
        binding.option1Furi.visibility = View.VISIBLE
        binding.option2Furi.visibility = View.VISIBLE
        binding.option3Furi.visibility = View.VISIBLE
        binding.option4Furi.visibility = View.VISIBLE
    }

    override fun displayQCMTv1(option: String, color: Int) {
        binding.option1Tv.text = option
        binding.option1Tv.textColor = ContextCompat.getColor(context!!, color)
    }

    override fun displayQCMTv2(option: String, color: Int) {
        binding.option2Tv.text = option
        binding.option2Tv.textColor = ContextCompat.getColor(context!!, color)
    }

    override fun displayQCMTv3(option: String, color: Int) {
        binding.option3Tv.text = option
        binding.option3Tv.textColor = ContextCompat.getColor(context!!, color)
    }

    override fun displayQCMTv4(option: String, color: Int) {
        binding.option4Tv.text = option
        binding.option4Tv.textColor = ContextCompat.getColor(context!!, color)
    }

    override fun displayQCMFuri1(optionFuri: String, start: Int, end: Int, color: Int) {
        binding.option1Furi.text_set(optionFuri, start, end, color)
    }

    override fun displayQCMFuri2(optionFuri: String, start: Int, end: Int, color: Int) {
        binding.option2Furi.text_set(optionFuri, start, end, color)
    }

    override fun displayQCMFuri3(optionFuri: String, start: Int, end: Int, color: Int) {
        binding.option3Furi.text_set(optionFuri, start, end, color)
    }

    override fun displayQCMFuri4(optionFuri: String, start: Int, end: Int, color: Int) {
        binding.option4Furi.text_set(optionFuri, start, end, color)
    }

    fun setOptionsFontSize(fontSize: Float) {
        binding.option1Tv.textSize = fontSize
        binding.option2Tv.textSize = fontSize
        binding.option3Tv.textSize = fontSize
        binding.option4Tv.textSize = fontSize
    }

    fun closeTTSSettings() {
        binding.settingsContainer.animate().setDuration(300).translationY(-400f).withEndAction {
            isSettingsOpen = false
            binding.settingsContainer.visibility = GONE
        }.start()
    }

    override fun showKeyboard() {
        val inputMethodManager = context!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.showSoftInput(binding.hiraganaEdit, InputMethodManager.SHOW_FORCED)
        binding.hiraganaEdit.requestFocus()
    }

    override fun hideKeyboard() {
        val inputMethodManager = context!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(binding.hiraganaEdit.windowToken, 0)
    }

    override fun animateColor(position: Int, word: Word, sentence: Sentence, quizType: QuizType, fromLevel: Int, toLevel: Int, fromPoints: Int, toPoints: Int) {
        val view = binding.pager.findViewWithTag<View>("pos_" + position)
        val btn_furi = view.findViewById<View>(R.id.btn_furi)
        val furi_sentence = view.findViewById<FuriganaView>(R.id.furi_sentence)
        val trad_sentence = view.findViewById<TextView>(R.id.trad_sentence)
        val sound = view.findViewById<ImageButton>(R.id.sound)
        val sentenceNoFuri = sentenceNoFuri(sentence)
        val colorAnimation = ValueAnimator.ofObject(ArgbEvaluator(),
            getWordColor(context!!, fromLevel, fromPoints),
            getWordColor(context!!, toLevel, toPoints))
        colorAnimation.addUpdateListener {
            animator ->
            run {
                when (quizType) {
                    QuizType.TYPE_PRONUNCIATION, QuizType.TYPE_PRONUNCIATION_QCM, QuizType.TYPE_JAP_EN -> {
                        val colorEntireWord = word.isKana == 2 && quizType == QuizType.TYPE_JAP_EN
                        val wordTruePosition = if (colorEntireWord) 0 else getWordPositionInFuriSentence(sentence.jap, word)
                        if (btn_furi.isSelected) {
                            if (colorEntireWord) 0 else wordTruePosition.let {
                                furi_sentence.text_set(
                                    if (colorEntireWord) sentence.jap else sentenceNoAnswerFuri(sentence, word),
                                    it,
                                    if (colorEntireWord) sentence.jap.length else wordTruePosition + word.japanese.length,
                                    animator.animatedValue as Int)
                            }
                        } else {
                            furi_sentence.text_set(
                                if (colorEntireWord) sentence.jap else sentenceNoFuri.replace("%", word.japanese),
                                (if (colorEntireWord) 0 else wordTruePosition),
                                if (colorEntireWord) sentence.jap.length else wordTruePosition + word.japanese.length,
                                animator.animatedValue as Int)
                        }
                    }
                    QuizType.TYPE_EN_JAP -> {
                        trad_sentence.textColor = animator.animatedValue as Int
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
        alert {
            val sessionLength = adapter!!.words.size
            message = getString(R.string.alert_session_finished, sessionLength)
            neutralPressed(getString(R.string.alert_continue)) { presenter.onLaunchNextProgressiveSession() }
            if (proposeErrors) {
                negativeButton(getString(R.string.alert_review_session_errors)) {
                    presenter.onLaunchErrorSession()
                }
            }
            positiveButton(getString(R.string.alert_quit)) { finishQuiz() }
            onCancelled { finishQuiz() }
        }.show()
    }

    override fun showAlertErrorSessionEnd(quizEnded: Boolean) {
        alert(getString(R.string.alert_error_review_finished)) {
            if (!quizEnded) {
                message = getString(R.string.alert_error_review_session_message)
                positiveButton(getString(R.string.alert_continue_quiz)) {
                    presenter.onContinueQuizAfterErrorSession()
                }
            } else {
                message = getString(R.string.alert_error_review_quiz_message)
                positiveButton(getString(R.string.alert_restart)) {
                    presenter.onRestartQuiz()
                }
            }
            neutralPressed(getString(R.string.alert_quit)) { finishQuiz() }
            onCancelled {
                presenter.onFinishQuiz()
            }
        }.show()
    }

    override fun showAlertNonProgressiveSessionEnd(proposeErrors: Boolean) {
        alert {
            message = getString(R.string.alert_session_finished, defaultSharedPreferences.getString("length", "-1")?.toInt())
            positiveButton(getString(R.string.alert_continue)) {
                presenter.onContinueAfterNonProgressiveSessionEnd()
            }
            if (proposeErrors) {
                negativeButton(getString(R.string.alert_review_session_errors)) {
                    // TODO handle shuffle ?
                    presenter.onLaunchErrorSession()
                }
            }
            neutralPressed(getString(R.string.alert_quit)) { presenter.onFinishQuiz() }
            onCancelled {
                presenter.onContinueAfterNonProgressiveSessionEnd()
            }
        }.show()
    }

    override fun showAlertQuizEnd(proposeErrors: Boolean) {
        alert {
            message = getString(R.string.alert_quiz_finished)
            neutralPressed(getString(R.string.alert_restart)) { presenter.onRestartQuiz() }
            if (proposeErrors) {
                negativeButton(getString(R.string.alert_review_quiz_errors)) {
                    presenter.onLaunchErrorSession()
                }
            }
            positiveButton(getString(R.string.alert_quit)) { finishQuiz() }
            onCancelled {
                finishQuiz()
            }
        }.show()
    }

    // TODO move to presenter ?
    /**
     * Selections
     */

    override fun selectionLoaded(quizzes: List<Quiz>) {
        selections = quizzes
    }

    override fun noSelections() {
        selections = emptyList()
    }

    /**
     * Actions
     */

    override fun setHiraganaConversion(enabled: Boolean) {
        binding.hiraganaEdit.isEnableConversion = enabled
    }

    override fun finishQuiz() {
        activity!!.finish()
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
                val speechAvailability = checkSpeechAvailability(activity!!, ttsSupported, getCategoryLevel(category))
                when (speechAvailability) {
                    SpeechAvailability.NOT_AVAILABLE -> {
                        speechNotSupportedAlert(activity!!, getCategoryLevel(category), {})
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
        val dialog = WordDetailDialogFragment().withArguments(
            Extras.EXTRA_WORD_ID to adapter!!.words[position].first.id,
            Extras.EXTRA_QUIZ_TYPE to if (presenter.hasMistaken()) null else adapter!!.words[position].second,
            Extras.EXTRA_SEARCH_STRING to "")
        dialog.show(childFragmentManager, "")
        dialog.isCancelable = true
    }

    override fun onSoundClick(button: ImageButton, position: Int) {
        presenter.onSpeakWordTTS()
    }

    override fun onSelectionClick(view: View, position: Int) {
        val popup = PopupMenu(activity, view)
        val word = adapter!!.words[position].first
        popup.menuInflater.inflate(R.menu.popup_selections, popup.menu)
        var i = 0
        for (selection in selections) {
            popup.menu.add(1, i, i, selection.getName()).isChecked = presenter.isWordInQuiz(word.id, selection.id)
            popup.menu.setGroupCheckable(1, true, false)
            i++
        }
        popup.setOnMenuItemClickListener {
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
        }
        popup.show()
    }

    override fun onReportClick(position: Int) {
        presenter.onReportClick(position)
    }

    override fun reportError(word: Word, sentence: Sentence) {
        reportError(activity!!, word, sentence)
    }

    override fun onFuriClick(position: Int, isSelected: Boolean) {
        presenter.setIsFuriDisplayed(isSelected)
    }

    override fun onSentenceTTSClick(position: Int) {
        presenter.onSpeakSentence()
    }

    override fun onTradClick(position: Int) {

    }

    fun unlockFullVersion() {
        adapter!!.notifyDataSetChanged()
    }

    private fun addSelection(wordId: Long) {
        alert {
            title = getString(R.string.new_selection)
            val input = EditText(activity)
            input.setSingleLine()
            input.hint = getString(R.string.selection_name)
            val container = FrameLayout(requireActivity())
            val params = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            params.leftMargin = DimensionHelper.getPixelFromDip(activity, 20)
            params.rightMargin = DimensionHelper.getPixelFromDip(activity, 20)
            input.layoutParams = params
            container.addView(input)
            customView = container
            okButton {
                val selectionId = presenter.createSelection(input.text.toString())
                presenter.addWordToSelection(wordId, selectionId)
                presenter.loadSelections()
            }
            cancelButton { }
        }.show()
    }

}
