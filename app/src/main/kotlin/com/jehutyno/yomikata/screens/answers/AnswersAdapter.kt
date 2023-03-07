package com.jehutyno.yomikata.screens.answers

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
//import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.core.text.HtmlCompat
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.databinding.VhAnswerBinding
import com.jehutyno.yomikata.model.*
import com.jehutyno.yomikata.util.sentenceNoFuri

/**
 * Created by valentin on 25/10/2016.
 */
class AnswersAdapter(private val context: Context, private val callback: Callback) : RecyclerView.Adapter<AnswersAdapter.ViewHolder>() {

    var items: List<Triple<Answer, Word, Sentence>> = listOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = VhAnswerBinding.inflate(layoutInflater, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val answer = items[position].first
        val word = items[position].second
        val sentence = items[position].third
        holder.answer_image.setImageResource(getCategoryIcon(word.baseCategory))
//        holder.answer_image.drawable?.setColorFilter(ContextCompat.getColor(context, R.color.answer_icon_color), PorterDuff.Mode.SRC_ATOP)
        val color = ContextCompat.getColor(context, R.color.answer_icon_color)
        holder.answer_image.drawable?.colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(color, BlendModeCompat.SRC_ATOP)

        holder.japanese.text_set(word.japanese, 0, word.japanese.length, getWordColor(context, word.level, word.points))
        holder.translation.text = word.getTrad()
//        holder.answer.text = Html.fromHtml(answer.answer)
        holder.answer.text = HtmlCompat.fromHtml(answer.answer, HtmlCompat.FROM_HTML_MODE_LEGACY)
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


    class ViewHolder(binding: VhAnswerBinding) : RecyclerView.ViewHolder(binding.root) {
        val answer_image = binding.answerImage
        val japanese = binding.japanese
        val translation = binding.translation
        val answer = binding.answer
        val sentence_jap = binding.sentenceJap
        val sentence_translation = binding.sentenceTranslation
        val btn_selection = binding.btnSelection
        val btn_report = binding.btnReport
        val btn_tts = binding.btnTts
        val sentence_tts = binding.sentenceTts
    }

    interface Callback {
        fun onSelectionClick(position: Int, view: View)
        fun onReportClick(position: Int)
        fun onTTSClick(position: Int)
        fun onSentenceTTSClick(position: Int)
    }

}