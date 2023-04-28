package com.jehutyno.yomikata.screens.search

import androidx.lifecycle.LiveData
import com.jehutyno.yomikata.BasePresenter
import com.jehutyno.yomikata.BaseView
import com.jehutyno.yomikata.model.Quiz
import com.jehutyno.yomikata.model.Word


/**
 * Created by valentin on 13/10/2016.
 */
interface SearchResultContract {

    interface View: BaseView<Presenter> {
        fun displayResults(words: List<Word>)
        fun displayNoResults()
    }

    interface Presenter: BasePresenter {
        val words : LiveData<List<Word>>
        suspend fun getSelections(): List<Quiz>
        fun updateSearchString(newSearchString: String)
        suspend fun isWordInQuiz(wordId: Long, quizId: Long): Boolean
        suspend fun createSelection(quizName: String): Long
        suspend fun addWordToSelection(wordId: Long, quizId: Long)
        suspend fun isWordInQuizzes(wordId: Long, quizIds: Array<Long>): ArrayList<Boolean>
        suspend fun deleteWordFromSelection(wordId: Long, selectionId: Long)
        suspend fun updateWordCheck(id: Long, check: Boolean)
    }

}
