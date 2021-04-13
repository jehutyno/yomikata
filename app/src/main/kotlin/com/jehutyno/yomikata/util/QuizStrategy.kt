package com.jehutyno.yomikata.util

/**
 * Created by valentin on 04/11/2016.
 */
enum class QuizStrategy(val value: Int) {
    STRAIGHT(0),
    SHUFFLE(1),
    PROGRESSIVE(2),
    LOW_STRAIGHT(3),
    MEDIUM_STRAIGHT(4),
    HIGH_STRAIGHT(5),
    MASTER_STRAIGHT(6),
    LOW_SHUFFLE(7),
    MEDIUM_SHUFFLE(8),
    HIGH_SHUFFLE(9),
    MASTER_SHUFFLE(10)
}