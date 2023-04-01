package com.jehutyno.yomikata.repository.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query


@Dao
interface StatsDao {
    @Insert
    fun addStatEntry(statEntry: RoomStatEntry)

    @Query("SELECT * FROM RoomStatEntry" +
           "WHERE data > :start AND date < :end")
    fun getStatEntriesOfTimeInterval(start: Long, end: Long): RoomStatEntry

    @Query("SELECT * FROM RoomStatEntry")
    fun getAllStatEntries(): RoomSentences

    @Query("DELETE FROM RoomStatEntry")
    fun removeAllStats()
}
