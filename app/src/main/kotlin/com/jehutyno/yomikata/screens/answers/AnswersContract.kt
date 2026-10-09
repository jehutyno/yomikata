package com.jehutyno.yomikata.screens.answers

import com.jehutyno.yomikata.presenters.BasePresenter
import com.jehutyno.yomikata.model.Answer
import com.jehutyno.yomikata.model.Sentence
import com.jehutyno.yomikata.model.Word
import com.jehutyno.yomikata.presenters.SelectionsInterface
import com.jehutyno.yomikata.presenters.WordInQuizInterface
import com.jehutyno.yomikata.util.quiz.Level


/**
 * Created by valentin on 25/10/2016.
 */
interface AnswersContract {

    interface View {
        fun displayAnswers()
    }

    interface Presenter: BasePresenter, SelectionsInterface, WordInQuizInterface {
        suspend fun getAnswersWordsSentences(answers: List<Answer>): List<Triple<Answer, Word, Sentence>>
        suspend fun getSentenceById(id: Long): Sentence
        suspend fun updateWordLevel(wordId: Long, level: Level)
    }

}
