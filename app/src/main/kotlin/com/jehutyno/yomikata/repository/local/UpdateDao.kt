package com.jehutyno.yomikata.repository.local

import androidx.room.Dao
import androidx.room.Query


@Dao
interface UpdateDao {
    @Query("SELECT * FROM RoomWords")
    fun getAllWords(): List<RoomWords>

    @Query("SELECT * FROM RoomStatEntry")
    fun getAllStatEntries(): List<RoomStatEntry>

    @Query("SELECT * FROM RoomQuizWord WHERE _id > :defaultSize")
    fun getAddedQuizWords(defaultSize: Long): List<RoomQuizWord>

    @Query("SELECT * FROM RoomQuiz WHERE _id > :defaultSize")
    fun getAddedQuizzes(defaultSize: Long): List<RoomQuiz>

    @Query("SELECT * FROM RoomKanjiSolo")
    fun getAllKanjiSolo(): List<RoomKanjiSolo>

    @Query("SELECT * FROM RoomRadicals")
    fun getAllRadicals(): List<RoomRadicals>

    @Query("SELECT * FROM RoomSentences")
    fun getAllSentences(): List<RoomSentences>
}
