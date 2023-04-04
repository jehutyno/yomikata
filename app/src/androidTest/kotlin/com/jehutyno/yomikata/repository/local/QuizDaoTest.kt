package com.jehutyno.yomikata.repository.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Assert.*

import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
@MediumTest
class QuizDaoTest {

    private lateinit var database: YomikataDataBase
    private lateinit var quizDao: QuizDao
    private lateinit var wordDao: WordDao

    @Before
    fun setupDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            YomikataDataBase::class.java
        ).allowMainThreadQueries().build()

        quizDao = database.quizDao()
        wordDao = database.wordDao()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun getQuizzesOfCategory() {
        val sampleRoomQuizWithId = sampleRoomQuiz.map {
            val id = quizDao.addQuiz(it)
            it.copy(_id = id)
        }
        val sampleByCategory = sampleRoomQuizWithId.groupBy { it.category }
        for (category in sampleByCategory.keys) {
            val retrievedRoomQuizzes = quizDao.getQuizzesOfCategory(category)
            assert (
                sampleByCategory[category]!!.toSet() == retrievedRoomQuizzes.toSet()
            )
        }
        val weirdCategory = 999
        if (weirdCategory in sampleByCategory.keys)
            return
        assert (
            quizDao.getQuizzesOfCategory(weirdCategory).isEmpty()
        )
    }

    @Test
    fun getQuizById() {
        for (roomQuiz in sampleRoomQuiz) {
            val id = quizDao.addQuiz(roomQuiz)
            assert (
                quizDao.getQuizById(id) == roomQuiz.copy(_id = id)
            )
        }
    }

    @Test
    fun addQuiz() {
        for (roomQuiz in sampleRoomQuiz) {
            val id = quizDao.addQuiz(roomQuiz)
            assert (
                quizDao.getAllQuizzes().contains(roomQuiz.copy(_id = id))
            )
        }
    }

    @Test
    fun deleteQuiz() {
        val sampleRoomQuizWithId = sampleRoomQuiz.map {
            val id = quizDao.addQuiz(it)
            it.copy(_id = id)
        }
        for (roomQuiz in sampleRoomQuizWithId) {
            quizDao.deleteQuiz(roomQuiz)
            assert (
                !quizDao.getAllQuizzes().contains(roomQuiz)
            )
        }
    }

    @Test
    fun updateQuizName() {
        val ids = sampleRoomQuiz.map {
            quizDao.addQuiz(it)
        }
        val newNames = listOf (
            "", "Hello", "部屋", "Baguette Frômage"
        )
        for (id in ids) {
            for (name in newNames) {
                quizDao.updateQuizName(id, name)
                assert(quizDao.getQuizById(id)!!.name_en == name)
                assert(quizDao.getQuizById(id)!!.name_fr == name)
            }
        }
    }

    @Test
    fun updateQuizSelected() {
        val ids = sampleRoomQuiz.map {
            quizDao.addQuiz(it)
        }
        for (id in ids) {
            quizDao.updateQuizSelected(id, false)
            assert(!quizDao.getQuizById(id)!!.isSelected)
            quizDao.updateQuizSelected(id, true)
            assert(quizDao.getQuizById(id)!!.isSelected)
        }
    }

    @Test
    fun addQuizWord() {
        val roomQuizWord = RoomQuizWord(0, 1, 2)
        val id = quizDao.addQuizWord(roomQuizWord)
        assert (
            quizDao.getAllQuizWords().contains(roomQuizWord.copy(_id = id))
        )
    }

    @Test
    fun deleteWordFromQuiz() {
        val ids = sampleRoomQuizWords.map {
            quizDao.addQuizWord(it)
        }
        val testRoomQuizWord = sampleRoomQuizWords[0]
        quizDao.deleteWordFromQuiz(testRoomQuizWord.word_id, testRoomQuizWord.quiz_id)
        assert (
            !quizDao.getAllQuizWords().contains(testRoomQuizWord.copy(_id = ids[0]))
        )
        for (i in 1 until sampleRoomQuizWords.size) {
            assert (
                quizDao.getAllQuizWords().contains(sampleRoomQuizWords[i].copy(_id = ids[i]))
            )
        }
    }

    @Test
    fun countWordsForLevel() {
        val test = sampleRoomWords[0]
        val id = wordDao.addWord(test)
        val quizId : Long = 56
        val quizWord = RoomQuizWord(0, quizId, id)
        quizDao.addQuizWord(quizWord)
        val level = test.level
        assert (
            quizDao.countWordsForLevel(longArrayOf(quizId), level) == 1
        )
    }

    @Test
    fun countWordsForQuizzes() {
        val coupledSamples = CoupledQuizWords(quizDao, wordDao)
        coupledSamples.addAllToDatabase()
        val quizIds = longArrayOf(1, 2)
        val actualCount = coupledSamples.countWordsForQuizzes(quizIds)
        assert (
            actualCount == quizDao.countWordsForQuizzes(quizIds)
        )
    }

}
