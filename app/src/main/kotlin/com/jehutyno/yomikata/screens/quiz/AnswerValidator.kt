package com.jehutyno.yomikata.screens.quiz

import com.jehutyno.yomikata.model.Word
import com.jehutyno.yomikata.util.QuizType


/**
 * Answer Validator
 *
 * Pure helper object for checking whether a user's answer matches the correct word.
 */
object AnswerValidator {

    /**
     * Check word
     *
     * Checks if the given answer matches the correct word.
     *
     * @param word The correct word
     * @param quizType The quizType corresponding to the word
     * @param answer The user's answer. If keyboard entry, this method will parse it
     * @return True if answer matches word, False otherwise.
     */
    fun checkWord(word: Word, quizType: QuizType, answer: String): Boolean {
        when (quizType) {
            QuizType.TYPE_JAP_EN -> {
                return word.getTrad().trim() == answer
            }
            QuizType.TYPE_EN_JAP -> {
                return word.japanese.trim() == answer
            }
            else -> {
                word.reading.split("/").forEach {
                    if (it.trim() == answer.trim().replace("-", "ー")) {
                        return true
                    }
                }
                word.reading.split(";").forEach {
                    if (it.trim() == answer.trim().replace("-", "ー")) {
                        return true
                    }
                }
            }
        }

        return false
    }

}
