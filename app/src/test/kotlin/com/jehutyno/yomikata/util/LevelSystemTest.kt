package com.jehutyno.yomikata.util

import com.jehutyno.yomikata.util.quiz.Level
import com.jehutyno.yomikata.util.quiz.QuizType
import com.jehutyno.yomikata.util.quiz.addPoints
import com.jehutyno.yomikata.util.quiz.getLevelFromPoints
import com.jehutyno.yomikata.util.quiz.getProgressToNextLevel
import com.jehutyno.yomikata.util.quiz.getRepetition
import com.jehutyno.yomikata.util.quiz.levelDown
import com.jehutyno.yomikata.util.quiz.levelUp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test


class LevelSystemTest {

    // --- getLevelFromPoints ---

    @Test
    fun getLevelFromPoints_zero_is_LOW() {
        assertEquals(Level.LOW, getLevelFromPoints(0))
    }

    @Test
    fun getLevelFromPoints_just_below_MEDIUM_threshold_is_LOW() {
        assertEquals(Level.LOW, getLevelFromPoints(Level.MEDIUM.minPoints - 1))
    }

    @Test
    fun getLevelFromPoints_at_MEDIUM_threshold_is_MEDIUM() {
        assertEquals(Level.MEDIUM, getLevelFromPoints(Level.MEDIUM.minPoints))
    }

    @Test
    fun getLevelFromPoints_at_HIGH_threshold_is_HIGH() {
        assertEquals(Level.HIGH, getLevelFromPoints(Level.HIGH.minPoints))
    }

    @Test
    fun getLevelFromPoints_at_MASTER_threshold_is_MASTER() {
        assertEquals(Level.MASTER, getLevelFromPoints(Level.MASTER.minPoints))
    }

    @Test
    fun getLevelFromPoints_above_MASTER_threshold_is_MASTER() {
        assertEquals(Level.MASTER, getLevelFromPoints(Level.MASTER.minPoints + 500))
    }

    // --- levelUp ---

    @Test
    fun levelUp_from_LOW_gives_MEDIUM() {
        assertEquals(Level.MEDIUM, getLevelFromPoints(levelUp(0)))
    }

    @Test
    fun levelUp_from_MEDIUM_gives_HIGH() {
        assertEquals(Level.HIGH, getLevelFromPoints(levelUp(Level.MEDIUM.minPoints)))
    }

    @Test
    fun levelUp_from_HIGH_gives_MASTER() {
        assertEquals(Level.MASTER, getLevelFromPoints(levelUp(Level.HIGH.minPoints)))
    }

    @Test
    fun levelUp_from_MASTER_stays_MASTER_and_increases_points() {
        val points = Level.MASTER.minPoints
        val newPoints = levelUp(points)
        assertEquals(Level.MASTER, getLevelFromPoints(newPoints))
        assertTrue(newPoints >= points)
    }

    @Test
    fun levelUp_preserves_progress_within_level() {
        // at 50% progress through LOW (minPoints=0, MEDIUM.minPoints=200) → points=100
        val points = 100
        val newPoints = levelUp(points)
        val progress = getProgressToNextLevel(newPoints)
        // progress should still be roughly 0.5 in the new level
        assertTrue("expected ~0.5 progress, got $progress", progress in 0.45f..0.55f)
    }

    // --- levelDown ---

    @Test
    fun levelDown_from_MEDIUM_gives_LOW() {
        assertEquals(Level.LOW, getLevelFromPoints(levelDown(Level.MEDIUM.minPoints)))
    }

    @Test
    fun levelDown_from_LOW_returns_zero() {
        assertEquals(0, levelDown(0))
    }

    @Test
    fun levelDown_from_LOW_non_zero_returns_zero() {
        assertEquals(0, levelDown(50))
    }

    @Test
    fun levelDown_from_MASTER_gives_HIGH() {
        assertEquals(Level.HIGH, getLevelFromPoints(levelDown(Level.MASTER.minPoints)))
    }

    // --- addPoints ---

    @Test
    fun addPoints_correct_increases_points() {
        val result = addPoints(100, true, QuizType.TYPE_JAP_EN, 2)
        assertTrue(result > 100)
    }

    @Test
    fun addPoints_wrong_decreases_points() {
        val result = addPoints(200, false, QuizType.TYPE_JAP_EN, 2)
        assertTrue(result < 200)
    }

    @Test
    fun addPoints_does_not_go_below_zero() {
        val result = addPoints(0, false, QuizType.TYPE_JAP_EN, 3)
        assertTrue(result >= 0)
    }

    @Test
    fun addPoints_does_not_exceed_max_850() {
        val result = addPoints(850, true, QuizType.TYPE_JAP_EN, 3)
        assertTrue(result <= 850)
    }

    @Test
    fun addPoints_harder_quiz_type_gives_more_points_on_correct() {
        val easy = addPoints(100, true, QuizType.TYPE_PRONUNCIATION_QCM, 2) // extraPoints = -10
        val hard = addPoints(100, true, QuizType.TYPE_PRONUNCIATION, 2)      // extraPoints = +15
        assertTrue(hard > easy)
    }

    @Test
    fun addPoints_higher_speed_increases_change() {
        val slow = addPoints(300, true, QuizType.TYPE_JAP_EN, 1)
        val fast = addPoints(300, true, QuizType.TYPE_JAP_EN, 3)
        assertTrue(fast > slow)
    }

    // --- getRepetition ---

    @Test
    fun getRepetition_is_always_positive() {
        assertTrue(getRepetition(0, true) > 0)
        assertTrue(getRepetition(0, false) > 0)
        assertTrue(getRepetition(850, true) > 0)
    }

    @Test
    fun getRepetition_increases_with_higher_points() {
        val low = getRepetition(0, true)
        val high = getRepetition(600, true)
        assertTrue(high > low)
    }

    @Test
    fun getRepetition_wrong_answer_is_lower_than_correct_at_same_points() {
        // wrong answer uses a larger normalization factor → smaller exponent → smaller repetition
        val correct = getRepetition(500, true)
        val wrong = getRepetition(500, false)
        assertTrue(wrong < correct)
    }
}
