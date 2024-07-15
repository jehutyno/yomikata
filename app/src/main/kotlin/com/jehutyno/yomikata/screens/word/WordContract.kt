package com.jehutyno.yomikata.screens.word

import androidx.lifecycle.LiveData
import com.jehutyno.yomikata.BasePresenter
import com.jehutyno.yomikata.model.KanjiSoloRadical
import com.jehutyno.yomikata.model.Sentence
import com.jehutyno.yomikata.model.Word
import com.jehutyno.yomikata.presenters.SelectionsInterface
import com.jehutyno.yomikata.presenters.WordInQuizInterface


/**
 * Created by valentin on 27/09/2016.
 */
interface WordContract {

    interface View {
        fun displayWords(words: List<Word>)
    }

    interface Presenter : BasePresenter, SelectionsInterface, WordInQuizInterface {
        val words : LiveData<List<Word>>?
        suspend fun getWordKanjiSoloRadicalSentenceList(words: List<Word>)
                                            : List<Triple<Word, List<KanjiSoloRadical?>, Sentence>>
        suspend fun getKanjiSoloList(word: Word): List<KanjiSoloRadical>
        suspend fun getSentence(word: Word): Sentence
        suspend fun levelUp(id: Long, points: Int)
        suspend fun levelDown(id: Long, points: Int)
    }

}
