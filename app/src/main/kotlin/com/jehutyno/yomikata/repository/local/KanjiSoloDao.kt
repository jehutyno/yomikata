package com.jehutyno.yomikata.repository.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query


@Dao
interface KanjiSoloDao {
    @Query("SELECT COUNT(*) FROM RoomKanjiSolo")
    fun kanjiSoloCount(): Int

    @Query("SELECT COUNT(*) FROM RoomRadicals")
    fun radicalsCount(): Int

    @Insert
    fun addKanjiSolo(kanjiSolo: RoomKanjiSolo)

    @Insert
    fun addRadical(radical: RoomRadicals)

    @Query("SELECT * FROM RoomKanjiSolo WHERE kanji = :kanji LIMIT 1")
    fun getSoloByKanji(kanji: String): RoomKanjiSolo?

    @Query("SELECT RoomKanjiSolo.*, RoomRadicals.strokes, RoomRadicals.reading, RoomRadicals.en, RoomRadicals.fr" +
           "FROM RoomKanjiSolo JOIN RoomRadicals" +
           "ON RoomKanjiSolo.radical = RoomRadicals.radical" +
           "WHERE RoomKanjiSolo.kanji = :kanji" +
           "LIMIT 1")
    fun getSoloByKanjiRadical(kanji: String): RoomKanjiSoloRadical?

    @Query("SELECT * FROM RoomRadicals WHERE radical = :radicalString LIMIT 1")
    fun getKanjiRadical(radicalString: String): RoomRadicals?
}
