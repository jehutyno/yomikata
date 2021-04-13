package com.jehutyno.yomikata.screens.content.word

import android.app.Activity
import android.content.Context
import androidx.viewpager.widget.PagerAdapter
import androidx.appcompat.widget.PopupMenu
import android.view.LayoutInflater
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import android.widget.Toast
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.model.KanjiSoloRadical
import com.jehutyno.yomikata.model.Sentence
import com.jehutyno.yomikata.model.Word
import com.jehutyno.yomikata.model.getWordColor
import com.jehutyno.yomikata.util.*
import kotlinx.android.synthetic.main.vh_kanji_solo.view.*
import kotlinx.android.synthetic.main.vh_word_detail.view.*
import org.jetbrains.anko.defaultSharedPreferences
import java.util.*

/**
 * Created by jehutyno on 08/10/2016.
 */
class WordPagerAdapter(val activity: Activity, var quizType: QuizType?, var callback: Callback) : PagerAdapter() {

    var words: ArrayList<Triple<Word, List<KanjiSoloRadical?>, Sentence>> = arrayListOf()

    override fun isViewFromObject(view: View, any: Any): Boolean {
        return view == any
    }

    override fun getCount(): Int {
        return words.count()
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val view = LayoutInflater.from(container.context).inflate(R.layout.vh_word_detail, container, false)
        val word = words[position]
        var wordDisplay =
        if (quizType != null) word.first.japanese else "    {${word.first.japanese};${word.first.reading}}    "
        view.level_down.visibility = if (quizType != null) GONE else VISIBLE
        view.level_up.visibility = if (quizType != null) GONE else VISIBLE
        wordDisplay?.let { view.furi_word.text_set(wordDisplay, 0, it.length, getWordColor(container.context, word.first.level, word.first.points)) }
        view.text_traduction.text = word.first.getTrad()
        val sentenceNoFuri = sentenceNoFuri(word.third)
        val wordTruePosition = word.third.jap?.let { getWordPositionInFuriSentence(it, word.first) }
        wordTruePosition?.let {
            view.furi_sentence.text_set(
                if (quizType == null) word.third.jap else sentenceNoAnswerFuri(word.third, word.first),
                it,
                wordTruePosition + word.first.japanese!!.length,
                getWordColor(activity, word.first.level, word.first.points))
        }
        view.text_traduction.visibility = if (quizType == QuizType.TYPE_JAP_EN || word.first.isKana == 2) INVISIBLE else VISIBLE
        view.text_sentence_en.text = word.third.getTrad()

        view.btn_selection.setOnClickListener { callback.onSelectionClick(view.btn_selection, position) }
        view.btn_report.setOnClickListener { callback.onReportClick(position) }
        view.btn_sentence_tts.setOnClickListener { callback.onSentenceTTSClick(position) }
        view.btn_tts.setOnClickListener { callback.onWordTTSClick(position) }
        view.btn_copy.setOnClickListener {
            val popup = PopupMenu(activity, view.btn_copy)
            popup.menuInflater.inflate(R.menu.popup_copy, popup.menu)
            popup.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.copy_word -> {
                        val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText(
                            activity.getString(R.string.copy_word),
                            words[position].first.japanese)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(activity, activity.getString(R.string.word_copied), Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText(
                            activity.getString(R.string.copy_sentence),
                            sentenceNoFuri(words[position].third))
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(activity, activity.getString(R.string.sentence_copied), Toast.LENGTH_SHORT).show()
                    }
                }
                true
            }
            popup.show()
        }
        view.level_down.setOnClickListener { callback.onLevelDown(position) }
        view.level_up.setOnClickListener { callback.onLevelUp(position) }
        view.close.setOnClickListener { callback.onCloseClick() }

        view.container_info.visibility = if (word.second.isEmpty() || word.second[0] == null) GONE else VISIBLE

        var displaySeparator = false
        word.second.forEach {
            if (it != null) {
                val radicalLayout = LayoutInflater.from(container.context).inflate(R.layout.vh_kanji_solo, view.container_info, false)
                radicalLayout.separator.visibility = if (displaySeparator) VISIBLE else GONE
                radicalLayout.kanji_solo.text = it.kanji
                radicalLayout.ks_trad.text = it.getTrad()
                radicalLayout.ks_strokes.text = it.strokes.toString()
                radicalLayout.kunyomi_title.visibility = if (it.kunyomi.isEmpty() || quizType != null) GONE else VISIBLE
                radicalLayout.ks_kunyomi.visibility = if (it.kunyomi.isEmpty() || quizType != null) GONE else VISIBLE
                radicalLayout.ks_kunyomi.text = it.kunyomi
                radicalLayout.onyomi_title.visibility = if (it.onyomi.isEmpty() || quizType != null) GONE else VISIBLE
                radicalLayout.ks_onyomi.visibility = if (it.onyomi.isEmpty() || quizType != null) GONE else VISIBLE
                radicalLayout.ks_onyomi.text = it.onyomi
                radicalLayout.radical.text = it.radical
                radicalLayout.radical_stroke.text = it.radStroke.toString()
                radicalLayout.radical_trad.text = it.getRadTrad()
                view.container_info.addView(radicalLayout)
                displaySeparator = true
            }
        }

        container.addView(view)
        return view
    }

    fun replaceData(list: List<Triple<Word, List<KanjiSoloRadical?>, Sentence>>) {
        words.clear()
        words.addAll(list)
        notifyDataSetChanged()
    }

    override fun getItemPosition(`object`: Any): Int {
        return POSITION_NONE
    }

    override fun destroyItem(collection: ViewGroup, position: Int, view: Any) {
        collection.removeView(view as View)
    }

    interface Callback {
        fun onSelectionClick(view: View, position: Int)
        fun onReportClick(position: Int)
        fun onWordTTSClick(position: Int)
        fun onSentenceTTSClick(position: Int)
        fun onLevelUp(position: Int)
        fun onLevelDown(position: Int)
        fun onCloseClick()
    }
}