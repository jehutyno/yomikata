package com.jehutyno.yomikata.presenters

import com.jehutyno.yomikata.model.Word


interface WordInQuizInterface {
    suspend fun getWordById(id: Long): Word
    suspend fun isWordInQuiz(wordId: Long, quizId: Long): Boolean
    suspend fun isWordInQuizzes(wordId: Long, quizIds: Array<Long>): ArrayList<Boolean>
    suspend fun updateWordCheck(id: Long, check: Boolean)
    suspend fun updateWordsCheck(ids: LongArray, check: Boolean)
}
