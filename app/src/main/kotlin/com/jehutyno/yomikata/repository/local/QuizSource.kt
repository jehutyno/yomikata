package com.jehutyno.yomikata.repository.local

import com.jehutyno.yomikata.dao.QuizDao
import com.jehutyno.yomikata.model.Quiz
import com.jehutyno.yomikata.repository.QuizRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map


/**
 * Created by valentin on 07/10/2016.
 */
class QuizSource(private val quizDao: QuizDao) : QuizRepository {

    override fun getQuiz(category: Int) : Flow<List<Quiz>> {
        val roomQuizList = quizDao.getQuizzesOfCategory(category)
        val quizList = roomQuizList.map { list ->
            list.map {
                it.toQuiz()
            }
        }
        return quizList
    }

    override suspend fun getQuiz(quizId: Long): Quiz? {
        val roomQuiz = quizDao.getQuizById(quizId)
        return roomQuiz?.toQuiz()
    }

    override suspend fun saveQuiz(quizName: String, category: Int): Long {
        val quiz = Quiz(0, quizName, quizName, category, false)
        val roomQuiz = RoomQuiz.from(quiz)
        return quizDao.addQuiz(roomQuiz)
    }

    override suspend fun deleteAllQuiz() {
        quizDao.deleteAllQuiz()
    }

    override suspend fun deleteQuiz(quizId: Long) {
        val quiz = quizDao.getQuizById(quizId)
        if (quiz != null)
            quizDao.deleteQuiz(quiz)
    }

    override suspend fun updateQuizName(quizId: Long, quizName: String) {
        quizDao.updateQuizName(quizId, quizName)
    }

    override suspend fun updateQuizSelected(quizId: Long, isSelected: Boolean) {
        quizDao.updateQuizSelected(quizId, isSelected)
    }

    override suspend fun addWordToQuiz(wordId: Long, quizId: Long) {
        quizDao.addQuizWord(RoomQuizWord(quizId, wordId))
    }

    override suspend fun deleteWordFromQuiz(wordId: Long, quizId: Long) {
        quizDao.deleteWordFromQuiz(wordId, quizId)
    }

    override suspend fun countWordsForLevel(quizIds: LongArray, level: Int): Int {
        return quizDao.countWordsForLevel(quizIds, level)
    }

    override suspend fun countWordsForQuizzes(quizIds: LongArray): Int {
        return quizDao.countWordsForQuizzes(quizIds)
    }
}
