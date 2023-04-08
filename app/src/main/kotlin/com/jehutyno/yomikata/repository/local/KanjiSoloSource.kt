package com.jehutyno.yomikata.repository.local

import com.jehutyno.yomikata.dao.KanjiSoloDao
import com.jehutyno.yomikata.model.KanjiSolo
import com.jehutyno.yomikata.model.KanjiSoloRadical
import com.jehutyno.yomikata.model.Radical
import com.jehutyno.yomikata.repository.KanjiSoloRepository


/**
 * Created by valentin on 10/01/2017.
 */
class KanjiSoloSource(private val kanjiSoloDao: KanjiSoloDao) : KanjiSoloRepository {
    override fun createKanjiSoloTable() {
        TODO("Not yet implemented")
    }

    override fun createRadicalsTable() {
        TODO("Not yet implemented")
    }

    override fun kanjiSoloCount(): Int {
        return kanjiSoloDao.kanjiSoloCount()
    }

    override fun radicalsCount(): Int {
        return kanjiSoloDao.radicalsCount()
    }

    override fun addKanjiSolo(kanjiSolo: KanjiSolo) {
        val roomKanjiSolo = RoomKanjiSolo.from(kanjiSolo)
        kanjiSoloDao.addKanjiSolo(roomKanjiSolo)
    }

    override fun addRadical(radical: Radical) {
        val roomRadical = RoomRadicals.from(radical)
        kanjiSoloDao.addRadical(roomRadical)
    }

    override fun getSoloByKanji(kanji: String): KanjiSolo? {
        val roomKanjiSolo = kanjiSoloDao.getSoloByKanji(kanji)
        return roomKanjiSolo?.toKanjiSolo()
    }

    override fun getSoloByKanjiRadical(kanji: String): KanjiSoloRadical? {
        val roomKanjiSoloRadical = kanjiSoloDao.getSoloByKanjiRadical(kanji)
        return roomKanjiSoloRadical?.toKanjiSoloRadical()
    }

    override fun getKanjiRadical(radicalString: String): Radical? {
        val roomRadical = kanjiSoloDao.getKanjiRadical(radicalString)
        return roomRadical?.toRadical()
    }

}
