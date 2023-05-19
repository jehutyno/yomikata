package com.jehutyno.yomikata.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.jehutyno.yomikata.repository.database.RoomStatEntry
import com.jehutyno.yomikata.repository.database.YomikataDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
@SmallTest
class StatsDaoTest {

    private lateinit var database: YomikataDatabase
    private lateinit var statsDao: StatsDao

    @Before
    fun setupDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            YomikataDatabase::class.java
        ).allowMainThreadQueries().build()

        statsDao = database.statsDao()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun addStatEntry() = runBlocking {
        for (roomStatEntry in sampleStatEntries) {
            val id = statsDao.addStatEntry(roomStatEntry)
            assert (
                statsDao.getAllStatEntries().first().contains(roomStatEntry.copy(_id = id))
            )
        }
    }

    @Test
    fun getStatEntriesOfTimeInterval() = runBlocking {
        val samples = listOf (
            RoomStatEntry(1, 2, 3, 1680455076292, 0),
            RoomStatEntry(2, 5, 12, 1680455075001, 1),
            RoomStatEntry(3, 0, 1, 1680455076393, 0),
            RoomStatEntry(4, 1, 1, 1680455089011, 0)
        )
        val start = 1680455076290
        val end = 1680455076493
        for (sample in samples) {
            statsDao.addStatEntry(sample)
        }
        val retrievedEntries = statsDao.getStatEntriesOfTimeInterval(start, end).first()
        for (sample in samples) {
            if (sample.date in (start + 1) until end) {
                assert ( retrievedEntries.contains(sample) )
            }
            else {
                assert ( !retrievedEntries.contains(sample) )
            }
        }
    }

    @Test
    fun removeAllStats() = runBlocking {
        for (sample in sampleStatEntries) {
            statsDao.addStatEntry(sample)
        }
        statsDao.removeAllStats()
        assert ( statsDao.getAllStatEntries().first().isEmpty() )
    }

}
