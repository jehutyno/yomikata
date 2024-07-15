package com.jehutyno.yomikata.repository

import com.jehutyno.yomikata.model.KanjiSolo
import com.jehutyno.yomikata.model.KanjiSoloRadical
import com.jehutyno.yomikata.model.Radical


/**
 * Created by valentin on 10/01/2017.
 */
interface KanjiSoloRepository {
    suspend fun kanjiSoloCount(): Int
    suspend fun addKanjiSolo(kanjiSolo: KanjiSolo)
    suspend fun getSoloByKanji(kanji: String): KanjiSolo?
    suspend fun radicalsCount(): Int
    suspend fun addRadical(radical: Radical)
    suspend fun getKanjiRadical(radicalString: String): Radical?
    suspend fun getSoloByKanjiRadical(kanji: String): KanjiSoloRadical?
    suspend fun getSoloByKanjiRadical(wordIds: LongArray): Map<Long, List<KanjiSoloRadical>>
}
