package com.jehutyno.yomikata.screens.content.word

import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.distinctUntilChanged
import com.jehutyno.yomikata.model.KanjiSoloRadical
import com.jehutyno.yomikata.model.Sentence
import com.jehutyno.yomikata.model.Word
import com.jehutyno.yomikata.presenters.SelectionsInterface
import com.jehutyno.yomikata.presenters.WordInQuizInterface
import com.jehutyno.yomikata.repository.KanjiSoloRepository
import com.jehutyno.yomikata.repository.SentenceRepository
import com.jehutyno.yomikata.repository.WordRepository
import com.jehutyno.yomikata.util.Level
import com.jehutyno.yomikata.util.getLevelFromPoints
import mu.KLogging


/**
 * Created by valentin on 29/09/2016.
 */
class WordPresenter(
    private val wordRepository: WordRepository,
    private val radicalSource: KanjiSoloRepository,
    private val sentenceRepository: SentenceRepository,
    selectionsInterface: SelectionsInterface,
    wordInQuizInterface: WordInQuizInterface,
    quizIds: LongArray?, level: Level?, searchString: String) : WordContract.Presenter,
            SelectionsInterface by selectionsInterface, WordInQuizInterface by wordInQuizInterface {

    companion object : KLogging()

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

    private suspend fun loadRadicals(kanjis: String): List<KanjiSoloRadical?> {
        val radicals = mutableListOf<KanjiSoloRadical?>()
        kanjis.forEach {
            radicals.add(radicalSource.getSoloByKanjiRadical(it.toString()))
        }

        return radicals
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
