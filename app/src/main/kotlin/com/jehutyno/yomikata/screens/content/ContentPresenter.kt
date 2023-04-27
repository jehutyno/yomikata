package com.jehutyno.yomikata.screens.content

import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.jehutyno.yomikata.model.Quiz
import com.jehutyno.yomikata.model.Word
import com.jehutyno.yomikata.repository.QuizRepository
import com.jehutyno.yomikata.repository.WordRepository
import com.jehutyno.yomikata.util.Categories
import mu.KLogging
import java.util.*


/**
 * Created by valentin on 29/09/2016.
 */
class ContentPresenter(
    private val wordRepository: WordRepository,
    private val quizRepository: QuizRepository,
    contentView: ContentContract.View,
    quizIds : LongArray, level : Int) : ContentContract.Presenter {

    companion object : KLogging()

    init {
        contentView.setPresenter(this)
    }

    // define LiveData
    override val words: LiveData<List<Word>> = wordRepository.getWordsByLevel(quizIds, level).asLiveData()
    override val selections: LiveData<List<Quiz>> = quizRepository.getQuiz(Categories.CATEGORY_SELECTIONS).asLiveData()

    override fun start() {
        logger.info("Content presenter start")
    }

    override suspend fun countQuiz(ids: LongArray): Int {
        return quizRepository.countWordsForQuizzes(ids)
    }

    override suspend fun countLow(ids: LongArray): Int {
        return quizRepository.countWordsForLevel(ids, 0)
    }

    override suspend fun countMedium(ids: LongArray): Int {
        return quizRepository.countWordsForLevel(ids, 1)
    }

    override suspend fun countHigh(ids: LongArray): Int {
        return quizRepository.countWordsForLevel(ids, 2)
    }

    override suspend fun countMaster(ids: LongArray): Int {
        return quizRepository.countWordsForLevel(ids, 3) + quizRepository.countWordsForLevel(ids, 4)
    }

    override suspend fun updateWordCheck(id: Long, check: Boolean) {
        wordRepository.updateWordSelected(id, check)
    }

    override suspend fun isWordInQuiz(wordId: Long, quizId: Long) : Boolean {
        return wordRepository.isWordInQuiz(wordId, quizId)
    }

    override suspend fun createSelection(quizName: String): Long {
        return quizRepository.saveQuiz(quizName, Categories.CATEGORY_SELECTIONS)
    }

    override suspend fun addWordToSelection(wordId: Long, quizId: Long) {
        quizRepository.addWordToQuiz(wordId, quizId)
    }

    override suspend fun isWordInQuizzes(wordId: Long, quizIds: Array<Long>) : ArrayList<Boolean> {
        return wordRepository.isWordInQuizzes(wordId, quizIds)
    }

    override suspend fun deleteWordFromSelection(wordId: Long, selectionId: Long) {
        quizRepository.deleteWordFromQuiz(wordId, selectionId)
    }

}
