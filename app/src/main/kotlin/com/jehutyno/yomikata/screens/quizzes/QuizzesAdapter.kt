package com.jehutyno.yomikata.screens.quizzes

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import com.jehutyno.yomikata.databinding.VhNewSelectionBinding
import com.jehutyno.yomikata.databinding.VhQuizBinding
import com.jehutyno.yomikata.model.Quiz
import com.jehutyno.yomikata.util.Categories

/**
 * Created by valentin on 04/10/2016.
 */
class QuizzesAdapter(val context: Context, val category: Int, private val callback: Callback, private var isSelections: Boolean) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var flag = false
    var items: MutableList<Quiz> = arrayListOf()

    companion object {
        val TYPE_NEW_SELECTION = 0
        val TYPE_QUIZ = 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)

        return when (viewType) {
            TYPE_NEW_SELECTION -> {
                val binding = VhNewSelectionBinding.inflate(layoutInflater, parent, false)
                ViewHolder.ViewHolderNewSelection(binding)
            }
            TYPE_QUIZ -> {
                val binding = VhQuizBinding.inflate(layoutInflater, parent, false)
                ViewHolder.ViewHolderQuiz(binding)
            }
            else -> throw IllegalArgumentException("Invalid ViewType Provided")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ViewHolder.ViewHolderNewSelection -> {
                holder.itemView.setOnClickListener {
                    callback.addSelection()
                }
            }
            is ViewHolder.ViewHolderQuiz -> {
                val quiz = items[position]
                val quizNames = quiz.getName().split("%")
                holder.quizName.text = quizNames[0]
                if (quizNames.size > 1)
                    holder.quizSubtitle.text = quiz.getName().split("%")[1]
                else {
                    holder.quizSubtitle.text = ""
                }
                flag = true
                holder.quizCheck.isChecked = quiz.isSelected
                flag = false
                holder.itemView.setOnClickListener {
                    callback.onItemClick(position)
                }
                holder.itemView.setOnLongClickListener {
                    callback.onItemLongClick(position)
                    true
                }
                holder.quizCheck.setOnCheckedChangeListener {
                    _, b ->
                    if (category != Categories.CATEGORY_SELECTIONS) {
                        if (!flag) {
                            run {
                                callback.onItemChecked(position, b)
                                items[position].isSelected = b
                                notifyItemChanged(position)
                            }
                        }
                    } else {
                        if (!flag) {
                            run {
                                items[position].isSelected = false
                                notifyItemChanged(position)
                            }
                        }
                    }
                }

            }
        }
    }

    override fun getItemCount(): Int {
        return if (isSelections)
            items.count() + 1
        else
            items.count()

    }

    override fun getItemViewType(position: Int): Int {
        return if (isSelections && position == items.count())
            TYPE_NEW_SELECTION
        else
            TYPE_QUIZ
    }

    fun replaceData(list: List<Quiz>, isSelections: Boolean) {
        this.isSelections = isSelections
        items.clear()
        items.addAll(list)
        @Suppress("notifyDataSetChanged")
        notifyDataSetChanged()
    }

    fun noData(isSelections: Boolean) {
        this.isSelections = isSelections
        val size = items.size
        items.clear()
        notifyItemRangeRemoved(0, size)
    }

    sealed class ViewHolder(binding: ViewBinding) : RecyclerView.ViewHolder(binding.root) {

        class ViewHolderQuiz(binding: VhQuizBinding) : ViewHolder(binding) {
            val quizName = binding.quizName
            val quizSubtitle = binding.quizSubtitle
            val quizCheck = binding.quizCheck
        }

        class ViewHolderNewSelection(binding: VhNewSelectionBinding) : ViewHolder(binding) {
            val quizName = binding.quizName
        }

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