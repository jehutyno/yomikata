package com.jehutyno.yomikata.screens.quizzes

import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.coroutineScope
import androidx.preference.PreferenceManager
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.databinding.FragmentQuizzesBinding
import com.jehutyno.yomikata.model.Quiz
import com.jehutyno.yomikata.screens.content.ContentActivity
import com.jehutyno.yomikata.screens.quiz.QuizActivity
import com.jehutyno.yomikata.util.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.kodein.di.DI
import org.kodein.di.instance
import org.kodein.di.newInstance
import splitties.alertdialog.appcompat.*
import java.lang.Thread.sleep


/**
 * Created by valentin on 30/09/2016.
 */
class QuizzesFragment(di: DI) : Fragment(), QuizzesContract.View, QuizzesAdapter.Callback, TextToSpeech.OnInitListener {

    // kodein
    private val mpresenter: QuizzesContract.Presenter by di.newInstance {
        QuizzesPresenter(instance(), instance(), instance(), this@QuizzesFragment, selectedCategory)
    }

    val REQUEST_TUTO: Int = 55
    private lateinit var adapter: QuizzesAdapter
    private var selectedCategory: Int = 0
    private var tts: TextToSpeech? = null
    private var ttsSupported: Int = TextToSpeech.LANG_NOT_SUPPORTED

    // seekBars
    private lateinit var seekBars : SeekBarsManager

    // View Binding
    private var _binding: FragmentQuizzesBinding? = null
    private val binding get() = _binding!!


    override fun setPresenter(presenter: QuizzesContract.Presenter) {
        // no need in this case since this is only used to set a reference to the mpresenter
        // that is explicitly created in this class already.
        // if this method is ever called because QuizzesPresenter was initialized from some
        // other activity/fragment, then this should be set (see ContentFragment for an example)
//        mpresenter = presenter
    }

    override fun onInit(status: Int) {
        ttsSupported = onTTSinit(context, status, tts)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // make sure selectedCategory is set before mpresenter is used to properly initialize with kodein
        selectedCategory = requireArguments().getInt(Extras.EXTRA_CATEGORY)
        adapter = QuizzesAdapter(requireActivity(), selectedCategory, this, selectedCategory == Categories.CATEGORY_SELECTIONS)
    }

    override fun onStart() {
        // use onStart so that viewPager2 can set everything up before the page becomes visible
        super.onStart()
        mpresenter.start()
        subscribeDisplayQuizzes()
    }

