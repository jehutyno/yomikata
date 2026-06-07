package com.jehutyno.yomikata.screens.quiz

import com.jehutyno.yomikata.model.StatAction
import com.jehutyno.yomikata.model.StatResult
import com.jehutyno.yomikata.model.Word
import com.jehutyno.yomikata.repository.StatsRepository
import com.jehutyno.yomikata.repository.WordRepository
import com.jehutyno.yomikata.util.quiz.Level
import java.util.Calendar


/**
 * Word Statistics Recorder
 *
 * Handles all stat/repetition/points/level persistence for words during a quiz session.
 */
class WordStatisticsRecorder(
    private val statsRepository: StatsRepository,
    private val wordRepository: WordRepository
) {

    suspend fun saveAnswerResultStat(word: Word, result: Boolean) {
        statsRepository.addStatEntry(StatAction.ANSWER_QUESTION, word.id,
            Calendar.getInstance().timeInMillis, if (result) StatResult.SUCCESS else StatResult.FAIL)
    }

    suspend fun saveWordSeenStat(word: Word) {
        statsRepository.addStatEntry(StatAction.WORD_SEEN, word.id,
            Calendar.getInstance().timeInMillis, StatResult.OTHER)
    }

    suspend fun updateRepetitions(id: Long, repetition: Int) {
        wordRepository.updateWordRepetition(id, repetition)
    }

    suspend fun decreaseAllRepetitions(quizIds: LongArray) {
        wordRepository.decreaseWordsRepetition(quizIds)
    }

    suspend fun updateWordPoints(wordId: Long, points: Int) {
        wordRepository.updateWordPoints(wordId, points)
    }

    suspend fun updateWordLevel(wordId: Long, level: Level) {
        wordRepository.updateWordLevel(wordId, level)
    }

}
