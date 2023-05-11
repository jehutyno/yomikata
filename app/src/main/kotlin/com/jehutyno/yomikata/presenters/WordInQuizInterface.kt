package com.jehutyno.yomikata.presenters


interface WordInQuizInterface {
    suspend fun isWordInQuiz(wordId: Long, quizId: Long): Boolean
    suspend fun isWordInQuizzes(wordId: Long, quizIds: Array<Long>): ArrayList<Boolean>
}
