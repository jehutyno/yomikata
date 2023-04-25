package com.jehutyno.yomikata.screens.content

import com.jehutyno.yomikata.BasePresenter
import com.jehutyno.yomikata.BaseView
import com.jehutyno.yomikata.model.Quiz
import com.jehutyno.yomikata.model.Word
import kotlinx.coroutines.Job
import java.util.*

/**
 * Created by valentin on 27/09/2016.
 */
interface ContentContract {

    interface View: BaseView<Presenter> {
        fun displayWords(words: List<Word>)
        fun displayStats(): Job
        fun selectionLoaded(quizzes: List<Quiz>)
        fun noSelections()
    }

    interface Presenter: BasePresenter {
        fun loadWords(quizIds: LongArray, level: Int) : Job
        suspend fun countLow(ids: LongArray): Int
        suspend fun countMedium(ids: LongArray): Int
        suspend fun countHigh(ids: LongArray): Int
        suspend fun countMaster(ids: LongArray): Int
        suspend fun countQuiz(ids: LongArray): Int
        suspend fun updateWordCheck(id: Long, check: Boolean)
        suspend fun loadSelections()
        suspend fun isWordInQuiz(wordId: Long, quizId: Long): Boolean
        suspend fun createSelection(quizName: String): Long
        suspend fun addWordToSelection(wordId: Long, quizId: Long)
        suspend fun isWordInQuizzes(wordId: Long, quizIds: Array<Long>): ArrayList<Boolean>
        suspend fun deleteWordFromSelection(wordId: Long, selectionId: Long)
    }

}
