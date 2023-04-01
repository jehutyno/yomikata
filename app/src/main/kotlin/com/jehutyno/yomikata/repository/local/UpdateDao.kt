package com.jehutyno.yomikata.repository.local

import androidx.room.Dao
import androidx.room.Query
import com.jehutyno.yomikata.model.*


@Dao
interface UpdateDao {
    @Query("SELECT * FROM RoomWords")
    fun getAllWords(): List<Word>

    @Query("SELECT * FROM RoomStatEntry")
    fun getAllStatEntries(): List<StatEntry>

    @Query("SELECT * FROM RoomQuizWord WHERE _id > :defaultSize")
    fun getAddedQuizWords(defaultSize: Long): List<QuizWord>

    @Query("SELECT * FROM RoomQuiz WHERE _id > :defaultSize")
    fun getAddedQuizzes(defaultSize: Long): List<Quiz>

    @Query("SELECT * FROM RoomKanjiSolo")
    fun getAllKanjiSolo(): List<KanjiSolo>

    @Query("SELECT * FROM RoomRadicals")
    fun getAllRadicals(): List<Radical>

    @Query("SELECT * FROM RoomSentences")
    fun getAllSentences(): List<Sentence>
}
