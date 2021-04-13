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
import com.jehutyno.yomikata.furigana.FuriganaView
import com.jehutyno.yomikata.managers.VoicesManager
import com.jehutyno.yomikata.model.*
import com.jehutyno.yomikata.screens.answers.AnswersActivity
import com.jehutyno.yomikata.screens.content.word.WordDetailDialogFragment
import com.jehutyno.yomikata.util.*
import com.jehutyno.yomikata.view.SwipeDirection
import kotlinx.android.synthetic.main.fragment_quiz.*
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

    override fun onInit(status: Int) {
        if (adapter != null && !adapter!!.words.isEmpty()) {
            ttsSupported = onTTSinit(activity, status, tts)
            presenter.setTTSSupported(ttsSupported)
            if (adapter!!.words[pager.currentItem].second == QuizType.TYPE_AUDIO ||
                defaultSharedPreferences.getBoolean("play_start", false)) {
                voicesManager.speakWord(adapter!!.words[pager.currentItem].first, ttsSupported, tts)
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
        hiragana_edit.inputType = if (defaultSharedPreferences.getBoolean("input_change", false))
            InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        else
            InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        presenter.start()
        presenter.loadSelections()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("edit", hiragana_edit.text.toString())
        outState.putInt("edit_color", currentEditColor)
        presenter.onSaveInstanceState(outState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_quiz, container, false)
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
            hiragana_edit.setText(savedInstanceState.getString("edit"))
            savedInstanceState.getString("edit")?.let { hiragana_edit.setSelection(it.length) }
            hiragana_edit.setTextColor(ContextCompat.getColor(activity!!, currentEditColor))
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
                val category = adapter!!.words[pager.currentItem].first.baseCategory
                val speechAvailability = checkSpeechAvailability(activity!!, ttsSupported, getCateogryLevel(category))
                when (speechAvailability) {
                    SpeechAvailability.NOT_AVAILABLE -> {
                        speechNotSupportedAlert(activity!!, getCateogryLevel(category), {})
                    }
                    else -> {
                        if (isSettingsOpen) {
                            closeTTSSettings()
                        } else {
                            if (speechAvailability == SpeechAvailability.VOICES_AVAILABLE) {
                                settings_speed.visibility = GONE
                                seek_speed.visibility = GONE
                            } else {
                                settings_speed.visibility = VISIBLE
                                seek_speed.visibility = VISIBLE
                            }
                            settings_container.animate().setDuration(300).translationY(0f).withStartAction {
                                settings_container.visibility = VISIBLE
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

    fun initUI() {
        initPager()
        initEditText()
        setUpAudioManager()
        initAnswersButtons()
    }

    fun initPager() {
        adapter = QuizItemPagerAdapter(context!!, this)
        pager.adapter = adapter
        pager.setAllowedSwipeDirection(SwipeDirection.none)
        pager.offscreenPageLimit = 0
        pager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {
            }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
            }

            override fun onPageSelected(position: Int) {
                hiragana_edit.isEnableConversion = adapter!!.words[position].first.isKana == 0
                holdOn = false
            }
        })
    }

    fun initEditText() {
        hiragana_edit.setOnEditorActionListener { _, i, keyEvent ->
            if (isSettingsOpen) closeTTSSettings()
            if (adapter!!.words[pager.currentItem].second == QuizType.TYPE_PRONUNCIATION
                && (i == EditorInfo.IME_ACTION_DONE || keyEvent?.keyCode == KeyEvent.KEYCODE_ENTER) && !holdOn) {
                // Validate Action
                holdOn = true
                hiragana_edit.setText(hiragana_edit.text.toString().replace("n", if (adapter!!.words[pager.currentItem].first.isKana >= 1) "n" else "ん"))
                presenter.onAnswerGiven(hiragana_edit.text.toString().trim().replace(" ", "").replace("　", "").replace("\n", "")) // TODO add function clean utils
            }
            true
        }

        hiragana_edit.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                if (isSettingsOpen) closeTTSSettings()
                // Return to nuraml color when typing again (because it becomes Red or Green when
                // you validate
                currentEditColor = R.color.lighter_gray
                hiragana_edit.setTextColor(ContextCompat.getColor(activity!!, currentEditColor))
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

        })

        edit_action.setOnClickListener {
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
        hiragana_edit.setText("")
    }

    override fun displayEditAnswer(answer: String) {
        hiragana_edit.setText(answer)
        currentEditColor = R.color.level_master_4
        hiragana_edit.setTextColor(ContextCompat.getColor(activity!!, R.color.level_master_4))
        hiragana_edit.setSelection(hiragana_edit.text.length)
    }

    fun setUpAudioManager() {
        val audioManager = activity!!.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        seek_volume.max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        seek_volume.progress = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        seek_volume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, p1, 0)
                presenter.onSpeakSentence()
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
            }

        })

        seek_speed.max = 250
        val rate = defaultSharedPreferences.getInt(Prefs.TTS_RATE.pref, 50)
        seek_speed.progress = rate
        tts?.setSpeechRate((rate + 50).toFloat() / 100)
        seek_speed.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
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
        settings_container.translationY = -400f
        settings_close.setOnClickListener {
            closeTTSSettings()
        }
    }

    fun initAnswersButtons() {
        quiz_container.setOnClickListener { if (isSettingsOpen) closeTTSSettings() }
        answer_container.setOnClickListener { if (isSettingsOpen) closeTTSSettings() }
        option_1_container.setOnClickListener {
            if (isSettingsOpen) closeTTSSettings()
            if (!holdOn) {
                holdOn = true
                presenter.onOption1Click()

            }
        }
        option_2_container.setOnClickListener {
            if (isSettingsOpen) closeTTSSettings()
            if (!holdOn) {
                holdOn = true
                presenter.onOption2Click()

            }
        }
        option_3_container.setOnClickListener {
            if (isSettingsOpen) closeTTSSettings()
            if (!holdOn) {
                holdOn = true
                presenter.onOption3Click()

            }
        }
        option_4_container.setOnClickListener {
            if (isSettingsOpen) closeTTSSettings()
            if (!holdOn) {
                holdOn = true
                presenter.onOption4Click()

            }
        }
        option_1_tv.setOnClickListener {
            if (isSettingsOpen) closeTTSSettings()
            if (!holdOn) {
                holdOn = true
                presenter.onOption1Click()

            }
        }
        option_2_tv.setOnClickListener {
            if (isSettingsOpen) closeTTSSettings()
            if (!holdOn) {
                holdOn = true
                presenter.onOption2Click()

            }
        }
        option_3_tv.setOnClickListener {
            if (isSettingsOpen) closeTTSSettings()
            if (!holdOn) {
                holdOn = true
                presenter.onOption3Click()

            }
        }
        option_4_tv.setOnClickListener {
            if (isSettingsOpen) closeTTSSettings()
            if (!holdOn) {
                holdOn = true
                presenter.onOption4Click()

            }
        }
        option_1_tv.movementMethod = ScrollingMovementMethod()
        option_2_tv.movementMethod = ScrollingMovementMethod()
        option_3_tv.movementMethod = ScrollingMovementMethod()
        option_4_tv.movementMethod = ScrollingMovementMethod()
    }

    override fun reInitUI() {
        hiragana_edit.setText("")
        edit_action.setImageResource(R.drawable.ic_cancel_black_24dp)
        edit_action.setColorFilter(ContextCompat.getColor(activity!!, R.color.lighter_gray))
        option_1_tv.setTextColor(ContextCompat.getColor(activity!!, android.R.color.white))
        option_2_tv.setTextColor(ContextCompat.getColor(activity!!, android.R.color.white))
        option_3_tv.setTextColor(ContextCompat.getColor(activity!!, android.R.color.white))
        option_4_tv.setTextColor(ContextCompat.getColor(activity!!, android.R.color.white))
        option_1_furi.setTextColor(ContextCompat.getColor(activity!!, android.R.color.white))
        option_2_furi.setTextColor(ContextCompat.getColor(activity!!, android.R.color.white))
        option_3_furi.setTextColor(ContextCompat.getColor(activity!!, android.R.color.white))
        option_4_furi.setTextColor(ContextCompat.getColor(activity!!, android.R.color.white))
    }

    /**
     * Graphical methods
     */

    override fun displayWords(quizWordsPair: List<Pair<Word, QuizType>>) {
        holdOn = false
        adapter!!.replaceData(quizWordsPair)
        // TODO do somthing with that
//        pager.post { tutos() }
    }

    override fun noWords() {
        qcm_container.visibility = GONE
        answer_container.visibility = GONE
        alert {
            message = getString(R.string.quiz_empty)
            okButton { activity!!.finish() }
            onCancelled { activity!!.finish() }
        }.show()
    }

    override fun setPagerPosition(position: Int) {
        pager.currentItem = position
    }

    override fun setSentence(sentence: Sentence) {
        adapter!!.replaceSentence(sentence)
        adapter!!.notifyDataSetChanged()
    }

    override fun setEditTextColor(color: Int) {
        hiragana_edit.setTextColor(ContextCompat.getColor(activity!!, color))
        hiragana_edit.setSelection(hiragana_edit.text.length)
    }

    override fun animateCheck(result: Boolean) {
        if (result) {
            check.setImageResource(R.drawable.ic_check_black_48dp)
            check.setColorFilter(ContextCompat.getColor(activity!!, R.color.level_master_4))
        } else {
            check.setImageResource(R.drawable.ic_clear_black_48dp)
            check.setColorFilter(ContextCompat.getColor(activity!!, R.color.level_low_1))
        }
        check.animate().alpha(1f).setDuration(200).setStartDelay(0).setListener(object : Animator.AnimatorListener {
            override fun onAnimationRepeat(animation: Animator?) {
            }

            override fun onAnimationEnd(animation: Animator?) {
                check.animate().alpha(0f).setDuration(300).setStartDelay(300).setListener(object : Animator.AnimatorListener {
                    override fun onAnimationRepeat(animation: Animator?) {
                    }

                    override fun onAnimationEnd(animation: Animator?) {
                        if (check != null)
                            check.visibility = View.GONE
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
                check.visibility = View.VISIBLE
            }

        }).start()

    }

    override fun displayEditDisplayAnswerButton() {
        edit_action.setImageResource(R.drawable.ic_visibility_black_24dp)
        edit_action.setColorFilter(ContextCompat.getColor(activity!!, R.color.level_master_4))
    }

    override fun displayQCMMode() {
        qcm_container.visibility = View.VISIBLE
        edit_container.visibility = View.GONE
    }

    override fun displayEditMode() {
        qcm_container.visibility = View.GONE
        edit_container.visibility = View.VISIBLE
    }

    override fun displayQCMNormalTextViews() {
        option_1_tv.textSize = defaultSharedPreferences.getString("font_size", "18")!!.toFloat()
        option_2_tv.textSize = defaultSharedPreferences.getString("font_size", "18")!!.toFloat()
        option_3_tv.textSize = defaultSharedPreferences.getString("font_size", "18")!!.toFloat()
        option_4_tv.textSize = defaultSharedPreferences.getString("font_size", "18")!!.toFloat()
        option_1_tv.visibility = View.VISIBLE
        option_2_tv.visibility = View.VISIBLE
        option_3_tv.visibility = View.VISIBLE
        option_4_tv.visibility = View.VISIBLE
        option_1_furi.visibility = View.GONE
        option_2_furi.visibility = View.GONE
        option_3_furi.visibility = View.GONE
        option_4_furi.visibility = View.GONE
    }

    override fun displayQCMFuriTextViews() {
        option_1_tv.visibility = View.GONE
        option_2_tv.visibility = View.GONE
        option_3_tv.visibility = View.GONE
        option_4_tv.visibility = View.GONE
        option_1_furi.visibility = View.VISIBLE
        option_2_furi.visibility = View.VISIBLE
        option_3_furi.visibility = View.VISIBLE
        option_4_furi.visibility = View.VISIBLE
    }

    override fun displayQCMTv1(option: String, color: Int) {
        option_1_tv.text = option
        option_1_tv.textColor = ContextCompat.getColor(context!!, color)
    }

    override fun displayQCMTv2(option: String, color: Int) {
        option_2_tv.text = option
        option_2_tv.textColor = ContextCompat.getColor(context!!, color)
    }

    override fun displayQCMTv3(option: String, color: Int) {
        option_3_tv.text = option
        option_3_tv.textColor = ContextCompat.getColor(context!!, color)
    }

    override fun displayQCMTv4(option: String, color: Int) {
        option_4_tv.text = option
        option_4_tv.textColor = ContextCompat.getColor(context!!, color)
    }

    override fun displayQCMFuri1(optionFuri: String, start: Int, end: Int, color: Int) {
        option_1_furi.text_set(optionFuri, start, end, color)
    }

    override fun displayQCMFuri2(optionFuri: String, start: Int, end: Int, color: Int) {
        option_2_furi.text_set(optionFuri, start, end, color)
    }

    override fun displayQCMFuri3(optionFuri: String, start: Int, end: Int, color: Int) {
        option_3_furi.text_set(optionFuri, start, end, color)
    }

    override fun displayQCMFuri4(optionFuri: String, start: Int, end: Int, color: Int) {
        option_4_furi.text_set(optionFuri, start, end, color)
    }

    fun setOptionsFontSize(fontSize: Float) {
        option_1_tv.textSize = fontSize
        option_2_tv.textSize = fontSize
        option_3_tv.textSize = fontSize
        option_4_tv.textSize = fontSize
    }

    fun closeTTSSettings() {
        settings_container.animate().setDuration(300).translationY(-400f).withEndAction {
            isSettingsOpen = false
            settings_container.visibility = GONE
        }.start()
    }

    override fun showKeyboard() {
        val inputMethodManager = context!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.showSoftInput(hiragana_edit, InputMethodManager.SHOW_FORCED)
        hiragana_edit.requestFocus()
    }

    override fun hideKeyboard() {
        val inputMethodManager = context!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(hiragana_edit.windowToken, 0)
    }

    override fun animateColor(position: Int, word: Word, sentence: Sentence, quizType: QuizType, fromLevel: Int, toLevel: Int, fromPoints: Int, toPoints: Int) {
        val view = pager.findViewWithTag<View>("pos_" + position)
        val btn_furi = view.findViewById<View>(R.id.btn_furi)
        val furi_sentence = view.findViewById<FuriganaView>(R.id.furi_sentence)
        val trad_sentence = view.findViewById<TextView>(R.id.trad_sentence)
        val sound = view.findViewById<ImageButton>(R.id.sound)
        var sentenceNoFuri = sentenceNoFuri(sentence)
        val colorAnimation = ValueAnimator.ofObject(ArgbEvaluator(),
            getWordColor(context!!, fromLevel, fromPoints),
            getWordColor(context!!, toLevel, toPoints))
        colorAnimation.addUpdateListener {
            animator ->
            run {
                when (quizType) {
                    QuizType.TYPE_PRONUNCIATION, QuizType.TYPE_PRONUNCIATION_QCM, QuizType.TYPE_JAP_EN -> {
                        val colorEntireWord = word.isKana == 2 && quizType == QuizType.TYPE_JAP_EN
                        val wordTruePosition = if (colorEntireWord) 0 else sentence.jap?.let { getWordPositionInFuriSentence(it, word) }
                        if (btn_furi.isSelected) {
                            if (colorEntireWord) 0 else wordTruePosition?.let {
                                furi_sentence.text_set(
                                    if (colorEntireWord) sentence.jap else sentenceNoAnswerFuri(sentence, word),
                                    it,
                                    if (colorEntireWord) sentence.jap!!.length else wordTruePosition + word.japanese!!.length,
                                    animator.animatedValue as Int)
                            }
                        } else {
                            furi_sentence.text_set(
                                if (colorEntireWord) sentence.jap else sentenceNoFuri.replace("%", word.japanese!!),
                                (if (colorEntireWord) 0 else wordTruePosition)!!,
                                if (colorEntireWord) sentence.jap!!.length else wordTruePosition!! + word.japanese!!.length,
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
        hiragana_edit.isEnableConversion = enabled
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
                val category = adapter!!.words[pager.currentItem].first.baseCategory
                val speechAvailability = checkSpeechAvailability(activity!!, ttsSupported, getCateogryLevel(category))
                when (speechAvailability) {
                    SpeechAvailability.NOT_AVAILABLE -> {
                        speechNotSupportedAlert(activity!!, getCateogryLevel(category), {})
                    }
                    else -> {
                        if (isSettingsOpen) {
                            closeTTSSettings()
                        } else {
                            if (speechAvailability == SpeechAvailability.VOICES_AVAILABLE) {
                                settings_speed.visibility = GONE
                                seek_speed.visibility = GONE
                            } else {
                                settings_speed.visibility = VISIBLE
                                seek_speed.visibility = VISIBLE
                            }
                            settings_container.animate().setDuration(300).translationY(0f).withStartAction {
                                settings_container.visibility = VISIBLE
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
