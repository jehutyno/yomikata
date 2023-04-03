package com.jehutyno.yomikata.repository.local

import android.database.sqlite.SQLiteDatabase
import com.jehutyno.yomikata.model.QuizWord
import com.jehutyno.yomikata.model.Word
import com.jehutyno.yomikata.repository.WordRepository
import com.jehutyno.yomikata.repository.migration.WordTable
import com.jehutyno.yomikata.util.HiraganaUtils
import com.jehutyno.yomikata.util.QuizType
import java.util.*


/**
 * Created by jehutyno on 08/10/2016.
 */
class WordSource(private val wordDao: WordDao) : WordRepository {

    override fun getAllWords(db: SQLiteDatabase?): List<Word> {
        return wordDao.getAllWords().map { it.toWord() }
    }

    private fun wordsCallback(
        roomWordsList: List<RoomWords>,
        callback: WordRepository.LoadWordsCallback
    ) {
        if (roomWordsList.isNotEmpty()) {
            val wordsList = roomWordsList.map { it.toWord() }
            callback.onWordsLoaded(wordsList)
        } else {
            callback.onDataNotAvailable()
        }
    }

    override fun getWords(quizId: Long, callback: WordRepository.LoadWordsCallback) {
        val roomWordsList = wordDao.getWords(quizId)
        wordsCallback(roomWordsList, callback)
    }

    override fun getWords(quizIds: LongArray, callback: WordRepository.LoadWordsCallback) {
        val roomWordsList = wordDao.getWords(quizIds)
        wordsCallback(roomWordsList, callback)
    }

    override fun getWordsByLevel(
        quizIds: LongArray,
        level: Int,
        callback: WordRepository.LoadWordsCallback
    ) {
        val levelsArray =
            if (level == 3)
                intArrayOf(level, level + 1)
            else
                intArrayOf(level)

        val roomWordsList = wordDao.getWordsByLevels(quizIds, levelsArray)
        wordsCallback(roomWordsList, callback)
    }

    override fun getWordsByRepetition(
        quizIds: LongArray,
        repetition: Int,
        limit: Int
    ): ArrayList<Word> {
        return wordDao.getWordsByRepetition(quizIds, repetition, limit)
            .map { it.toWord() } as ArrayList<Word>
    }

    override fun getRandomWords(
        wordId: Long,
        answer: String,
        wordSize: Int,
        limit: Int,
        quizType: QuizType
    ): ArrayList<Word> {
        val column = when (quizType) {
            QuizType.TYPE_PRONUNCIATION -> "${RoomWords.getTableName()}.reading"
            QuizType.TYPE_PRONUNCIATION_QCM -> "${RoomWords.getTableName()}.reading"
            QuizType.TYPE_AUDIO -> "${RoomWords.getTableName()}.japanese"
            QuizType.TYPE_EN_JAP -> "${RoomWords.getTableName()}.japanese"
            QuizType.TYPE_JAP_EN -> "${RoomWords.getTableName()}.japanese"
            else -> "${RoomWords.getTableName()}.japanese"
        }

        val roomWordsList = wordDao.getRandomWords(wordId, answer, wordSize, column, 96, limit).toMutableList()

        if (roomWordsList.size < limit) {
            val extraRoomWordsList = wordDao.getRandomWords(
                wordId,
                answer,
                wordSize,
                column,
                96,
                limit - roomWordsList.size
            )
            roomWordsList += extraRoomWordsList
        }

        return roomWordsList.map { it.toWord() } as ArrayList<Word>
    }

    override fun searchWords(searchString: String, callback: WordRepository.LoadWordsCallback) {
        val hiragana = HiraganaUtils.toHiragana(searchString)
        val roomWordsList = wordDao.searchWords(searchString, hiragana)
        wordsCallback(roomWordsList, callback)
    }

    override fun isWordInQuiz(wordId: Long, quizId: Long): Boolean {
        return wordDao.isWordInQuiz(wordId, quizId)
    }

    override fun isWordInQuizzes(wordId: Long, quizIds: Array<Long>): ArrayList<Boolean> {
        val isInQuizArrayList: ArrayList<Boolean> = arrayListOf()
        for (quizId in quizIds)
            isInQuizArrayList.add(isWordInQuiz(wordId, quizId))

        return isInQuizArrayList
    }

    override fun getWordById(wordId: Long): Word {
        return wordDao.getWordById(wordId)!!.toWord()
    }

    override fun saveWord(task: Word) {

    }

    override fun refreshWords() {

    }

    override fun deleteAllWords() {
        wordDao.deleteAllWords()
    }

    override fun deleteWord(wordId: Long) {
        val roomWord = wordDao.getWordById(wordId)
        if (roomWord != null)
            wordDao.deleteWord(roomWord)
    }

    override fun updateWordPoints(wordId: Long, points: Int) {
        wordDao.updateWordPoints(wordId, points)
    }

    override fun updateWordLevel(wordId: Long, level: Int) {
        wordDao.updateWordLevel(wordId, level)
    }

    override fun updateWordRepetition(wordId: Long, repetition: Int) {
        wordDao.updateWordRepetition(wordId, repetition)
    }

    override fun decreaseWordsRepetition(quizIds: LongArray) {
        val idList = wordDao.getWordsWithRepetitionStrictlyGreaterThan(quizIds, 0)
        wordDao.decreaseWordRepetitionByOne(idList)
    }

    override fun restoreWord(word: String, pronunciation: String, wordTable: WordTable) {
        val points = when (wordTable.priority) {
            1 -> 75
            2 -> 50
            3 -> 100
            else -> 0
        }
        val priority = when (wordTable.priority) {
            1 -> 0
            else -> wordTable.priority
        }

        wordDao.restoreWord(word, pronunciation, priority, points,
                            wordTable.counterFail, wordTable.counterTry, wordTable.counterSuccess)
    }

    override fun updateWord(updateWord: Word, word: Word?) {
        if (word != null) {
            val newWord = Word(
                word.id,
                updateWord.japanese,
                updateWord.english,
                updateWord.french,
                updateWord.reading,
                word.level,
                word.countTry,
                word.countSuccess,
                word.countFail,
                word.isKana,
                word.repetition,
                word.points,
                word.baseCategory,
                word.isSelected,
                updateWord.sentenceId
            )
            wordDao.updateWord(RoomWords.from(newWord))
        } else {
            val newWord = Word(
                0, updateWord.japanese, updateWord.english, updateWord.french,
                updateWord.reading, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0
            )
            wordDao.updateWord(RoomWords.from(newWord))
        }
    }

    override fun updateWordProgression(updateWord: Word, word: Word) {
        val newWord = Word(
            word.id,
            word.japanese,
            word.english,
            word.french,
            word.reading,
            updateWord.level,
            updateWord.countTry,
            updateWord.countSuccess,
            updateWord.countFail,
            word.isKana,
            updateWord.repetition,
            updateWord.points,
            word.baseCategory,
            updateWord.isSelected,
            word.sentenceId
        )
        wordDao.updateWord(RoomWords.from(newWord))
    }

    override fun updateWordSelected(wordId: Long, check: Boolean) {
        wordDao.updateWordSelected(wordId, check)
    }

    fun getQuizWordFromId(quizId: Long, wordId: Long): QuizWord? {
        return wordDao.getQuizWordFromId(quizId, wordId)?.toQuizWord()
    }

    fun addQuizWord(quizId: Long, wordId: Long) {
        wordDao.addQuizWord(RoomQuizWord(0, quizId, wordId))
    }

}
