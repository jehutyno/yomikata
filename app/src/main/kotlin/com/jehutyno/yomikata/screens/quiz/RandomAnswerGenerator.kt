package com.jehutyno.yomikata.screens.quiz

import android.R
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
        val targetString = when (quizType) {
            QuizType.TYPE_PRONUNCIATION, QuizType.TYPE_PRONUNCIATION_QCM -> word.reading.split("/")[0].split(";")[0]
            else -> word.japanese.split("/")[0].split(";")[0]
        }
        val wordSize = targetString.length

        // Generate 3 different random words
        val random = wordRepository.getRandomWords(word.id, answerToAvoid, wordSize, 3, quizType)
        val randoms = arrayListOf<Pair<Word, Int>>()
        random.forEach { randoms.add(Pair(it, R.color.white)) }

        // If we have fewer than 3 distractors, fill up using getAllWords or padding
        if (randoms.size < 3) {
            val allWords = try {
                wordRepository.getAllWords().shuffled(rng)
            } catch (_: Exception) {
                emptyList()
            }
            val existingIds = randoms.map { it.first.id }.toSet() + word.id
            for (w in allWords) {
                if (randoms.size >= 3) break
                if (w.id !in existingIds) {
                    randoms.add(Pair(w, R.color.white))
                }
            }
        }

        // If still fewer than 3 (e.g. DB has < 4 words total), pad with word
        while (randoms.size < 3) {
            randoms.add(Pair(word, R.color.white))
        }

        // Ensure exactly 3 distractors before inserting the correct word
        val distractors = if (randoms.size > 3) ArrayList(randoms.subList(0, 3)) else randoms

        // Insert the good answer at a random place (0 to size inclusive)
        val insertIndex = rng.nextInt(distractors.size + 1)
        distractors.add(insertIndex, Pair(word, R.color.white))

        return distractors
    }

}
