package com.jehutyno.yomikata.repository.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.jehutyno.yomikata.model.QuizWord
import com.jehutyno.yomikata.model.Word
import com.jehutyno.yomikata.repository.WordRepository
import com.jehutyno.yomikata.repository.migration.WordTable
import com.jehutyno.yomikata.util.HiraganaUtils
import com.jehutyno.yomikata.util.QuizType
import org.jetbrains.anko.db.*
import java.util.*

/**
 * Created by jehutyno on 08/10/2016.
 */
class WordSource(var context: Context) : WordRepository {

    override fun migration_8to9() {
        val query0 = "ALTER TABLE ${SQLiteTables.WORDS.tableName} RENAME TO 'OLD_${SQLiteTables.WORDS.tableName}'"

        val query1 = "CREATE TABLE words (\n" +
            "    ${SQLiteWord.ID.column}           INTEGER      PRIMARY KEY AUTOINCREMENT,\n" +
            "    ${SQLiteWord.JAPANESE.column}      TEXT         NOT NULL,\n" +
            "    ${SQLiteWord.ENGLISH.column}       TEXT         NOT NULL,\n" +
            "    ${SQLiteWord.FRENCH.column}        TEXT         NOT NULL,\n" +
            "    ${SQLiteWord.READING.column}       TEXT         NOT NULL,\n" +
            "    ${SQLiteWord.LEVEL.column}         INTEGER,\n" +
            "    ${SQLiteWord.COUNT_TRY.column}     INTEGER,\n" +
            "    ${SQLiteWord.COUNT_SUCCESS.column} INTEGER,\n" +
            "    ${SQLiteWord.COUNT_FAIL.column}    INTEGER,\n" +
            "    ${SQLiteWord.IS_KANA.column}       BOOLEAN,\n" +
            "    ${SQLiteWord.REPETITION.column}    INTEGER (-1),\n" +
            "    ${SQLiteWord.POINTS.column}        INTEGER,\n" +
            "    ${SQLiteWord.BASE_CATEGORY.column} INTEGER,\n" +
            "    ${SQLiteWord.IS_SELECTED.column}    INTEGER      DEFAULT (0),\n" +
            "    ${SQLiteWord.SENTENCE_ID.column}   INTEGER      DEFAULT ( -1) \n" +
            ");"

        val query2 = "INSERT INTO ${SQLiteTables.WORDS.tableName} " +
            "(${SQLiteWord.ID.column}, ${SQLiteWord.JAPANESE.column}, ${SQLiteWord.ENGLISH.column}, ${SQLiteWord.FRENCH.column}," +
            "${SQLiteWord.READING.column}, ${SQLiteWord.LEVEL.column}, ${SQLiteWord.COUNT_TRY.column}, " +
            "${SQLiteWord.COUNT_SUCCESS.column}, ${SQLiteWord.COUNT_FAIL.column}, ${SQLiteWord.IS_KANA.column}, " +
            "${SQLiteWord.REPETITION.column}, ${SQLiteWord.POINTS.column}, ${SQLiteWord.BASE_CATEGORY.column}, " +
            "${SQLiteWord.IS_SELECTED.column}) " +
            "SELECT ${SQLiteWord.ID.column}, ${SQLiteWord.JAPANESE.column}, ${SQLiteWord.ENGLISH.column}, ${SQLiteWord.FRENCH.column}, " +
            "${SQLiteWord.READING.column}, ${SQLiteWord.LEVEL.column}, ${SQLiteWord.COUNT_TRY.column}, " +
            "${SQLiteWord.COUNT_SUCCESS.column}, ${SQLiteWord.COUNT_FAIL.column}, ${SQLiteWord.IS_KANA.column}, " +
            "${SQLiteWord.REPETITION.column}, ${SQLiteWord.POINTS.column}, ${SQLiteWord.BASE_CATEGORY.column}, " +
            "${SQLiteWord.IS_SELECTED.column} FROM OLD_${SQLiteTables.WORDS.tableName}"

        val query3 = "DROP TABLE OLD_${SQLiteTables.WORDS.tableName}"
            context.database.use {
                execSQL(query0)
                execSQL(query1)
                execSQL(query2)
                execSQL(query3)
            }
    }

