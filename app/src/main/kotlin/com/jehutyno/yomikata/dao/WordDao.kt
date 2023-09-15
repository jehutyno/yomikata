package com.jehutyno.yomikata.dao

import androidx.room.*
import androidx.sqlite.db.SupportSQLiteQuery
import com.jehutyno.yomikata.repository.local.RoomQuizWord
import com.jehutyno.yomikata.repository.local.RoomWords
import kotlinx.coroutines.flow.Flow


@Dao
interface WordDao {
    @Query("SELECT * FROM words")
    suspend fun getAllWords(): List<RoomWords>

    @Query("SELECT words.* FROM words JOIN quiz_word " +
           "ON quiz_word.word_id = words._id " +
           "AND quiz_word.quiz_id = :quizId")
    fun getWords(quizId: Long): Flow<List<RoomWords>>

    @Query("SELECT words.* FROM words JOIN quiz_word " +
            "ON quiz_word.word_id = words._id " +
            "AND quiz_word.quiz_id IN (:quizIds)")
    fun getWords(quizIds: LongArray): Flow<List<RoomWords>>

    @Query("SELECT words.* FROM words JOIN quiz_word " +
           "ON quiz_word.word_id = words._id " +
           "AND quiz_word.quiz_id IN (:quizIds) " +
           "AND words.level IN (:levels)")
    fun getWordsByLevels(quizIds: LongArray, levels: IntArray): Flow<List<RoomWords>>

    @Query("SELECT words.* FROM words JOIN quiz_word " +
           "ON quiz_word.word_id = words._id " +
           "AND quiz_word.quiz_id IN (:quizIds) " +
           "AND words.repetition = :repetition ORDER BY words._id LIMIT :limit")
    suspend fun getWordsByRepetition(quizIds: LongArray, repetition: Int, limit: Int): List<RoomWords>

    @Query("SELECT words._id FROM words JOIN quiz_word " +
           "ON quiz_word.word_id = words._id " +
           "AND quiz_word.quiz_id IN (:quizIds) " +
           "AND words.repetition > :repetition")
    suspend fun getWordIdsWithRepetitionStrictlyGreaterThan(quizIds: LongArray, repetition: Int): LongArray

    @Query("UPDATE words SET repetition = repetition - 1 WHERE _id IN (:wordIds)")
    suspend fun decreaseWordRepetitionByOne(wordIds: LongArray)

    @Query("SELECT words._id FROM words JOIN quiz_word " +
            "ON quiz_word.word_id = words._id " +    // select all quiz_words of the correct word id
            "AND quiz_word.quiz_id =  " +
            "( " +
            "SELECT quiz_id FROM quiz_word " +          // such that the quiz_id matches
            "WHERE quiz_word.word_id = :wordId " +      // that of the word with id=wordId
            "AND ( " +                              // category 8 = custom selections
            " (SELECT category FROM quiz WHERE _id = quiz_id LIMIT 1) != 8 " +
            ") " +
            ") " +
            "AND LENGTH(words.japanese) = :wordSize " +
            "AND words._id != :wordId")
    suspend fun getWordsOfSizeRelatedTo(wordId: Long, wordSize: Int): List<Long>

    @RawQuery
    suspend fun getRandomWords(rawQuery: SupportSQLiteQuery): List<RoomWords>

    @Query("SELECT * FROM words " +
           "WHERE reading LIKE '%' || (:searchString) || '%' " +
           "OR reading LIKE '%' || (:hiraganaString) || '%' " +
           "OR japanese LIKE '%' || (:searchString) || '%' " +
           "OR japanese LIKE '%' || (:hiraganaString) || '%' " +
           "OR english LIKE '%' || (:searchString) || '%' " +
           "OR french LIKE '%' || (:searchString) || '%'")
    fun searchWords(searchString: String, hiraganaString: String): Flow<List<RoomWords>>

    @Query("SELECT EXISTS ( " +
            "SELECT * FROM quiz_word " +
            "WHERE word_id = :wordId AND quiz_id = :quizId " +
           ")")
    suspend fun isWordInQuiz(wordId: Long, quizId: Long): Boolean

    @Query("SELECT * FROM words WHERE _id = :wordId LIMIT 1")
    suspend fun getWordById(wordId: Long): RoomWords?

    @Query("DELETE FROM words")
    suspend fun deleteAllWords()

    @Delete
    suspend fun deleteWord(word: RoomWords)

    @Update
    suspend fun updateWord(word: RoomWords)

    @Query("UPDATE words SET points = :points WHERE _id = :wordId")
    suspend fun updateWordPoints(wordId: Long, points: Int)

    @Query("UPDATE words SET level = :level, " +
                                "points = 0 " +
           "WHERE _id = :wordId")
    suspend fun updateWordLevel(wordId: Long, level: Int)

    @Query("UPDATE words SET repetition = :repetition WHERE _id = :wordId")
    suspend fun updateWordRepetition(wordId: Long, repetition: Int)

    @Query("UPDATE words SET isSelected = :isSelected WHERE _id = :wordId")
    suspend fun updateWordSelected(wordId: Long, isSelected: Boolean)

    @Query("UPDATE words SET isSelected = :isSelected WHERE _id IN (:wordIds)")
    suspend fun updateWordsSelected(wordIds: LongArray, isSelected: Boolean)

    @Insert
    suspend fun addQuizWord(quizWord: RoomQuizWord): Long

    @Insert
    suspend fun addWord(word: RoomWords): Long
}
