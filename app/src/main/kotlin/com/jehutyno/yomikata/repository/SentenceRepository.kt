package com.jehutyno.yomikata.repository

import com.jehutyno.yomikata.model.Sentence
import com.jehutyno.yomikata.model.Word


/**
 * Created by valentin on 27/09/2016.
 */
interface SentenceRepository {
    suspend fun getRandomSentence(word: Word, maxLevel: Int): Sentence?
    suspend fun getSentenceById(id: Long): Sentence
    suspend fun getSentencesByIds(ids: LongArray): List<Sentence>
    suspend fun addSentence(sentence: Sentence)
    suspend fun updateSentence(updateSentence: Sentence, sentence: Sentence?)
    suspend fun getAllSentences(): List<Sentence>
}
