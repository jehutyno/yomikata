package com.jehutyno.yomikata.screens.answers

import com.jehutyno.yomikata.BasePresenter
import com.jehutyno.yomikata.BaseView
import com.jehutyno.yomikata.model.Answer
import com.jehutyno.yomikata.model.Quiz
import com.jehutyno.yomikata.model.Sentence
import com.jehutyno.yomikata.model.Word
import java.util.*

/**
 * Created by valentin on 25/10/2016.
 */
interface AnswersContract {

    interface View: BaseView<Presenter> {
        fun displayAnswers()
        fun selectionLoaded(quizzes: List<Quiz>)
        fun noSelections()
    }

    interface Presenter: BasePresenter {

        fun loadSelections()
        fun createSelection(quizName: String): Long
        fun addWordToSelection(wordId: Long, quizId: Long)
        fun isWordInQuiz(wordId: Long, quizId: Long): Boolean
        fun isWordInQuizzes(wordId: Long, quizIds: Array<Long>): ArrayList<Boolean>
        fun deleteWordFromSelection(wordId: Long, selectionId: Long)
        fun getWordById(id: Long): Word
        fun getAnswersWordsSentences(answers: List<Answer>): List<Triple<Answer, Word, Sentence>>
        fun getSentenceById(id: Long): Sentence
    }

}
