package com.jehutyno.yomikata.repository.local

import android.database.sqlite.SQLiteDatabase
import com.jehutyno.yomikata.model.Sentence
import com.jehutyno.yomikata.model.Word
import com.jehutyno.yomikata.repository.SentenceRepository


/**
 * Created by valentinlanfranchi on 19/05/2017.
 */
class SentenceSource(private val sentenceDao: SentenceDao) : SentenceRepository {

    override fun createSentencesTable() {
        TODO("Not yet implemented")
    }

    override fun addSentence(sentence: Sentence) {
        sentenceDao.addSentence(RoomSentences.from(sentence))
    }

    override fun getRandomSentence(word: Word, maxLevel: Int): Sentence? {
        return sentenceDao.getRandomSentence(RoomWords.from(word), maxLevel)?.toSentence()
    }

    override fun getSentenceById(id: Long): Sentence {
        return sentenceDao.getSentenceById(id)!!.toSentence()
    }

    override fun getAllSentences(db: SQLiteDatabase?): List<Sentence> {
        return sentenceDao.getAllSentences().map { it.toSentence() }
    }

    override fun updateSentence(updateSentence: Sentence, sentence: Sentence?) {
        if (sentence != null) {
            // update only jap, en, fr (NOT id, level)
            val roomUpdateSentence = RoomSentences(sentence.id, updateSentence.jap,
                                    updateSentence.en, updateSentence.fr, sentence.level)
            sentenceDao.updateSentence(roomUpdateSentence)
        } else {
            sentenceDao.addSentence(RoomSentences.from(updateSentence))
        }
    }

}
