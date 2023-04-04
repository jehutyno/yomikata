package com.jehutyno.yomikata.repository.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query


@Dao
interface StatsDao {
    @Insert
    fun addStatEntry(statEntry: RoomStatEntry): Long

    @Query("SELECT * FROM stat_entry " +
           "WHERE date > :start AND date < :end")
    fun getStatEntriesOfTimeInterval(start: Long, end: Long): List<RoomStatEntry>

    @Query("SELECT * FROM stat_entry")
    fun getAllStatEntries(): List<RoomStatEntry>

    @Query("DELETE FROM stat_entry")
    fun removeAllStats()
}
