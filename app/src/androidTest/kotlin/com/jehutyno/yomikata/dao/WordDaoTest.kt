package com.jehutyno.yomikata.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.jehutyno.yomikata.repository.local.RoomQuizWord
import com.jehutyno.yomikata.repository.local.RoomWords
import com.jehutyno.yomikata.repository.local.YomikataDataBase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
@MediumTest
class WordDaoTest {

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
    fun getWords() = runBlocking {
        val sample = CoupledQuizWords(quizDao, wordDao)
        sample.addAllToDatabase()
        val quizIdsTest = listOf (
            longArrayOf(1), longArrayOf(1, 2), longArrayOf(55, 89)
        )
        for (quizIds in quizIdsTest) {
            val retrievedRoomWords = wordDao.getWords(quizIds).first()
            val actualWords = sample.getWords(quizIds)
            assert (
                retrievedRoomWords.toSet() == actualWords.toSet()
            )
        }
    }

    @Test
    fun getWordsByLevels() = runBlocking {
        val sample = CoupledQuizWords(quizDao, wordDao)
        sample.addAllToDatabase()
        val quizIdsAndLevelsTest = listOf (
            Pair(longArrayOf(1), intArrayOf(2)), Pair(longArrayOf(1, 2), intArrayOf(2, 4, 5)),
            Pair(longArrayOf(55, 89), intArrayOf(99))
        )
        for (quizIdsAndLevels in quizIdsAndLevelsTest) {
            val quizIds = quizIdsAndLevels.first
            val levels = quizIdsAndLevels.second
            val retrievedRoomWords = wordDao.getWordsByLevels(quizIds, levels).first()
            val actualWords = sample.getWordsByLevels(quizIds, levels)
            assert (
                retrievedRoomWords.toSet() == actualWords.toSet()
            )
        }
    }

    @Test
    fun getWordsByRepetition() = runBlocking {
        val sample = CoupledQuizWords(quizDao, wordDao)
        sample.addAllToDatabase()
        val quizIdsAndRepetitionsTest = listOf (
            Pair(longArrayOf(1), 2), Pair(longArrayOf(1, 2), -1),
            Pair(longArrayOf(55, 89), -2)
        )
        for (quizIdsAndRepetition in quizIdsAndRepetitionsTest) {
            val quizIds = quizIdsAndRepetition.first
            val repetition = quizIdsAndRepetition.second
            val limit = 4
            val retrievedRoomWords = wordDao.getWordsByRepetition(quizIds, repetition, limit)
            val actualWords = sample.getWordsByRepetition(quizIds, repetition, limit)
            assert (
                retrievedRoomWords.toSet() == actualWords.toSet()
            )
        }
    }

    @Test
    fun getWordsWithRepetitionStrictlyGreaterThan() = runBlocking {
        val sample = CoupledQuizWords(quizDao, wordDao)
        sample.addAllToDatabase()
        val quizIdsAndRepetitionsTest = listOf (
            Pair(longArrayOf(1), 2), Pair(longArrayOf(1, 2), -1),
            Pair(longArrayOf(55, 89), 99)
        )
        for (quizIdsAndRepetition in quizIdsAndRepetitionsTest) {
            val quizIds = quizIdsAndRepetition.first
            val repetition = quizIdsAndRepetition.second
            val retrievedRoomWords = wordDao.getWordIdsWithRepetitionStrictlyGreaterThan(quizIds, repetition)
            val actualWords = sample.getWordIdsWithRepetitionStrictlyGreaterThan(quizIds, repetition)
            assert (
                retrievedRoomWords.toSet() == actualWords.toSet()
            )
        }
    }

    @Test
    fun decreaseWordRepetitionByOne() = runBlocking {
        val ids = sampleRoomWords.map {
            wordDao.addWord(it)
        }.toLongArray()
        wordDao.decreaseWordRepetitionByOne(ids)
        for (i in ids.indices) {
            assert (
                wordDao.getWordById(ids[i])!!.repetition == sampleRoomWords[i].repetition - 1
            )
        }
    }

