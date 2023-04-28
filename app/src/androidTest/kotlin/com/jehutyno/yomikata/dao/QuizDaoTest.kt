package com.jehutyno.yomikata.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.jehutyno.yomikata.repository.local.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
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
    fun getQuizzesOfCategory() = runBlocking {
        val sampleRoomQuizWithId = sampleRoomQuiz.map {
            val id = quizDao.addQuiz(it)
            it.copy(_id = id)
        }
        val sampleByCategory = sampleRoomQuizWithId.groupBy { it.category }
        for (category in sampleByCategory.keys) {
            val retrievedRoomQuizzes = quizDao.getQuizzesOfCategory(category).first()
            assert (
                sampleByCategory[category]!!.toSet() == retrievedRoomQuizzes.toSet()
            )
        }
        val weirdCategory = 999
        if (weirdCategory in sampleByCategory.keys)
            return@runBlocking
        assert (
            quizDao.getQuizzesOfCategory(weirdCategory).first().isEmpty()
        )
    }

    @Test
    fun getQuizById() = runBlocking {
        for (roomQuiz in sampleRoomQuiz) {
            val id = quizDao.addQuiz(roomQuiz)
            assert (
                quizDao.getQuizById(id) == roomQuiz.copy(_id = id)
            )
        }
    }

    @Test
    fun addQuiz() = runBlocking {
        for (roomQuiz in sampleRoomQuiz) {
            val id = quizDao.addQuiz(roomQuiz)
            assert (
                quizDao.getAllQuizzes().contains(roomQuiz.copy(_id = id))
            )
        }
    }

    @Test
    fun deleteQuiz() = runBlocking {
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
    fun updateQuizName() = runBlocking {
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
    fun updateQuizSelected() = runBlocking {
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
    fun addQuizWord() = runBlocking {
        // must add quiz and word first to satisfy foreign key constraint
        quizDao.addQuiz(sampleRoomQuiz[0].copy(_id = 1))
        wordDao.addWord(sampleRoomWords[0].copy(_id = 2))
        val roomQuizWord = RoomQuizWord(1, 2)
        quizDao.addQuizWord(roomQuizWord)
        assert (
            quizDao.getAllQuizWords().contains(roomQuizWord)
        )
    }

    @Test
    fun deleteWordFromQuiz() = runBlocking {
        sampleRoomQuizWords.forEach {
            wordDao.addWord(getRandomRoomWord(it.word_id))
            quizDao.addQuiz(getRandomRoomQuiz(it.quiz_id))
            quizDao.addQuizWord(it)
        }

        val testRoomQuizWord = sampleRoomQuizWords[0]
        quizDao.deleteWordFromQuiz(testRoomQuizWord.word_id, testRoomQuizWord.quiz_id)
        assert (
            !quizDao.getAllQuizWords().contains(testRoomQuizWord)
        )
        for (i in 1 until sampleRoomQuizWords.size) {
            assert (
                quizDao.getAllQuizWords().contains(sampleRoomQuizWords[i])
            )
        }
    }

    @Test
    fun countWordsForLevel() = runBlocking {
        val test = sampleRoomWords[0]
        val id = wordDao.addWord(test)
        val quizId : Long = 56
        quizDao.addQuiz(getRandomRoomQuiz(quizId))
        val quizWord = RoomQuizWord(quizId, id)
        quizDao.addQuizWord(quizWord)
        val level = test.level
        assert (
            quizDao.countWordsForLevel(longArrayOf(quizId), level) == 1
        )
    }

    @Test
    fun countWordsForQuizzes() = runBlocking {
        val coupledSamples = CoupledQuizWords(quizDao, wordDao)
        coupledSamples.addAllToDatabase()
        val quizIds = longArrayOf(1, 2)
        val actualCount = coupledSamples.countWordsForQuizzes(quizIds)
        assert (
            actualCount == quizDao.countWordsForQuizzes(quizIds)
        )
    }

}
