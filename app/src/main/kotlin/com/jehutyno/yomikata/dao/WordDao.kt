package com.jehutyno.yomikata.dao

import androidx.room.*
import androidx.sqlite.db.SupportSQLiteQuery
import com.jehutyno.yomikata.repository.local.RoomQuizWord
import com.jehutyno.yomikata.repository.local.RoomWords


@Dao
interface WordDao {
    @Query("SELECT * FROM words")
    fun getAllWords(): List<RoomWords>

    @Query("SELECT words.* FROM words JOIN quiz_word " +
           "ON quiz_word.word_id = words._id " +
           "AND quiz_word.quiz_id = :quizId")
    fun getWords(quizId: Long): List<RoomWords>

    @Query("SELECT words.* FROM words JOIN quiz_word " +
            "ON quiz_word.word_id = words._id " +
            "AND quiz_word.quiz_id IN (:quizIds)")
    fun getWords(quizIds: LongArray): List<RoomWords>

    @Query("SELECT words.* FROM words JOIN quiz_word " +
           "ON quiz_word.word_id = words._id " +
           "AND quiz_word.quiz_id IN (:quizIds) " +
           "AND words.level IN (:levels)")
    fun getWordsByLevels(quizIds: LongArray, levels: IntArray): List<RoomWords>

    @Query("SELECT words.* FROM words JOIN quiz_word " +
           "ON quiz_word.word_id = words._id " +
           "AND quiz_word.quiz_id IN (:quizIds) " +
           "AND words.repetition = :repetition ORDER BY words._id LIMIT :limit")
    fun getWordsByRepetition(quizIds: LongArray, repetition: Int, limit: Int): List<RoomWords>

    @Query("SELECT words._id FROM words JOIN quiz_word " +
           "ON quiz_word.word_id = words._id " +
           "AND quiz_word.quiz_id IN (:quizIds) " +
           "AND words.repetition > :repetition")
    fun getWordIdsWithRepetitionStrictlyGreaterThan(quizIds: LongArray, repetition: Int): LongArray

    @Query("UPDATE words SET repetition = repetition - 1 WHERE _id IN (:wordIds)")
    fun decreaseWordRepetitionByOne(wordIds: LongArray)

    @Query("SELECT words._id FROM words JOIN quiz_word " +
            "ON quiz_word.word_id = words._id " +    // select all quiz_words of the correct word id
            "AND quiz_word.quiz_id =  " +
            "( " +
            "SELECT quiz_id FROM quiz_word " +
            "WHERE quiz_word.word_id = :wordId " +      // such that the quiz_id matches
            "AND quiz_id <= :defaultSize " +            // that of the word with id=wordId
            ") " +
            "AND LENGTH(words.japanese) = :wordSize " +
            "AND words._id != :wordId")
    fun getWordsOfSizeRelatedTo(wordId: Long, wordSize: Int, defaultSize: Int): List<Long>

    @RawQuery
    fun getRandomWords(rawQuery: SupportSQLiteQuery): List<RoomWords>

    @Query("SELECT * FROM words " +
           "WHERE reading LIKE '%' || (:searchString) || '%' " +
           "OR reading LIKE '%' || (:hiraganaString) || '%' " +
           "OR japanese LIKE '%' || (:searchString) || '%' " +
           "OR japanese LIKE '%' || (:hiraganaString) || '%' " +
           "OR english LIKE '%' || (:searchString) || '%' " +
           "OR french LIKE '%' || (:searchString) || '%'")
    fun searchWords(searchString: String, hiraganaString: String): List<RoomWords>

    @Query("SELECT EXISTS ( " +
            "SELECT * FROM quiz_word " +
            "WHERE word_id = :wordId AND quiz_id = :quizId " +
           ")")
    fun isWordInQuiz(wordId: Long, quizId: Long): Boolean

    @Query("SELECT * FROM words WHERE _id = :wordId LIMIT 1")
    fun getWordById(wordId: Long): RoomWords?

    @Query("DELETE FROM words")
    fun deleteAllWords()

    @Delete
    fun deleteWord(word: RoomWords)

    @Update
    fun updateWord(word: RoomWords)

    @Query("UPDATE words SET points = :points WHERE _id = :wordId")
    fun updateWordPoints(wordId: Long, points: Int)

    @Query("UPDATE words SET level = :level, " +
                                "points = 0 " +
           "WHERE _id = :wordId")
    fun updateWordLevel(wordId: Long, level: Int)

    @Query("UPDATE words SET repetition = :repetition WHERE _id = :wordId")
    fun updateWordRepetition(wordId: Long, repetition: Int)

    @Query("UPDATE words SET isSelected = :isSelected WHERE _id = :wordId")
    fun updateWordSelected(wordId: Long, isSelected: Boolean)

    @Query("SELECT * FROM quiz_word WHERE quiz_id = :quizId AND word_id = :wordId")
    fun getQuizWordFromId(quizId: Long, wordId: Long): RoomQuizWord?

    @Insert
    fun addQuizWord(quizWord: RoomQuizWord): Long

    @Insert
    fun addWord(word: RoomWords): Long
}
