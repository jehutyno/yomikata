package com.jehutyno.yomikata.repository

import com.jehutyno.yomikata.model.KanjiSolo
import com.jehutyno.yomikata.model.KanjiSoloRadical
import com.jehutyno.yomikata.model.Radical


/**
 * Created by valentin on 10/01/2017.
 */
interface KanjiSoloRepository {
    fun kanjiSoloCount(): Int
    fun addKanjiSolo(kanjiSolo: KanjiSolo)
    fun getSoloByKanji(kanji: String): KanjiSolo?
    fun radicalsCount(): Int
    fun addRadical(radical: Radical)
    fun getKanjiRadical(radicalString: String): Radical?
    fun getSoloByKanjiRadical(kanji: String): KanjiSoloRadical?
}
