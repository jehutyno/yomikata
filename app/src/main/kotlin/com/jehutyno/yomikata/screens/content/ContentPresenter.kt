package com.jehutyno.yomikata.screens.content

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
    private val contentView: ContentContract.View) : ContentContract.Presenter {

    companion object : KLogging()

    init {
        contentView.setPresenter(this)
    }

    override fun start() {
        logger.info("Content presenter start")
    }

    override fun loadWords(quizIds: LongArray, level: Int) {
        if (level > -1) {
            wordRepository.getWordsByLevel(quizIds, level, object : WordRepository.LoadWordsCallback {
                override fun onWordsLoaded(words: List<Word>) {
                    contentView.displayWords(words)
                }

                override fun onDataNotAvailable() {
                    contentView.displayWords(emptyList())
                }

            })
        } else {
            wordRepository.getWords(quizIds, object : WordRepository.LoadWordsCallback {
                override fun onWordsLoaded(words: List<Word>) {
                    contentView.displayWords(words)
                }

                override fun onDataNotAvailable() {
                    contentView.displayWords(emptyList())
                }

            })
        }
    }

    override fun countQuiz(ids: LongArray): Int {
        return quizRepository.countWordsForQuizzes(ids)
    }

    override fun countLow(ids: LongArray): Int {
        return quizRepository.countWordsForLevel(ids, 0)
    }

    override fun countMedium(ids: LongArray): Int {
        return quizRepository.countWordsForLevel(ids, 1)
    }

    override fun countHigh(ids: LongArray): Int {
        return quizRepository.countWordsForLevel(ids, 2)
    }

    override fun countMaster(ids: LongArray): Int {
        return quizRepository.countWordsForLevel(ids, 3) + quizRepository.countWordsForLevel(ids, 4)
    }

    override fun updateWordCheck(id: Long, check: Boolean) {
        wordRepository.updateWordSelected(id, check)
    }

    override fun loadSelections() {
        quizRepository.getQuiz(Categories.CATEGORY_SELECTIONS, object: QuizRepository.LoadQuizCallback {
            override fun onQuizLoaded(quizzes: List<Quiz>) {
                contentView.selectionLoaded(quizzes)
            }

            override fun onDataNotAvailable() {
                contentView.noSelections()
            }

        })
    }

    override fun isWordInQuiz(wordId: Long, quizId: Long) : Boolean {
        return wordRepository.isWordInQuiz(wordId, quizId)
    }

    override fun createSelection(quizName: String): Long {
        return quizRepository.saveQuiz(quizName, Categories.CATEGORY_SELECTIONS)
    }

    override fun addWordToSelection(wordId: Long, quizId: Long) {
        quizRepository.addWordToQuiz(wordId, quizId)
    }

    override fun isWordInQuizzes(wordId: Long, quizIds: Array<Long>) : ArrayList<Boolean> {
        return wordRepository.isWordInQuizzes(wordId, quizIds)
    }

    override fun deleteWordFromSelection(wordId: Long, selectionId: Long) {
        quizRepository.deleteWordFromQuiz(wordId, selectionId)
    }

}
