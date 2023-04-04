package com.jehutyno.yomikata.repository.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query


@Dao
interface KanjiSoloDao {
    @Query("SELECT * FROM kanji_solo")
    fun getAllKanjiSolo(): List<RoomKanjiSolo>

    @Query("SELECT * FROM radicals")
    fun getAllRadicals(): List<RoomRadicals>

    @Query("SELECT COUNT(*) FROM kanji_solo")
    fun kanjiSoloCount(): Int

    @Query("SELECT COUNT(*) FROM radicals")
    fun radicalsCount(): Int

    @Insert
    fun addKanjiSolo(kanjiSolo: RoomKanjiSolo): Long

    @Insert
    fun addRadical(radical: RoomRadicals): Long

    @Query("SELECT * FROM kanji_solo WHERE kanji = :kanji LIMIT 1")
    fun getSoloByKanji(kanji: String): RoomKanjiSolo?

    @Query("SELECT kanji_solo.*, " +
           "radicals.strokes AS radStroke, radicals.reading AS radReading, " +
           "radicals.en AS radEn, radicals.fr AS radFr " +
           "FROM kanji_solo JOIN radicals " +
           "ON kanji_solo.radical = radicals.radical " +
           "WHERE kanji_solo.kanji = :kanji " +
           "LIMIT 1")
    fun getSoloByKanjiRadical(kanji: String): RoomKanjiSoloRadical?

    @Query("SELECT * FROM radicals WHERE radical = :radicalString LIMIT 1")
    fun getKanjiRadical(radicalString: String): RoomRadicals?
}
