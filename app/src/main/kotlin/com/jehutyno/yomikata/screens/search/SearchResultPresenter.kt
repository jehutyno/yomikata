package com.jehutyno.yomikata.screens.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.distinctUntilChanged
import com.jehutyno.yomikata.model.Quiz
import com.jehutyno.yomikata.model.Word
import com.jehutyno.yomikata.repository.QuizRepository
import com.jehutyno.yomikata.repository.WordRepository
import com.jehutyno.yomikata.util.Categories
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import mu.KLogging


/**
 * Created by valentin on 13/10/2016.
 */
class SearchResultPresenter(
    private val wordRepository: WordRepository,
    private val quizRepository: QuizRepository,
    searchResultView: SearchResultContract.View, coroutineScope: CoroutineScope) : SearchResultContract.Presenter {


    companion object : KLogging()

    private val job: Job
    private lateinit var selections: StateFlow<List<Quiz>>
    init {
        job = coroutineScope.launch {
            selections = quizRepository.getQuiz(Categories.CATEGORY_SELECTIONS).stateIn(coroutineScope)
        }
        searchResultView.setPresenter(this)
    }

    // LiveData
    private val searchString : MutableLiveData<String> by lazy { MutableLiveData<String>() }
    // Room
    @kotlinx.coroutines.ExperimentalCoroutinesApi
    override val words : LiveData<List<Word>> = searchString.asFlow().flatMapLatest{
        wordRepository.searchWords(it)
    }.asLiveData().distinctUntilChanged()
    override fun start() {

    }

    override suspend fun getSelections(): List<Quiz> {
        job.join()
        return selections.value
    }

    override fun updateSearchString(newSearchString: String) {
        searchString.value = newSearchString
    }

    override suspend fun updateWordCheck(id: Long, check: Boolean) {
        wordRepository.updateWordSelected(id, check)
    }

    override suspend fun updateWordsCheck(ids: LongArray, check: Boolean) {
        wordRepository.updateWordsSelected(ids, check)
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
