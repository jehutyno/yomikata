package com.jehutyno.yomikata.screens.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.distinctUntilChanged
import com.jehutyno.yomikata.model.Word
import com.jehutyno.yomikata.presenters.SelectionsInterface
import com.jehutyno.yomikata.presenters.WordInQuizInterface
import com.jehutyno.yomikata.repository.WordRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import mu.KLogging


/**
 * Created by valentin on 13/10/2016.
 */
class SearchResultPresenter(
    wordRepository : WordRepository,
    selectionsInterface: SelectionsInterface,
    wordInQuizInterface: WordInQuizInterface) : SearchResultContract.Presenter,
            SelectionsInterface by selectionsInterface, WordInQuizInterface by wordInQuizInterface {


    companion object : KLogging()

    // LiveData
    private val searchString : MutableLiveData<String> by lazy { MutableLiveData<String>() }
    // Room
    @ExperimentalCoroutinesApi
    override val words : LiveData<List<Word>> = searchString.asFlow().flatMapLatest{
        wordRepository.searchWords(it)
    }.asLiveData().distinctUntilChanged()

    override fun start() {

    }

    override fun updateSearchString(newSearchString: String) {
        searchString.value = newSearchString
    }

}
