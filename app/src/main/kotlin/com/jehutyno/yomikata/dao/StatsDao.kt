package com.jehutyno.yomikata.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.jehutyno.yomikata.repository.database.RoomStatEntry
import kotlinx.coroutines.flow.Flow


@Dao
interface StatsDao {
    @Insert
    suspend fun addStatEntry(statEntry: RoomStatEntry): Long

    @Query("SELECT * FROM stat_entry " +
           "WHERE date > :start AND date < :end")
    fun getStatEntriesOfTimeInterval(start: Long, end: Long): Flow<List<RoomStatEntry>>

    @Query("SELECT * FROM stat_entry")
    fun getAllStatEntries(): Flow<List<RoomStatEntry>>

    @Query("DELETE FROM stat_entry")
    suspend fun removeAllStats()
}
