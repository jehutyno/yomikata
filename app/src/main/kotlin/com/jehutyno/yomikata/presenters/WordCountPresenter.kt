package com.jehutyno.yomikata.presenters

import androidx.lifecycle.LiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.distinctUntilChanged
import com.jehutyno.yomikata.repository.QuizRepository
import com.jehutyno.yomikata.util.Level
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow


class WordCountPresenter(quizRepository: QuizRepository, quizIdsFlow: Flow<LongArray>): WordCountInterface {

    // If quizIds does not change, use this constructor
    constructor(quizRepository: QuizRepository, quizIds: LongArray) : this(quizRepository, flow{emit(quizIds)})

    private fun getCount(quizRepository: QuizRepository, quizIds: LongArray, level: Level?): Flow<Int> {
        return if (level == null)
            quizRepository.countWordsForQuizzes(quizIds)
        else
            quizRepository.countWordsForLevel(quizIds, level)
    }

    @ExperimentalCoroutinesApi
    private fun getCountFlatMap(quizRepository: QuizRepository, quizIdsFlow: Flow<LongArray>, level: Level?): LiveData<Int> {
        return quizIdsFlow.flatMapLatest { quizIds ->
            getCount(quizRepository, quizIds, level)
        }.asLiveData().distinctUntilChanged()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override val quizCount = getCountFlatMap(quizRepository, quizIdsFlow, null)
    @OptIn(ExperimentalCoroutinesApi::class)
    override val lowCount = getCountFlatMap(quizRepository, quizIdsFlow, Level.LOW)
    @OptIn(ExperimentalCoroutinesApi::class)
    override val mediumCount = getCountFlatMap(quizRepository, quizIdsFlow, Level.MEDIUM)
    @OptIn(ExperimentalCoroutinesApi::class)
    override val highCount = getCountFlatMap(quizRepository, quizIdsFlow, Level.HIGH)
    @OptIn(ExperimentalCoroutinesApi::class)
    override val masterCount = getCountFlatMap(quizRepository, quizIdsFlow, Level.MASTER).asFlow().combine(
                getCountFlatMap(quizRepository, quizIdsFlow, Level.MAX).asFlow()
        ) { master, max -> master + max }.asLiveData().distinctUntilChanged()

}
