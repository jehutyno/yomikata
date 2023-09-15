package com.jehutyno.yomikata.screens.content

import androidx.lifecycle.LiveData
import com.jehutyno.yomikata.BasePresenter
import com.jehutyno.yomikata.BaseView
import com.jehutyno.yomikata.model.Quiz
import com.jehutyno.yomikata.model.Word


/**
 * Created by valentin on 27/09/2016.
 */
interface ContentContract {

    interface View: BaseView<Presenter> {
        fun displayWords(words: List<Word>)
        fun displayStats()
        fun selectionLoaded(quizzes: List<Quiz>)
    }

    interface Presenter: BasePresenter {
        val words: LiveData<List<Word>>
        val selections: LiveData<List<Quiz>>
        val quizCount: LiveData<Int>
        val lowCount: LiveData<Int>
        val mediumCount: LiveData<Int>
        val highCount: LiveData<Int>
        val masterCount: LiveData<Int>
        suspend fun updateWordCheck(id: Long, check: Boolean)
        suspend fun updateWordsCheck(ids: LongArray, check: Boolean)
        suspend fun isWordInQuiz(wordId: Long, quizId: Long): Boolean
        suspend fun createSelection(quizName: String): Long
        suspend fun addWordToSelection(wordId: Long, quizId: Long)
        suspend fun isWordInQuizzes(wordId: Long, quizIds: Array<Long>): ArrayList<Boolean>
        suspend fun deleteWordFromSelection(wordId: Long, selectionId: Long)
    }

}
