package com.jehutyno.yomikata.repository

import com.jehutyno.yomikata.model.Word
import com.jehutyno.yomikata.util.QuizType


/**
 * Created by valentin on 27/09/2016.
 */
interface WordRepository {
    interface LoadWordsCallback {
        fun onWordsLoaded(words: List<Word>)
        fun onDataNotAvailable()
    }
    interface GetWordsCallback {
        fun onWordsLoaded(task:Word)
        fun onDataNotAvailable()
    }
    suspend fun getWords(quizId: Long, callback: LoadWordsCallback)
    suspend fun getWords(quizIds: LongArray, callback: LoadWordsCallback)
    suspend fun searchWords(searchString: String, callback: LoadWordsCallback)
    suspend fun deleteAllWords()
    suspend fun deleteWord(wordId: Long)
    suspend fun isWordInQuiz(wordId:Long, quizId:Long) : Boolean
    suspend fun isWordInQuizzes(wordId: Long, quizIds: Array<Long>) : ArrayList<Boolean>
    suspend fun updateWordLevel(wordId: Long, level: Int)
    suspend fun getRandomWords(wordId: Long, answer: String, wordSize: Int, limit: Int, quizType: QuizType): ArrayList<Word>
    suspend fun updateWordPoints(wordId: Long, points: Int)
    suspend fun getWordsByRepetition(quizIds: LongArray, repetition: Int, limit: Int): ArrayList<Word>
    suspend fun updateWordRepetition(wordId: Long, repetition: Int)
    suspend fun decreaseWordsRepetition(quizIds: LongArray)
    suspend fun updateWordSelected(wordId: Long, check: Boolean)
    suspend fun getWordsByLevel(quizIds: LongArray, level: Int, callback: LoadWordsCallback)
    suspend fun getAllWords() : List<Word>
    suspend fun getWordById(wordId: Long): Word
    suspend fun updateWord(updateWord: Word, word: Word?)
    suspend fun updateWordProgression(updateWord: Word, word: Word)
}
