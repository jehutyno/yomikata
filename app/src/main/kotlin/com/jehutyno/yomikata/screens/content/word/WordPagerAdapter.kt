package com.jehutyno.yomikata.screens.content.word

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import android.view.ContextThemeWrapper
import androidx.core.content.ContextCompat
import androidx.viewpager.widget.PagerAdapter
import com.google.android.material.chip.Chip
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.databinding.VhKanjiSoloBinding
import com.jehutyno.yomikata.databinding.VhWordDetailBinding
import com.jehutyno.yomikata.model.KanjiSoloRadical
import com.jehutyno.yomikata.model.Sentence
import com.jehutyno.yomikata.model.Word
import com.jehutyno.yomikata.model.getWordColor
import com.jehutyno.yomikata.util.quiz.QuizType
import com.jehutyno.yomikata.util.getWordPositionInFuriSentence
import com.jehutyno.yomikata.util.sentenceNoAnswerFuri
import com.jehutyno.yomikata.util.sentenceNoFuri

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
        val wordDisplay =
                if (quizType != null) word.first.japanese else "    {${word.first.japanese};${word.first.reading}}    "
        binding.levelDown.visibility = if (quizType != null) GONE else VISIBLE
        binding.levelUp.visibility = if (quizType != null) GONE else VISIBLE
        wordDisplay.let { binding.furiWord.text_set(wordDisplay, 0, it.length, getWordColor(container.context, word.first.points)) }
        binding.textTraduction.text = word.first.getTrad()
        populatePosChips(binding, word.first, container.context)
//        val sentenceNoFuri = sentenceNoFuri(word.third)
        val wordTruePosition = word.third.jap.let { getWordPositionInFuriSentence(it, word.first) }
        wordTruePosition.let {
            binding.furiSentence.text_set(
                    if (quizType == null) word.third.jap else sentenceNoAnswerFuri(word.third, word.first),
                    it,
                    wordTruePosition + word.first.japanese.length,
                    getWordColor(activity, word.first.points))
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

        val view = binding.root
        container.addView(view)
        return view
    }

    private fun populatePosChips(binding: VhWordDetailBinding, word: Word, context: Context) {
        binding.chipGroupPos.removeAllViews()
        val tokens = word.getPosTokens()
        if (tokens.isEmpty()) return

        val chipContext = ContextThemeWrapper(context, com.google.android.material.R.style.Theme_MaterialComponents_DayNight)
        tokens.forEach { token ->
            val (labelRes, colorRes) = posTokenToResources(token) ?: return@forEach
            val chip = Chip(chipContext).apply {
                text = context.getString(labelRes)
                isClickable = false
                isCheckable = false
                isChipIconVisible = false
                isCheckedIconVisible = false
                isCloseIconVisible = false
                chipBackgroundColor = android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(context, colorRes)
                )
                setTextColor(ContextCompat.getColor(context, R.color.pos_chip_text))
                textSize = 11f
                // Padding symétrique pour centrer le texte dans le chip
                chipStartPadding = 8f
                chipEndPadding = 8f
            }
            binding.chipGroupPos.addView(chip)
        }
    }

    /** Maps a POS token string to a (labelStringRes, chipColorRes) pair, or null to skip. */
    private fun posTokenToResources(token: String): Pair<Int, Int>? {
        return when {
            token == "n" || token == "n-t"   -> Pair(R.string.pos_noun,         R.color.pos_noun)
            token == "n-suf"                 -> Pair(R.string.pos_suffix,       R.color.pos_noun)
            token == "n-adv"                 -> Pair(R.string.pos_adverb,       R.color.pos_adverb)
            token == "pn"                    -> Pair(R.string.pos_pronoun,      R.color.pos_noun)
            token == "vi"                    -> Pair(R.string.pos_intransitive, R.color.pos_verb)
            token == "vt"                    -> Pair(R.string.pos_transitive,   R.color.pos_verb)
            token == "v1"                    -> Pair(R.string.pos_verb_ichidan, R.color.pos_verb)
            token == "vz"                    -> Pair(R.string.pos_verb_zuru,    R.color.pos_verb)
            token == "vs"                    -> Pair(R.string.pos_verb_suru,    R.color.pos_verb)
            token.startsWith("v5")           -> Pair(R.string.pos_verb_godan,   R.color.pos_verb)
            token == "aux-v" || token == "aux-adj" -> Pair(R.string.pos_auxiliary, R.color.pos_verb)
            token == "adj-i"                 -> Pair(R.string.pos_adj_i,        R.color.pos_adjective)
            token == "adj-na"                -> Pair(R.string.pos_adj_na,       R.color.pos_adjective)
            token == "adj-no"                -> Pair(R.string.pos_adj_no,       R.color.pos_adjective)
            token == "adj-t"                 -> Pair(R.string.pos_adj_t,        R.color.pos_adjective)
            token == "adv" || token == "adv-to" -> Pair(R.string.pos_adverb,   R.color.pos_adverb)
            token == "pref"                  -> Pair(R.string.pos_prefix,       R.color.pos_other)
            token == "suf"                   -> Pair(R.string.pos_suffix,       R.color.pos_other)
            token == "exp"                   -> Pair(R.string.pos_expression,   R.color.pos_other)
            token == "num"                   -> Pair(R.string.pos_number,       R.color.pos_other)
            token == "ctr"                   -> Pair(R.string.pos_counter,      R.color.pos_other)
            token == "abbr"                  -> Pair(R.string.pos_abbreviation, R.color.pos_other)
            token == "pol"                   -> Pair(R.string.pos_polite,       R.color.pos_other)
            token == "hum"                   -> Pair(R.string.pos_humble,       R.color.pos_other)
            token == "int"                   -> Pair(R.string.pos_interjection, R.color.pos_other)
            else                             -> null
        }
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