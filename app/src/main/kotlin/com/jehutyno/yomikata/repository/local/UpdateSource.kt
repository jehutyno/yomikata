package com.jehutyno.yomikata.repository.local

import com.jehutyno.yomikata.model.*


/**
 * Created by jehutyno on 08/10/2016.
 */
class UpdateSource(private val updateDao: UpdateDao) {

    fun getAllWords(): List<Word> {
        return updateDao.getAllWords().map { it.toWord() }
    }

    fun getAllStatEntries(): List<StatEntry> {
        return updateDao.getAllStatEntries().map { it.toStatEntry() }
    }

    fun getAddedQuizWords(): List<QuizWord> {
        return updateDao.getAddedQuizWords(7504).map { it.toQuizWord() }
    }

    fun getAddedQuizzes(): List<Quiz> {
        return updateDao.getAddedQuizzes(96).map { it.toQuiz() }
    }

    fun getAllKanjiSolo(): List<KanjiSolo> {
        return updateDao.getAllKanjiSolo().map { it.toKanjiSolo() }
    }

    fun getAllRadicals(): List<Radical> {
        return updateDao.getAllRadicals().map { it.toRadical() }
    }

    fun getAllSentences(): List<Sentence> {
        return updateDao.getAllSentences().map { it.toSentence() }
    }

}
