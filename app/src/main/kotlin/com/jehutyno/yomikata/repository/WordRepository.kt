package com.jehutyno.yomikata.repository

import com.jehutyno.yomikata.model.Word
import com.jehutyno.yomikata.util.Level
import com.jehutyno.yomikata.util.QuizType
import kotlinx.coroutines.flow.Flow


/**
 * Created by valentin on 27/09/2016.
 */
interface WordRepository {
    fun getWords(quizId: Long): Flow<List<Word>>
    fun getWords(quizIds: LongArray): Flow<List<Word>>
    fun searchWords(searchString: String): Flow<List<Word>>
    suspend fun deleteAllWords()
    suspend fun deleteWord(wordId: Long)
    suspend fun isWordInQuiz(wordId:Long, quizId:Long) : Boolean
    suspend fun isWordInQuizzes(wordId: Long, quizIds: Array<Long>) : ArrayList<Boolean>
    suspend fun updateWordLevel(wordId: Long, level: Level)
    suspend fun getRandomWords(wordId: Long, answer: String, wordSize: Int, limit: Int, quizType: QuizType): ArrayList<Word>
    suspend fun updateWordPoints(wordId: Long, points: Int)
    suspend fun getWordsByRepetition(quizIds: LongArray, repetition: Int, limit: Int): ArrayList<Word>
    suspend fun getWordsByMinRepetition(quizIds: LongArray, minRepetition: Int, limit: Int): ArrayList<Word>
    suspend fun updateWordRepetition(wordId: Long, repetition: Int)
    suspend fun decreaseWordsRepetition(quizIds: LongArray)
    suspend fun updateWordSelected(wordId: Long, check: Boolean)
    suspend fun updateWordsSelected(wordIds: LongArray, check: Boolean)
    fun getWordsByLevel(quizIds: LongArray, level: Int): Flow<List<Word>>
    suspend fun getAllWords() : List<Word>
    suspend fun getWordById(wordId: Long): Word
    suspend fun updateWord(updateWord: Word, word: Word?)
    suspend fun updateWordProgression(updateWord: Word, word: Word)
    suspend fun addQuizWord(quizId: Long, wordId: Long)
}
