package com.jehutyno.yomikata.repository.local

import androidx.room.*


@Dao
interface QuizDao {
    @Query("SELECT * FROM quiz WHERE category = :category")
    fun getQuizzesOfCategory(category: Int): List<RoomQuiz>

    @Query("SELECT * FROM quiz WHERE _id = :quizId LIMIT 1")
    fun getQuizById(quizId: Long): RoomQuiz?

    @Query("SELECT * FROM quiz")
    fun getAllQuizzes(): List<RoomQuiz>

    @Insert
    fun addQuiz(quiz: RoomQuiz): Long

    @Delete
    fun deleteQuiz(quiz: RoomQuiz)

    @Query("UPDATE quiz SET name_en = :quizName, " +
                           "name_fr = :quizName " +
           "WHERE _id = :quizId")
    fun updateQuizName(quizId: Long, quizName: String)

    @Query("UPDATE quiz SET isSelected = :isSelected " +
           "WHERE _id = :quizId")
    fun updateQuizSelected(quizId: Long, isSelected: Boolean)

    @Query("SELECT * FROM quiz_word")
    fun getAllQuizWords(): List<RoomQuizWord>

    @Insert
    fun addQuizWord(quiz_word: RoomQuizWord): Long

    @Query("DELETE FROM quiz_word " +
           "WHERE word_id = :wordId AND quiz_id = :quizId")
    fun deleteWordFromQuiz(wordId: Long, quizId: Long)

    @Query("SELECT COUNT(*) FROM words JOIN quiz_word " +
           "ON quiz_word.word_id = words._id " +
           "AND quiz_word.quiz_id IN (:quizIds) " +
           "AND words.level = :level")
    fun countWordsForLevel(quizIds: LongArray, level: Int): Int

    @Query("SELECT COUNT(*) FROM words JOIN quiz_word " +
           "ON quiz_word.word_id = words._id " +
           "AND quiz_word.quiz_id IN (:quizIds)")
    fun countWordsForQuizzes(quizIds: LongArray): Int
}
