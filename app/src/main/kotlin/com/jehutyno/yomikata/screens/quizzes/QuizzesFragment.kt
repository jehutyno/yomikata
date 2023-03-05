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
import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.databinding.FragmentQuizzesBinding
import com.jehutyno.yomikata.model.Quiz
import com.jehutyno.yomikata.screens.content.ContentActivity
import com.jehutyno.yomikata.screens.quiz.QuizActivity
import com.jehutyno.yomikata.util.*
import com.wooplr.spotlight.utils.SpotlightListener
import org.jetbrains.anko.*
import org.jetbrains.anko.support.v4.alert
import org.jetbrains.anko.support.v4.defaultSharedPreferences
import org.jetbrains.anko.support.v4.toast
import java.lang.Thread.sleep


/**
 * Created by valentin on 30/09/2016.
 */
class QuizzesFragment : Fragment(), QuizzesContract.View, QuizzesAdapter.Callback, TextToSpeech.OnInitListener {

    val REQUEST_TUTO: Int = 55
    private var mpresenter: QuizzesContract.Presenter? = null
    private lateinit var adapter: QuizzesAdapter
    private var selectedCategory: Int = 0
    private var tts: TextToSpeech? = null
    private var ttsSupported: Int = TextToSpeech.LANG_NOT_SUPPORTED

    // View Binding
    private var _binding: FragmentQuizzesBinding? = null
    private val binding get() = _binding!!


    override fun setPresenter(presenter: QuizzesContract.Presenter) {
        mpresenter = presenter
    }

    override fun onInit(status: Int) {
        ttsSupported = onTTSinit(context, status, tts)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        selectedCategory = arguments!!.getInt(Extras.EXTRA_CATEGORY)
        adapter = QuizzesAdapter(activity!!, selectedCategory, this, selectedCategory == Categories.CATEGORY_SELECTIONS)
    }

