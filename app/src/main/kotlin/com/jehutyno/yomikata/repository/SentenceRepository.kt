package com.jehutyno.yomikata.repository

import android.database.sqlite.SQLiteDatabase
import com.jehutyno.yomikata.model.Sentence
import com.jehutyno.yomikata.model.Word


/**
 * Created by valentin on 27/09/2016.
 */
interface SentenceRepository {
    fun getRandomSentence(word: Word, maxLevel: Int): Sentence?
    fun getSentenceById(id: Long): Sentence
    fun createSentencesTable()
    fun addSentence(sentence: Sentence)
    fun updateSentence(updateSentence: Sentence, sentence: Sentence?)
    fun getAllSentences(db: SQLiteDatabase?): List<Sentence>
}
