package com.jehutyno.yomikata.screens.quiz

import com.jehutyno.yomikata.model.Word
import com.jehutyno.yomikata.util.quiz.Level
import com.jehutyno.yomikata.util.quiz.QuizType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test


class AnswerValidatorTest {

    // english == french so tests are locale-independent
    private fun word(translation: String = "apple", japanese: String = "リンゴ", reading: String = "りんご") =
        Word(1L, japanese, translation, translation, reading, Level.LOW, 0, 0, 0, 0, 0, 0, 0, 0, null)

    // --- TYPE_JAP_EN (translation matching) ---

    @Test
    fun jap_en_correct_answer() {
        assertTrue(AnswerValidator.checkWord(word(translation = "apple"), QuizType.TYPE_JAP_EN, "apple"))
    }

    @Test
    fun jap_en_wrong_answer() {
        assertFalse(AnswerValidator.checkWord(word(translation = "apple"), QuizType.TYPE_JAP_EN, "orange"))
    }

    @Test
    fun jap_en_trims_word_whitespace() {
        // getTrad() trims, so leading/trailing spaces in the stored value are stripped
        assertTrue(AnswerValidator.checkWord(word(translation = " apple "), QuizType.TYPE_JAP_EN, "apple"))
    }

    @Test
    fun jap_en_case_sensitive() {
        assertFalse(AnswerValidator.checkWord(word(translation = "apple"), QuizType.TYPE_JAP_EN, "Apple"))
    }

    // --- TYPE_EN_JAP (japanese matching) ---

    @Test
    fun en_jap_correct_answer() {
        assertTrue(AnswerValidator.checkWord(word(japanese = "リンゴ"), QuizType.TYPE_EN_JAP, "リンゴ"))
    }

    @Test
    fun en_jap_wrong_answer() {
        assertFalse(AnswerValidator.checkWord(word(japanese = "リンゴ"), QuizType.TYPE_EN_JAP, "ミカン"))
    }

    // --- Pronunciation types (reading matching) ---

    @Test
    fun pronunciation_correct_single_reading() {
        assertTrue(AnswerValidator.checkWord(word(reading = "りんご"), QuizType.TYPE_PRONUNCIATION, "りんご"))
    }

    @Test
    fun pronunciation_wrong_answer() {
        assertFalse(AnswerValidator.checkWord(word(reading = "りんご"), QuizType.TYPE_PRONUNCIATION, "ミカン"))
    }

    @Test
    fun pronunciation_slash_separator_first_reading() {
        val w = word(reading = "りんご/リンゴ")
        assertTrue(AnswerValidator.checkWord(w, QuizType.TYPE_PRONUNCIATION, "りんご"))
    }

    @Test
    fun pronunciation_slash_separator_second_reading() {
        val w = word(reading = "りんご/リンゴ")
        assertTrue(AnswerValidator.checkWord(w, QuizType.TYPE_PRONUNCIATION, "リンゴ"))
    }

    @Test
    fun pronunciation_semicolon_separator_first_reading() {
        val w = word(reading = "りんご;リンゴ")
        assertTrue(AnswerValidator.checkWord(w, QuizType.TYPE_PRONUNCIATION, "りんご"))
    }

    @Test
    fun pronunciation_semicolon_separator_second_reading() {
        val w = word(reading = "りんご;リンゴ")
        assertTrue(AnswerValidator.checkWord(w, QuizType.TYPE_PRONUNCIATION, "リンゴ"))
    }

    @Test
    fun pronunciation_dash_replaced_by_long_vowel_mark() {
        // User types ASCII dash, stored reading uses ー (katakana long vowel)
        val w = word(reading = "ゆーびん")
        assertTrue(AnswerValidator.checkWord(w, QuizType.TYPE_PRONUNCIATION, "ゆ-びん"))
    }

    @Test
    fun pronunciation_qcm_uses_same_reading_logic() {
        val w = word(reading = "りんご/リンゴ")
        assertTrue(AnswerValidator.checkWord(w, QuizType.TYPE_PRONUNCIATION_QCM, "リンゴ"))
    }

    @Test
    fun audio_uses_reading_logic() {
        val w = word(reading = "りんご")
        assertTrue(AnswerValidator.checkWord(w, QuizType.TYPE_AUDIO, "りんご"))
    }
}
