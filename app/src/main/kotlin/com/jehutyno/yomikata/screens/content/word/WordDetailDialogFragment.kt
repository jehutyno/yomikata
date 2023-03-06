package com.jehutyno.yomikata.screens.content.word

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.fragment.app.DialogFragment
import androidx.viewpager.widget.ViewPager
import androidx.appcompat.widget.PopupMenu
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import com.github.salomonbrys.kodein.*
import com.github.salomonbrys.kodein.android.appKodein
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.managers.VoicesManager
import com.jehutyno.yomikata.model.KanjiSoloRadical
import com.jehutyno.yomikata.model.Quiz
import com.jehutyno.yomikata.model.Sentence
import com.jehutyno.yomikata.model.Word
import com.jehutyno.yomikata.util.*
import org.jetbrains.anko.cancelButton
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.okButton
import org.jetbrains.anko.support.v4.alert
import org.jetbrains.anko.uiThread

/**
 * Created by jehutyno on 08/10/2016.
 */
class WordDetailDialogFragment : DialogFragment(), WordContract.View, WordPagerAdapter.Callback, TextToSpeech.OnInitListener {

    private val injector = KodeinInjector()
    private val wordPresenter: WordContract.Presenter by injector.instance()
    private val voicesManager: VoicesManager by injector.instance()
    private lateinit var adapter: WordPagerAdapter
    private var wordId: Long = -1
    private var quizIds: LongArray? = null
    private var quizType: QuizType? = null
    private var wordPosition: Int = -1
    private var searchString: String = ""
    private var quizTitle: String? = ""
    private var level: Int = -1
    private lateinit var selections: List<Quiz>
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
            wordId = arguments!!.getLong(Extras.EXTRA_WORD_ID, -1L)
            quizType = arguments!!.getSerializable(Extras.EXTRA_QUIZ_TYPE) as QuizType?
            quizIds = arguments!!.getLongArray(Extras.EXTRA_QUIZ_IDS)
            quizTitle = arguments!!.getString(Extras.EXTRA_QUIZ_TITLE)
            wordPosition = arguments!!.getInt(Extras.EXTRA_WORD_POSITION)
            searchString = arguments!!.getString(Extras.EXTRA_SEARCH_STRING) ?: ""
            level = arguments!!.getInt(Extras.EXTRA_LEVEL)
        }

        if (savedInstanceState != null) {
            wordPosition = savedInstanceState.getInt("position")
        }

        val dialog = Dialog(activity!!, R.style.full_screen_dialog)
        dialog.setContentView(R.layout.dialog_word_detail)
        dialog.setCanceledOnTouchOutside(true)
        adapter = WordPagerAdapter(activity!!, quizType, this)
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

        injector.inject(Kodein {
            extend(appKodein())
            import(wordPresenterModule(this@WordDetailDialogFragment))
//            import(voicesManagerModule(activity))
            bind<WordContract.Presenter>() with provider { WordPresenter(instance(), instance(), instance(), instance(), instance()) }
            bind<VoicesManager>() with singleton { VoicesManager(activity!!) }
        })

        return dialog
    }

    fun setArrowDisplay(position: Int) {
        arrowRight.visibility = if (position >= adapter.count - 1 || adapter.count <= 1) View.INVISIBLE else View.VISIBLE
        arrowLeft.visibility = if (position == 0) View.INVISIBLE else View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        wordPresenter.start()
        if (quizIds != null && !quizIds!!.isEmpty())
            wordPresenter.loadWords(quizIds!!, level)
        else if (!searchString.isEmpty())
            (wordPresenter.searchWords(searchString))
        else if (wordId != -1L)
            wordPresenter.loadWord(wordId)
        wordPresenter.loadSelections()
    }

    override fun displayWords(words: List<Triple<Word, List<KanjiSoloRadical?>, Sentence>>) {
        adapter.replaceData(words)
        viewPager.currentItem = wordPosition
        setArrowDisplay(wordPosition)
    }

    override fun setPresenter(presenter: WordContract.Presenter) {

    }

    override fun onSelectionClick(view: View, position: Int) {
        val popup = PopupMenu(activity!!, view)
        popup.menuInflater.inflate(R.menu.popup_selections, popup.menu)
        var i = 0
        for (selection in selections) {
            popup.menu.add(1, i, i, selection.getName()).isChecked = wordPresenter.isWordInQuiz(adapter.words[position].first.id, selection.id)
            popup.menu.setGroupCheckable(1, true, false)
            i++
        }
        popup.setOnMenuItemClickListener {
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
        }
        popup.show()
    }

    private fun addSelection(wordId: Long) {
        alert {
            title = getString(R.string.new_selection)
            val input = EditText(activity)
            input.setSingleLine()
            input.hint = getString(R.string.selection_name)
            val container = FrameLayout(activity!!)
            val params = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            params.leftMargin = DimensionHelper.getPixelFromDip(activity, 20)
            params.rightMargin = DimensionHelper.getPixelFromDip(activity, 20)
            input.layoutParams = params
            container.addView(input)
            customView = container
            okButton {
                var selectionId = wordPresenter.createSelection(input.text.toString())
                wordPresenter.addWordToSelection(wordId, selectionId)
                wordPresenter.loadSelections()
            }
            cancelButton { }
        }.show()
    }

    override fun onReportClick(position: Int) {
        reportError(activity!!, adapter.words[position].first, adapter.words[position].third)
    }

    override fun onWordTTSClick(position: Int) {
        voicesManager.speakWord(adapter.words[position].first, ttsSupported, tts)
    }

    override fun onSentenceTTSClick(position: Int) {
        val sentence = adapter.words[position].third
        voicesManager.speakSentence(sentence, ttsSupported, tts)
    }

    override fun onLevelUp(position: Int) {
        val newLevel = wordPresenter.levelUp(adapter.words[position].first.id, adapter.words[position].first.level)
        waitAndUpdateLevel(position, if (newLevel == 4) 3 else newLevel, if (newLevel == 4) 100 else adapter.words[position].first.points)
    }

    override fun onLevelDown(position: Int) {
        val newLevel = wordPresenter.levelDown(adapter.words[position].first.id, adapter.words[position].first.level)
        waitAndUpdateLevel(position, newLevel, 0)
    }

    override fun onCloseClick() {
        dialog?.dismiss()
    }

    fun waitAndUpdateLevel(position: Int, newLevel: Int, points: Int) {
        if (newLevel != adapter.words[position].first.level) {
            adapter.words[position].first.level = newLevel
        }
        adapter.words[position].first.points = points
        doAsync {
            Thread.sleep(300)
            uiThread {
                adapter.notifyDataSetChanged()
            }
        }
    }

    override fun selectionLoaded(quizzes: List<Quiz>) {
        selections = quizzes
    }

    override fun noSelections() {
        selections = emptyList()
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