    override fun onResume() {
        super.onResume()
        val position = (binding.recyclerview.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
        mpresenter.start()
        seekBars.animateAll()    // call this after loadQuizzes, since seekBars variables are set there
        binding.recyclerview.scrollToPosition(position)
        tutos()

        // check if voices downloads have changed (e.g. voices files have been deleted in preferences)
        updateVoicesDownloadVisibility()
    }

    override fun onPause() {
        super.onPause()

        // cancel animation in case it is currently running
        seekBars.cancelAll()

        // set all to zero to prepare for the next animation when the page resumes again
        binding.seekLow.progress = 0
        binding.seekMedium.progress = 0
        binding.seekHigh.progress = 0
        binding.seekMaster.progress = 0
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentQuizzesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tts = TextToSpeech(activity, this)
        binding.recyclerview.let {
            it.adapter = adapter
            it.layoutManager = LinearLayoutManager(context)
        }

        mpresenter.initQuizTypes()

        binding.btnPronunciationQcmSwitch.setOnClickListener {
            mpresenter.pronunciationQcmSwitch()
            spotlightTuto(requireActivity(), binding.btnPronunciationQcmSwitch, getString(R.string.tutos_pronunciation_mcq), getString(R.string.tutos_pronunciation_mcq_message)
            ) { }
        }
        binding.btnPronunciationSwitch.setOnClickListener {
            mpresenter.pronunciationSwitch()
            spotlightTuto(requireActivity(), binding.btnPronunciationSwitch, getString(R.string.tutos_pronunciation_quiz), getString(R.string.tutos_pronunciation_quiz_message)
            ) { }
        }
        binding.btnAudioSwitch.setOnClickListener {
            spotlightTuto(requireActivity(), binding.btnAudioSwitch, getString(R.string.tutos_audio_quiz), getString(R.string.tutos_audio_quiz_message)
            ) { }
            when (checkSpeechAvailability(requireActivity(), ttsSupported, getCategoryLevel(selectedCategory))) {
                SpeechAvailability.NOT_AVAILABLE -> speechNotSupportedAlert(requireActivity(), getCategoryLevel(selectedCategory)) { (activity as QuizzesActivity).quizzesAdapter.notifyDataSetChanged() }
                else -> mpresenter.audioSwitch()
            }
        }
        binding.btnEnJapSwitch.setOnClickListener {
            mpresenter.enJapSwitch()
            spotlightTuto(requireActivity(), binding.btnEnJapSwitch, getString(R.string.tutos_en_jp), getString(R.string.tutos_en_jp_message)
            ) { }
        }
        binding.btnJapEnSwitch.setOnClickListener {
            mpresenter.japEnSwitch()
            spotlightTuto(requireActivity(), binding.btnJapEnSwitch, getString(R.string.tutos_jp_en), getString(R.string.tutos_jp_en_message)
            ) { }
        }
        binding.btnAutoSwitch.setOnClickListener {
            mpresenter.autoSwitch()
            spotlightTuto(requireActivity(), binding.btnAutoSwitch, getString(R.string.tutos_auto_quiz), getString(R.string.tutos_auto_quiz_message)
            ) { }
        }

        binding.playLow.setOnClickListener {
            openContent(selectedCategory, 0)
        }
        binding.playMedium.setOnClickListener {
            openContent(selectedCategory, 1)
        }
        binding.playHigh.setOnClickListener {
            openContent(selectedCategory, 2)
        }
        binding.playMaster.setOnClickListener {
            openContent(selectedCategory, 3)
        }

        updateVoicesDownloadVisibility()

        binding.download.setOnClickListener {
            requireContext().alertDialog {
                if (getLevelDownloadVersion(getCategoryLevel(selectedCategory)) > 0 && previousVoicesDownloaded(getLevelDownloadVersion(getCategoryLevel(selectedCategory)))) {
                    titleResource = R.string.update_voices_alert
                    message = getString(R.string.update_voices_alert_message, getLevelDownloadSize(getCategoryLevel(selectedCategory)))
                } else {
                    titleResource = R.string.download_voices_alert
                    message = getString(R.string.download_voices_alert_message, getLevelDownloadSize(getCategoryLevel(selectedCategory)))
                }
                okButton {
                    (activity as QuizzesActivity).voicesDownload(getCategoryLevel(selectedCategory)) {
                        binding.download.visibility = GONE
                    }
                }
                cancelButton { }
            }.show()
        }

        // initialize seekBarsManager
        seekBars = SeekBarsManager(binding.seekLow, binding.seekMedium, binding.seekHigh, binding.seekMaster)
    }

    private fun updateVoicesDownloadVisibility() {
        val pref = PreferenceManager.getDefaultSharedPreferences(requireContext())
        if (selectedCategory == -1 || selectedCategory == 8
                || pref.getBoolean(Prefs.VOICE_DOWNLOADED_LEVEL_V.pref +
                        "${getLevelDownloadVersion(getCategoryLevel(selectedCategory))}_${getCategoryLevel(selectedCategory)}", false)) {
            binding.download.visibility = GONE
        } else {
            binding.download.visibility = VISIBLE
            if (getLevelDownloadVersion(getCategoryLevel(selectedCategory)) > 0 && previousVoicesDownloaded(getLevelDownloadVersion(getCategoryLevel(selectedCategory)))) {
                binding.download.text = getString(R.string.update_voices, getLevelDownloadSize(getCategoryLevel(selectedCategory)))
            } else {
                binding.download.text = getString(R.string.download_voices, getLevelDownloadSize(getCategoryLevel(selectedCategory)))
            }
        }
    }

    private fun previousVoicesDownloaded(downloadVersion: Int): Boolean {
        val pref = PreferenceManager.getDefaultSharedPreferences(requireContext())
        return (0 until downloadVersion).any {
            pref.getBoolean("${Prefs.VOICE_DOWNLOADED_LEVEL_V.pref}${it}_${getCategoryLevel(selectedCategory)}", false)
        }
    }

    override fun selectPronunciationQcm(isSelected: Boolean) {
        binding.btnPronunciationQcmSwitch.isSelected = isSelected
    }

    override fun selectPronunciation(isSelected: Boolean) {
        binding.btnPronunciationSwitch.isSelected = isSelected
    }

    override fun selectAudio(isSelected: Boolean) {
        binding.btnAudioSwitch.isSelected = isSelected
    }

    override fun selectEnJap(isSelected: Boolean) {
        binding.btnEnJapSwitch.isSelected = isSelected
    }

    override fun selectJapEn(isSelected: Boolean) {
        binding.btnJapEnSwitch.isSelected = isSelected
    }

    override fun selectAuto(isSelected: Boolean) {
        binding.btnAutoSwitch.isSelected = isSelected
    }

    override fun launchQuiz(strategy: QuizStrategy, selectedTypes: IntArray, title: String) {
        val ids = mutableListOf<Long>()
        adapter.items.forEach {
            if (it.isSelected)
                ids.add(it.id)
        }

        //If nothing selected, use all quizzes
        if (ids.size == 0 || strategy == QuizStrategy.LOW_STRAIGHT || strategy == QuizStrategy.MEDIUM_STRAIGHT
            || strategy == QuizStrategy.HIGH_STRAIGHT || strategy == QuizStrategy.MASTER_STRAIGHT) {
            ids.clear()
            adapter.items.forEach {
                ids.add(it.id)
            }
        }

        // No quiz to launch
        if (ids.size == 0 || runBlocking {mpresenter.countQuiz(ids.toLongArray())} <= 0) {
            val toast = Toast.makeText(context, R.string.error_no_quiz_no_word, Toast.LENGTH_SHORT)
            toast.show()
            return
        }

        val intent = Intent(activity, QuizActivity::class.java).apply {
            putExtra(Extras.EXTRA_QUIZ_IDS, ids.toLongArray())
            putExtra(Extras.EXTRA_QUIZ_TITLE, title)
            putExtra(Extras.EXTRA_QUIZ_STRATEGY, strategy)
            putExtra(Extras.EXTRA_QUIZ_TYPES, selectedTypes)
        }
        startActivity(intent)
    }

    fun launchQuizClick(strategy: QuizStrategy, title: String) = runBlocking {
        mpresenter.launchQuizClick(strategy, title, selectedCategory)
    }

    private fun subscribeDisplayQuizzes() {
        mpresenter.quizList.observe(viewLifecycleOwner) {
            displayQuizzes(it)
            seekBars.animateAll()
        }
    }

    override fun displayQuizzes(quizzes: List<Quiz>) {
        binding.recyclerview.visibility
        adapter.replaceData(quizzes, selectedCategory == Categories.CATEGORY_SELECTIONS)
        binding.recyclerview.scrollToPosition(0)

        val ids = arrayListOf<Long>()
        quizzes.forEach {
            ids.add(it.id)
        }

        lifecycle.coroutineScope.launch {
            seekBars.count = mpresenter.countQuiz(ids.toLongArray())
            seekBars.low = mpresenter.countLow(ids.toLongArray())
            seekBars.medium = mpresenter.countMedium(ids.toLongArray())
            seekBars.high = mpresenter.countHigh(ids.toLongArray())
            seekBars.master = mpresenter.countMaster(ids.toLongArray())

            binding.textLow.text = seekBars.low.toString()
            binding.textMedium.text = seekBars.medium.toString()
            binding.textHigh.text = seekBars.high.toString()
            binding.textMaster.text = seekBars.master.toString()

            binding.playLow.visibility = if (seekBars.low > 0) VISIBLE else INVISIBLE
            binding.playMedium.visibility = if (seekBars.medium > 0) VISIBLE else INVISIBLE
            binding.playHigh.visibility = if (seekBars.high > 0) VISIBLE else INVISIBLE
            binding.playMaster.visibility = if (seekBars.master > 0) VISIBLE else INVISIBLE
        }
    }

    private fun openContent(position: Int, level: Int) {
        if ((selectedCategory == Categories.CATEGORY_SELECTIONS)) {
            // TODO: ?
        } else {

        }
        val intent = Intent(context, ContentActivity::class.java).apply {
            putExtra(Extras.EXTRA_CATEGORY, selectedCategory)
            putExtra(Extras.EXTRA_QUIZ_POSITION, position)
            putExtra(Extras.EXTRA_QUIZ_TYPES, mpresenter.getSelectedTypes())
            putExtra(Extras.EXTRA_LEVEL, level)
        }
        startActivity(intent)
    }

    override fun displayNoData() {
        adapter.noData(selectedCategory == Categories.CATEGORY_SELECTIONS)
        animateSeekBar(binding.seekLow, 0, 0, 0)
        binding.textLow.text = 0.toString()
        animateSeekBar(binding.seekMedium, 0, 0, 0)
        binding.textMedium.text = 0.toString()
        animateSeekBar(binding.seekHigh, 0, 0, 0)
        binding.textHigh.text = 0.toString()
        animateSeekBar(binding.seekMaster, 0, 0, 0)
        binding.textMaster.text = 0.toString()
    }

    override fun onItemClick(position: Int) {
        openContent(position, -1)
    }

    override fun onItemChecked(position: Int, checked: Boolean) = runBlocking {
        mpresenter.updateQuizCheck(adapter.items[position].id, checked)

    }

    override fun onItemLongClick(position: Int) {
        if (selectedCategory != Categories.CATEGORY_SELECTIONS) {
            // TODO propose to add all words to selections
            return
        }
        val input = EditText(activity)
        input.setSingleLine()
        input.hint = getString(R.string.selection_name)
        input.setText(adapter.items[position].getName())

        val container = FrameLayout(requireActivity())
        val params = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        params.leftMargin = DimensionHelper.getPixelFromDip(activity, 20)
        params.rightMargin = DimensionHelper.getPixelFromDip(activity, 20)
        input.layoutParams = params

        input.addTextChangedListener(object : TextValidator(input) {
            override fun validate(textView: TextView, text: String) {
                if (text.isEmpty())
                    input.error = getString(R.string.selection_not_empty_name)
                else
                    input.error = null
            }
        })
        container.addView(input)

        requireContext().alertDialog {
            titleResource = R.string.selection_edit
            setView(container)

            neutralButton(R.string.action_delete) {
                requireContext().alertDialog {
                    titleResource = R.string.selection_delete_sure
                    okButton {
                        runBlocking {
                            mpresenter.deleteQuiz(adapter.items[position].id)
                            adapter.deleteItem(position)
                        }
                    }
                    cancelButton { }
                }.show()
            }
            okButton {
                if (adapter.items[position].getName() != input.text.toString() && input.error == null) {
                    runBlocking {
                        mpresenter.updateQuizName(adapter.items[position].id, input.text.toString())
                    }
                    adapter.items[position].nameFr = input.text.toString()
                    adapter.items[position].nameEn = input.text.toString()
                    adapter.notifyItemChanged(position)
                }
            }
            cancelButton { }
        }.show()
    }

    override fun addSelection() {
        val input = EditText(activity)
        input.setSingleLine()
        input.inputType = InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        input.hint = getString(R.string.selection_name)

        val container = FrameLayout(requireActivity())
        val params = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        params.leftMargin = DimensionHelper.getPixelFromDip(activity, 20)
        params.rightMargin = DimensionHelper.getPixelFromDip(activity, 20)
        input.layoutParams = params

        input.addTextChangedListener(object : TextValidator(input) {
            override fun validate(textView: TextView, text: String) {
                if (text.isEmpty()) {
                    input.error = getString(R.string.selection_not_empty_name)
                } else {
                    input.error = null
                }
            }
        })
        input.error = getString(R.string.selection_not_empty_name)
        container.addView(input)

        requireContext().alertDialog {
            titleResource = R.string.new_selection
            setView(container)

            okButton {
                if (input.error == null) {
                    runBlocking {
                        mpresenter.createQuiz(input.text.toString())
                    }
                }
            }
            cancelButton { }
        }.show()
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun tutos() = lifecycle.coroutineScope.launch {
        withContext(IO) {
            sleep(500)
        }
        if (activity != null) {
            spotlightTuto(requireActivity(), binding.btnPronunciationQcmSwitch, getString(R.string.tuto_quiz_type), getString(R.string.tuto_quiz_type_message)
            ) {
                if (activity != null) {
                    spotlightTuto(requireActivity(),
                        binding.textLow,
                        getString(R.string.tuto_progress),
                        getString(R.string.tuto_progress_message)
                    ) {
                        if (activity != null) {
                            spotlightTuto(requireActivity(),
                                binding.recyclerview.findViewHolderForAdapterPosition(0)?.itemView?.findViewById(
                                    R.id.quiz_check
                                ),
                                getString(R.string.tuto_part_selection),
                                getString(R.string.tuto_part_selection_message)
                            ) { }
                        }
                    }
                }
            }
        }
    }
}
