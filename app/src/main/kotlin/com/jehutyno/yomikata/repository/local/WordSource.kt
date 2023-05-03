package com.jehutyno.yomikata.repository.local

import androidx.sqlite.db.SimpleSQLiteQuery
import com.jehutyno.yomikata.dao.WordDao
import com.jehutyno.yomikata.model.QuizWord
import com.jehutyno.yomikata.model.Word
import com.jehutyno.yomikata.repository.WordRepository
import com.jehutyno.yomikata.util.HiraganaUtils
import com.jehutyno.yomikata.util.QuizType


/**
 * Created by jehutyno on 08/10/2016.
 */
class WordSource(private val wordDao: WordDao) : WordRepository {

    override fun getAllWords(): List<Word> {
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
        val wordsTableName = "words" // CHANGE THIS IF WORDS TABLE NAME IS CHANGED
        val column = when (quizType) {
            QuizType.TYPE_PRONUNCIATION -> "${wordsTableName}.reading"
            QuizType.TYPE_PRONUNCIATION_QCM -> "${wordsTableName}.reading"
            QuizType.TYPE_AUDIO -> "${wordsTableName}.japanese"
            QuizType.TYPE_EN_JAP -> "${wordsTableName}.japanese"
            QuizType.TYPE_JAP_EN -> "${wordsTableName}.japanese"
            else -> "${wordsTableName}.japanese"
        }

        val wordIds = mutableListOf<Long>()
        var tryWordSize = wordSize
        while (wordIds.size <= 1 && tryWordSize > 0) {
            wordIds += wordDao.getWordsOfSizeRelatedTo(wordId, tryWordSize)
            tryWordSize--   // get smaller sizes in case there are no other words of the same size
        }

        // use @RawQuery since column names cannot be inserted in @Query by Room
        val rawQuery = "SELECT * FROM words " +
                       "WHERE _id IN (${wordIds.joinToString(",")}) " +
                       "AND $column != (?) " +
                       "GROUP BY $column ORDER BY RANDOM() LIMIT ?"
        var supportSQLiteQuery = SimpleSQLiteQuery(rawQuery, arrayOf<Any>(answer, limit))

        val roomWordsList = wordDao.getRandomWords(supportSQLiteQuery).toMutableList()

        // try two more times to make sure size is at least 3
        for (i in 1..2) {
            if (roomWordsList.size < limit) {
                supportSQLiteQuery = SimpleSQLiteQuery(rawQuery, arrayOf<Any>(answer, limit - roomWordsList.size))
                val extraRoomWordsList = wordDao.getRandomWords(supportSQLiteQuery)
                roomWordsList += extraRoomWordsList
            }
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
        val idList = wordDao.getWordIdsWithRepetitionStrictlyGreaterThan(quizIds, 0)
        wordDao.decreaseWordRepetitionByOne(idList)
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
        wordDao.addQuizWord(RoomQuizWord(quizId, wordId))
    }

}
