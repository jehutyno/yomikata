package com.jehutyno.yomikata.repository.local

import androidx.room.Dao
import androidx.room.Query


@Dao
interface UpdateDao {
    @Query("SELECT * FROM words")
    fun getAllWords(): List<RoomWords>

    @Query("SELECT * FROM stat_entry")
    fun getAllStatEntries(): List<RoomStatEntry>

    @Query("SELECT * FROM quiz_word WHERE _id > :defaultSize")
    fun getAddedQuizWords(defaultSize: Long): List<RoomQuizWord>

    @Query("SELECT * FROM quiz WHERE _id > :defaultSize")
    fun getAddedQuizzes(defaultSize: Long): List<RoomQuiz>

    @Query("SELECT * FROM kanji_solo")
    fun getAllKanjiSolo(): List<RoomKanjiSolo>

    @Query("SELECT * FROM radicals")
    fun getAllRadicals(): List<RoomRadicals>

    @Query("SELECT * FROM sentences")
    fun getAllSentences(): List<RoomSentences>
}