    override fun getAllWords(db: SQLiteDatabase?): List<Word> {
        var words = listOf<Word>()
        if (db == null) {
            context.database.use {
                select(SQLiteTables.WORDS.tableName, *SQLiteTable.allColumns(SQLiteWord.values()))
                    .exec {
                        words = parseList(getWordsParser())
                    }
            }
        } else {
            db.select(SQLiteTables.WORDS.tableName, *SQLiteTable.allColumns(SQLiteWord.values()))
                .exec {
                    words = parseList(getWordsParser())
                }
        }
        return words
    }

    override fun getWords(quizId: Long, callback: WordRepository.LoadWordsCallback) {
        val query = "select ${SQLiteTables.WORDS.tableName}.${SQLiteTable.allColumns(SQLiteWord.values()).joinToString(",")} " +
            "from ${SQLiteTables.WORDS.tableName} join ${SQLiteTables.QUIZ_WORD.tableName} " +
            "ON ${SQLiteQuizWord.WORD_ID.column} = ${SQLiteTables.WORDS.tableName}.${SQLiteWord.ID.column} " +
            "and ${SQLiteQuizWord.QUIZ_ID.column} = $quizId"

        context.database.use {
            val cursor = rawQuery(query, null)
            if (cursor.count > 0)
                callback.onWordsLoaded(cursor.parseList(getWordsParser()))
            else
                callback.onDataNotAvailable()
            cursor.close()

        }

    }

    override fun getWords(quizIds: LongArray, callback: WordRepository.LoadWordsCallback) {
        val query = "select ${SQLiteTables.WORDS.tableName}.${SQLiteTable.allColumns(SQLiteWord.values()).joinToString(",")} " +
            "from ${SQLiteTables.WORDS.tableName} join ${SQLiteTables.QUIZ_WORD.tableName} " +
            "ON ${SQLiteQuizWord.WORD_ID.column} = ${SQLiteTables.WORDS.tableName}.${SQLiteWord.ID.column} " +
            "and ${SQLiteQuizWord.QUIZ_ID.column} in (${quizIds.joinToString(",")})"

        context.database.use {
            val cursor = rawQuery(query, null)
            if (cursor.count > 0)
                callback.onWordsLoaded(cursor.parseList(getWordsParser()))
            else
                callback.onDataNotAvailable()
            cursor.close()

        }
    }

    override fun getWordsByLevel(quizIds: LongArray, level: Int, callback: WordRepository.LoadWordsCallback) {
        val query = "select ${SQLiteTables.WORDS.tableName}.${SQLiteTable.allColumns(SQLiteWord.values()).joinToString(",")} " +
            "from ${SQLiteTables.WORDS.tableName} join ${SQLiteTables.QUIZ_WORD.tableName} " +
            "ON ${SQLiteQuizWord.WORD_ID.column} = ${SQLiteTables.WORDS.tableName}.${SQLiteWord.ID.column} " +
            "and ${SQLiteQuizWord.QUIZ_ID.column} in (${quizIds.joinToString(",")}) " +
            "and ${SQLiteTables.WORDS.tableName}.${SQLiteWord.LEVEL.column} in ($level" +
            if (level == 3)
                ",${level+1})"
            else
                ")"

        context.database.use {
            val cursor = rawQuery(query, null)
            if (cursor.count > 0)
                callback.onWordsLoaded(cursor.parseList(getWordsParser()))
            else
                callback.onDataNotAvailable()
            cursor.close()

        }
    }

    override fun getWordsByRepetition(quizIds: LongArray, repetition: Int, limit: Int): ArrayList<Word> {
        val words = arrayListOf<Word>()
        val query = "select ${SQLiteTables.WORDS.tableName}.${SQLiteTable.allColumns(SQLiteWord.values()).joinToString(",")} " +
            "from ${SQLiteTables.WORDS.tableName} join ${SQLiteTables.QUIZ_WORD.tableName} " +
            "ON ${SQLiteQuizWord.WORD_ID.column} = ${SQLiteTables.WORDS.tableName}.${SQLiteWord.ID.column} " +
            "and ${SQLiteQuizWord.QUIZ_ID.column} in (${quizIds.joinToString(",")}) " +
            "and ${SQLiteWord.REPETITION} = $repetition limit $limit"

        context.database.use {
            val cursor = rawQuery(query, null)
            words.addAll(cursor.parseList(getWordsParser()))
            cursor.close()
        }

        return words
    }

