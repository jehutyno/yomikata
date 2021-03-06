package com.jehutyno.yomikata.screens.content

import android.content.Context
import android.graphics.PorterDuff
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.model.Word
import com.jehutyno.yomikata.model.getCategoryIcon
import com.jehutyno.yomikata.model.getWordColor
import com.jehutyno.yomikata.util.Prefs
import kotlinx.android.synthetic.main.vh_word_short.view.*
import org.jetbrains.anko.defaultSharedPreferences

/**
 * Created by valentin on 04/10/2016.
 */
class WordsAdapter(private val context: Context, private val callback: Callback) : RecyclerView.Adapter<WordsAdapter.ViewHolder>() {

    var items: MutableList<Word>
    var flag = false

    init {
        items = arrayListOf<Word>()
    }

    var checkMode = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent?.context)
        return ViewHolder(layoutInflater.inflate(R.layout.vh_word_short, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val word = items[position]
        holder?.wordName?.text = word.japanese
        holder?.wordName?.setTextColor(getWordColor(context, word.level, word.points))
        holder?.categoryIcon?.setImageResource(getCategoryIcon(word.baseCategory))
        holder?.categoryIcon?.drawable?.setColorFilter(ContextCompat.getColor(context, R.color.content_icon_color), PorterDuff.Mode.SRC_ATOP)
        flag = true
        holder?.checkBox?.isChecked = word.isSelected == 1
        flag = false

        if (checkMode) {
            holder?.categoryIcon?.visibility = GONE
            holder?.checkBox?.visibility = VISIBLE
        } else {
            holder?.categoryIcon?.visibility = VISIBLE
            holder?.checkBox?.visibility = GONE
        }

        with(holder!!.itemView) {
            setOnClickListener({ v -> callback.onItemClick(position) })
        }

        with(holder.categoryIcon) {
            setOnClickListener {
                checkMode = true
                word.isSelected = 1
                notifyDataSetChanged()
                callback.onCategoryIconClick(position)
            }
        }

        with(holder.checkBox) {
            setOnCheckedChangeListener { _, b ->
                if (!flag) {
                    run {
                        callback.onCheckChange(position, b)
                        items[position].isSelected = if (b) 1 else 0
                        notifyItemChanged(position)
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return items.count()
    }

    fun replaceData(list: List<Word>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val wordName = view.kanji_word!!
        val categoryIcon = view.category_icon!!
        val checkBox = view.word_check!!
    }

    interface Callback {
        fun onItemClick(position: Int)
        fun onCategoryIconClick(position: Int)
        fun onCheckChange(position: Int, check: Boolean)
    }

    fun clearData() {
        items.clear()
        notifyDataSetChanged()
    }

}