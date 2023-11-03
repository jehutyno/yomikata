package com.jehutyno.yomikata.repository.local

import com.jehutyno.yomikata.dao.KanjiSoloDao
import com.jehutyno.yomikata.model.KanjiSolo
import com.jehutyno.yomikata.model.KanjiSoloRadical
import com.jehutyno.yomikata.model.Radical
import com.jehutyno.yomikata.repository.KanjiSoloRepository
import com.jehutyno.yomikata.repository.database.RoomKanjiSolo
import com.jehutyno.yomikata.repository.database.RoomRadicals


/**
 * Created by valentin on 10/01/2017.
 */
class KanjiSoloSource(private val kanjiSoloDao: KanjiSoloDao) : KanjiSoloRepository {
    override suspend fun kanjiSoloCount(): Int {
        return kanjiSoloDao.kanjiSoloCount()
    }

    override suspend fun radicalsCount(): Int {
        return kanjiSoloDao.radicalsCount()
    }

    override suspend fun addKanjiSolo(kanjiSolo: KanjiSolo) {
        val roomKanjiSolo = RoomKanjiSolo.from(kanjiSolo)
        kanjiSoloDao.addKanjiSolo(roomKanjiSolo)
    }

    override suspend fun addRadical(radical: Radical) {
        val roomRadical = RoomRadicals.from(radical)
        kanjiSoloDao.addRadical(roomRadical)
    }

    override suspend fun getSoloByKanji(kanji: String): KanjiSolo? {
        val roomKanjiSolo = kanjiSoloDao.getSoloByKanji(kanji)
        return roomKanjiSolo?.toKanjiSolo()
    }

    override suspend fun getSoloByKanjiRadical(kanji: String): KanjiSoloRadical? {
        val roomKanjiSoloRadical = kanjiSoloDao.getSoloByKanjiRadical(kanji)
        return roomKanjiSoloRadical?.toKanjiSoloRadical()
    }

    override suspend fun getKanjiRadical(radicalString: String): Radical? {
        val roomRadical = kanjiSoloDao.getKanjiRadical(radicalString)
        return roomRadical?.toRadical()
    }

}
