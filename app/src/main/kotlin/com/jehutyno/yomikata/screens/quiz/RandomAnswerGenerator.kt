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
     * Generates 3 random distractor words from the repository and inserts the correct word at a
     * random position among them.
     *
     * @param word The correct word for this question
     * @param quizType The quiz type (used for filtering distractors)
     * @param answerToAvoid The answer string to avoid duplicating in distractors
     * @return An ArrayList of 4 (Word, colorInt) pairs with the correct word at a random index
     */
    suspend fun generateQCMRandoms(word: Word, quizType: QuizType, answerToAvoid: String): ArrayList<Pair<Word, Int>> {
        // Generate 3 different random words
        val random = wordRepository.getRandomWords(word.id, answerToAvoid, word.japanese.length, 3, quizType)
        // TODO: this may crash if getRandomWords returns less than 3 words
        val randoms = arrayListOf<Pair<Word, Int>>()
        random.forEach { randoms.add(Pair(it, android.R.color.white)) }
        // Add the good answer at a random place
        randoms.add(rng.nextInt(4), Pair(word, android.R.color.white))

        return randoms
    }

}
