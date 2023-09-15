package com.jehutyno.yomikata.repository.local

import com.jehutyno.yomikata.dao.SentenceDao
import com.jehutyno.yomikata.model.Sentence
import com.jehutyno.yomikata.model.Word
import com.jehutyno.yomikata.repository.SentenceRepository


/**
 * Created by valentinlanfranchi on 19/05/2017.
 */
class SentenceSource(private val sentenceDao: SentenceDao) : SentenceRepository {
    override suspend fun addSentence(sentence: Sentence) {
        sentenceDao.addSentence(RoomSentences.from(sentence))
    }

    override suspend fun getRandomSentence(word: Word, maxLevel: Int): Sentence? {
        return sentenceDao.getRandomSentence(word.japanese, word.reading, maxLevel)?.toSentence()
    }

    override suspend fun getSentenceById(id: Long): Sentence {
        return sentenceDao.getSentenceById(id)!!.toSentence()
    }

    override suspend fun getAllSentences(): List<Sentence> {
        return sentenceDao.getAllSentences().map { it.toSentence() }
    }

    override suspend fun updateSentence(updateSentence: Sentence, sentence: Sentence?) {
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
