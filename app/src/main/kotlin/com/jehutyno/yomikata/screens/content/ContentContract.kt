package com.jehutyno.yomikata.screens.content

import androidx.lifecycle.LiveData
import com.jehutyno.yomikata.BasePresenter
import com.jehutyno.yomikata.BaseView
import com.jehutyno.yomikata.model.Quiz
import com.jehutyno.yomikata.model.Word
import com.jehutyno.yomikata.presenters.SelectionsInterface
import com.jehutyno.yomikata.presenters.WordCountInterface
import com.jehutyno.yomikata.presenters.WordInQuizInterface


/**
 * Created by valentin on 27/09/2016.
 */
interface ContentContract {

    interface View: BaseView<Presenter> {
        fun displayWords(words: List<Word>)
        fun displayStats()
        fun selectionLoaded(quizzes: List<Quiz>)
    }

    interface Presenter: BasePresenter, SelectionsInterface, WordCountInterface, WordInQuizInterface {
        val words: LiveData<List<Word>>
        val selections: LiveData<List<Quiz>>
        suspend fun updateWordCheck(id: Long, check: Boolean)
        suspend fun updateWordsCheck(ids: LongArray, check: Boolean)
    }

}
