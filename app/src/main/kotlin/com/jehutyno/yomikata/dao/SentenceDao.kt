package com.jehutyno.yomikata.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.jehutyno.yomikata.repository.local.RoomSentences


@Dao
interface SentenceDao {
    @Insert
    fun addSentence(sentence: RoomSentences): Long

    @Query("SELECT * FROM sentences " +
           "WHERE jap LIKE '%{' || (:japanese) || ';' || (:reading) || '}%' " +
           "AND level <= :maxLevel " +
           "ORDER BY RANDOM() LIMIT 1")
    fun getRandomSentence(japanese: String, reading: String, maxLevel: Int): RoomSentences?

    @Query("SELECT * FROM sentences WHERE _id = :id")
    fun getSentenceById(id: Long): RoomSentences?

    @Query("SELECT * FROM sentences")
    fun getAllSentences(): List<RoomSentences>

    @Update
    fun updateSentence(sentence: RoomSentences)
}
