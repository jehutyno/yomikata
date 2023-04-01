package com.jehutyno.yomikata.repository.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.jehutyno.yomikata.model.KanjiSolo
import com.jehutyno.yomikata.model.KanjiSoloRadical
import com.jehutyno.yomikata.model.Radical


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
    fun getSoloByKanji(kanji: String): KanjiSolo?

    @Query("SELECT RoomKanjiSolo.*, RoomRadicals.strokes, RoomRadicals.reading, RoomRadicals.en, RoomRadicals.fr" +
           "FROM RoomKanjiSolo JOIN RoomRadicals" +
           "ON RoomKanjiSolo.radical = RoomRadicals.radical" +
           "WHERE RoomKanjiSolo.kanji = :kanji" +
           "LIMIT 1")
    fun getSoloByKanjiRadical(kanji: String): KanjiSoloRadical?

    @Query("SELECT * FROM RoomRadicals WHERE radical = :radicalString LIMIT 1")
    fun getKanjiRadical(radicalString: String): Radical?
}
