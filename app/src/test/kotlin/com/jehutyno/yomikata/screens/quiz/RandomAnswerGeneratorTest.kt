package com.jehutyno.yomikata.screens.quiz

import com.jehutyno.yomikata.model.Word
import com.jehutyno.yomikata.repository.WordRepository
import com.jehutyno.yomikata.util.quiz.QuizType
import com.jehutyno.yomikata.util.quiz.getLevelFromPoints
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

/**
 * Tests for [RandomAnswerGenerator] — the QCM choice builder.
 *
 * The key invariant: the 4 returned choices always contain the 3 repository distractors plus the
 * correct word, and the correct word lands exactly at the index chosen by the RNG. A regression
 * here would either drop the correct answer or make its position predictable.
 */
class RandomAnswerGeneratorTest {

    private fun word(id: Long, en: String) = Word(
        id, "日本$id", en, en, "に",
        getLevelFromPoints(0), 0, 0, 0, 0, -1, 0, 3, 0, null
    )

    private val distractors = listOf(word(2, "a"), word(3, "b"), word(4, "c"))

    /** Generator whose repository returns [distractors] and whose RNG always returns [index]. */
    private fun generator(index: Int): RandomAnswerGenerator {
        val repo = mockk<WordRepository>()
        coEvery { repo.getRandomWords(any(), any(), any(), any(), any()) } returns ArrayList(distractors)
        val rng = object : Random() {
            override fun nextInt(bound: Int) = index
        }
        return RandomAnswerGenerator(repo, rng)
    }

    @Test
    fun `correct word is inserted at the rng index`() = runBlocking {
        val correct = word(1, "right")
        val result = generator(2).generateQCMRandoms(correct, QuizType.TYPE_JAP_EN, "right")
        assertEquals(4, result.size)
        assertEquals(correct, result[2].first)
    }

    @Test
    fun `the four choices contain the correct word and all three distractors`() = runBlocking {
        val correct = word(1, "right")
        val words = generator(0).generateQCMRandoms(correct, QuizType.TYPE_JAP_EN, "right").map { it.first }
        assertTrue("correct word missing", words.contains(correct))
        distractors.forEach { assertTrue("distractor ${it.id} missing", words.contains(it)) }
    }

    @Test
    fun `correct word can land at the first or last position`() = runBlocking {
        val correct = word(1, "right")
        assertEquals(correct,
            generator(0).generateQCMRandoms(correct, QuizType.TYPE_JAP_EN, "x")[0].first)
        assertEquals(correct,
            generator(3).generateQCMRandoms(correct, QuizType.TYPE_JAP_EN, "x")[3].first)
    }
}
