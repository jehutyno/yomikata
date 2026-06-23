package com.jehutyno.yomikata.screens.quiz

import com.jehutyno.yomikata.model.StatAction
import com.jehutyno.yomikata.model.StatResult
import com.jehutyno.yomikata.model.Word
import com.jehutyno.yomikata.repository.StatsRepository
import com.jehutyno.yomikata.repository.WordRepository
import com.jehutyno.yomikata.util.quiz.Level
import com.jehutyno.yomikata.util.quiz.getLevelFromPoints
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Test

/**
 * Tests for [WordStatisticsRecorder] — verifies it maps quiz outcomes to the right stat
 * action/result and delegates word-state updates to the repositories unchanged.
 */
class WordStatisticsRecorderTest {

    private val statsRepository = mockk<StatsRepository>(relaxed = true)
    private val wordRepository = mockk<WordRepository>(relaxed = true)
    private val recorder = WordStatisticsRecorder(statsRepository, wordRepository)

    private fun word(id: Long) = Word(
        id, "水", "water", "eau", "みず",
        getLevelFromPoints(0), 0, 0, 0, 0, -1, 0, 3, 0, null
    )

    @Test
    fun `saveAnswerResultStat records SUCCESS for a correct answer`() = runBlocking {
        recorder.saveAnswerResultStat(word(7), true)
        coVerify(exactly = 1) {
            statsRepository.addStatEntry(StatAction.ANSWER_QUESTION, 7L, any(), StatResult.SUCCESS)
        }
    }

    @Test
    fun `saveAnswerResultStat records FAIL for a wrong answer`() = runBlocking {
        recorder.saveAnswerResultStat(word(7), false)
        coVerify(exactly = 1) {
            statsRepository.addStatEntry(StatAction.ANSWER_QUESTION, 7L, any(), StatResult.FAIL)
        }
    }

    @Test
    fun `saveWordSeenStat records WORD_SEEN with OTHER result`() = runBlocking {
        recorder.saveWordSeenStat(word(9))
        coVerify(exactly = 1) {
            statsRepository.addStatEntry(StatAction.WORD_SEEN, 9L, any(), StatResult.OTHER)
        }
    }

    @Test
    fun `update helpers delegate to the word repository`() = runBlocking {
        val quizIds = slot<LongArray>()
        recorder.updateRepetitions(3L, 12)
        recorder.decreaseAllRepetitions(longArrayOf(1L, 2L))
        recorder.updateWordPoints(3L, 250)
        recorder.updateWordLevel(3L, Level.MEDIUM)

        coVerify(exactly = 1) { wordRepository.updateWordRepetition(3L, 12) }
        coVerify(exactly = 1) { wordRepository.decreaseWordsRepetition(capture(quizIds)) }
        coVerify(exactly = 1) { wordRepository.updateWordPoints(3L, 250) }
        coVerify(exactly = 1) { wordRepository.updateWordLevel(3L, Level.MEDIUM) }
        // LongArray uses reference equality, so capture and compare contents explicitly
        assertArrayEquals(longArrayOf(1L, 2L), quizIds.captured)
    }
}
