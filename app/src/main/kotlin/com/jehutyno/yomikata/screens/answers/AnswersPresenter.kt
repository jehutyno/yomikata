package com.jehutyno.yomikata.screens.answers

import com.jehutyno.yomikata.model.Answer
import com.jehutyno.yomikata.model.Sentence
import com.jehutyno.yomikata.model.Word
import com.jehutyno.yomikata.presenters.SelectionsInterface
import com.jehutyno.yomikata.presenters.WordInQuizInterface
import com.jehutyno.yomikata.repository.SentenceRepository


/**
 * Created by valentin on 25/10/2016.
 */
class AnswersPresenter(
    selectionsInterface: SelectionsInterface,
    wordInQuizInterface: WordInQuizInterface,
    private val sentenceRepository: SentenceRepository)
                : AnswersContract.Presenter, SelectionsInterface by selectionsInterface,
                                             WordInQuizInterface by wordInQuizInterface{

    override fun start() {
    }


    override suspend fun getSentenceById(id: Long): Sentence {
        return sentenceRepository.getSentenceById(id)
    }

    override suspend fun getAnswersWordsSentences(answers: List<Answer>): List<Triple<Answer, Word, Sentence>> {
        val answersWordsSentences = mutableListOf<Triple<Answer, Word, Sentence>>()
        answers.forEach {
            answersWordsSentences.add(Triple(it, getWordById(it.wordId), getSentenceById(it.sentenceId)))
        }
        return answersWordsSentences
    }

}