    override fun onResume() {
        super.onResume()
        val position = (binding.recyclerview.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
        mpresenter!!.start()
        mpresenter!!.loadQuizzes(selectedCategory)
        binding.recyclerview.scrollToPosition(position)
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

        if (mpresenter == null)
            mpresenter = QuizzesPresenter(activity!!.appKodein.invoke().instance(), context!!.appKodein.invoke().instance(), context!!.appKodein.invoke().instance(), this)
        mpresenter!!.initQuizTypes()

        binding.btnPronunciationQcmSwitch.setOnClickListener {
            mpresenter!!.pronunciationQcmSwitch()
            spotlightTuto(activity!!, binding.btnPronunciationQcmSwitch, getString(R.string.tutos_pronunciation_mcq), getString(R.string.tutos_pronunciation_mcq_message), SpotlightListener { })
        }
        binding.btnPronunciationSwitch.setOnClickListener {
            mpresenter!!.pronunciationSwitch()
            spotlightTuto(activity!!, binding.btnPronunciationSwitch, getString(R.string.tutos_pronunciation_quiz), getString(R.string.tutos_pronunciation_quiz_message), SpotlightListener { })
        }
        binding.btnAudioSwitch.setOnClickListener {
            spotlightTuto(activity!!, binding.btnAudioSwitch, getString(R.string.tutos_audio_quiz), getString(R.string.tutos_audio_quiz_message), SpotlightListener { })
            val speechAvailability = checkSpeechAvailability(activity!!, ttsSupported, getCateogryLevel(selectedCategory))
            when (speechAvailability) {
                SpeechAvailability.NOT_AVAILABLE -> speechNotSupportedAlert(activity!!, getCateogryLevel(selectedCategory), { (activity as QuizzesActivity).quizzesAdapter.notifyDataSetChanged() })
                else -> mpresenter!!.audioSwitch()
            }
        }
        binding.btnEnJapSwitch.setOnClickListener {
            mpresenter!!.enJapSwitch()
            spotlightTuto(activity!!, binding.btnEnJapSwitch, getString(R.string.tutos_en_jp), getString(R.string.tutos_en_jp_message), SpotlightListener { })
        }
        binding.btnJapEnSwitch.setOnClickListener {
            mpresenter!!.japEnSwitch()
            spotlightTuto(activity!!, binding.btnJapEnSwitch, getString(R.string.tutos_jp_en), getString(R.string.tutos_jp_en_message), SpotlightListener { })
        }
        binding.btnAutoSwitch.setOnClickListener {
            mpresenter!!.autoSwitch()
            spotlightTuto(activity!!, binding.btnAutoSwitch, getString(R.string.tutos_auto_quiz), getString(R.string.tutos_auto_quiz_message), SpotlightListener { })
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

        if (selectedCategory == -1 || selectedCategory == 8
            || defaultSharedPreferences.getBoolean(Prefs.VOICE_DOWNLOADED_LEVEL_V.pref +
            "${getLevelDownloadVersion(getCateogryLevel(selectedCategory))}_${getCateogryLevel(selectedCategory)}", false)) {
            binding.download.visibility = GONE
        } else {
            binding.download.visibility = VISIBLE
            if (getLevelDownloadVersion(getCateogryLevel(selectedCategory)) > 0 && previousVoicesDownloaded(getLevelDownloadVersion(getCateogryLevel(selectedCategory)))) {
                binding.download.text = getString(R.string.update_voices, getLevelDonwloadSize(getCateogryLevel(selectedCategory)))
            } else {
                binding.download.text = getString(R.string.download_voices, getLevelDonwloadSize(getCateogryLevel(selectedCategory)))
            }
        }

        binding.download.setOnClickListener {
            alert {
                if (getLevelDownloadVersion(getCateogryLevel(selectedCategory)) > 0 && previousVoicesDownloaded(getLevelDownloadVersion(getCateogryLevel(selectedCategory)))) {
                    titleResource = R.string.update_voices_alert
                    message = getString(R.string.update_voices_alert_message, getLevelDonwloadSize(getCateogryLevel(selectedCategory)))
                } else {
                    titleResource = R.string.download_voices_alert
                    message = getString(R.string.download_voices_alert_message, getLevelDonwloadSize(getCateogryLevel(selectedCategory)))
                }
                okButton { (activity as QuizzesActivity).voicesDownload(getCateogryLevel(selectedCategory)) }
                cancelButton { }
            }.show()
        }
    }

    fun previousVoicesDownloaded(downloadVersion: Int): Boolean {
        return (0 until downloadVersion).any {
            defaultSharedPreferences.getBoolean("${Prefs.VOICE_DOWNLOADED_LEVEL_V.pref}${it}_${getCateogryLevel(selectedCategory)}", false)
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
        var ids = mutableListOf<Long>()
        adapter.items.forEach {
            if (it.isSelected == 1)
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
        if (ids.size == 0 || mpresenter!!.countQuiz(ids.toLongArray()) <= 0) {
            toast(getString(R.string.error_no_quiz_no_word))
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

    fun launchQuizClick(strategy: QuizStrategy, title: String) {
        mpresenter!!.launchQuizClick(strategy, title, selectedCategory)
    }

    override fun displayQuizzes(quizzes: List<Quiz>) {
        binding.recyclerview.visibility
        adapter.replaceData(quizzes, selectedCategory == Categories.CATEGORY_SELECTIONS)
        binding.recyclerview.scrollToPosition(0)

        var ids = arrayListOf<Long>()
        quizzes.forEach {
            ids.add(it.id)
        }

        val count = mpresenter!!.countQuiz(ids.toLongArray())
        val low = mpresenter!!.countLow(ids.toLongArray())
        val medium = mpresenter!!.countMedium(ids.toLongArray())
        val high = mpresenter!!.countHigh(ids.toLongArray())
        val master = mpresenter!!.countMaster(ids.toLongArray())
        animateSeekBar(binding.seekLow, 0, low, count)
        binding.textLow.text = low.toString()
        animateSeekBar(binding.seekMedium, 0, medium, count)
        binding.textMedium.text = medium.toString()
        animateSeekBar(binding.seekHigh, 0, high, count)
        binding.textHigh.text = high.toString()
        animateSeekBar(binding.seekMaster, 0, master, count)
        binding.textMaster.text = master.toString()

        binding.playLow.visibility = if (low > 0) VISIBLE else INVISIBLE
        binding.playMedium.visibility = if (medium > 0) VISIBLE else INVISIBLE
        binding.playHigh.visibility = if (high > 0) VISIBLE else INVISIBLE
        binding.playMaster.visibility = if (master > 0) VISIBLE else INVISIBLE
    }

    fun openContent(position: Int, level: Int) {
        if ((selectedCategory == Categories.CATEGORY_SELECTIONS)) {

        } else {
            val intent = Intent(context, ContentActivity::class.java).apply {
                putExtra(Extras.EXTRA_CATEGORY, selectedCategory)
                putExtra(Extras.EXTRA_QUIZ_POSITION, position)
                putExtra(Extras.EXTRA_QUIZ_TYPES, mpresenter!!.getSelectedTypes())
                putExtra(Extras.EXTRA_LEVEL, level)
            }
            startActivity(intent)
        }
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

    override fun onMenuItemClick(category: Int) {
        selectedCategory = category
        mpresenter!!.loadQuizzes(selectedCategory)
    }

    override fun onItemClick(position: Int) {
        openContent(position, -1)
    }

    override fun onItemChecked(position: Int, checked: Boolean) {
        mpresenter!!.updateQuizCheck(adapter.items[position].id, checked)

    }

    override fun onItemLongClick(position: Int) {
        if (selectedCategory == Categories.CATEGORY_SELECTIONS) {
            alert {
                title = getString(R.string.selection_edit)
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
                customView = container

                neutralPressed(R.string.action_delete) {
                    alert {
                        title = getString(R.string.selection_delete_sure)
                        okButton {
                            mpresenter!!.deleteQuiz(adapter.items[position].id)
                            adapter.deleteItem(position)
                        }
                        cancelButton { }
                    }.show()
                }
                okButton {
                    if (adapter.items[position].getName() != input.text.toString() && input.error == null) {
                        mpresenter!!.updateQuizName(adapter.items[position].id, input.text.toString())
                        adapter.items[position].nameFr = input.text.toString()
                        adapter.items[position].nameEn = input.text.toString()
                        adapter.notifyItemChanged(position)
                    }
                }
                cancelButton { }
            }.show()
        } else {
            // TODO propose to add all words to selections
        }
    }

    override fun addSelection() {
        alert {
            title = getString(R.string.new_selection)
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
            customView = container
            okButton {
                if (input.error == null) {
                    mpresenter!!.createQuiz(input.text.toString())
                    mpresenter!!.loadQuizzes(selectedCategory)
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

    fun tutos() {
        doAsync {
            sleep(500)
            uiThread {
                if (activity != null) {
                    spotlightTuto(activity!!, binding.btnPronunciationQcmSwitch, getString(R.string.tuto_quiz_type), getString(R.string.tuto_quiz_type_message),
                        SpotlightListener {
                            if (activity != null) {
                                spotlightTuto(activity!!, binding.textLow, getString(R.string.tuto_progress), getString(R.string.tuto_progress_message),
                                    SpotlightListener {
                                        if (activity != null) {
                                            spotlightTuto(activity!!, binding.recyclerview.findViewHolderForAdapterPosition(0)?.itemView?.find<View>(R.id.quiz_check), getString(R.string.tuto_part_selection), getString(R.string.tuto_part_selection_message),
                                                SpotlightListener {})
                                        }
                                    })
                            }
                        })
                }
            }
        }
    }
}