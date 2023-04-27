package com.jehutyno.yomikata.screens.answers

import androidx.lifecycle.LiveData
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
    }

    interface Presenter: BasePresenter {
        val selections: LiveData<List<Quiz>>
        suspend fun createSelection(quizName: String): Long
        suspend fun addWordToSelection(wordId: Long, quizId: Long)
        suspend fun isWordInQuiz(wordId: Long, quizId: Long): Boolean
        suspend fun isWordInQuizzes(wordId: Long, quizIds: Array<Long>): ArrayList<Boolean>
        suspend fun deleteWordFromSelection(wordId: Long, selectionId: Long)
        suspend fun getWordById(id: Long): Word
        suspend fun getAnswersWordsSentences(answers: List<Answer>): List<Triple<Answer, Word, Sentence>>
        suspend fun getSentenceById(id: Long): Sentence
    }

}
