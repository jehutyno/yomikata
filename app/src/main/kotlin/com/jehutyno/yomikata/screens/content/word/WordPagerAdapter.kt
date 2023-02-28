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
import com.jehutyno.yomikata.databinding.VhKanjiSoloBinding
import com.jehutyno.yomikata.databinding.VhWordDetailBinding
import com.jehutyno.yomikata.model.KanjiSoloRadical
import com.jehutyno.yomikata.model.Sentence
import com.jehutyno.yomikata.model.Word
import com.jehutyno.yomikata.model.getWordColor
import com.jehutyno.yomikata.util.*
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
        val layoutInflater = LayoutInflater.from(container.context)
        val binding = VhWordDetailBinding.inflate(layoutInflater, container, false)

        val word = words[position]
        var wordDisplay =
                if (quizType != null) word.first.japanese else "    {${word.first.japanese};${word.first.reading}}    "
        binding.levelDown.visibility = if (quizType != null) GONE else VISIBLE
        binding.levelUp.visibility = if (quizType != null) GONE else VISIBLE
        wordDisplay.let { binding.furiWord.text_set(wordDisplay, 0, it.length, getWordColor(container.context, word.first.level, word.first.points)) }
        binding.textTraduction.text = word.first.getTrad()
        val sentenceNoFuri = sentenceNoFuri(word.third)
        val wordTruePosition = word.third.jap.let { getWordPositionInFuriSentence(it, word.first) }
        wordTruePosition.let {
            binding.furiSentence.text_set(
                    if (quizType == null) word.third.jap else sentenceNoAnswerFuri(word.third, word.first),
                    it,
                    wordTruePosition + word.first.japanese.length,
                    getWordColor(activity, word.first.level, word.first.points))
        }
        binding.textTraduction.visibility = if (quizType == QuizType.TYPE_JAP_EN || word.first.isKana == 2) INVISIBLE else VISIBLE
        binding.textSentenceEn.text = word.third.getTrad()

        binding.btnSelection.setOnClickListener { callback.onSelectionClick(binding.btnSelection, position) }
        binding.btnReport.setOnClickListener { callback.onReportClick(position) }
        binding.btnSentenceTts.setOnClickListener { callback.onSentenceTTSClick(position) }
        binding.btnTts.setOnClickListener { callback.onWordTTSClick(position) }
        binding.btnCopy.setOnClickListener {
            val popup = PopupMenu(activity, binding.btnCopy)
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
        binding.levelDown.setOnClickListener { callback.onLevelDown(position) }
        binding.levelUp.setOnClickListener { callback.onLevelUp(position) }
        binding.close.setOnClickListener { callback.onCloseClick() }

        binding.containerInfo.visibility = if (word.second.isEmpty() || word.second[0] == null) GONE else VISIBLE

        var displaySeparator = false
        word.second.forEach {
            if (it != null) {
                val radicalLayoutBinding = VhKanjiSoloBinding.inflate(layoutInflater, container, false)
                radicalLayoutBinding.separator.visibility = if (displaySeparator) VISIBLE else GONE
                radicalLayoutBinding.kanjiSolo.text = it.kanji
                radicalLayoutBinding.ksTrad.text = it.getTrad()
                radicalLayoutBinding.ksStrokes.text = it.strokes.toString()
                radicalLayoutBinding.kunyomiTitle.visibility = if (it.kunyomi.isEmpty() || quizType != null) GONE else VISIBLE
                radicalLayoutBinding.ksKunyomi.visibility = if (it.kunyomi.isEmpty() || quizType != null) GONE else VISIBLE
                radicalLayoutBinding.ksKunyomi.text = it.kunyomi
                radicalLayoutBinding.onyomiTitle.visibility = if (it.onyomi.isEmpty() || quizType != null) GONE else VISIBLE
                radicalLayoutBinding.ksOnyomi.visibility = if (it.onyomi.isEmpty() || quizType != null) GONE else VISIBLE
                radicalLayoutBinding.ksOnyomi.text = it.onyomi
                radicalLayoutBinding.radical.text = it.radical
                radicalLayoutBinding.radicalStroke.text = it.radStroke.toString()
                radicalLayoutBinding.radicalTrad.text = it.getRadTrad()
                binding.containerInfo.addView(radicalLayoutBinding.root)
                displaySeparator = true
            }
        }

        container.addView(binding.root)
        return binding
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