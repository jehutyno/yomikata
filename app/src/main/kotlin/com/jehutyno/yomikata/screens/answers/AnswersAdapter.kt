package com.jehutyno.yomikata.screens.answers

import android.content.Context
import android.graphics.PorterDuff
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.model.*
import com.jehutyno.yomikata.util.Prefs
import com.jehutyno.yomikata.util.sentenceNoFuri
import kotlinx.android.synthetic.main.vh_answer.view.*
import org.jetbrains.anko.defaultSharedPreferences

/**
 * Created by valentin on 25/10/2016.
 */
class AnswersAdapter(private val context: Context, private val callback: Callback) : RecyclerView.Adapter<AnswersAdapter.ViewHolder>() {

    var items: List<Triple<Answer, Word, Sentence>> = listOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return ViewHolder(layoutInflater.inflate(R.layout.vh_answer, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val answer = items[position].first
        val word = items[position].second
        val sentence = items[position].third
        holder.answer_image.setImageResource(getCategoryIcon(word.baseCategory))
        holder.answer_image.drawable?.setColorFilter(ContextCompat.getColor(context, R.color.answer_icon_color), PorterDuff.Mode.SRC_ATOP)
        holder.japanese.text_set(word.japanese, 0, word.japanese.length, getWordColor(context, word.level, word.points))
        holder.translation.text = word.getTrad()
        holder.answer.text = Html.fromHtml(answer.answer)
        holder.sentence_jap.text_set(
            sentence.jap,
            sentenceNoFuri(sentence).indexOf(word.japanese), sentenceNoFuri(sentence).indexOf(word.japanese) + word.japanese.length,
            getWordColor(context, word.level, word.points))
        holder.sentence_translation.text = sentence.getTrad()

        with(holder.btn_selection) {
            setOnClickListener {
                callback.onSelectionClick(position, holder.btn_selection)
            }
        }
        with(holder.btn_report) { setOnClickListener { callback.onReportClick(position) } }
        with(holder.btn_tts) { setOnClickListener { callback.onTTSClick(position) } }
        with(holder.sentence_tts) { setOnClickListener { callback.onSentenceTTSClick(position) } }
    }

    override fun getItemCount(): Int {
        return items.count()
    }

    fun replaceData(list: List<Triple<Answer, Word, Sentence>>) {
        items = list
        notifyDataSetChanged()
    }


    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val answer_image = view.answer_image!!
        val japanese = view.japanese!!
        val translation = view.translation!!
        val answer = view.answer!!
        val sentence_jap = view.sentence_jap!!
        val sentence_translation = view.sentence_translation!!
        val btn_selection = view.btn_selection!!
        val btn_report = view.btn_report!!
        val btn_tts = view.btn_tts!!
        val sentence_tts = view.sentence_tts!!
    }

    interface Callback {
        fun onSelectionClick(position: Int, view: View)
        fun onReportClick(position: Int)
        fun onTTSClick(position: Int)
        fun onSentenceTTSClick(position: Int)
    }

}