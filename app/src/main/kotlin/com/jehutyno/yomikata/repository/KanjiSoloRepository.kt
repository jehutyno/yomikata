package com.jehutyno.yomikata.repository.local

import android.database.sqlite.SQLiteDatabase
import com.jehutyno.yomikata.model.KanjiSolo
import com.jehutyno.yomikata.model.KanjiSoloRadical
import com.jehutyno.yomikata.model.Radical

/**
 * Created by valentin on 10/01/2017.
 */
interface KanjiSoloRepository {
    fun createKanjiSoloTable(db: SQLiteDatabase?)
    fun kanjiSoloCount(db: SQLiteDatabase?): Int
    fun addKanjiSolo(kanjiSolo: KanjiSolo)
    fun getSoloByKanji(kanji: String): KanjiSolo?
    fun createRadicalsTable(db: SQLiteDatabase?)
    fun radicalsCount(db: SQLiteDatabase?): Int
    fun addRadical(radical: Radical)
    fun getKanjiRadical(radicalString: String): Radical?
    fun getSoloByKanjiRadical(kanji: String): KanjiSoloRadical?
}