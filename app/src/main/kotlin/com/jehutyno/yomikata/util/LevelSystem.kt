package com.jehutyno.yomikata.util

import kotlin.math.ceil
import kotlin.math.exp

/**
 * Levels are used to sort words in separate lists that the user can easily select which
 * words to study based on how well they know them.
 */

enum class Level(val level: Int, val minPoints: Int) {
    // level ints must be declared as array indices (see Int.toLevel)
    LOW(0, 0),
    MEDIUM(1, 100),          // 0 -> 100     diff = 100
    HIGH(2, 250),            // 100 -> 250   diff = 150
    MASTER(3, 475),          // 250 -> 475   diff = 225
    MAX(4, 700)              // 700 gives repetition of about 1200 (see getRepetition)
}

fun Int.toLevel(): Level {
    return Level.values()[this]
}

/**
 * Get next level
 *
 * WARNING: remember to update the points as well!
 *
 * @param level Current level
 * @return The next level, or MAX if level is the MAX level
 */
fun getNextLevel(level: Level): Level {
    // add one, but cap at max level
    val levelPlusOne = (level.level + 1).coerceAtMost(Level.MAX.level)
    return levelPlusOne.toLevel()
}

/**
 * Get previous level
 *
 * WARNING: remember to update the points as well!
 *
 * @param level Current level
 * @return The previous level, or LOW if current level is LOW
 */
fun getPreviousLevel(level: Level): Level {
    // subtract one, but don't go below min level
    val levelPlusOne = (level.level - 1).coerceAtLeast(Level.LOW.level)
    return levelPlusOne.toLevel()
}

/**
 * The points of a word decides how well the user knows the word. The level is uniquely set by
 * the points based on exponentially spaced intervals. The gap between each interval is
 * multiplied by 1.5 each level up. This means higher levels get harder and harder to
 * reach. This goes along with the repetition values, which also grow exponentially so that
 * the high level words become rare in progressive study.
 */

fun getLevelFromPoints(points: Int): Level {
    return if (points < Level.MEDIUM.minPoints) {
        Level.LOW
    } else if (points < Level.HIGH.minPoints) {
        Level.MEDIUM
    } else if (points < Level.MASTER.minPoints) {
        Level.HIGH
    } else if (points < Level.MAX.minPoints) {
        Level.MASTER
    } else {
        Level.MAX
    }
}

/**
 * Get progress to next level
 *
 * @param points Current points
 * @return a Float between 0.0 and 1.0 corresponding to the progress to the next level
 */
fun getProgressToNextLevel(points: Int): Float {
    val currentLevel = getLevelFromPoints(points)
    val nextLevel = getNextLevel(currentLevel)

    val diff = (points - currentLevel.minPoints).toFloat()
    val totalDiff = (nextLevel.minPoints - currentLevel.minPoints).toFloat()
    return diff / totalDiff
}

/**
 * Get points with same progress
 *
 * @param points Current points
 * @param newLevel Level to update the points to
 * @return The new points with the same progress to the next level as the current points
 */
private fun getPointsWithSameProgress(points: Int, newLevel: Level): Int {
    // keep same point progress between levels
    val pointsProgress = getProgressToNextLevel(points) // current progress
    val newPointDifference = (getNextLevel(newLevel).minPoints - newLevel.minPoints).toFloat()
    return newLevel.minPoints + (pointsProgress * newPointDifference).toInt()
}

/**
 * Level up
 *
 * Returns new points by leveling up while keeping points progress the same.
 * If level is MAX, then points will be set to the MAX.minPoints (which is the max amount).
 *
 * @param points Current points
 * @return The new points
 */
fun levelUp(points: Int): Int {
    val level = getLevelFromPoints(points)
    val higherLevel = getNextLevel(level)
    if (higherLevel == level)
        return Level.MAX.minPoints    // no change in level --> lowest level --> return 0

    return getPointsWithSameProgress(points, higherLevel)
}

/**
 * Level down
 *
 * Returns new points by leveling down while keeping points progress the same.
 * If level is LOW, then points will be set to zero.
 *
 * @param points Current points
 * @return The new points
 */
fun levelDown(points: Int): Int {
    val level = getLevelFromPoints(points)
    val lowerLevel = getPreviousLevel(level)
    if (lowerLevel == level)
        return 0    // no change in level --> lowest level --> return 0

    return getPointsWithSameProgress(points, lowerLevel)
}

fun getRepetition(points: Int, answerIsCorrect: Boolean): Int {
    val maxRepetition = 1000
    val base =
        if (answerIsCorrect)
            100f    // for a correct answer: apply standard normalization factor
        else
            200f    // for a wrong answer: apply larger factor to reduce time to next word encounter
    val exponent: Float = points.toFloat() / base
    return ceil(exp(exponent)).toInt().coerceAtMost(maxRepetition)
}
