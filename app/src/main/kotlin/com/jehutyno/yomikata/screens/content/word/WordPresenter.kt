package com.jehutyno.yomikata.screens.content.word

import com.jehutyno.yomikata.model.KanjiSoloRadical
import com.jehutyno.yomikata.model.Quiz
import com.jehutyno.yomikata.model.Sentence
import com.jehutyno.yomikata.model.Word
import com.jehutyno.yomikata.repository.QuizRepository
import com.jehutyno.yomikata.repository.SentenceRepository
import com.jehutyno.yomikata.repository.WordRepository
import com.jehutyno.yomikata.repository.KanjiSoloRepository
import com.jehutyno.yomikata.util.Categories
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import mu.KLogging
import java.util.*

/**
 * Created by valentin on 29/09/2016.
 */
class WordPresenter(
    private val wordRepository: WordRepository,
    private val quizRepository: QuizRepository,
    private val radicalSource: KanjiSoloRepository,
    private val sentenceRepository: SentenceRepository,
    private val contentView: WordContract.View) : WordContract.Presenter {

    companion object : KLogging()

    init {
        contentView.setPresenter(this)
    }

    override fun start() {
        logger.info("Content presenter start")
    }

    override suspend fun loadWords(quizIds: LongArray, level: Int) {
        val loadWordsCallback = object : WordRepository.LoadWordsCallback {
            override fun onWordsLoaded(words: List<Word>) = runBlocking {
                val wordsRad = mutableListOf<Triple<Word, List<KanjiSoloRadical?>, Sentence>>()
                words.forEach {
                    val sentence = sentenceRepository.getSentenceById(it.sentenceId!!)
                    wordsRad.add(Triple(it, loadRadicals(it.japanese), sentence))
                }
                contentView.displayWords(wordsRad)
            }

            override fun onDataNotAvailable() {
                contentView.displayWords(emptyList())
            }
        }

        if (level > -1) {
            wordRepository.getWordsByLevel(quizIds, level, loadWordsCallback)
        } else {
            wordRepository.getWords(quizIds, loadWordsCallback)
        }
    }


    override fun loadWord(wordId: Long) = CoroutineScope(Dispatchers.IO).launch {
        val word = wordRepository.getWordById(wordId)
        val sentence = sentenceRepository.getSentenceById(word.sentenceId!!)
        val wordsRad = mutableListOf<Triple<Word, List<KanjiSoloRadical?>, Sentence>>()
        wordsRad.add(Triple(word, loadRadicals(word.japanese), sentence))
        withContext(Dispatchers.IO) {
            contentView.displayWords(wordsRad)
        }
    }

    override fun searchWords(searchString: String) = CoroutineScope(Dispatchers.Main).launch {
        wordRepository.searchWords(searchString, object : WordRepository.LoadWordsCallback {
            override fun onWordsLoaded(words: List<Word>) = runBlocking {
                val wordsRad = mutableListOf<Triple<Word, List<KanjiSoloRadical?>, Sentence>>()
                words.forEach {
                    val sentence = sentenceRepository.getSentenceById(it.sentenceId!!)
                    wordsRad.add(Triple(it, loadRadicals(it.japanese), sentence))
                }
                contentView.displayWords(wordsRad)
            }

            override fun onDataNotAvailable() {
                logger.info { "*************** NO DATA FOUND FOR QUIZ : $searchString ***************" }
            }

        })
    }

    override suspend fun loadSelections() {
        quizRepository.getQuiz(Categories.CATEGORY_SELECTIONS, object : QuizRepository.LoadQuizCallback {
            override fun onQuizLoaded(quizzes: List<Quiz>) {
                contentView.selectionLoaded(quizzes)
            }

            override fun onDataNotAvailable() {
                contentView.noSelections()
            }

        })
    }

    suspend fun loadRadicals(kanjis: String): List<KanjiSoloRadical?> {
        val radicals = mutableListOf<KanjiSoloRadical?>()
        kanjis.forEach {
            radicals.add(radicalSource.getSoloByKanjiRadical(it.toString()))
        }

        return radicals
    }

    override suspend fun createSelection(quizName: String): Long {
        return quizRepository.saveQuiz(quizName, Categories.CATEGORY_SELECTIONS)
    }

    override suspend fun addWordToSelection(wordId: Long, quizId: Long) {
        quizRepository.addWordToQuiz(wordId, quizId)
    }

    override suspend fun isWordInQuiz(wordId: Long, quizId: Long): Boolean {
        return wordRepository.isWordInQuiz(wordId, quizId)
    }

    override suspend fun isWordInQuizzes(wordId: Long, quizIds: Array<Long>): ArrayList<Boolean> {
        return wordRepository.isWordInQuizzes(wordId, quizIds)
    }

    override suspend fun deleteWordFromSelection(wordId: Long, selectionId: Long) {
        quizRepository.deleteWordFromQuiz(wordId, selectionId)
    }

    override suspend fun levelUp(id: Long, level: Int): Int {
        return if (level < 3) {
            wordRepository.updateWordLevel(id, level + 1)
            level + 1
        } else if (level == 3) {
            wordRepository.updateWordPoints(id, 100)
            3
        } else {
            level
        }
    }

    override suspend fun levelDown(id: Long, level: Int): Int {
        return if (level > 0) {
            wordRepository.updateWordPoints(id, 0)
            wordRepository.updateWordLevel(id, level - 1)
            level - 1
        } else {
            wordRepository.updateWordPoints(id, 0)
            level
        }
    }
}