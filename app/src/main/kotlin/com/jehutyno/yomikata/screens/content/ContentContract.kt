package com.jehutyno.yomikata.screens.content

import com.jehutyno.yomikata.BasePresenter
import com.jehutyno.yomikata.BaseView
import com.jehutyno.yomikata.model.Quiz
import com.jehutyno.yomikata.model.Word
import java.util.*

/**
 * Created by valentin on 27/09/2016.
 */
interface ContentContract {

    interface View: BaseView<Presenter> {
        fun displayWords(words: List<Word>)
        fun displayStats()
        fun selectionLoaded(quizzes: List<Quiz>)
        fun noSelections()
    }

    interface Presenter: BasePresenter {
        fun loadWords(quizIds: LongArray, level: Int)
        fun countLow(ids: LongArray): Int
        fun countMedium(ids: LongArray): Int
        fun countHigh(ids: LongArray): Int
        fun countMaster(ids: LongArray): Int
        fun countQuiz(ids: LongArray): Int
        fun updateWordCheck(id: Long, check: Boolean)
        fun loadSelections()
        fun isWordInQuiz(wordId: Long, quizId: Long): Boolean
        fun createSelection(quizName: String): Long
        fun addWordToSelection(wordId: Long, quizId: Long)
        fun isWordInQuizzes(wordId: Long, quizIds: Array<Long>): ArrayList<Boolean>
        fun deleteWordFromSelection(wordId: Long, selectionId: Long)
    }

}