    override fun getRandomWords(wordId: Long, answer: String, wordSize: Int, limit: Int, quizType: QuizType): ArrayList<Word> {
        val words = arrayListOf<Word>()
        val column = when(quizType) {
            QuizType.TYPE_PRONUNCIATION -> "${SQLiteTables.WORDS.tableName}.${SQLiteWord.READING.column}"
            QuizType.TYPE_PRONUNCIATION_QCM -> "${SQLiteTables.WORDS.tableName}.${SQLiteWord.READING.column}"
            QuizType.TYPE_AUDIO -> "${SQLiteTables.WORDS.tableName}.${SQLiteWord.JAPANESE.column}"
            QuizType.TYPE_EN_JAP -> "${SQLiteTables.WORDS.tableName}.${SQLiteWord.JAPANESE.column}"
            QuizType.TYPE_JAP_EN -> "${SQLiteTables.WORDS.tableName}.${SQLiteWord.JAPANESE.column}"
            else -> "${SQLiteTables.WORDS.tableName}.${SQLiteWord.JAPANESE.column}"
        }

        var query = "select ${SQLiteTables.WORDS.tableName}.${SQLiteTable.allColumns(SQLiteWord.values()).joinToString(",")} " +
            "from ${SQLiteTables.WORDS.tableName} join ${SQLiteTables.QUIZ_WORD.tableName} " +
            "ON ${SQLiteQuizWord.WORD_ID.column} = ${SQLiteTables.WORDS.tableName}.${SQLiteWord.ID.column} " +
            "and ${SQLiteQuizWord.QUIZ_ID.column} = " +
            "(select ${SQLiteQuizWord.QUIZ_ID.column} from ${SQLiteTables.QUIZ_WORD.tableName} " +
            "where ${SQLiteQuizWord.WORD_ID.column} = $wordId and ${SQLiteQuizWord.QUIZ_ID.column} < 97) " +
            "and length(${SQLiteWord.JAPANESE.column}) = $wordSize " +
            "and $column != '$answer'" +
            "and ${SQLiteTables.WORDS.tableName}.${SQLiteWord.ID.column} != $wordId group by $column order by RANDOM() limit $limit"

        context.database.use {
            val cursor = rawQuery(query, null)
            words.addAll(cursor.parseList(getWordsParser()))
            cursor.close()

        }

        if (words.size < limit) {
            query = "select ${SQLiteTables.WORDS.tableName}.${SQLiteTable.allColumns(SQLiteWord.values()).joinToString(",")} " +
                "from ${SQLiteTables.WORDS.tableName} join ${SQLiteTables.QUIZ_WORD.tableName} " +
                "ON ${SQLiteQuizWord.WORD_ID.column} = ${SQLiteTables.WORDS.tableName}.${SQLiteWord.ID.column} " +
                "and ${SQLiteQuizWord.QUIZ_ID.column} = " +
                "(select ${SQLiteQuizWord.QUIZ_ID.column}  from ${SQLiteTables.QUIZ_WORD.tableName} " +
                "where ${SQLiteQuizWord.WORD_ID.column} = $wordId and ${SQLiteQuizWord.QUIZ_ID.column} < 97) " +
                "and $column != '$answer'" +
                "and ${SQLiteTables.WORDS.tableName}.${SQLiteWord.ID.column} != $wordId group by $column order by RANDOM() limit $limit - ${words.size}"

            context.database.use {
                val cursor = rawQuery(query, null)
                words.addAll(cursor.parseList(getWordsParser()))
                cursor.close()

            }

        }

        return words
    }

