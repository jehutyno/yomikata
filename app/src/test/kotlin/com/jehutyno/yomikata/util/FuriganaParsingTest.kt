package com.jehutyno.yomikata.util

import com.jehutyno.yomikata.model.Sentence
import com.jehutyno.yomikata.model.Word
import com.jehutyno.yomikata.util.quiz.getLevelFromPoints
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests for the furigana plain-text helpers in ActionsUtils.kt.
 *
 * Source annotation format is `{kanji;reading}`:
 *  - sentenceFuri replaces each annotation with its reading (kana),
 *  - sentenceNoFuri replaces each annotation with the kanji,
 *  - sentenceNoAnswerFuri strips the annotation only for the quiz answer word.
 *
 * Only well-formed inputs are exercised (the malformed-input path logs via android.util.Log,
 * which is not available on the JVM).
 */
class FuriganaParsingTest {

    private fun sentence(jap: String) = Sentence(1, jap, "en", "fr", 0)

    @Test
    fun `sentenceFuri replaces each annotation with its reading`() {
        assertEquals("きょうはあつい", sentenceFuri(sentence("{今日;きょう}は{暑;あつ}い")))
    }

    @Test
    fun `sentenceNoFuri replaces each annotation with the kanji`() {
        assertEquals("今日は暑い", sentenceNoFuri(sentence("{今日;きょう}は{暑;あつ}い")))
    }

    @Test
    fun `a sentence without annotations is returned unchanged`() {
        assertEquals("普通の文", sentenceFuri(sentence("普通の文")))
        assertEquals("普通の文", sentenceNoFuri(sentence("普通の文")))
    }

    @Test
    fun `sentenceNoAnswerFuri strips furigana only for the answer word`() {
        val word = Word(
            1, "今日", "today", "aujourd'hui", "きょう",
            getLevelFromPoints(0), 0, 0, 0, 0, -1, 0, 3, 0, null
        )
        assertEquals(
            "今日は{暑;あつ}い",
            sentenceNoAnswerFuri(sentence("{今日;きょう}は{暑;あつ}い"), word)
        )
    }
}
