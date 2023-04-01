package com.jehutyno.yomikata.repository.local

import androidx.room.*
import com.jehutyno.yomikata.model.Quiz


@Dao
interface QuizDao {
    @Query("SELECT * FROM RoomQuiz WHERE category = :category")
    fun getQuizzesOfCategory(category: Int): Array<Quiz>

    @Insert
    fun addQuiz(quiz: RoomQuiz): Long

    @Delete
    fun deleteQuiz(quiz: RoomQuiz)

    @Update
    fun updateQuiz(quiz: RoomQuiz)

    @Delete
    fun deleteWordFromQuiz(quiz_word: RoomQuizWord)

    @Query("SELECT COUNT(*) FROM RoomWords JOIN RoomQuizWord" +
           "ON RoomQuizWord.word_id = RoomWords._id" +
           "AND RoomQuizWord.quiz_id IN :quizIds" +
           "AND RoomWords.words = :level")
    fun countWordsForLevel(quizIds: LongArray, level: Int): Int

    @Query("SELECT COUNT(*) FROM RoomWords JOIN RoomQuizWord" +
           "ON RoomQuizWord.word_id = RoomWords._id" +
           "AND RoomQuizWord.quiz_id IN quizIds")
    fun countWordsForQuizzes(quizIds: LongArray): Int
}
