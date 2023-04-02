package com.jehutyno.yomikata.repository.local

import androidx.room.*
import kotlin.collections.ArrayList


@Dao
interface WordDao {
    @Query("SELECT * FROM RoomWords")
    fun getAllWords(): List<RoomWords>

    @Query("SELECT * FROM RoomWords JOIN RoomQuizWord" +
           "ON RoomQuizWord.word_id = RoomWord._id" +
           "AND RoomQuizWord.quiz_id = :quizId")
    fun getWords(quizId: Long): List<RoomWords>

    @Query("SELECT * FROM RoomWords JOIN RoomQuizWord" +
            "ON RoomQuizWord.word_id = RoomWord._id" +
            "AND RoomQuizWord.quiz_id IN :quizId")
    fun getWords(quizIds: LongArray): List<RoomWords>

    @Query("SELECT * FROM RoomWords JOIN RoomQuizWord" +
           "ON RoomQuizWord.word_id = RoomWords._id" +
           "AND RoomQuizWord.quiz_id IN :quizIds" +
           "AND RoomWords.level IN :levels")
    fun getWordsByLevels(quizIds: LongArray, levels: IntArray): List<RoomWords>

    @Query("SELECT * FROM RoomWords JOIN RoomQuizWord" +
           "ON RoomQuizWord.word_id = RoomWords._id" +
           "AND RoomQuizWord.quiz_id IN :quizIds" +
           "AND RoomWords.repetition = :repetition LIMIT :limit")
    fun getWordsByRepetition(quizIds: LongArray, repetition: Int, limit: Int): ArrayList<RoomWords>

    @Query("SELECT RoomWords._id FROM RoomWords JOIN RoomQuizWords" +
           "ON RoomQuizWord.word_id = RoomWords._id" +
           "AND RoomQuizWord.quiz_id IN :quizIds" +
           "AND RoomWords.repetition > :repetition")
    fun getWordsWithRepetitionStrictlyGreaterThan(quizIds: LongArray, repetition: Int): LongArray

    @Query("UPDATE RoomWords SET repetition = repetition - 1 WHERE _id IN wordIds")
    fun decreaseWordRepetitionByOne(wordIds: LongArray)

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
           "AND RoomWords._id != :wordId" +
           "GROUP BY :column ORDER BY RANDOM() LIMIT :limit")
    fun getRandomWords(wordId: Long, answer: String, wordSize: Int, column: String,
                       defaultSize: Int, limit: Int): ArrayList<RoomWords>

    @Query("SELECT * FROM RoomWords" +
           "WHERE reading LIKE ':searchString'" +
           "OR reading LIKE ':hiraganaString'" +
           "OR japanese LIKE ':searchString'" +
           "OR japanese LIKE ':hiraganaString'" +
           "OR english LIKE ':searchString'" +
           "OR french LIKE ':searchString'")
    fun searchWords(searchString: String, hiraganaString: String): List<RoomWords>

    @Query("EXISTS (" +
            "SELECT * FROM RoomQuizWord" +
            "WHERE word_id = :wordId AND quiz_id = :quizId" +
           ")")
    fun isWordInQuiz(wordId: Long, quizId: Long): Boolean

    @Query("SELECT * FROM RoomWords WHERE _id = :wordId LIMIT 1")
    fun getWordById(wordId: Long): RoomWords?

    @Query("UPDATE RoomWords SET level = :priority" +
                                "points = :points" +
                                "count_fail = :counterFail" +
                                "count_try = :counterTry" +
                                "count_success = :counterSuccess" +
           "WHERE japanese = ':word' AND reading LIKE '%:pronunciation%'")
    fun restoreWord(word: String, pronunciation: String, priority: Int, points: Int,
                    counterFail: Int, counterTry: Int, counterSuccess: Int)

    @Query("DELETE FROM RoomWords")
    fun deleteAllWords()

    @Delete
    fun deleteWord(word: RoomWords)

    @Update
    fun updateWord(word: RoomWords)

    @Query("UPDATE RoomWords SET points = :points WHERE _id = :wordId")
    fun updateWordPoints(wordId: Long, points: Int)

    @Query("UPDATE RoomWords SET level = :level," +
                                "points = 0" +
           "WHERE _id = :wordId")
    fun updateWordLevel(wordId: Long, level: Int)

    @Query("UPDATE RoomWords SET repetition = :repetition WHERE _id = :wordId")
    fun updateWordRepetition(wordId: Long, repetition: Int)

    @Query("UPDATE RoomWords SET isSelected = :isSelected WHERE _id = :wordId")
    fun updateWordSelected(wordId: Long, isSelected: Boolean)

    @Query("SELECT * FROM RoomQuizWord WHERE quiz_id = :quizId AND word_id = :wordId")
    fun getQuizWordFromId(quizId: Long, wordId: Long): RoomQuizWord?

    @Insert
    fun addQuizWord(quizWord: RoomQuizWord)
}
