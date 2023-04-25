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
    suspend fun getQuiz(category: Int, callback: LoadQuizCallback)
    suspend fun getQuiz(quizId: Long, callback: GetQuizCallback)
    suspend fun saveQuiz(quizName: String, category: Int) : Long
    suspend fun deleteAllQuiz()
    suspend fun deleteQuiz(quizId:Long)
    suspend fun updateQuizName(quizId: Long, quizName: String)
    suspend fun updateQuizSelected(quizId: Long, isSelected: Boolean)
    suspend fun addWordToQuiz(wordId: Long, quizId: Long)
    suspend fun deleteWordFromQuiz(wordId: Long, quizId: Long)
    suspend fun countWordsForLevel(quizIds: LongArray, level: Int): Int
    suspend fun countWordsForQuizzes(quizIds: LongArray): Int
}
