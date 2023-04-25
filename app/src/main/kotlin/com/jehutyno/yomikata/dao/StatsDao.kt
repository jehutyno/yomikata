package com.jehutyno.yomikata.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.jehutyno.yomikata.repository.local.RoomStatEntry


@Dao
interface StatsDao {
    @Insert
    suspend fun addStatEntry(statEntry: RoomStatEntry): Long

    @Query("SELECT * FROM stat_entry " +
           "WHERE date > :start AND date < :end")
    suspend fun getStatEntriesOfTimeInterval(start: Long, end: Long): List<RoomStatEntry>

    @Query("SELECT * FROM stat_entry")
    suspend fun getAllStatEntries(): List<RoomStatEntry>

    @Query("DELETE FROM stat_entry")
    suspend fun removeAllStats()
}