    override fun searchWords(searchString: String, callback: WordRepository.LoadWordsCallback) {
        val hiragana = HiraganaUtils.toHiragana(searchString)
        context.database.use {
            select(SQLiteTables.WORDS.tableName, *SQLiteTable.allColumns(SQLiteWord.values()))
                .whereArgs("${SQLiteWord.READING} LIKE '%$searchString%'" +
                        " or ${SQLiteWord.READING} LIKE '%$hiragana%'" +
                        " or ${SQLiteWord.JAPANESE} LIKE '%$searchString%'" +
                        " or ${SQLiteWord.JAPANESE} LIKE '%$hiragana%'" +
                        " or ${SQLiteWord.ENGLISH} LIKE '%$searchString%'" +
                        " or ${SQLiteWord.FRENCH} LIKE '%$searchString%'" /*+
                        " or ${SQLiteWord.SENTENCE_JAP} LIKE '%$searchString%'" +
                        " or ${SQLiteWord.SENTENCE_JAP} LIKE '%$hiragana%'" +
                        " or ${SQLiteWord.SENTENCE_EN} LIKE '%$searchString%'" +
                        " or ${SQLiteWord.SENTENCE_EN} LIKE '%$hiragana%'" +
                        " or ${SQLiteWord.SENTENCE_FR} LIKE '%$searchString%'" +
                        " or ${SQLiteWord.SENTENCE_FR} LIKE '%$hiragana%'"*/
                )
                .exec {
                    if (count > 0)
                        callback.onWordsLoaded(parseList(getWordsParser()))
                    else
                        callback.onDataNotAvailable()
                }
        }
    }

    override fun isWordInQuiz(wordId: Long, quizId: Long): Boolean {
        var ret = false
        context.database.use {
            select(SQLiteTables.QUIZ_WORD.tableName).whereArgs(
                    "${SQLiteQuizWord.WORD_ID.column} = $wordId AND " +
                            "${SQLiteQuizWord.QUIZ_ID.column} = $quizId").exec {
                ret = count > 0
            }
        }
        return ret
    }

    override fun isWordInQuizzes(wordId: Long, quizIds: Array<Long>): ArrayList<Boolean> {
        val ret: ArrayList<Boolean> = arrayListOf()
        context.database.use {
            for (quizId in quizIds) {
                select(SQLiteTables.QUIZ_WORD.tableName).whereArgs(
                        "${SQLiteQuizWord.WORD_ID.column} = $wordId AND " +
                                "${SQLiteQuizWord.QUIZ_ID.column} = $quizId").exec {
                    ret.add(count > 0)
                }
            }
        }
        return ret
    }


    override fun getWordById(wordId: Long): Word {
        var word: Word? = null
        context.database.use {
            select(SQLiteTables.WORDS.tableName).whereArgs(
                    "${SQLiteWord.ID.column} = $wordId").exec {
                word = parseSingle(getWordsParser())
            }
        }
        return word!!
    }

    override fun saveWord(task: Word) {

    }

    override fun refreshWords() {

    }

    override fun deleteAllWords() {

    }

    override fun deleteWord(wordId: String) {

    }

    override fun updateWordPoints(wordId: Long, points: Int) {
        context.database.use {
            val values = ContentValues()
            values.put(SQLiteWord.POINTS.column, points)
            update(SQLiteTables.WORDS.tableName, values, "${SQLiteWord.ID.column} = ?", arrayOf(wordId.toString()))
        }
    }

    override fun updateWordLevel(wordId: Long, level: Int) {
        context.database.use {
            val values = ContentValues()
            values.put(SQLiteWord.LEVEL.column, level)
            values.put(SQLiteWord.POINTS.column, 0)
            update(SQLiteTables.WORDS.tableName, values, "${SQLiteWord.ID.column} = ?", arrayOf(wordId.toString()))
        }
    }

    override fun updateWordRepetition(wordId: Long, repetition: Int) {
        context.database.use {
            update(SQLiteTables.WORDS.tableName, SQLiteWord.REPETITION.column to repetition)
                .whereArgs("${SQLiteWord.ID.column} = {wordId}", "wordId" to wordId)
                .exec()
        }
    }

