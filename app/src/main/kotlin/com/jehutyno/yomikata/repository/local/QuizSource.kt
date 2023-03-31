package com.jehutyno.yomikata.repository.local

import android.content.ContentValues
import android.content.Context
import com.jehutyno.yomikata.model.Quiz
import com.jehutyno.yomikata.repository.QuizRepository
import org.jetbrains.anko.db.parseList
import org.jetbrains.anko.db.parseSingle
import org.jetbrains.anko.db.rowParser
import org.jetbrains.anko.db.select

/**
 * Created by valentin on 07/10/2016.
 */
class QuizSource(var context: Context) : QuizRepository {

    override fun getQuiz(category: Int, callback: QuizRepository.LoadQuizCallback) {
        context.database.use {
            select(SQLiteTables.QUIZ.tableName, *SQLiteTable.allColumns(SQLiteQuiz.values()))
                .whereArgs("${SQLiteQuiz.CATEGORY.column} = $category")
                .exec {
                    val rowParser = rowParser(::Quiz)
                    if (count > 0)
                        callback.onQuizLoaded(parseList(rowParser))
                    else
                        callback.onDataNotAvailable()
                }
        }
    }

    override fun getQuiz(quizId: Long, callback: QuizRepository.GetQuizCallback) {
        context.database.use {
            select(SQLiteTables.QUIZ.tableName, *SQLiteTable.allColumns(SQLiteQuiz.values()))
                .whereArgs("${SQLiteQuiz.ID.column} = $quizId").limit(1)
                .exec {
                    val rowParser = rowParser(::Quiz)
                    if (count > 0)
                        callback.onQuizLoaded(parseSingle(rowParser))
                    else
                        callback.onDataNotAvailable()
                }
        }
    }

    fun getQuiz(quizId: Long): Quiz? {
        var quiz : Quiz? = null
        context.database.use {
            select(SQLiteTables.QUIZ.tableName, *SQLiteTable.allColumns(SQLiteQuiz.values()))
                .whereArgs("${SQLiteQuiz.ID.column} = $quizId").limit(1)
                .exec {
                    val rowParser = rowParser(::Quiz)
                    if (count > 0)
                        quiz = parseSingle(rowParser)
                }
        }
        return quiz
    }

    override fun saveQuiz(quizName: String, category: Int): Long {
        var ret = 0L
        context.database.use {
            val values = ContentValues()
            values.put(SQLiteQuiz.NAME_EN.column, quizName)
            values.put(SQLiteQuiz.NAME_FR.column, quizName)
            values.put(SQLiteQuiz.CATEGORY.column, category)
            values.put(SQLiteQuiz.IS_SELECTED.column, 0)
            ret = insert(SQLiteTables.QUIZ.tableName, null, values)
        }
        return ret
    }

    override fun refreshQuiz() {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun deleteAllQuiz() {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun deleteQuiz(quizId: Long) {
        context.database.use {
            delete(SQLiteTables.QUIZ.tableName, "${SQLiteQuiz.ID.column} = ?", arrayOf(quizId.toString()))
        }
    }

    override fun updateQuizName(quizId: Long, quizName: String) {
        context.database.use {
            val values = ContentValues()
            values.put(SQLiteQuiz.NAME_EN.column, quizName)
            values.put(SQLiteQuiz.NAME_FR.column, quizName)
            update(SQLiteTables.QUIZ.tableName, values, "${SQLiteQuiz.ID.column} = ?", arrayOf(quizId.toString()))
        }
    }

    override fun updateQuizSelected(quizId: Long, isSelected: Boolean) {
        context.database.use {
            val values = ContentValues()
            values.put(SQLiteQuiz.IS_SELECTED.column, if (isSelected) 1 else 0)
            update(SQLiteTables.QUIZ.tableName, values, "${SQLiteQuiz.ID.column} = ?", arrayOf(quizId.toString()))
        }
    }

    override fun addWordToQuiz(wordId: Long, quizId: Long) {
        context.database.use {
            val values = ContentValues()
            values.put(SQLiteQuizWord.QUIZ_ID.column, quizId)
            values.put(SQLiteQuizWord.WORD_ID.column, wordId)
            insert(SQLiteTables.QUIZ_WORD.tableName, null, values)
        }
    }

    override fun deleteWordFromQuiz(wordId: Long, selectionId: Long) {
        context.database.use {
            delete(SQLiteTables.QUIZ_WORD.tableName, "${SQLiteQuizWord.QUIZ_ID.column} = ? AND ${SQLiteQuizWord.WORD_ID.column} = ?", arrayOf(selectionId.toString(), wordId.toString()))
        }
    }

    override fun countWordsForLevel(quizIds: LongArray, level: Int): Int {
        var count = 0
        val query = "select ${SQLiteTables.WORDS.tableName}.${SQLiteTable.allColumns(SQLiteWord.values()).joinToString(",")} " +
            "from ${SQLiteTables.WORDS.tableName} join ${SQLiteTables.QUIZ_WORD.tableName} " +
            "ON ${SQLiteQuizWord.WORD_ID.column} = ${SQLiteTables.WORDS.tableName}.${SQLiteWord.ID.column} " +
            "and ${SQLiteQuizWord.QUIZ_ID.column} in (${quizIds.joinToString(",")}) " +
            "and ${SQLiteWord.LEVEL.column} = $level"

        context.database.use {
            val cursor = rawQuery(query, null)
            count = cursor.count
            cursor.close()
        }

        return count
    }

    override fun countWordsForQuizzes(quizIds: LongArray): Int {
        var count = 0
        val query = "select ${SQLiteTables.WORDS.tableName}.${SQLiteTable.allColumns(SQLiteWord.values()).joinToString(",")} " +
            "from ${SQLiteTables.WORDS.tableName} join ${SQLiteTables.QUIZ_WORD.tableName} " +
            "ON ${SQLiteQuizWord.WORD_ID.column} = ${SQLiteTables.WORDS.tableName}.${SQLiteWord.ID.column} " +
            "and ${SQLiteQuizWord.QUIZ_ID.column} in (${quizIds.joinToString(",")})"

        context.database.use {
            val cursor = rawQuery(query, null)
            count = cursor.count
            cursor.close()
        }

        return count
    }
}