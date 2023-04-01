package com.jehutyno.yomikata.repository.local

import android.database.sqlite.SQLiteDatabase
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.jehutyno.yomikata.model.Sentence


@Dao
interface SentenceDao {
    @Insert
    fun addSentence(sentence: RoomSentences)

    @Query("SELECT * FROM RoomSentences" +
           "WHERE jap LIKE '%{:word.japanese ; :word.reading}%'" +
           "AND level <= :maxLevel" +
           "ORDER BY RANDOM() LIMIT 1")
    fun getRandomSentence(word: RoomWords, maxLevel: Int): Sentence?

    @Query("SELECT * FROM RoomSentences WHERE _id = :id")
    fun getSentenceById(id: Long): Sentence

    @Query("SELECT * FROM RoomSentences")
    fun getAllSentences(): List<Sentence>

    @Update
    fun updateSentence(sentence: RoomSentences)
}
