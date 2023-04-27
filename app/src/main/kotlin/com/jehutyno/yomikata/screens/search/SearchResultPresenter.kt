package com.jehutyno.yomikata.screens.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import com.jehutyno.yomikata.model.Word
import com.jehutyno.yomikata.repository.QuizRepository
import com.jehutyno.yomikata.repository.WordRepository
import com.jehutyno.yomikata.util.Categories
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.transform
import mu.KLogging
import java.util.*


/**
 * Created by valentin on 13/10/2016.
 */
class SearchResultPresenter(
    private val wordRepository: WordRepository,
    private val quizRepository: QuizRepository,
    searchResultView: SearchResultContract.View) : SearchResultContract.Presenter {


    companion object : KLogging()

    // LiveData
    private val searchString : MutableLiveData<String> by lazy { MutableLiveData<String>() }
    // Room
    override val words : LiveData<List<Word>> = searchString.asFlow().transform<String, List<Word>> {
        emit(wordRepository.searchWords(it).first())
    }.asLiveData()
    init {
        searchResultView.setPresenter(this)
    }

    override fun start() {

    }

    override fun updateSearchString(newSearchString: String) {
        searchString.value = newSearchString
    }

    override suspend fun updateWordCheck(id: Long, check: Boolean) {
        wordRepository.updateWordSelected(id, check)
    }

    override suspend fun loadSelections() {
//        quizRepository.getQuiz(Categories.CATEGORY_SELECTIONS, object: QuizRepository.LoadQuizCallback {
//            override fun onQuizLoaded(quizzes: List<Quiz>) {
//                searchResultView.selectionLoaded(quizzes)
//            }
//
//            override fun onDataNotAvailable() {
//                searchResultView.noSelections()
//            }
//
//        })
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
