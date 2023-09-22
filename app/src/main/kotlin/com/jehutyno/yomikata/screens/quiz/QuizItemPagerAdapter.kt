package com.jehutyno.yomikata.screens.quiz

import android.content.Context
import android.text.method.ScrollingMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import androidx.viewpager.widget.PagerAdapter
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.furigana.FuriganaView
import com.jehutyno.yomikata.model.Sentence
import com.jehutyno.yomikata.model.Word
import com.jehutyno.yomikata.model.getWordColor
import com.jehutyno.yomikata.util.Prefs
import com.jehutyno.yomikata.util.QuizType
import com.jehutyno.yomikata.util.getWordPositionInFuriSentence
import com.jehutyno.yomikata.util.sentenceNoAnswerFuri
import com.jehutyno.yomikata.util.sentenceNoFuri
import kotlinx.coroutines.Job

/**
 * Created by jehutyno on 08/10/2016.
 */
class QuizItemPagerAdapter(var context: Context, var callback: Callback) : PagerAdapter() {

    var words: ArrayList<Pair<Word, QuizType>> = arrayListOf()
    var sentence: Sentence = Sentence()
    var isInfiniteSize: Int? = null

    override fun isViewFromObject(view: View, any: Any): Boolean {
        return view == any
    }

    override fun getCount(): Int {
        return words.count()
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val view = LayoutInflater.from(container.context).inflate(R.layout.vh_quiz_item, container, false)
        val word = words[position].first
        val wholeSentenceLayout = view.findViewById<View>(R.id.whole_sentence_layout)
        val btnFuri = view.findViewById<View>(R.id.btn_furi)
        val btnTrad = view.findViewById<View>(R.id.btn_trad)
        val btnCopy = view.findViewById<View>(R.id.btn_copy)
        val btnSelection = view.findViewById<View>(R.id.btn_selection)
        val btnReport = view.findViewById<View>(R.id.btn_report)
        val btnTts = view.findViewById<View>(R.id.btn_tts)
        val tradSentence = view.findViewById<TextView>(R.id.trad_sentence)
        val furiSentence = view.findViewById<FuriganaView>(R.id.furi_sentence)
        val sound = view.findViewById<ImageButton>(R.id.sound)
        val sessionCount = view.findViewById<TextView>(R.id.session_count)

        sessionCount.text =
            if (isInfiniteSize != null)
                "${isInfiniteSize!!}"
            else
                "${position + 1} / ${words.size}"

        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        btnFuri.isSelected = pref.getBoolean(Prefs.FURI_DISPLAYED.pref, true)
        btnTrad.isSelected = pref.getBoolean(Prefs.TRAD_DISPLAYED.pref, true)

        when (words[position].second) {
            QuizType.TYPE_PRONUNCIATION, QuizType.TYPE_PRONUNCIATION_QCM, QuizType.TYPE_JAP_EN -> {
                sound.visibility = View.GONE
                btnTrad.visibility = View.VISIBLE
                tradSentence.visibility = View.VISIBLE
                tradSentence.textSize = 16f
                tradSentence.setTextColor(ContextCompat.getColor(context, R.color.lighter_gray))
                val sentenceNoFuri = sentenceNoFuri(sentence)
                val colorEntireWord = word.isKana == 2 && words[position].second == QuizType.TYPE_JAP_EN
                val wordTruePosition = if (colorEntireWord) 0 else getWordPositionInFuriSentence(sentence.jap, word)
                if (btnFuri.isSelected) {
                    furiSentence.text_set(
                        if (colorEntireWord) sentence.jap else sentenceNoAnswerFuri(sentence, word),
                        if (colorEntireWord) 0 else wordTruePosition,
                        if (colorEntireWord) sentence.jap.length else wordTruePosition + word.japanese.length,
                        getWordColor(context, word.points))
                } else {
                    furiSentence.text_set(
                        sentenceNoFuri,
                        if (colorEntireWord) 0 else wordTruePosition,
                        if (colorEntireWord) sentence.jap.length else wordTruePosition + word.japanese.length,
                        getWordColor(context, word.points))
                }
                btnTrad.visibility = if (words[position].second != QuizType.TYPE_JAP_EN) View.VISIBLE else View.GONE
                tradSentence.text = if (words[position].first.isKana == 2) "" else sentence.getTrad()
                tradSentence.visibility = if (btnTrad.isSelected && words[position].second != QuizType.TYPE_JAP_EN) View.VISIBLE else View.INVISIBLE
            }
            QuizType.TYPE_EN_JAP -> {
                sound.visibility = View.GONE
                btnFuri.visibility = View.VISIBLE
                furiSentence.visibility = View.INVISIBLE
                btnTrad.visibility = View.GONE
                tradSentence.visibility = View.VISIBLE
                tradSentence.movementMethod = ScrollingMovementMethod()
                tradSentence.setTextColor(getWordColor(context, word.points))
                tradSentence.textSize = PreferenceManager.getDefaultSharedPreferences(context).getString("font_size", "18")!!.toFloat()
                tradSentence.text = word.getTrad()
            }
            QuizType.TYPE_AUDIO -> {
                sound.visibility = View.VISIBLE
                furiSentence.visibility = View.GONE
                btnTrad.visibility = View.GONE
                btnFuri.visibility = View.GONE
                btnTts.visibility = View.GONE
                sound.setColorFilter(getWordColor(context, word.points))
            }
            else -> {
            }
        }

        if (words[position].second != QuizType.TYPE_EN_JAP && words[position].second != QuizType.TYPE_AUDIO) {
            wholeSentenceLayout.setOnClickListener {
                callback.onItemClick(position)
            }
        }

        sound.setOnClickListener {
            callback.onSoundClick(sound, position)
        }

        btnFuri.setOnClickListener {
            btnFuri.isSelected = !btnFuri.isSelected
            pref.edit().putBoolean(Prefs.FURI_DISPLAYED.pref, btnFuri.isSelected).apply()
            notifyDataSetChanged()
            callback.onFuriClick(position, btnFuri.isSelected)
        }

        btnTrad.setOnClickListener {
            btnTrad.isSelected = !btnTrad.isSelected
            pref.edit().putBoolean(Prefs.TRAD_DISPLAYED.pref, btnTrad.isSelected).apply()
            notifyDataSetChanged()
            callback.onTradClick(position)
        }

        btnSelection.setOnClickListener {
            callback.onSelectionClick(it, position)
        }

        btnReport.setOnClickListener {
            callback.onReportClick(position)
        }

        btnTts.setOnClickListener {
            callback.onSentenceTTSClick(position)
        }

        btnCopy.setOnClickListener {
            val popup = PopupMenu(context, btnCopy)
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