package com.jehutyno.yomikata.screens.word

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.View
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.databinding.DialogWordDetailBinding
import com.jehutyno.yomikata.managers.VoicesManager
import com.jehutyno.yomikata.model.KanjiSoloRadical
import com.jehutyno.yomikata.model.Sentence
import com.jehutyno.yomikata.model.Word
import com.jehutyno.yomikata.util.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.kodein.di.*
import splitties.alertdialog.appcompat.*


/**
 * Created by jehutyno on 08/10/2016.
 */
class WordDetailDialogFragment(private val di: DI) : DialogFragment(), WordContract.View,
    WordPagerAdapter.Callback, TextToSpeech.OnInitListener {

    // kodein
    private val subDI = DI.lazy {
        extend(di)
//            import(voicesManagerModule(activity))
        bind<WordContract.Presenter>() with provider {
            WordPresenter (
                instance(), instance(), instance(),
                instance(arg = lifecycleScope), instance(),
                quizIds, level, searchString
            )
        }
        bind<VoicesManager>() with singleton { VoicesManager(requireActivity()) }
    }
    @Suppress("unused")
    private val wordPresenter: WordContract.Presenter by subDI.instance()
    @Suppress("unused")
    private val voicesManager: VoicesManager by subDI.instance()

    private lateinit var adapter: WordPagerAdapter
    private var locked: Boolean = true     // if locked -> don't adapt to database changes
    private var initialLoad: Boolean = false    // used if locked = true to make sure initial load happened
    private var wordId: Long = -1
    private var quizIds: LongArray? = null
    private var quizType: QuizType? = null
    private var wordPosition: Int = -1
    private var searchString: String = ""
    private var quizTitle: String? = ""
    private var level: Level? = null

    private var tts: TextToSpeech? = null
    private var ttsSupported: Int = TextToSpeech.LANG_NOT_SUPPORTED

    // View Binding
    private var _binding: DialogWordDetailBinding? = null
    private val binding get () = _binding!!


    override fun onInit(status: Int) {
        if (activity != null)
            ttsSupported = onTTSinit(activity, status, tts)
    }

    override fun onStart() {
        super.onStart()
        // safety check
        if (dialog == null)
            return

        val dialogWidth = DimensionHelper.getScreenWidth(activity) - DimensionHelper.getPixelFromDip(activity, 20)
        val dialogHeight = DimensionHelper.getScreenHeight(activity) - DimensionHelper.getPixelFromDip(activity, 60)
        dialog?.window?.setLayout(dialogWidth, dialogHeight)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("position", binding.viewpagerWords.currentItem)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // set binding here instead of in onCreateView, since onCreateDialog is called first
        _binding = DialogWordDetailBinding.inflate(layoutInflater)

        tts = TextToSpeech(activity, this)
        if (arguments != null) {
            wordId = requireArguments().getLong(Extras.EXTRA_WORD_ID, -1L)

            quizType = requireArguments().getSerializableHelper(Extras.EXTRA_QUIZ_TYPE, QuizType::class.java)

            quizIds = requireArguments().getLongArray(Extras.EXTRA_QUIZ_IDS)
            quizTitle = requireArguments().getString(Extras.EXTRA_QUIZ_TITLE)
            wordPosition = requireArguments().getInt(Extras.EXTRA_WORD_POSITION)
            searchString = requireArguments().getString(Extras.EXTRA_SEARCH_STRING) ?: ""
            level = requireArguments().getSerializableHelper(Extras.EXTRA_LEVEL, Level::class.java)
        }

        if (savedInstanceState != null) {
            wordPosition = savedInstanceState.getInt("position")
        }

        val dialog = Dialog(requireActivity(), R.style.full_screen_dialog)
        dialog.setContentView(binding.root)
        dialog.setCanceledOnTouchOutside(true)
        adapter = WordPagerAdapter(this, lifecycleScope, quizType, this, wordPresenter)
        binding.viewpagerWords.adapter = adapter
        binding.viewpagerWords.registerOnPageChangeCallback(
            object: ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    wordPosition = position
                    setArrowDisplay(position)
                }
            }
        )

        binding.arrowLeft.setOnClickListener { binding.viewpagerWords.setCurrentItem(binding.viewpagerWords.currentItem - 1, true) }
        binding.arrowRight.setOnClickListener { binding.viewpagerWords.setCurrentItem(binding.viewpagerWords.currentItem + 1, true) }

        return dialog
    }

    fun setArrowDisplay(position: Int) {
        binding.arrowRight.visibility = if (position >= adapter.count - 1 || adapter.count <= 1) View.INVISIBLE else View.VISIBLE
        binding.arrowLeft.visibility = if (position == 0) View.INVISIBLE else View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        wordPresenter.start()
        wordPresenter.words?.let {
            it.observe(this) { words ->
                if (initialLoad && locked)
                    return@observe
                lifecycleScope.launch {
                    displayWords(words)
                }
                initialLoad = true
            }
        }
        if (wordPresenter.words == null) {
            lifecycleScope.launch {
                val oneWordList = listOf(wordPresenter.getWordById(wordId))
                displayWords(oneWordList)
            }
        }
    }

    @Synchronized
    override fun displayWords(words: List<Word>) {
        adapter.replaceData(words)
        binding.viewpagerWords.currentItem = wordPosition
        setArrowDisplay(wordPosition)
    }

    override fun onSelectionClick(view: View, word: Word) = runBlocking {
        val selections = wordPresenter.getSelections()
        val popup = PopupMenu(requireActivity(), view)
        popup.menuInflater.inflate(R.menu.popup_selections, popup.menu)
        for ((i, selection) in selections.withIndex()) {
            popup.menu.add(1, i, i, selection.getName()).isChecked = wordPresenter.isWordInQuiz(word.id, selection.id)
            popup.menu.setGroupCheckable(1, true, false)
        }
        popup.setOnMenuItemClickListener { runBlocking {
            when (it.itemId) {
                R.id.add_selection -> addSelection(word.id)
                else -> {
                    if (!it.isChecked)
                        wordPresenter.addWordToSelection(word.id, selections[it.itemId].id)
                    else {
                        wordPresenter.deleteWordFromSelection(word.id, selections[it.itemId].id)
                    }
                    it.isChecked = !it.isChecked
                }
            }
            true
        }}
        popup.show()
    }

    private fun addSelection(wordId: Long) {
        requireActivity().createNewSelectionDialog("", { selectionName ->
            lifecycleScope.launch {
                val selectionId = wordPresenter.createSelection(selectionName)
                wordPresenter.addWordToSelection(wordId, selectionId)
            }
        }, null)
    }

    override fun onReportClick(wordKanjiSentence: Triple<Word, List<KanjiSoloRadical?>, Sentence>) {
        reportError(requireActivity(), wordKanjiSentence.first, wordKanjiSentence.third)
    }

    override fun onWordTTSClick(word: Word) {
        voicesManager.speakWord(word, ttsSupported, tts)
    }

    override fun onSentenceTTSClick(sentence: Sentence) {
        voicesManager.speakSentence(sentence, ttsSupported, tts)
    }

    override fun onLevelUp(word: MutableLiveData<Word>) = runBlocking {
        wordPresenter.levelUp(word.value!!.id, word.value!!.points)
        updateAdapterPointsAndLevel(word, levelUp(word.value!!.points))
    }

    override fun onLevelDown(word: MutableLiveData<Word>) = runBlocking {
        wordPresenter.levelDown(word.value!!.id, word.value!!.points)
        updateAdapterPointsAndLevel(word, levelDown(word.value!!.points))
    }

    override fun onCloseClick() {
        dialog?.dismiss()
    }

    private fun updateAdapterPointsAndLevel(word: MutableLiveData<Word>, points: Int) {
        word.value = word.value!!.copy(points = points, level = getLevelFromPoints(points))
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        val parentFragment = parentFragment
        if (parentFragment is DialogInterface.OnDismissListener) {
            parentFragment.onDismiss(dialog)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
