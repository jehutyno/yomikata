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
    private fun generator(index: Int): RandomAnswerGenerator = generator(index, distractors)

    /**
     * Generator whose repository returns [available] distractors and whose RNG always returns
     * [index], clamped to the bound like a real [Random] would.
     */
    private fun generator(index: Int, available: List<Word>): RandomAnswerGenerator {
        val repo = mockk<WordRepository>()
        coEvery { repo.getRandomWords(any(), any(), any(), any(), any()) } returns ArrayList(available)
        val rng = object : Random() {
            override fun nextInt(bound: Int) = index.coerceAtMost(bound - 1)
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

    @Test
    fun `generateQCMRandoms handles fewer than 3 repository distractors without crashing`() = runBlocking {
        val correct = word(1, "right")
        val repo = mockk<WordRepository>()
        coEvery { repo.getRandomWords(any(), any(), any(), any(), any()) } returns ArrayList(listOf(word(2, "a")))
        coEvery { repo.getAllWords() } returns listOf(correct, word(2, "a"), word(3, "b"), word(4, "c"))
        val rng = object : Random() {
            override fun nextInt(bound: Int) = 1
        }
        val generator = RandomAnswerGenerator(repo, rng)
        val result = generator.generateQCMRandoms(correct, QuizType.TYPE_JAP_EN, "right")
        assertEquals(4, result.size)
        assertTrue(result.any { it.first.id == correct.id })
    }

    /**
     * The repository can return fewer than 3 distractors. Drawing the insertion index over a fixed
     * 4 then crashed with `IndexOutOfBoundsException` (crash Crashlytics 2.1.0) — the correct word
     * must still be returned, whatever the number of distractors available.
     */
    @Test
    fun `no distractor available still returns the correct word`() = runBlocking {
        val correct = word(1, "right")
        val result = generator(3, emptyList()).generateQCMRandoms(correct, QuizType.TYPE_JAP_EN, "right")
        assertEquals(1, result.size)
        assertEquals(correct, result[0].first)
    }

    @Test
    fun `fewer distractors than expected still returns the correct word`() = runBlocking {
        val correct = word(1, "right")
        for (count in 1..3) {
            val available = distractors.take(count)
            for (index in 0..3) {
                val result = generator(index, available)
                    .generateQCMRandoms(correct, QuizType.TYPE_JAP_EN, "right")
                assertEquals(count + 1, result.size)
                assertTrue("correct word missing (count=$count, index=$index)",
                    result.map { it.first }.contains(correct))
            }
        }
    }
}
