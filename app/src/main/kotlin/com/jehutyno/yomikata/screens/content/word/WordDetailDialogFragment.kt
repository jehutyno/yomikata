package com.jehutyno.yomikata.screens.content.word

import android.app.Dialog
import android.content.DialogInterface
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager.widget.ViewPager
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.managers.VoicesManager
import com.jehutyno.yomikata.model.KanjiSoloRadical
import com.jehutyno.yomikata.model.Sentence
import com.jehutyno.yomikata.model.Word
import com.jehutyno.yomikata.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.kodein.di.*
import splitties.alertdialog.appcompat.*


/**
 * Created by jehutyno on 08/10/2016.
 */
class WordDetailDialogFragment(private val di: DI) : DialogFragment(), WordContract.View, WordPagerAdapter.Callback, TextToSpeech.OnInitListener {

    // kodein
    private val subDI = DI.lazy {
        extend(di)
        import(wordPresenterModule(this@WordDetailDialogFragment))
//            import(voicesManagerModule(activity))
        bind<WordContract.Presenter>() with provider {
            WordPresenter(instance(), instance(), instance(), instance(), instance(),
                            lifecycleScope, quizIds, level, searchString)
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
    private var level: Int = -1
    private lateinit var viewPager: ViewPager
    private lateinit var arrowLeft: ImageView
    private lateinit var arrowRight: ImageView

    private var tts: TextToSpeech? = null
    private var ttsSupported: Int = TextToSpeech.LANG_NOT_SUPPORTED

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
        outState.putInt("position", viewPager.currentItem)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        tts = TextToSpeech(activity, this)
        if (arguments != null) {
            wordId = requireArguments().getLong(Extras.EXTRA_WORD_ID, -1L)

            quizType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requireArguments().getSerializable(Extras.EXTRA_QUIZ_TYPE, QuizType::class.java)
            }
            else {
                @Suppress("DEPRECATION")
                requireArguments().getSerializable(Extras.EXTRA_QUIZ_TYPE) as QuizType?
            }

            quizIds = requireArguments().getLongArray(Extras.EXTRA_QUIZ_IDS)
            quizTitle = requireArguments().getString(Extras.EXTRA_QUIZ_TITLE)
            wordPosition = requireArguments().getInt(Extras.EXTRA_WORD_POSITION)
            searchString = requireArguments().getString(Extras.EXTRA_SEARCH_STRING) ?: ""
            level = requireArguments().getInt(Extras.EXTRA_LEVEL)
        }

        if (savedInstanceState != null) {
            wordPosition = savedInstanceState.getInt("position")
        }

        val dialog = Dialog(requireActivity(), R.style.full_screen_dialog)
        dialog.setContentView(R.layout.dialog_word_detail)
        dialog.setCanceledOnTouchOutside(true)
        adapter = WordPagerAdapter(requireActivity(), quizType, this)
        viewPager = dialog.findViewById(R.id.viewpager_words)
        arrowLeft = dialog.findViewById(R.id.arrow_left)
        arrowRight = dialog.findViewById(R.id.arrow_right)
        viewPager.adapter = adapter
        with(viewPager) {
            addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
                override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                }

                override fun onPageSelected(position: Int) {
                    wordPosition = position
                    setArrowDisplay(position)
                }

                override fun onPageScrollStateChanged(state: Int) {
                }
            })
        }

        arrowLeft.setOnClickListener { viewPager.setCurrentItem(viewPager.currentItem - 1, true) }
        arrowRight.setOnClickListener { viewPager.setCurrentItem(viewPager.currentItem + 1, true) }

        return dialog
    }

    fun setArrowDisplay(position: Int) {
        arrowRight.visibility = if (position >= adapter.count - 1 || adapter.count <= 1) View.INVISIBLE else View.VISIBLE
        arrowLeft.visibility = if (position == 0) View.INVISIBLE else View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        wordPresenter.start()
        wordPresenter.words?.let {
            it.observe(this) { words ->
                if (initialLoad && locked)
                    return@observe
                lifecycleScope.launch {
                    displayWords(wordPresenter.getWordKanjiSoloRadicalSentenceList(words))
                }
                initialLoad = true
            }
        }
        if (wordPresenter.words == null) {
            lifecycleScope.launch {
                val oneWordList = listOf(wordPresenter.getWord(wordId))
                displayWords(wordPresenter.getWordKanjiSoloRadicalSentenceList(oneWordList))
            }
        }
    }

    @Synchronized
    override fun displayWords(words: List<Triple<Word, List<KanjiSoloRadical?>, Sentence>>) {
        adapter.replaceData(words)
        viewPager.currentItem = wordPosition
        setArrowDisplay(wordPosition)
    }

    override fun setPresenter(presenter: WordContract.Presenter) {

    }

    override fun onSelectionClick(view: View, position: Int) = runBlocking {
        val selections = wordPresenter.getSelections()
        val popup = PopupMenu(requireActivity(), view)
        popup.menuInflater.inflate(R.menu.popup_selections, popup.menu)
        for ((i, selection) in selections.withIndex()) {
            popup.menu.add(1, i, i, selection.getName()).isChecked = wordPresenter.isWordInQuiz(adapter.words[position].first.id, selection.id)
            popup.menu.setGroupCheckable(1, true, false)
        }
        popup.setOnMenuItemClickListener { runBlocking {
            when (it.itemId) {
                R.id.add_selection -> addSelection(adapter.words[position].first.id)
                else -> {
                    if (!it.isChecked)
                        wordPresenter.addWordToSelection(adapter.words[position].first.id, selections[it.itemId].id)
                    else {
                        wordPresenter.deleteWordFromSelection(adapter.words[position].first.id, selections[it.itemId].id)
                    }
                    it.isChecked = !it.isChecked
                }
            }
            true
        }}
        popup.show()
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
                    val selectionId = wordPresenter.createSelection(input.text.toString())
                    wordPresenter.addWordToSelection(wordId, selectionId)
                }
            }
            cancelButton { }
        }.show()
    }

    override fun onReportClick(position: Int) {
        reportError(requireActivity(), adapter.words[position].first, adapter.words[position].third)
    }

    override fun onWordTTSClick(position: Int) {
        voicesManager.speakWord(adapter.words[position].first, ttsSupported, tts)
    }

    override fun onSentenceTTSClick(position: Int) {
        val sentence = adapter.words[position].third
        voicesManager.speakSentence(sentence, ttsSupported, tts)
    }

    override fun onLevelUp(position: Int) = runBlocking {
        val newLevel = wordPresenter.levelUp(adapter.words[position].first.id, adapter.words[position].first.level)
        waitAndUpdateLevel(position, if (newLevel == 4) 3 else newLevel, if (newLevel == 4) 100 else adapter.words[position].first.points)
    }

    override fun onLevelDown(position: Int) = runBlocking {
        val newLevel = wordPresenter.levelDown(adapter.words[position].first.id, adapter.words[position].first.level)
        waitAndUpdateLevel(position, newLevel, 0)
    }

    override fun onCloseClick() {
        dialog?.dismiss()
    }

    private fun waitAndUpdateLevel(position: Int, newLevel: Int, points: Int) {
        if (newLevel != adapter.words[position].first.level) {
            adapter.words[position].first.level = newLevel
        }
        adapter.words[position].first.points = points
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                Thread.sleep(300)
            }
            withContext(Dispatchers.Main) {
                adapter.notifyDataSetChanged()
            }
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        val parentFragment = parentFragment
        if (parentFragment is DialogInterface.OnDismissListener) {
            parentFragment.onDismiss(dialog)
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}