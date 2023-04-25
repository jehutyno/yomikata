package com.jehutyno.yomikata.screens.answers

import com.jehutyno.yomikata.model.Answer
import com.jehutyno.yomikata.model.Quiz
import com.jehutyno.yomikata.model.Sentence
import com.jehutyno.yomikata.model.Word
import com.jehutyno.yomikata.repository.QuizRepository
import com.jehutyno.yomikata.repository.SentenceRepository
import com.jehutyno.yomikata.repository.WordRepository
import com.jehutyno.yomikata.util.Categories
import java.util.*

/**
 * Created by valentin on 25/10/2016.
 */
class AnswersPresenter(
    private val wordRepository: WordRepository,
    private val quizRepository: QuizRepository,
    private val sentenceRepository: SentenceRepository,
    private val answersView: AnswersContract.View) : AnswersContract.Presenter {

    init {
        answersView.setPresenter(this)
    }

    override fun start() {
    }

    override suspend fun loadSelections() {
        quizRepository.getQuiz(Categories.CATEGORY_SELECTIONS, object: QuizRepository.LoadQuizCallback {
            override fun onQuizLoaded(quizzes: List<Quiz>) {
                answersView.selectionLoaded(quizzes)
            }

            override fun onDataNotAvailable() {
                answersView.noSelections()
            }

        })
    }

    override suspend fun createSelection(quizName: String): Long {
        return quizRepository.saveQuiz(quizName, Categories.CATEGORY_SELECTIONS)
    }

    override suspend fun addWordToSelection(wordId: Long, quizId: Long) {
        quizRepository.addWordToQuiz(wordId, quizId)
    }

    override suspend fun isWordInQuiz(wordId: Long, quizId: Long) : Boolean {
        return wordRepository.isWordInQuiz(wordId, quizId)
    }

    override suspend fun isWordInQuizzes(wordId: Long, quizIds: Array<Long>) : ArrayList<Boolean> {
        return wordRepository.isWordInQuizzes(wordId, quizIds)
    }

    override suspend fun deleteWordFromSelection(wordId: Long, selectionId: Long) {
        quizRepository.deleteWordFromQuiz(wordId, selectionId)
    }

    override suspend fun getWordById(id: Long): Word {
        return wordRepository.getWordById(id)
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
