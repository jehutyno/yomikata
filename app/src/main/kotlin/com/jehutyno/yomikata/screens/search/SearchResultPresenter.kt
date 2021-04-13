package com.jehutyno.yomikata.screens.search

import com.jehutyno.yomikata.model.Quiz
import com.jehutyno.yomikata.model.Word
import com.jehutyno.yomikata.repository.QuizRepository
import com.jehutyno.yomikata.repository.WordRepository
import com.jehutyno.yomikata.util.Categories
import mu.KLogging
import java.util.*

/**
 * Created by valentin on 13/10/2016.
 */
class SearchResultPresenter(
    private val wordRepository: WordRepository,
    private val quizRepository: QuizRepository,
    private val searchResultView: SearchResultContract.View) : SearchResultContract.Presenter {


    companion object : KLogging()

    init {
        searchResultView.setPresenter(this)
    }

    override fun start() {

    }

    override fun loadWords(searchString: String) {
        wordRepository.searchWords(searchString, object: WordRepository.LoadWordsCallback {
            override fun onWordsLoaded(words: List<Word>) {
                searchResultView.displayResults(words)
            }

            override fun onDataNotAvailable() {
                searchResultView.displayNoResults()
            }

        })
    }

    override fun updateWordCheck(id: Long, check: Boolean) {
        wordRepository.updateWordSelected(id, check)
    }

    override fun loadSelections() {
        quizRepository.getQuiz(Categories.CATEGORY_SELECTIONS, object: QuizRepository.LoadQuizCallback {
            override fun onQuizLoaded(quizzes: List<Quiz>) {
                searchResultView.selectionLoaded(quizzes)
            }

            override fun onDataNotAvailable() {
                searchResultView.noSelections()
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
