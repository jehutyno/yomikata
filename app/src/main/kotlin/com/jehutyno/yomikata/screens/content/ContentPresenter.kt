package com.jehutyno.yomikata.screens.content

import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.distinctUntilChanged
import com.jehutyno.yomikata.model.Word
import com.jehutyno.yomikata.presenters.SelectionsInterface
import com.jehutyno.yomikata.presenters.WordCountInterface
import com.jehutyno.yomikata.presenters.WordInQuizInterface
import com.jehutyno.yomikata.repository.WordRepository
import com.jehutyno.yomikata.util.Level
import mu.KLogging


/**
 * Created by valentin on 29/09/2016.
 */
class ContentPresenter(
    wordRepository: WordRepository,
    contentView: ContentContract.View,
    selectionsInterface: SelectionsInterface,
    wordCountInterface: WordCountInterface,
    wordInQuizInterface: WordInQuizInterface,
    quizIds : LongArray, level : Level?) : ContentContract.Presenter,
                                           SelectionsInterface by selectionsInterface,
                                           WordCountInterface by wordCountInterface,
                                           WordInQuizInterface by wordInQuizInterface {

    companion object : KLogging()

    init {
        contentView.setPresenter(this)
    }

    // define LiveData
    override val words: LiveData<List<Word>> =
        wordRepository.getWordsByLevel(quizIds, level).asLiveData().distinctUntilChanged()

    override fun start() {
        logger.info("Content presenter start")
    }

}
