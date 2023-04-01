package com.jehutyno.yomikata.repository.local

import androidx.room.*
import com.jehutyno.yomikata.model.QuizWord
import com.jehutyno.yomikata.model.Word
import java.util.ArrayList


@Dao
interface WordDao {
    @Query("SELECT * FROM RoomWords")
    fun getAllWords(): List<Word>

    @Query("SELECT * FROM RoomWords JOIN RoomQuizWord" +
           "ON RoomQuizWord.word_id = RoomWord._id" +
           "AND RoomQuizWord.quiz_id = :quizId")
    fun getWords(quizId: Long): List<Word>

    @Query("SELECT * FROM RoomWords JOIN RoomQuizWord" +
            "ON RoomQuizWord.word_id = RoomWord._id" +
            "AND RoomQuizWord.quiz_id IN :quizId")
    fun getWords(quizIds: LongArray): List<Word>

    @Query("SELECT * FROM RoomWords JOIN RoomQuizWord" +
           "ON RoomQuizWord.word_id = RoomWords._id" +
           "AND RoomQuizWord.quiz_id IN :quizIds" +
           "AND RoomWords.level IN :levels")
    fun getWordsByLevels(quizIds: LongArray, levels: IntArray): List<Word>

    @Query("SELECT * FROM RoomWords JOIN RoomQuizWord" +
           "ON RoomQuizWord.word_id = RoomWords._id" +
           "AND RoomQuizWord.quiz_id IN :quizIds" +
           "AND RoomWords.repetition = :repetition LIMIT :limit")
    fun getWordsByRepetition(quizIds: LongArray, repetition: Int, limit: Int): ArrayList<Word>

    // TODO: simplify query
    @Query("SELECT * FROM RoomWords JOIN RoomQuizWord" +
           "ON RoomQuizWord.word_id = RoomWords._id" +
           "AND RoomQuizWord.quiz_id = " +
           "(" +
                "SELECT quiz_id FROM RoomQuizWord" +
                "WHERE RoomQuizWord.word_id = :wordId" +
                "AND quiz_id <= :defaultSize" +
           ")" +
           "AND LENGTH(RoomWords.japanese) = :wordSize" +
           "AND :column != ':answer'" +
           "AND RoomWords._id != :wordId GROUP BY :column" +
           "ORDER BY RANDOM() LIMIT :limit")
    fun getRandomWords(wordId: Long, answer: String, wordSize: Int, column: String,
                       defaultSize: Int, limit: Int): ArrayList<Word>

    @Query("SELECT * FROM RoomWords" +
           "WHERE reading LIKE ':searchString'" +
           "OR reading LIKE ':hiraganaString'" +
           "OR japanese LIKE ':searchString'" +
           "OR japanese LIKE ':hiraganaString'" +
           "OR english LIKE ':searchString'" +
           "OR french LIKE ':searchString'")
    fun searchWords(searchString: String, hiraganaString: String): List<Word>

    @Query("EXISTS (" +
            "SELECT * FROM RoomQuizWord" +
            "WHERE word_id = :wordId AND quiz_id = :quizId" +
           ")")
    fun isWordInQuiz(wordId: Long, quizId: Long): Boolean

    @Query("SELECT * FROM RoomWords WHERE _id = :wordId")
    fun getWordById(wordId: Long): Word

    @Query("DELETE FROM RoomWords")
    fun deleteAllWords()

    @Delete
    fun deleteWord(word: RoomWords)

    @Update
    fun updateWord(word: RoomWords)

    @Query("SELECT * FROM RoomQuizWord WHERE quiz_id = :quizId AND word_id = :wordId")
    fun getQuizWordFromIds(quizId: Long, wordId: Long): QuizWord?

    @Insert
    fun addQuizWord(quizWord: RoomQuizWord)
}
