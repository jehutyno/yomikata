package com.jehutyno.yomikata.screens.quiz

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.text.method.ScrollingMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.databinding.VhQuizItemBinding
import com.jehutyno.yomikata.model.Sentence
import com.jehutyno.yomikata.model.Word
import com.jehutyno.yomikata.model.getWordColor
import com.jehutyno.yomikata.util.Prefs
import com.jehutyno.yomikata.util.QuizType
import com.jehutyno.yomikata.util.cleanForQCM
import com.jehutyno.yomikata.util.getWordPositionInFuriSentence
import com.jehutyno.yomikata.util.sentenceNoAnswerFuri
import com.jehutyno.yomikata.util.sentenceNoFuri
import kotlinx.coroutines.Job


/**
 * Created by jehutyno on 08/10/2016.
 */
class QuizItemPagerAdapter(private val context: Context, private val callback: Callback)
    : RecyclerView.Adapter<QuizItemPagerAdapter.ViewHolder>() {

    var words: ArrayList<Pair<Word, QuizType>> = arrayListOf()
    var sentence: Sentence = Sentence()
    /** only non-null if session size = infinite AND quiz strategy = progressive.
     *
     *  if non-null -> set to count of number of words that have been seen + 1 */
    var isInfiniteSize: Int? = null

    private data class AnimationParameters(
        var fromPoints: Int,
        var toPoints: Int,
        var quizType: QuizType
    )
    private val animationParameters = AnimationParameters(0, 0, QuizType.TYPE_AUTO)

    fun setAnimation(fromPoints: Int, toPoints: Int, quizType: QuizType) {
        animationParameters.fromPoints = fromPoints
        animationParameters.toPoints = toPoints
        animationParameters.quizType = quizType
    }

    class ViewHolder(binding: VhQuizItemBinding) : RecyclerView.ViewHolder(binding.root) {
        val wholeSentenceLayout = binding.wholeSentenceLayout
        val btnFuri = binding.btnFuri
        val btnTrad = binding.btnTrad
        val btnCopy = binding.btnCopy
        val btnSelection = binding.btnSelection
        val btnReport = binding.btnReport
        val btnTts = binding.btnTts
        val tradSentence = binding.tradSentence
        val furiSentence = binding.furiSentence
        val sound = binding.sound
        val sessionCount = binding.sessionCount
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = VhQuizItemBinding.inflate(layoutInflater, parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return words.count()
    }

    class PlayAnimation

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else {
            // play animation if any of the payloads are PlayAnimation
            if (payloads.any{ it is PlayAnimation })
                animateColor(words[position].first, sentence, holder)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val word = words[position].first
        holder.sessionCount.text =
            if (isInfiniteSize != null)
                "${isInfiniteSize!!}"
            else
                "${position + 1} / ${words.size}"

        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        holder.btnFuri.isSelected = pref.getBoolean(Prefs.FURI_DISPLAYED.pref, true)
        holder.btnTrad.isSelected = pref.getBoolean(Prefs.TRAD_DISPLAYED.pref, true)

        when (words[position].second) {
            QuizType.TYPE_PRONUNCIATION, QuizType.TYPE_PRONUNCIATION_QCM, QuizType.TYPE_JAP_EN -> {
                holder.sound.visibility = View.GONE

                holder.btnFuri.visibility = View.VISIBLE
                holder.btnTrad.visibility = if (words[position].second != QuizType.TYPE_JAP_EN) View.VISIBLE else View.GONE
                holder.btnTts.visibility = View.VISIBLE

                holder.furiSentence.visibility = View.VISIBLE
                val sentenceNoFuri = sentenceNoFuri(sentence)
                val colorEntireWord = word.isKana == 2 && words[position].second == QuizType.TYPE_JAP_EN
                val wordTruePosition = if (colorEntireWord) 0 else getWordPositionInFuriSentence(sentence.jap, word)
                if (holder.btnFuri.isSelected) {
                    holder.furiSentence.text_set(
                        if (colorEntireWord) sentence.jap else sentenceNoAnswerFuri(sentence, word),
                        if (colorEntireWord) 0 else wordTruePosition,
                        if (colorEntireWord) sentence.jap.length else wordTruePosition + word.japanese.length,
                        getWordColor(context, word.points))
                } else {
                    holder.furiSentence.text_set(
                        sentenceNoFuri,
                        if (colorEntireWord) 0 else wordTruePosition,
                        if (colorEntireWord) sentence.jap.length else wordTruePosition + word.japanese.length,
                        getWordColor(context, word.points))
                }

                holder.tradSentence.visibility = if (holder.btnTrad.isSelected && words[position].second != QuizType.TYPE_JAP_EN) View.VISIBLE else View.INVISIBLE
                holder.tradSentence.text = if (words[position].first.isKana == 2) "" else sentence.getTrad()
                holder.tradSentence.textSize = 16f
                holder.tradSentence.setTextColor(ContextCompat.getColor(context, R.color.lighter_gray))
            }
            QuizType.TYPE_EN_JAP -> {
                holder.sound.visibility = View.GONE

                holder.btnFuri.visibility = View.VISIBLE
                holder.btnTrad.visibility = View.GONE
                holder.btnTts.visibility = View.VISIBLE

                holder.furiSentence.visibility = View.GONE

                holder.tradSentence.visibility = View.VISIBLE
                holder.tradSentence.movementMethod = ScrollingMovementMethod()
                holder.tradSentence.setTextColor(getWordColor(context, word.points))
                holder.tradSentence.textSize = PreferenceManager.getDefaultSharedPreferences(context).getString("font_size", "18")!!.toFloat()
                holder.tradSentence.text = word.getTrad().cleanForQCM(false)
            }
            QuizType.TYPE_AUDIO -> {
                holder.sound.visibility = View.VISIBLE
                holder.sound.setColorFilter(getWordColor(context, word.points))

                holder.btnFuri.visibility = View.GONE
                holder.btnTrad.visibility = View.GONE
                holder.btnTts.visibility = View.GONE

                holder.furiSentence.visibility = View.GONE
                holder.tradSentence.visibility = View.INVISIBLE // not gone, because spacing is needed
            }
            else -> {
            }
        }

        if (words[position].second != QuizType.TYPE_EN_JAP && words[position].second != QuizType.TYPE_AUDIO) {
            holder.wholeSentenceLayout.setOnClickListener {
                callback.onItemClick(position)
            }
        }

        holder.sound.setOnClickListener {
            callback.onSoundClick(holder.sound, position)
        }

        holder.btnFuri.setOnClickListener {
            holder.btnFuri.isSelected = !holder.btnFuri.isSelected
            pref.edit().putBoolean(Prefs.FURI_DISPLAYED.pref, holder.btnFuri.isSelected).apply()
            notifyItemChanged(position)
            callback.onFuriClick(position, holder.btnFuri.isSelected)
        }

        holder.btnTrad.setOnClickListener {
            holder.btnTrad.isSelected = !holder.btnTrad.isSelected
            pref.edit().putBoolean(Prefs.TRAD_DISPLAYED.pref, holder.btnTrad.isSelected).apply()
            notifyItemChanged(position)
            callback.onTradClick(position)
        }

        holder.btnSelection.setOnClickListener {
            callback.onSelectionClick(it, position)
        }

        holder.btnReport.setOnClickListener {
            callback.onReportClick(position)
        }

        holder.btnTts.setOnClickListener {
            callback.onSentenceTTSClick(position)
        }

        holder.btnCopy.setOnClickListener {
            val popup = PopupMenu(context, holder.btnCopy)
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
    }

    private fun animateColor(word: Word, sentence: Sentence, holder: ViewHolder) {
        val fromPoints = animationParameters.fromPoints
        val toPoints = animationParameters.toPoints
        val quizType = animationParameters.quizType

        val btnFuri = holder.btnFuri
        val furiSentence = holder.furiSentence
        val tradSentence = holder.tradSentence
        val sound = holder.sound
        val sentenceNoFuri = sentenceNoFuri(sentence)
        val colorAnimation = ValueAnimator.ofObject(ArgbEvaluator(),
            getWordColor(context, fromPoints),
            getWordColor(context, toPoints))
        colorAnimation.addUpdateListener {
                animator ->
            run {
                when (quizType) {
                    QuizType.TYPE_PRONUNCIATION, QuizType.TYPE_PRONUNCIATION_QCM, QuizType.TYPE_JAP_EN -> {
                        val colorEntireWord = word.isKana == 2 && quizType == QuizType.TYPE_JAP_EN
                        val wordTruePosition = if (colorEntireWord) 0 else getWordPositionInFuriSentence(sentence.jap, word)
                        if (btnFuri.isSelected) {
                            if (!colorEntireWord) wordTruePosition.let {
                                furiSentence.text_set(
                                    sentenceNoAnswerFuri(sentence, word), it,
                                    wordTruePosition + word.japanese.length,
                                    animator.animatedValue as Int)
                            }
                        } else {
                            furiSentence.text_set(
                                if (colorEntireWord) sentence.jap else sentenceNoFuri.replace("%", word.japanese),
                                (if (colorEntireWord) 0 else wordTruePosition),
                                if (colorEntireWord) sentence.jap.length else wordTruePosition + word.japanese.length,
                                animator.animatedValue as Int)
                        }
                    }
                    QuizType.TYPE_EN_JAP -> {
                        tradSentence.setTextColor(animator.animatedValue as Int)
                    }
                    QuizType.TYPE_AUDIO -> {
                        sound.setColorFilter(animator.animatedValue as Int)
                    }
                    else -> {
                    }
                }
            }
        }
        colorAnimation.start()
    }

    fun addNewData(list: List<Pair<Word, QuizType>>) {
        words.addAll(list)
        // no need to notify of changes, since additions are at the end of the list
        // if you insert into the list, use notifyItemInserted
    }

    fun replaceData(list: List<Pair<Word, QuizType>>) {
        words.clear()
        words.addAll(list)
        @Suppress("notifyDataSetChanged")
        notifyDataSetChanged()
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
