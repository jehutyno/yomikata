package com.jehutyno.yomikata.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.jehutyno.yomikata.repository.database.RoomSentences


@Dao
interface SentenceDao {
    @Insert
    suspend fun addSentence(sentence: RoomSentences): Long

    @Query("SELECT * FROM sentences " +
           "WHERE jap LIKE '%{' || (:japanese) || ';' || (:reading) || '}%' " +
           "AND level <= :maxLevel " +
           "ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomSentence(japanese: String, reading: String, maxLevel: Int): RoomSentences?

    @Query("SELECT * FROM sentences WHERE _id = :id")
    suspend fun getSentenceById(id: Long): RoomSentences?

    @Query("SELECT * FROM sentences WHERE _id IN (:ids)")
    suspend fun getSentencesByIds(ids: LongArray): List<RoomSentences>

    @Query("SELECT * FROM sentences")
    suspend fun getAllSentences(): List<RoomSentences>

    @Update
    suspend fun updateSentence(sentence: RoomSentences)
}
