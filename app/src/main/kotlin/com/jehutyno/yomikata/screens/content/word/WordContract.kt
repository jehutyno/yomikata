package com.jehutyno.yomikata.screens.content.word

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
        fun selectionLoaded(quizzes: List<Quiz>)
        fun noSelections()
    }

    interface Presenter : BasePresenter {
        fun loadWords(quizIds: LongArray, level: Int)
        fun loadSelections()
        fun createSelection(quizName: String): Long
        fun addWordToSelection(wordId: Long, quizId: Long)
        fun isWordInQuizzes(wordId: Long, quizIds: Array<Long>) : ArrayList<Boolean>
        fun isWordInQuiz(wordId: Long, quizId: Long) : Boolean
        fun deleteWordFromSelection(wordId: Long, selectionId: Long)
        fun searchWords(seazrchString: String)
        fun levelUp(id: Long, level: Int) : Int
        fun levelDown(id: Long, level: Int) : Int
        fun loadWord(wordId: Long)
    }

}