    override fun decreaseWordsRepetition(quizIds: LongArray) {
        val query = "select ${SQLiteTables.WORDS.tableName}.${SQLiteWord.ID.column},${SQLiteTables.WORDS.tableName}.${SQLiteWord.REPETITION.column} " +
            "from ${SQLiteTables.WORDS.tableName} join ${SQLiteTables.QUIZ_WORD.tableName} " +
            "ON ${SQLiteQuizWord.WORD_ID.column} = ${SQLiteTables.WORDS.tableName}.${SQLiteWord.ID.column} " +
            "and ${SQLiteQuizWord.QUIZ_ID.column} in (${quizIds.joinToString(",")}) and ${SQLiteWord.REPETITION.column} > 0"

        context.database.use {
            val cursor = rawQuery(query, null)
            while (cursor.moveToNext()) {
                update(SQLiteTables.WORDS.tableName, SQLiteWord.REPETITION.column to cursor.getInt(cursor.getColumnIndex(SQLiteWord.REPETITION.column)) - 1)
                    .where("${SQLiteWord.ID.column} = {id}", "id" to cursor.getLong(cursor.getColumnIndex(SQLiteWord.ID.column)))
                    .exec()
            }
            cursor.close()
        }
    }

    override fun restoreWord(word: String, prononciation: String, wordTable: WordTable) {
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

        context.database.use {
            update(SQLiteTables.WORDS.tableName,
                SQLiteWord.LEVEL.column to priority,
                SQLiteWord.POINTS.column to points,
                SQLiteWord.COUNT_FAIL.column to wordTable.counterFail,
                SQLiteWord.COUNT_TRY.column to wordTable.counterTry,
                SQLiteWord.COUNT_SUCCESS.column to wordTable.counterSuccess).whereArgs(
                "${SQLiteWord.JAPANESE.column} = '$word' AND " +
                    "${SQLiteWord.READING.column} LIKE '%$prononciation%'").exec()
        }
    }

    override fun updateWord(updateWord: Word, word: Word?) {
        context.database.use {
            if (word != null && updateWord.japanese != word.japanese) {
                update(SQLiteTables.WORDS.tableName,
                    SQLiteWord.JAPANESE.column to updateWord.japanese).whereArgs(
                    "${SQLiteWord.ID.column} = '${updateWord.id}'").exec()
            }
            if (word != null && updateWord.english != word.english) {
                update(SQLiteTables.WORDS.tableName,
                    SQLiteWord.ENGLISH.column to updateWord.english).whereArgs(
                    "${SQLiteWord.ID.column} = '${updateWord.id}'").exec()
            }
            if (word != null && updateWord.french != word.french) {
                update(SQLiteTables.WORDS.tableName,
                    SQLiteWord.FRENCH.column to updateWord.french).whereArgs(
                    "${SQLiteWord.ID.column} = '${updateWord.id}'").exec()
            }
            if (word != null && updateWord.reading != word.reading) {
                update(SQLiteTables.WORDS.tableName,
                    SQLiteWord.READING.column to updateWord.reading).whereArgs(
                    "${SQLiteWord.ID.column} = '${updateWord.id}'").exec()
            }
            if (word != null && updateWord.sentenceId != word.sentenceId) {
                update(SQLiteTables.WORDS.tableName,
                    SQLiteWord.SENTENCE_ID.column to updateWord.sentenceId).whereArgs(
                    "${SQLiteWord.ID.column} = '${updateWord.id}'").exec()
            }

            if (word == null) {
                insert(SQLiteTables.WORDS.tableName,
                    SQLiteWord.JAPANESE.column to updateWord.japanese,
                    SQLiteWord.ENGLISH.column to updateWord.english,
                    SQLiteWord.FRENCH.column to updateWord.french,
                    SQLiteWord.READING.column to updateWord.reading,
                    SQLiteWord.LEVEL.column to 0,
                    SQLiteWord.COUNT_TRY.column to 0,
                    SQLiteWord.COUNT_SUCCESS.column to 0,
                    SQLiteWord.COUNT_FAIL.column to 0,
                    SQLiteWord.IS_KANA.column to 0,
                    SQLiteWord.REPETITION.column to 0,
                    SQLiteWord.POINTS.column to 0,
                    SQLiteWord.IS_SELECTED.column to 0,
                    SQLiteWord.IS_SELECTED.column to 0)
            }
        }
    }

