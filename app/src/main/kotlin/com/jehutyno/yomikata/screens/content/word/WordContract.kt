package com.jehutyno.yomikata.screens.content.word

import androidx.lifecycle.LiveData
import com.jehutyno.yomikata.BasePresenter
import com.jehutyno.yomikata.BaseView
import com.jehutyno.yomikata.model.KanjiSoloRadical
import com.jehutyno.yomikata.model.Quiz
import com.jehutyno.yomikata.model.Sentence
import com.jehutyno.yomikata.model.Word
import java.util.*


/**
 * Created by valentin on 27/09/2016.
 */
interface WordContract {

    interface View : BaseView<Presenter> {
        fun displayWords(words: List<Triple<Word, List<KanjiSoloRadical?>, Sentence>>)
    }

    interface Presenter : BasePresenter {
        val words : LiveData<List<Word>>?
        suspend fun getWordKanjiSoloRadicalSentenceList(words: List<Word>)
                                            : List<Triple<Word, List<KanjiSoloRadical?>, Sentence>>
        suspend fun getSelections() : List<Quiz>
        suspend fun createSelection(quizName: String) : Long
        suspend fun addWordToSelection(wordId: Long, quizId: Long)
        suspend fun isWordInQuizzes(wordId: Long, quizIds: Array<Long>) : ArrayList<Boolean>
        suspend fun isWordInQuiz(wordId: Long, quizId: Long) : Boolean
        suspend fun deleteWordFromSelection(wordId: Long, selectionId: Long)
        suspend fun levelUp(id: Long, level: Int) : Int
        suspend fun levelDown(id: Long, level: Int) : Int
        suspend fun getWord(wordId: Long): Word
    }

}
