package com.jehutyno.yomikata.screens.content.word

import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.distinctUntilChanged
import com.jehutyno.yomikata.model.KanjiSoloRadical
import com.jehutyno.yomikata.model.Quiz
import com.jehutyno.yomikata.model.Sentence
import com.jehutyno.yomikata.model.Word
import com.jehutyno.yomikata.repository.KanjiSoloRepository
import com.jehutyno.yomikata.repository.QuizRepository
import com.jehutyno.yomikata.repository.SentenceRepository
import com.jehutyno.yomikata.repository.WordRepository
import com.jehutyno.yomikata.util.Categories
import com.jehutyno.yomikata.util.getLevelFromPoints
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import mu.KLogging


/**
 * Created by valentin on 29/09/2016.
 */
class WordPresenter(
    private val wordRepository: WordRepository,
    private val quizRepository: QuizRepository,
    private val radicalSource: KanjiSoloRepository,
    private val sentenceRepository: SentenceRepository,
    contentView: WordContract.View,
    coroutineScope: CoroutineScope,
    quizIds: LongArray?, level: Int, searchString: String) : WordContract.Presenter {

    companion object : KLogging()

    // use of StateFlow doesn't require observers, unlike LiveData
    private val job: Job
    private lateinit var selections : StateFlow<List<Quiz>>

    init {
        contentView.setPresenter(this)
        job = coroutineScope.launch {
            selections = quizRepository.getQuiz(Categories.CATEGORY_SELECTIONS).stateIn(coroutineScope)
        }
    }

    // LiveData to get word list to load
    // will be null if only a single word needs to be loaded
    // in that case, call the getWord method
    override val words : LiveData<List<Word>>? =
        if (quizIds != null && quizIds.isNotEmpty()) {
            wordRepository.getWordsByLevel(quizIds, level).asLiveData().distinctUntilChanged()
        }
        else if (searchString.isNotEmpty())
            wordRepository.searchWords(searchString).asLiveData().distinctUntilChanged()
        else
            null


    override fun start() {
        logger.info("Content presenter start")
    }

    override suspend fun getWordKanjiSoloRadicalSentenceList(words: List<Word>) : List<Triple<Word, List<KanjiSoloRadical?>, Sentence>> {
        // TODO: maybe add dedicated Dao for this operation to increase performance?
        val wordsRad = mutableListOf<Triple<Word, List<KanjiSoloRadical?>, Sentence>>()
        words.forEach {
            val sentence = sentenceRepository.getSentenceById(it.sentenceId!!)
            wordsRad.add(Triple(it, loadRadicals(it.japanese), sentence))
        }
        return wordsRad
    }

    override suspend fun getWord(wordId: Long): Word {
        return wordRepository.getWordById(wordId)
    }

    /**
     * Get selections
     *
     * @return The current selections as a List of Quiz objects
     */
    override suspend fun getSelections(): List<Quiz> {
        job.join()
        return selections.value
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

    override suspend fun levelUp(id: Long, points: Int) {
        val newPoints = com.jehutyno.yomikata.util.levelUp(points)
        wordRepository.updateWordPoints(id, newPoints)
        wordRepository.updateWordLevel(id, getLevelFromPoints(newPoints))
    }

    override suspend fun levelDown(id: Long, points: Int) {
        val newPoints = com.jehutyno.yomikata.util.levelDown(points)
        wordRepository.updateWordPoints(id, newPoints)
        wordRepository.updateWordLevel(id, getLevelFromPoints(newPoints))
    }
}
