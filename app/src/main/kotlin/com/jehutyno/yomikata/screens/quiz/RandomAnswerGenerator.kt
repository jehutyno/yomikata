package com.jehutyno.yomikata.screens.quiz

import com.jehutyno.yomikata.model.Word
import com.jehutyno.yomikata.repository.WordRepository
import com.jehutyno.yomikata.util.quiz.QuizType
import java.util.Random


/**
 * Random Answer Generator
 *
 * Generates the 4 QCM answer choices (3 distractors + 1 correct answer) for a quiz question.
 */
class RandomAnswerGenerator(
    private val wordRepository: WordRepository,
    private val rng: Random
) {

    /**
     * Generate QCM randoms
     *
     * Generates up to 3 random distractor words from the repository and inserts the correct word at
     * a random position among them.
     *
     * The repository can return **fewer than 3** distractors (word whose only quiz is a user
     * selection, no other word of a comparable size in the same quiz, all candidates filtered out
     * by [answerToAvoid]...). The insertion index is therefore drawn over the number of choices
     * actually available — drawing over a fixed 4 crashed with `IndexOutOfBoundsException` when the
     * list was shorter (crash Crashlytics 2.1.0).
     *
     * @param word The correct word for this question
     * @param quizType The quiz type (used for filtering distractors)
     * @param answerToAvoid The answer string to avoid duplicating in distractors
     * @return An ArrayList of up to 4 (Word, colorInt) pairs with the correct word at a random index
     */
    suspend fun generateQCMRandoms(word: Word, quizType: QuizType, answerToAvoid: String): ArrayList<Pair<Word, Int>> {
        // Generate up to 3 different random words
        val random = wordRepository.getRandomWords(word.id, answerToAvoid, word.japanese.length, 3, quizType)
        val randoms = arrayListOf<Pair<Word, Int>>()
        random.forEach { randoms.add(Pair(it, android.R.color.white)) }
        // Add the good answer at a random place among the available slots
        randoms.add(rng.nextInt(randoms.size + 1), Pair(word, android.R.color.white))

        return randoms
    }

}
