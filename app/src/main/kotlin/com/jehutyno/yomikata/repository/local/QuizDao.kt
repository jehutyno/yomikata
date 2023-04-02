package com.jehutyno.yomikata.repository.local

import androidx.room.*


@Dao
interface QuizDao {
    @Query("SELECT * FROM RoomQuiz WHERE category = :category")
    fun getQuizzesOfCategory(category: Int): List<RoomQuiz>

    @Query("SELECT * FROM RoomQuiz WHERE _id = :quizId LIMIT 1")
    fun getQuizById(quizId: Long): RoomQuiz?

    @Insert
    fun addQuiz(quiz: RoomQuiz): Long

    @Delete
    fun deleteQuiz(quiz: RoomQuiz)

    @Query("UPDATE RoomQuiz SET name_en = :quizName," +
                               "name_fr = :quizName" +
           "WHERE _id = :quizId")
    fun updateQuizName(quizId: Long, quizName: String)

    @Query("UPDATE RoomQuiz SET isSelected = :isSelected" +
           "WHERE _id = :quizId")
    fun updateQuizSelected(quizId: Long, isSelected: Boolean)

    @Insert
    fun addQuizWord(quiz_word: RoomQuizWord)

    @Query("DELETE * FROM RoomQuizWord" +
           "WHERE word_id = :wordId AND quiz_id = :quizId")
    fun deleteWordFromQuiz(wordId: Long, quizId: Long)

    @Query("SELECT COUNT(*) FROM RoomWords JOIN RoomQuizWord" +
           "ON RoomQuizWord.word_id = RoomWords._id" +
           "AND RoomQuizWord.quiz_id IN :quizIds" +
           "AND RoomWords.level = :level")
    fun countWordsForLevel(quizIds: LongArray, level: Int): Int

    @Query("SELECT COUNT(*) FROM RoomWords JOIN RoomQuizWord" +
           "ON RoomQuizWord.word_id = RoomWords._id" +
           "AND RoomQuizWord.quiz_id IN quizIds")
    fun countWordsForQuizzes(quizIds: LongArray): Int
}