    @Test
    fun getWordsOfSizeRelatedTo() = runBlocking {
        val words = listOf (
            RoomWords(1, "月", "moon; Monday", "lune; lundi", "げつ",
                0, 0, 0, 0, 0,
                -1, 0, 2, 0, null),
            RoomWords(2, "酒", "sake; alcohol", "saké; alcool", "さけ",
                0, 0, 0, 0, 0,
                -1, 0, 2, 0, null),
            RoomWords(3, "石炭", "(n) coal;(P)", "(n) charbon;houille",
                "せきたん;いしずみ", 0, 0, 0, 0, 0,
                -1, 0, 5, 0, null),
            RoomWords(4, "式", "ceremony", "cérémonie", "しき",
                0, 0, 0, 0, 0, -1, 0,
                6, 0, null)
        )
        words.forEach { wordDao.addWord(it) }
        quizDao.addQuiz(sampleRoomQuiz[0].copy(_id = 1))
        val quizWords = listOf (
            RoomQuizWord(1, 1),
            RoomQuizWord(1, 2),
            RoomQuizWord(1, 3),
            RoomQuizWord(1, 4)
        )
        quizWords.forEach { wordDao.addQuizWord(it) }
        val retrievedWordIds = wordDao.getWordsOfSizeRelatedTo(1, 1)
        assert (
            retrievedWordIds.toSet() == setOf(words[1]._id, words[3]._id)
        )
    }

    @Test
    fun getRandomWords() = runBlocking {

    }

    @Test
    fun searchWords() = runBlocking {
        val sample = RoomWords(1, "金", "metal; Friday", "métal; vendredi", "きん",
            0, 0, 0, 0, 0, -1, 0,
            2, 0, null)
        val weird = "OZFEOZ3°OK?ZEVK°9K9jéPAODFAFPO?3O233"  // string that should not be found in db
        wordDao.addWord(sample)
        assert ( wordDao.searchWords("met", weird).first() == listOf(sample) )
        assert ( wordDao.searchWords("metal", weird).first() == listOf(sample) )
        assert ( wordDao.searchWords(weird, "きん").first() == listOf(sample) )
        assert ( wordDao.searchWords("vendre", "まさか").first() == listOf(sample) )

        assert ( wordDao.searchWords("vendredit", weird).first().isEmpty() )
        assert ( wordDao.searchWords("metalic", weird).first().isEmpty() )
        assert ( wordDao.searchWords(weird, "まさか").first().isEmpty() )

    }

    @Test
    fun isWordInQuiz() = runBlocking {
        val sample = sampleRoomQuizWords[0]
        assert ( !wordDao.isWordInQuiz(sample.word_id, sample.quiz_id) )
        wordDao.addWord(getRandomRoomWord(sample.word_id))
        quizDao.addQuiz(getRandomRoomQuiz(sample.quiz_id))
        wordDao.addQuizWord(sample)
        assert ( wordDao.isWordInQuiz(sample.word_id, sample.quiz_id) )
    }

    @Test
    fun getWordById() = runBlocking {
        for (sample in sampleRoomWords) {
            val id = wordDao.addWord(sample)
            assert ( wordDao.getWordById(id) == sample.copy(_id = id) )
        }
    }

    @Test
    fun deleteAllWords() = runBlocking {
        for (sample in sampleRoomWords) {
            wordDao.addWord(sample)
        }
        wordDao.deleteAllWords()
        assert ( wordDao.getAllWords().isEmpty() )
    }

    @Test
    fun deleteWord() = runBlocking {
        val ids = sampleRoomWords.map {
            wordDao.addWord(it)
        }
        for (i in ids.indices) {
            val word = sampleRoomWords[i].copy(_id = ids[i])
            wordDao.deleteWord(word)
            assert (
                !wordDao.getAllWords().contains(word)
            )
        }
    }

    @Test
    fun updateWord() = runBlocking {
    }

    @Test
    fun updateWordPoints() = runBlocking {
    }

    @Test
    fun updateWordLevel() = runBlocking {
    }

    @Test
    fun updateWordRepetition() = runBlocking {
    }

    @Test
    fun updateWordSelected() = runBlocking {
    }

    @Test
    fun getQuizWordFromId() = runBlocking {
    }

    @Test
    fun addQuizWord() = runBlocking {
    }

    @Test
    fun addWord() = runBlocking {
    }

}
