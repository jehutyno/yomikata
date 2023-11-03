package com.jehutyno.yomikata.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.jehutyno.yomikata.repository.database.RoomSentences
import com.jehutyno.yomikata.repository.database.YomikataDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
@SmallTest
class SentenceDaoTest {

    private lateinit var database: YomikataDatabase
    private lateinit var sentenceDao: SentenceDao

    @Before
    fun setupDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            YomikataDatabase::class.java
        ).allowMainThreadQueries().build()

        sentenceDao = database.sentenceDao()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun addSentence() = runBlocking {
        for (roomSentence in sampleRoomSentences) {
            val id = sentenceDao.addSentence(roomSentence)
            assert (
                sentenceDao.getAllSentences().contains(roomSentence.copy(_id = id))
            )
        }
    }

    @Test
    fun getRandomSentence() = runBlocking {
        val sample = RoomSentences(1, "{彼;かれ}が{試験;しけん}に{受;う}かるかどうか{は;わ}{五;ご}{分;ぶ}{五;ご}{分;ぶ}だ。",
            "It's 50/50 whether he passes the test or not.",
            "Les chances qu'il réussisse l'examen ou pas sont de 50/50.", 4)
        sentenceDao.addSentence(sample)
        val retrievedRoomSentence = sentenceDao.getRandomSentence("彼", "かれ", 4)
        assert ( retrievedRoomSentence == sample )
        val retrievedRoomSentence2 = sentenceDao.getRandomSentence("彼", "かれ", 3)
        assert ( retrievedRoomSentence2 == null )
        val retrievedRoomSentence3 = sentenceDao.getRandomSentence("彼", "か", 4)
        assert ( retrievedRoomSentence3 == null )
    }

    @Test
    fun getSentenceById() = runBlocking {
        for (roomSentence in sampleRoomSentences) {
            val id = sentenceDao.addSentence(roomSentence)
            assert (
                sentenceDao.getSentenceById(id) == roomSentence.copy(_id = id)
            )
        }
    }

    @Test
    fun updateSentence() = runBlocking {
        val sample = sampleRoomSentences[0]
        val id = sentenceDao.addSentence(sample)
        val editSample = sample.copy(_id = id, en = "edit")
        sentenceDao.updateSentence(editSample)
        assert ( sentenceDao.getAllSentences().contains(editSample) )
        assert ( !sentenceDao.getAllSentences().contains(sample.copy(_id = id)) )
    }

}