    override fun updateWordProgression(updateWord: Word, word: Word?) {
        context.database.use {
            if (word != null && updateWord.countFail > 0) {
                update(SQLiteTables.WORDS.tableName,
                    SQLiteWord.COUNT_FAIL.column to updateWord.countFail).whereArgs(
                  "${SQLiteWord.ID.column} = '${updateWord.id}'").exec()
            }
            if (word != null && updateWord.countSuccess > 0) {
                update(SQLiteTables.WORDS.tableName,
                    SQLiteWord.COUNT_SUCCESS.column to updateWord.countSuccess).whereArgs(
                    "${SQLiteWord.ID.column} = '${updateWord.id}'").exec()
            }
            if (word != null && updateWord.countTry > 0) {
                update(SQLiteTables.WORDS.tableName,
                    SQLiteWord.COUNT_TRY.column to updateWord.countTry).whereArgs(
                    "${SQLiteWord.ID.column} = '${updateWord.id}'").exec()
            }
            if (word != null && updateWord.isSelected > 0) {
                update(SQLiteTables.WORDS.tableName,
                    SQLiteWord.IS_SELECTED.column to updateWord.isSelected).whereArgs(
                    "${SQLiteWord.ID.column} = '${updateWord.id}'").exec()
            }
            if (word != null && updateWord.level > 0) {
                update(SQLiteTables.WORDS.tableName,
                    SQLiteWord.LEVEL.column to updateWord.level).whereArgs(
                    "${SQLiteWord.ID.column} = '${updateWord.id}'").exec()
            }
            if (word != null && updateWord.points > 0) {
                update(SQLiteTables.WORDS.tableName,
                    SQLiteWord.POINTS.column to updateWord.points).whereArgs(
                    "${SQLiteWord.ID.column} = '${updateWord.id}'").exec()
            }
            if (word != null && updateWord.repetition > 0) {
                update(SQLiteTables.WORDS.tableName,
                    SQLiteWord.REPETITION.column to updateWord.repetition).whereArgs(
                    "${SQLiteWord.ID.column} = '${updateWord.id}'").exec()
            }

//            update(SQLiteTables.WORDS.tableName,
//                SQLiteWord.LEVEL.column to updateWord.level,
//                SQLiteWord.POINTS.column to updateWord.points,
//                SQLiteWord.COUNT_FAIL.column to updateWord.countFail,
//                SQLiteWord.COUNT_TRY.column to updateWord.countTry,
//                SQLiteWord.COUNT_SUCCESS.column to updateWord.countSuccess).where(
//                "${SQLiteWord.ID.column} = '${updateWord.id}'").exec()

        }
    }

    override fun updateWordSelected(id: Long, check: Boolean) {
//        context.database.use {
//            val values = ContentValues()
//            values.put(SQLiteWord.IS_SELECTED.column, if (isSelected) 1 else 0)
//            update(SQLiteTables.WORDS.tableName, values, "${SQLiteWord.ID.column} = ?", arrayOf(wordId.toString()))
//        }
    }

    fun getQuizWordFromIds(quizId: Long, wordId: Long): QuizWord? {
        var quizWord: QuizWord? = null
        context.database.use {
            select(SQLiteTables.QUIZ_WORD.tableName)
                .whereArgs("${SQLiteQuizWord.QUIZ_ID.column} = $quizId and ${SQLiteQuizWord.WORD_ID.column} = $wordId")
                .exec {
                    if (count > 0)
                        quizWord = parseSingle(rowParser(::QuizWord))
                }
        }

        return quizWord
    }

    fun addQuizWord(quizId: Long, wordId: Long) {
        context.database.use {
            val values = ContentValues()
            values.put(SQLiteQuizWord.QUIZ_ID.column, quizId)
            values.put(SQLiteQuizWord.WORD_ID.column, wordId)
            insert(SQLiteTables.QUIZ_WORD.tableName, null, values)
        }
    }

}

fun getWordsParser(): RowParser<Word> {
    val rowParser = rowParser { id: Long, japanese: String, english: String, french: String, reading: String, level: Int, countTry: Int, countSuccess: Int, countFail: Int, isKana: Int, repetition: Int, points: Int, baseCategory: Int, isSelected: Int, sentenceId: Long ->
        Word(id, japanese, english, french, reading, level, countTry, countSuccess, countFail, isKana, repetition, points, baseCategory, isSelected, sentenceId)
    }

    return rowParser
}
