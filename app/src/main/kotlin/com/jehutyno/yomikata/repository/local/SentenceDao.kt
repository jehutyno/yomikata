package com.jehutyno.yomikata.repository.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update


@Dao
interface SentenceDao {
    @Insert
    fun addSentence(sentence: RoomSentences)

    @Query("SELECT * FROM RoomSentences" +
           "WHERE jap LIKE '%{:word.japanese ; :word.reading}%'" +
           "AND level <= :maxLevel" +
           "ORDER BY RANDOM() LIMIT 1")
    fun getRandomSentence(word: RoomWords, maxLevel: Int): RoomSentences?

    @Query("SELECT * FROM RoomSentences WHERE _id = :id")
    fun getSentenceById(id: Long): RoomSentences?

    @Query("SELECT * FROM RoomSentences")
    fun getAllSentences(): List<RoomSentences>

    @Update
    fun updateSentence(sentence: RoomSentences)
}
