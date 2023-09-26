package com.jehutyno.yomikata.util

import kotlin.math.ceil
import kotlin.math.pow


/**
 * Level
 *
 * Levels are used to sort words in separate lists so that the user can easily select which
 * words to study based on how well they know them.
 *
 * @property level Integer value corresponding to the level, should be usable as indices for an
 * array, so: 0, 1, 2, ...
 * @property minPoints The minimum number of points for a word to be in this Level.
 */
enum class Level(val level: Int, val minPoints: Int) {
    // level ints must be declared as array indices (see Int.toLevel)
    // make sure the declaration order is from lowest to highest
    LOW(0, 0),
    MEDIUM(1, 200),         // rep = 6
    HIGH(2, 400),           // rep = 28
    MASTER(3, 600)          // rep = 149
}
private const val MAX_POINTS = 850  // 850 gives repetition of about 1200 (see getRepetition)


fun Int.toLevel(): Level {
    return Level.values()[this]
}

/**
 * Get next level
 *
 * WARNING: remember to update the points as well! (see levelUp)
 *
 * @param level Current level
 * @return The next level, or MAX if level is the MAX level
 */
fun getNextLevel(level: Level): Level {
    // add one, but cap at max level
    val levelPlusOne = (level.level + 1).coerceAtMost(Level.MASTER.level)
    return levelPlusOne.toLevel()
}

/**
 * Get previous level
 *
 * WARNING: remember to update the points as well! (see levelDown)
 *
 * @param level Current level
 * @return The previous level, or LOW if current level is LOW
 */
fun getPreviousLevel(level: Level): Level {
    // subtract one, but don't go below min level
    val levelMinusOne = (level.level - 1).coerceAtLeast(Level.LOW.level)
    return levelMinusOne.toLevel()
}

/**
 * Get level from points
 *
 * The points of a word decides how well the user knows the word. The level is uniquely determined by
 * the points based on linearly spaced intervals.
 * The repetition values grow exponentially so that words will come up frequently at the lower
 * levels, but the high level words become rare in progressive study.
 *
 * @param points Integer
 * @return Level corresponding to the points
 */
fun getLevelFromPoints(points: Int): Level {
    return if (points < Level.MEDIUM.minPoints) {
        Level.LOW
    } else if (points < Level.HIGH.minPoints) {
        Level.MEDIUM
    } else if (points < Level.MASTER.minPoints) {
        Level.HIGH
    } else {
        Level.MASTER
    }
}

/**
 * Get progress to next level
 *
 * @param points Current points
 * @return a Float between 0.0 and 1.0 corresponding to the progress to the next level, or the
 * progress to [MAX_POINTS] if level is maxed.
 */
fun getProgressToNextLevel(points: Int): Float {
    val currentLevel = getLevelFromPoints(points)
    val nextLevel = getNextLevel(currentLevel)
    val nextLevelPoints = if (nextLevel == currentLevel) {
        MAX_POINTS  // current level is max
    } else {
        nextLevel.minPoints
    }

    val diff = (points - currentLevel.minPoints).toFloat()
    val totalDiff = (nextLevelPoints - currentLevel.minPoints).toFloat()
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
 * If current level is MASTER, then points are set to the maximum value.
 *
 * @param points Current points
 * @return The new points
 */
fun levelUp(points: Int): Int {
    val level = getLevelFromPoints(points)
    val higherLevel = getNextLevel(level)
    if (higherLevel == level)
        return MAX_POINTS    // no change in level --> highest level --> return max points

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

/**
 * Add points
 *
 * Returns the new number of points after answering a question based on QuizType and speed.
 *
 * @param points Current number of points
 * @param answerIsCorrect True if the answer was correct, false otherwise
 * @param quizType The QuizType of the answered quiz, used to rate the difficulty
 * @param speed The speed setting,  slow = 1, default = 2, fast = 3
 * @return The new points
 */
fun addPoints(points: Int, answerIsCorrect: Boolean, quizType: QuizType, speed: Int): Int {
    // if correct: simply add the points, if wrong: subtract base but add extra points
    val plusOrMinus = (if (answerIsCorrect) 1 else -1)
    return (
        points + (plusOrMinus * BASE_POINTS + quizType.extraPoints) * speed
    ).coerceIn(0, MAX_POINTS)   // should be positive and less than MAX
}

/**
 * Get repetition
 *
 * Gives the repetition using an exponential growth.
 *
 * @param points Integer of current points
 * @param answerIsCorrect True if the given answer was correct, false otherwise. Used to create
 * shorter repetition to allow incorrect words to be studied faster in progressive study.
 * @return
 */
fun getRepetition(points: Int, answerIsCorrect: Boolean): Int {
    val norm =      // used to normalize the exponent: big norm -> small changes to repetition
        if (answerIsCorrect)
            100f    // for a correct answer: apply standard normalization factor
        else
            200f    // for a wrong answer: apply larger factor to reduce time to next word encounter
    val exponent: Float = points.toFloat() / norm
    // value to multiply the repetition by if delta-points / norm = 1
    // for example: if delta-points = BASE_POINTS * default_speed = 50 * 2 = 100 and norm = 100
    val base = 2.3f
    return ceil(base.pow(exponent)).toInt()
}
