package com.jehutyno.yomikata.presenters

import com.jehutyno.yomikata.repository.WordRepository


class WordInQuizPresenter(private val wordRepository: WordRepository): WordInQuizInterface {

    override suspend fun isWordInQuiz(wordId: Long, quizId: Long) : Boolean {
        return wordRepository.isWordInQuiz(wordId, quizId)
    }

    override suspend fun isWordInQuizzes(wordId: Long, quizIds: Array<Long>) : ArrayList<Boolean> {
        return wordRepository.isWordInQuizzes(wordId, quizIds)
    }
}
