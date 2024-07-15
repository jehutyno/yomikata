package com.jehutyno.yomikata.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.MapInfo
import androidx.room.Query
import com.jehutyno.yomikata.repository.database.RoomKanjiSolo
import com.jehutyno.yomikata.repository.database.RoomKanjiSoloRadical
import com.jehutyno.yomikata.repository.database.RoomRadicals


@Dao
interface KanjiSoloDao {
    @Query("SELECT * FROM kanji_solo")
    suspend fun getAllKanjiSolo(): List<RoomKanjiSolo>

    @Query("SELECT * FROM radicals")
    suspend fun getAllRadicals(): List<RoomRadicals>

    @Query("SELECT COUNT(*) FROM kanji_solo")
    suspend fun kanjiSoloCount(): Int

    @Query("SELECT COUNT(*) FROM radicals")
    suspend fun radicalsCount(): Int

    @Insert
    suspend fun addKanjiSolo(kanjiSolo: RoomKanjiSolo)

    @Insert
    suspend fun addRadical(radical: RoomRadicals)

    @Query("SELECT * FROM kanji_solo WHERE kanji = :kanji LIMIT 1")
    suspend fun getSoloByKanji(kanji: String): RoomKanjiSolo?

    @Query("SELECT kanji_solo.*, " +
           "radicals.strokes AS radStroke, radicals.reading AS radReading, " +
           "radicals.en AS radEn, radicals.fr AS radFr " +
           "FROM kanji_solo JOIN radicals " +
           "ON kanji_solo.radical = radicals.radical " +
           "WHERE kanji_solo.kanji = :kanji " +
           "LIMIT 1")
    suspend fun getSoloByKanjiRadical(kanji: String): RoomKanjiSoloRadical?

    @MapInfo(keyColumn = "_id", keyTable = "words")
    @Query("SELECT words._id, kanji_solo.*, " +
            "radicals.strokes AS radStroke, radicals.reading AS radReading, " +
            "radicals.en AS radEn, radicals.fr AS radFr " +
            "FROM words JOIN kanji_solo JOIN radicals " +
            "ON words.japanese LIKE '%' || kanji_solo.kanji || '%' " +
            "AND kanji_solo.radical = radicals.radical " +
            "AND words._id IN (:wordIds)")
    suspend fun getSoloByKanjiRadical(wordIds: LongArray): Map<Long, List<RoomKanjiSoloRadical>>

    @Query("SELECT * FROM radicals WHERE radical = :radicalString LIMIT 1")
    suspend fun getKanjiRadical(radicalString: String): RoomRadicals?
}
