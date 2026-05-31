package com.jehutyno.yomikata.screens.quiz

import com.jehutyno.yomikata.model.Word
import com.jehutyno.yomikata.util.QuizType


/**
 * Holds the mutable state of an ongoing quiz session: the word lists, current positions,
 * and whether the session is in error-review mode.
 */
class QuizSessionState {

    /** False for normal session, True for error session (reviewing incorrect words).
     *  Does not apply for progressive study sessions */
    var errorMode = false

    var quizWords = listOf<Pair<Word, QuizType>>()
    var errors = arrayListOf<Pair<Word, QuizType>>()

    /** The index into [quizWords] of the current word */
    var currentItem = -1

    /** The index into [errors] when in error-review mode */
    var currentItemErrorMode = -1

    /** Increment the active index */
    fun increment() {
        if (errorMode) currentItemErrorMode++ else currentItem++
    }

    /** Reset the active index to -1 */
    fun reset() {
        if (errorMode) currentItemErrorMode = -1 else currentItem = -1
    }

    /** Returns the active index */
    fun getActiveIndex(): Int = if (errorMode) currentItemErrorMode else currentItem

    /** Get the current word depending on whether [errorMode] is active */
    fun getCurrentWord(index: Int? = null): Word {
        return if (errorMode)
            errors[index ?: currentItemErrorMode].first
        else
            quizWords[index ?: currentItem].first
    }

    /** Get the current quiz type depending on whether [errorMode] is active */
    fun getCurrentQuizType(index: Int? = null): QuizType {
        return if (errorMode)
            errors[index ?: currentItemErrorMode].second
        else
            quizWords[index ?: currentItem].second
    }
}
