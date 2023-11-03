package com.jehutyno.yomikata.screens.quizzes

import androidx.lifecycle.LiveData
import com.jehutyno.yomikata.BasePresenter
import com.jehutyno.yomikata.model.Quiz
import com.jehutyno.yomikata.presenters.WordCountInterface
import com.jehutyno.yomikata.util.Level
import com.jehutyno.yomikata.util.QuizStrategy
import com.jehutyno.yomikata.util.QuizType


/**
 * Created by valentin on 27/09/2016.
 */
interface QuizzesContract {

    interface View {
        fun displayQuizzes(quizzes: List<Quiz>)
        fun displayNoData()
        fun selectQuizType(quizType: QuizType, isSelected: Boolean)
        fun launchQuiz(strategy: QuizStrategy, level: Level?, selectedTypes: ArrayList<QuizType>, title: String)
    }

    interface Presenter : BasePresenter, WordCountInterface {
        val quizList : LiveData<List<Quiz>>
        val selectedTypes: LiveData<ArrayList<QuizType>>
        suspend fun createQuiz(quizName: String)
        suspend fun updateQuizName(quizId: Long, quizName: String)
        suspend fun deleteQuiz(quizId: Long)
        suspend fun countQuiz(ids: LongArray): Int
        suspend fun updateQuizCheck(id: Long, checked: Boolean)
        fun initQuizTypes()
        fun quizTypeSwitch(quizType: QuizType)
        suspend fun onLaunchQuizClick(category: Int)
        fun getSelectedTypes(): ArrayList<QuizType>
    }
}
