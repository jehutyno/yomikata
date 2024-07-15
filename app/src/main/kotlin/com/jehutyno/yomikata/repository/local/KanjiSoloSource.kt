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

    override suspend fun getSoloByKanjiRadical(wordIds: LongArray): Map<Long, List<KanjiSoloRadical>> {
        val stepSize = 500  // to avoid going over max number of SQL variables (999)
        val ret = mutableMapOf<Long, ArrayList<KanjiSoloRadical>>()
        // make sure that all wordIds are put in ret, since SQL query may not find any KanjiSoloRadical
        wordIds.forEach { id ->
            ret[id] = arrayListOf()
        }

        var index = 0
        while (index < wordIds.size) {
            val slice = wordIds.sliceArray(
                index until (index + stepSize).coerceAtMost(wordIds.size)
            )
            kanjiSoloDao.getSoloByKanjiRadical(slice).mapValues {
                    lst -> lst.value.map{ it.toKanjiSoloRadical() }
            }.forEach { (key, value) ->
                ret[key]!! += value
            }
            index += stepSize
        }
        return ret
    }

}
