package com.jehutyno.yomikata.util

/**
 * Created by valentin on 04/11/2016.
 */
enum class QuizStrategy {
    STRAIGHT,    // do all words in default order
    SHUFFLE,     // do all words in a random order
    PROGRESSIVE; // study words depending on repetition values
}
