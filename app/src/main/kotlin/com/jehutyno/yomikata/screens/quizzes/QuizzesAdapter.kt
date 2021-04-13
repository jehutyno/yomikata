package com.jehutyno.yomikata.screens.quizzes

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.model.Quiz
import com.jehutyno.yomikata.util.Categories
import com.jehutyno.yomikata.util.Prefs
import kotlinx.android.synthetic.main.vh_quiz.view.*
import org.jetbrains.anko.defaultSharedPreferences

/**
 * Created by valentin on 04/10/2016.
 */
class QuizzesAdapter(val context: Context, val category: Int, private val callback: Callback, private var isSelections: Boolean) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var flag = false
    var items: MutableList<Quiz>

    init {
        items = arrayListOf<Quiz>()
    }

    companion object {
        val TYPE_NEW_SELECTION = 0
        val TYPE_QUIZ = 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        when (viewType) {
            TYPE_NEW_SELECTION -> return ViewHolderNewSelection(layoutInflater.inflate(R.layout.vh_new_selection, parent, false))
            else -> return ViewHolder(layoutInflater.inflate(R.layout.vh_quiz, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            TYPE_NEW_SELECTION -> {
                holder.itemView.setOnClickListener {
                    callback.addSelection()
                }
            }
            else -> {
                val quiz = items[position]
                val quizNames = quiz.getName().split("%")
                (holder as ViewHolder?)?.quizName?.text = quizNames[0]
                if (quizNames.size > 1)
                    holder.quizSubtitle.text = quiz.getName().split("%")[1]
                else {
                    holder.quizSubtitle.text = ""
                }
                flag = true
                holder.quizCheck.isChecked = quiz.isSelected == 1
                flag = false
                holder.itemView.setOnClickListener {
                    callback.onItemClick(position)
                }
                holder.itemView.setOnLongClickListener {
                    callback.onItemLongClick(position)
                    true
                }
                holder.quizCheck.setOnCheckedChangeListener {
                    compoundButton, b ->
                    if (category != Categories.CATEGORY_SELECTIONS) {
                        if (!flag) {
                            run {
                                callback.onItemChecked(position, b)
                                items[position].isSelected = if (b) 1 else 0
                                notifyItemChanged(position)
                            }
                        }
                    } else {
                        if (!flag) {
                            run {
                                items[position].isSelected = 0
                                notifyItemChanged(position)
                            }
                        }
                    }
                }

            }
        }
    }

    override fun getItemCount(): Int {
        if (isSelections)
            return items.count() + 1
        else
            return items.count()

    }

    override fun getItemViewType(position: Int): Int {
        if (isSelections && position == items.count())
            return TYPE_NEW_SELECTION
        else
            return TYPE_QUIZ
    }

    fun replaceData(list: List<Quiz>, isSelections: Boolean) {
        this.isSelections = isSelections
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    fun noData(isSelections: Boolean) {
        this.isSelections = isSelections
        items.clear()
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val quizName = view.quiz_name!!
        val quizSubtitle = view.quiz_subtitle!!
        val quizCheck = view.quiz_check!!
    }

    class ViewHolderNewSelection(view: View) : RecyclerView.ViewHolder(view) {

    }

    interface Callback {
        fun onItemClick(position: Int)
        fun onItemLongClick(position: Int)
        fun onItemChecked(position: Int, checked: Boolean)
        fun addSelection()
    }

    fun deleteItem(position: Int) {
        items.removeAt(position)
        notifyItemRemoved(position)
    }

}