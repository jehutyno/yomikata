package com.jehutyno.yomikata.screens.quizzes

import com.jehutyno.yomikata.BasePresenter
import com.jehutyno.yomikata.BaseView
import com.jehutyno.yomikata.model.Quiz
import com.jehutyno.yomikata.util.QuizStrategy

/**
 * Created by valentin on 27/09/2016.
 */
interface QuizzesContract {

    interface View : BaseView<Presenter> {
        fun onMenuItemClick(category: Int)
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
        fun loadQuizzes(category: Int)
        fun createQuiz(quizName: String)
        fun updateQuizName(quizId: Long, quizName: String)
        fun deleteQuiz(quizId: Long)
        fun updateQuizCheck(id: Long, checked: Boolean)
        fun countLow(ids: LongArray): Int
        fun countQuiz(ids: LongArray): Int
        fun countMedium(ids: LongArray): Int
        fun countHigh(ids: LongArray): Int
        fun countMaster(ids: LongArray): Int
        fun initQuizTypes()
        fun pronunciationQcmSwitch()
        fun pronunciationSwitch()
        fun audioSwitch()
        fun enJapSwitch()
        fun japEnSwitch()
        fun autoSwitch()
        fun launchQuizClick(strategy: QuizStrategy, title: String, category: Int)
        fun getSelectedTypes(): IntArray
    }
}