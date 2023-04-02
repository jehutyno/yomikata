package com.jehutyno.yomikata.repository

import com.jehutyno.yomikata.model.Quiz


/**
 * Created by valentin on 27/09/2016.
 */
interface QuizRepository {
    interface LoadQuizCallback {
        fun onQuizLoaded(quizzes: List<Quiz>)
        fun onDataNotAvailable()
    }
    interface GetQuizCallback {
        fun onQuizLoaded(quiz: Quiz)
        fun onDataNotAvailable()
    }
    fun getQuiz(category: Int, callback: LoadQuizCallback)
    fun getQuiz(quizId: Long, callback: GetQuizCallback)
    fun saveQuiz(quizName: String, category: Int) : Long
    fun refreshQuiz()
    fun deleteAllQuiz()
    fun deleteQuiz(quizId:Long)
    fun updateQuizName(quizId: Long, quizName: String)
    fun updateQuizSelected(quizId: Long, isSelected: Boolean)
    fun addWordToQuiz(wordId: Long, quizId: Long)
    fun deleteWordFromQuiz(wordId: Long, quizId: Long)
    fun countWordsForLevel(quizIds: LongArray, level: Int): Int
    fun countWordsForQuizzes(quizIds: LongArray): Int
}
