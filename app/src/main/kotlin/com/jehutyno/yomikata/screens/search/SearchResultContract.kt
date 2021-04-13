package com.jehutyno.yomikata.screens.search

import com.jehutyno.yomikata.BasePresenter
import com.jehutyno.yomikata.BaseView
import com.jehutyno.yomikata.model.Quiz
import com.jehutyno.yomikata.model.Word
import java.util.*

/**
 * Created by valentin on 13/10/2016.
 */
interface SearchResultContract {

    interface View: BaseView<Presenter> {
        fun displayResults(words: List<Word>)
        fun displayNoResults()
        fun selectionLoaded(quizzes: List<Quiz>)
        fun noSelections()
    }

    interface Presenter: BasePresenter {
        fun loadWords(searchString: String)
        fun loadSelections()
        fun isWordInQuiz(wordId: Long, quizId: Long): Boolean
        fun createSelection(quizName: String): Long
        fun addWordToSelection(wordId: Long, quizId: Long)
        fun isWordInQuizzes(wordId: Long, quizIds: Array<Long>): ArrayList<Boolean>
        fun deleteWordFromSelection(wordId: Long, selectionId: Long)
        fun updateWordCheck(id: Long, check: Boolean)
    }

}