package com.jehutyno.yomikata.repository

import android.database.sqlite.SQLiteDatabase
import androidx.annotation.NonNull
import com.jehutyno.yomikata.model.Word
import com.jehutyno.yomikata.repository.migration.WordTable
import com.jehutyno.yomikata.util.QuizType
import java.util.*

/**
 * Created by valentin on 27/09/2016.
 */
interface WordRepository {
    interface LoadWordsCallback {
        fun onWordsLoaded(words:List<Word>)
        fun onDataNotAvailable()
    }
    interface GetWordsCallback {
        fun onWordsLoaded(task:Word)
        fun onDataNotAvailable()
    }
    fun getWords(quizId: Long, @NonNull callback:LoadWordsCallback)
    fun getWords(quizIds: LongArray, @NonNull callback:LoadWordsCallback)
    fun searchWords(searchString: String, @NonNull callback:LoadWordsCallback)
    fun saveWord(@NonNull task:Word)
    fun refreshWords()
    fun deleteAllWords()
    fun deleteWord(@NonNull wordId:String)
    fun isWordInQuiz(wordId:Long, quizId:Long) : Boolean
    fun isWordInQuizzes(wordId: Long, quizIds: Array<Long>) : ArrayList<Boolean>
    fun updateWordLevel(wordId: Long, level: Int)
    fun getRandomWords(wordId: Long, answer: String, wordSize: Int, limit: Int, quizType: QuizType): ArrayList<Word>
    fun updateWordPoints(wordId: Long, points: Int)
    fun getWordsByRepetition(quizIds: LongArray, repetition: Int, limit: Int): ArrayList<Word>
    fun updateWordRepetition(wordId: Long, repetition: Int)
    fun decreaseWordsRepetition(quizIds: LongArray)
    fun restoreWord(word: String, prononciation: String, wordTable: WordTable)
    fun updateWordSelected(id: Long, check: Boolean)
    fun getWordsByLevel(quizIds: LongArray, level: Int, callback: LoadWordsCallback)
    fun getAllWords(db: SQLiteDatabase?) : List<Word>
    fun getWordById(wordId: Long): Word
    fun updateWord(updateWord: Word, word: Word?)
    fun updateWordProgression(updateWord: Word, word: Word?)
    fun migration_8to9()
}