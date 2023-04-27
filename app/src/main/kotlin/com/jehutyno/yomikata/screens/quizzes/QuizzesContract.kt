package com.jehutyno.yomikata.screens.quizzes

import androidx.lifecycle.LiveData
import com.jehutyno.yomikata.BasePresenter
import com.jehutyno.yomikata.BaseView
import com.jehutyno.yomikata.model.Quiz
import com.jehutyno.yomikata.util.QuizStrategy


/**
 * Created by valentin on 27/09/2016.
 */
interface QuizzesContract {

    interface View : BaseView<Presenter> {
        fun displayQuizzes(quizzes: List<Quiz>)
        fun displayNoData()
        fun selectPronunciationQcm(isSelected: Boolean)
        fun selectPronunciation(isSelected: Boolean)
        fun selectAudio(isSelected: Boolean)
        fun selectEnJap(isSelected: Boolean)
        fun selectJapEn(isSelected: Boolean)
        fun selectAuto(isSelected: Boolean)
        fun launchQuiz(strategy: QuizStrategy, selectedTypes: IntArray, title: String)
    }

    interface Presenter : BasePresenter {
        val quizList : LiveData<List<Quiz>>
        suspend fun createQuiz(quizName: String)
        suspend fun updateQuizName(quizId: Long, quizName: String)
        suspend fun deleteQuiz(quizId: Long)
        suspend fun updateQuizCheck(id: Long, checked: Boolean)
        suspend fun countLow(ids: LongArray): Int
        suspend fun countQuiz(ids: LongArray): Int
        suspend fun countMedium(ids: LongArray): Int
        suspend fun countHigh(ids: LongArray): Int
        suspend fun countMaster(ids: LongArray): Int
        fun initQuizTypes()
        fun pronunciationQcmSwitch()
        fun pronunciationSwitch()
        fun audioSwitch()
        fun enJapSwitch()
        fun japEnSwitch()
        fun autoSwitch()
        suspend fun launchQuizClick(strategy: QuizStrategy, title: String, category: Int)
        fun getSelectedTypes(): IntArray
    }
}
