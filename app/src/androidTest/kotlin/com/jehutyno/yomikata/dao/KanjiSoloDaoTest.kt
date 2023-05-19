package com.jehutyno.yomikata.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.jehutyno.yomikata.repository.database.RoomKanjiSolo
import com.jehutyno.yomikata.repository.database.RoomRadicals
import com.jehutyno.yomikata.repository.database.YomikataDatabase
import com.jehutyno.yomikata.repository.local.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
@MediumTest
class KanjiSoloDaoTest {

    private lateinit var database: YomikataDatabase
    private lateinit var kanjiSoloDao: KanjiSoloDao

    @Before
    fun setupDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            YomikataDatabase::class.java
        ).allowMainThreadQueries().build()

        kanjiSoloDao = database.kanjiSoloDao()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun kanjiSoloCount() = runBlocking {
        val size = sampleRoomKanjiSolo.size
        for (roomKanjiSolo in sampleRoomKanjiSolo)
            kanjiSoloDao.addKanjiSolo(roomKanjiSolo)
        assert (
            size == kanjiSoloDao.kanjiSoloCount()
        )
    }

    @Test
    fun kanjiSoloCountWithOther() = runBlocking {
        val size = sampleRoomKanjiSolo.size
        for (roomKanjiSolo in sampleRoomKanjiSolo)
            kanjiSoloDao.addKanjiSolo(roomKanjiSolo)
        for (roomRadical in sampleRoomRadicals)
            kanjiSoloDao.addRadical(roomRadical)
        assert (
            size == kanjiSoloDao.kanjiSoloCount()
        )
    }

    @Test
    fun radicalsCount() = runBlocking {
        val size = sampleRoomRadicals.size
        for (roomRadical in sampleRoomRadicals)
            kanjiSoloDao.addRadical(roomRadical)
        assert (
            size == kanjiSoloDao.radicalsCount()
        )
    }

    @Test
    fun radicalsCountWithOthers() = runBlocking {
        val size = sampleRoomRadicals.size
        for (roomKanjiSolo in sampleRoomKanjiSolo)
            kanjiSoloDao.addKanjiSolo(roomKanjiSolo)
        for (roomRadical in sampleRoomRadicals)
            kanjiSoloDao.addRadical(roomRadical)
        assert (
            size == kanjiSoloDao.radicalsCount()
        )
    }

    @Test
    fun addKanjiSolo() = runBlocking {
        for (roomKanjiSolo in sampleRoomKanjiSolo) {
            kanjiSoloDao.addKanjiSolo(roomKanjiSolo)
            assert (
                kanjiSoloDao.getAllKanjiSolo().contains(roomKanjiSolo)
            )
        }
    }

    @Test
    fun addRadical() = runBlocking {
        for (roomRadical in sampleRoomRadicals) {
            kanjiSoloDao.addRadical(roomRadical)
            assert (
                kanjiSoloDao.getAllRadicals().contains(roomRadical)
            )
        }
    }

    @Test
    fun getSoloByKanji() = runBlocking {
        for (roomKanjiSolo in sampleRoomKanjiSolo) {
            val kanji = roomKanjiSolo.kanji
            kanjiSoloDao.addKanjiSolo(roomKanjiSolo)
            val retrievedKanjiSolo = kanjiSoloDao.getSoloByKanji(kanji)
            assert (
                roomKanjiSolo == retrievedKanjiSolo
            )
        }
    }

    // test what happens when there is no kanjiSolo with the specified kanji
    @Test
    fun getSoloByKanjiNotFound() = runBlocking {
        val nonExistentKanji = "幽霊"
        for (roomKanjiSolo in sampleRoomKanjiSolo) {
            val kanji = roomKanjiSolo.kanji
            kanjiSoloDao.addKanjiSolo(roomKanjiSolo)
            val retrievedKanjiSolo = kanjiSoloDao.getSoloByKanji(nonExistentKanji)
            if (kanji == nonExistentKanji) {
                continue
            }
            assert (
                retrievedKanjiSolo == null
            )
        }
    }

    @Test
    fun getSoloByKanjiRadical() = runBlocking {
        for (roomKanjiSoloRadical in sampleRoomKanjiSoloRadical) {
            with(roomKanjiSoloRadical) {
                val roomKanjiSolo = RoomKanjiSolo.from(this.toKanjiSolo())
                val roomRadical = RoomRadicals.from(this.toRadical())
                kanjiSoloDao.addKanjiSolo(roomKanjiSolo)
                kanjiSoloDao.addRadical(roomRadical)
            }
            val kanji = roomKanjiSoloRadical.kanji
            val retrievedKanjiSoloRadical = kanjiSoloDao.getSoloByKanjiRadical(kanji)
            assert (
                roomKanjiSoloRadical == retrievedKanjiSoloRadical
            )
        }
    }

    @Test
    fun getKanjiRadical() = runBlocking {
        for (roomRadical in sampleRoomRadicals) {
            kanjiSoloDao.addRadical(roomRadical)
            val radicalString = roomRadical.radical
            val retrievedRoomRadical = kanjiSoloDao.getKanjiRadical(radicalString)
            assert (
                roomRadical == retrievedRoomRadical
            )
        }
    }

}
