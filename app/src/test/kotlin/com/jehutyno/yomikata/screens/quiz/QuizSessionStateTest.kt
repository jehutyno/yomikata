package com.jehutyno.yomikata.screens.quiz

import com.jehutyno.yomikata.model.Word
import com.jehutyno.yomikata.util.quiz.Level
import com.jehutyno.yomikata.util.quiz.QuizType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test


class QuizSessionStateTest {

    private lateinit var state: QuizSessionState

    private fun makeWord(id: Long) =
        Word(id, "jp$id", "en$id", "fr$id", "rd$id", Level.LOW, 0, 0, 0, 0, 0, 0, 0, 0, null)

    @Before
    fun setUp() {
        state = QuizSessionState().also {
            it.quizWords = listOf(
                Pair(makeWord(1L), QuizType.TYPE_JAP_EN),
                Pair(makeWord(2L), QuizType.TYPE_EN_JAP),
            )
            it.errors = arrayListOf(
                Pair(makeWord(10L), QuizType.TYPE_PRONUNCIATION),
            )
        }
    }

    @Test
    fun initial_state_is_normal_mode() {
        assertFalse(state.errorMode)
        assertEquals(-1, state.currentItem)
        assertEquals(-1, state.currentItemErrorMode)
    }

    // --- increment ---

    @Test
    fun increment_advances_currentItem_in_normal_mode() {
        state.errorMode = false
        state.increment()
        assertEquals(0, state.currentItem)
        assertEquals(-1, state.currentItemErrorMode) // error index unchanged
    }

    @Test
    fun increment_advances_currentItemErrorMode_in_error_mode() {
        state.errorMode = true
        state.increment()
        assertEquals(-1, state.currentItem) // normal index unchanged
        assertEquals(0, state.currentItemErrorMode)
    }

    @Test
    fun multiple_increments_accumulate() {
        state.errorMode = false
        repeat(3) { state.increment() }
        assertEquals(2, state.currentItem)
    }

    // --- reset ---

    @Test
    fun reset_sets_currentItem_to_minus1_in_normal_mode() {
        state.errorMode = false
        state.currentItem = 5
        state.reset()
        assertEquals(-1, state.currentItem)
    }

    @Test
    fun reset_sets_currentItemErrorMode_to_minus1_in_error_mode() {
        state.errorMode = true
        state.currentItemErrorMode = 5
        state.reset()
        assertEquals(-1, state.currentItemErrorMode)
    }

    // --- getActiveIndex ---

    @Test
    fun getActiveIndex_returns_currentItem_in_normal_mode() {
        state.errorMode = false
        state.currentItem = 1
        assertEquals(1, state.getActiveIndex())
    }

    @Test
    fun getActiveIndex_returns_currentItemErrorMode_in_error_mode() {
        state.errorMode = true
        state.currentItemErrorMode = 0
        assertEquals(0, state.getActiveIndex())
    }

    // --- getCurrentWord ---

    @Test
    fun getCurrentWord_returns_from_quizWords_in_normal_mode() {
        state.errorMode = false
        state.currentItem = 0
        assertEquals(1L, state.getCurrentWord().id)
    }

    @Test
    fun getCurrentWord_returns_second_quizWord_in_normal_mode() {
        state.errorMode = false
        state.currentItem = 1
        assertEquals(2L, state.getCurrentWord().id)
    }

    @Test
    fun getCurrentWord_returns_from_errors_in_error_mode() {
        state.errorMode = true
        state.currentItemErrorMode = 0
        assertEquals(10L, state.getCurrentWord().id)
    }

    @Test
    fun getCurrentWord_with_explicit_index_overrides_current() {
        state.errorMode = false
        state.currentItem = 0
        assertEquals(2L, state.getCurrentWord(1).id)
    }

    // --- getCurrentQuizType ---

    @Test
    fun getCurrentQuizType_returns_correct_type_in_normal_mode() {
        state.errorMode = false
        state.currentItem = 1
        assertEquals(QuizType.TYPE_EN_JAP, state.getCurrentQuizType())
    }

    @Test
    fun getCurrentQuizType_returns_correct_type_in_error_mode() {
        state.errorMode = true
        state.currentItemErrorMode = 0
        assertEquals(QuizType.TYPE_PRONUNCIATION, state.getCurrentQuizType())
    }

    // --- mode switching ---

    @Test
    fun switching_to_error_mode_uses_error_index() {
        state.currentItem = 1
        state.currentItemErrorMode = 0
        state.errorMode = true
        assertEquals(0, state.getActiveIndex())
    }

    @Test
    fun switching_back_to_normal_mode_restores_normal_index() {
        state.currentItem = 1
        state.errorMode = true
        state.errorMode = false
        assertEquals(1, state.getActiveIndex())
    }
}
