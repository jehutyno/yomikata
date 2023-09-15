package com.jehutyno.yomikata.screens.quiz

import android.content.Context
import androidx.preference.PreferenceManager
import androidx.core.content.ContextCompat
import androidx.viewpager.widget.PagerAdapter
import androidx.appcompat.widget.PopupMenu
import android.text.method.ScrollingMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.furigana.FuriganaView
import com.jehutyno.yomikata.model.Sentence
import com.jehutyno.yomikata.model.Word
import com.jehutyno.yomikata.model.getWordColor
import com.jehutyno.yomikata.util.*
import kotlinx.coroutines.Job
import java.util.*

/**
 * Created by jehutyno on 08/10/2016.
 */
class QuizItemPagerAdapter(var context: Context, var callback: Callback) : PagerAdapter() {

    var words: ArrayList<Pair<Word, QuizType>> = arrayListOf()
    var sentence: Sentence = Sentence()

    override fun isViewFromObject(view: View, any: Any): Boolean {
        return view == any
    }

    override fun getCount(): Int {
        return words.count()
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val view = LayoutInflater.from(container.context).inflate(R.layout.vh_quiz_item, container, false)
        val word = words[position].first
        val whole_sentence_layout = view.findViewById<View>(R.id.whole_sentence_layout)
        val btn_furi = view.findViewById<View>(R.id.btn_furi)
        val btn_trad = view.findViewById<View>(R.id.btn_trad)
        val btn_copy = view.findViewById<View>(R.id.btn_copy)
        val btn_selection = view.findViewById<View>(R.id.btn_selection)
        val btn_report = view.findViewById<View>(R.id.btn_report)
        val btn_tts = view.findViewById<View>(R.id.btn_tts)
        val trad_sentence = view.findViewById<TextView>(R.id.trad_sentence)
        val furi_sentence = view.findViewById<FuriganaView>(R.id.furi_sentence)
        val sound = view.findViewById<ImageButton>(R.id.sound)
        val session_count = view.findViewById<TextView>(R.id.session_count)

        session_count.text = "${position + 1} / ${words.size}"

        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        btn_furi.isSelected = pref.getBoolean(Prefs.FURI_DISPLAYED.pref, true)
        btn_trad.isSelected = pref.getBoolean(Prefs.TRAD_DISPLAYED.pref, true)

        when (words[position].second) {
            QuizType.TYPE_PRONUNCIATION, QuizType.TYPE_PRONUNCIATION_QCM, QuizType.TYPE_JAP_EN -> {
                sound.visibility = View.GONE
                btn_trad.visibility = View.VISIBLE
                trad_sentence.visibility = View.VISIBLE
                trad_sentence.textSize = 16f
                trad_sentence.setTextColor(ContextCompat.getColor(context, R.color.lighter_gray))
                val sentenceNoFuri = sentenceNoFuri(sentence)
                val colorEntireWord = word.isKana == 2 && words[position].second == QuizType.TYPE_JAP_EN
                val wordTruePosition = if (colorEntireWord) 0 else getWordPositionInFuriSentence(sentence.jap, word)
                if (btn_furi.isSelected) {
                    furi_sentence.text_set(
                        if (colorEntireWord) sentence.jap else sentenceNoAnswerFuri(sentence, word),
                        if (colorEntireWord) 0 else wordTruePosition,
                        if (colorEntireWord) sentence.jap.length else wordTruePosition + word.japanese.length,
                        getWordColor(context, word.level, word.points))
                } else {
                    furi_sentence.text_set(
                        sentenceNoFuri,
                        if (colorEntireWord) 0 else wordTruePosition,
                        if (colorEntireWord) sentence.jap.length else wordTruePosition + word.japanese.length,
                        getWordColor(context, word.level, word.points))
                }
                btn_trad.visibility = if (words[position].second != QuizType.TYPE_JAP_EN) View.VISIBLE else View.GONE
                trad_sentence.text = if (words[position].first.isKana == 2) "" else sentence.getTrad()
                trad_sentence.visibility = if (btn_trad.isSelected && words[position].second != QuizType.TYPE_JAP_EN) View.VISIBLE else View.INVISIBLE
            }
            QuizType.TYPE_EN_JAP -> {
                sound.visibility = View.GONE
                btn_furi.visibility = View.VISIBLE
                furi_sentence.visibility = View.INVISIBLE
                btn_trad.visibility = View.GONE
                trad_sentence.visibility = View.VISIBLE
                trad_sentence.movementMethod = ScrollingMovementMethod()
                trad_sentence.setTextColor(getWordColor(context, word.level, word.points))
                trad_sentence.textSize = PreferenceManager.getDefaultSharedPreferences(context).getString("font_size", "18")!!.toFloat()
                trad_sentence.text = word.getTrad()
            }
            QuizType.TYPE_AUDIO -> {
                sound.visibility = View.VISIBLE
                furi_sentence.visibility = View.GONE
                btn_trad.visibility = View.GONE
                btn_furi.visibility = View.GONE
                btn_tts.visibility = View.GONE
                sound.setColorFilter(getWordColor(context, word.level, word.points))
            }
            else -> {
            }
        }

        if (words[position].second != QuizType.TYPE_EN_JAP && words[position].second != QuizType.TYPE_AUDIO) {
            whole_sentence_layout.setOnClickListener {
                callback.onItemClick(position)
            }
        }

        sound.setOnClickListener {
            callback.onSoundClick(sound, position)
        }

        btn_furi.setOnClickListener {
            btn_furi.isSelected = !btn_furi.isSelected
            pref.edit().putBoolean(Prefs.FURI_DISPLAYED.pref, btn_furi.isSelected).apply()
            notifyDataSetChanged()
            callback.onFuriClick(position, btn_furi.isSelected)
        }

        btn_trad.setOnClickListener {
            btn_trad.isSelected = !btn_trad.isSelected
            pref.edit().putBoolean(Prefs.TRAD_DISPLAYED.pref, btn_trad.isSelected).apply()
            notifyDataSetChanged()
            callback.onTradClick(position)
        }

        btn_selection.setOnClickListener {
            callback.onSelectionClick(it, position)
        }

        btn_report.setOnClickListener {
            callback.onReportClick(position)
        }

        btn_tts.setOnClickListener {
            callback.onSentenceTTSClick(position)
        }

        btn_copy.setOnClickListener {
            val popup = PopupMenu(context, btn_copy)
            popup.menuInflater.inflate(R.menu.popup_copy, popup.menu)
            popup.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.copy_word -> {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText(
                            context.getString(R.string.copy_word),
                            words[position].first.japanese)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, context.getString(R.string.word_copied), Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText(
                            context.getString(R.string.copy_sentence),
                            sentenceNoFuri(sentence))
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, context.getString(R.string.sentence_copied), Toast.LENGTH_SHORT).show()
                    }
                }
                true
            }
            popup.show()
        }

        view.tag = "pos_$position"

        container.addView(view)
        return view
    }

    fun addNewData(list: List<Pair<Word, QuizType>>) {
        words.addAll(list)
        notifyDataSetChanged()
    }

    fun replaceData(list: List<Pair<Word, QuizType>>) {
        words.clear()
        words.addAll(list)
        notifyDataSetChanged()
    }

    fun replaceSentence(sentence: Sentence) {
        this.sentence = sentence
    }

    override fun getItemPosition(`object`: Any): Int {
        return POSITION_NONE
    }

    override fun destroyItem(collection: ViewGroup, position: Int, view: Any) {
        collection.removeView(view as View)
    }

    interface Callback {
        fun onItemClick(position: Int)
        fun onSoundClick(button: ImageButton, position: Int)
        fun onSelectionClick(view: View, position: Int)
        fun onReportClick(position: Int)
        fun onSentenceTTSClick(position: Int)
        fun onFuriClick(position: Int, isSelected: Boolean): Job
        fun onTradClick(position: Int)
    }

}