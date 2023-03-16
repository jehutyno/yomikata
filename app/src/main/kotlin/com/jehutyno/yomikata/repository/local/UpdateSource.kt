package com.jehutyno.yomikata.repository.local

import android.content.Context
import android.database.sqlite.SQLiteException
import com.jehutyno.yomikata.model.*
import org.jetbrains.anko.db.parseList
import org.jetbrains.anko.db.rowParser
import org.jetbrains.anko.db.select

/**
 * Created by jehutyno on 08/10/2016.
 */
class UpdateSource(private var context: Context, private val filePath: String) {

    fun getAllWords(): List<Word> {
        var words = listOf<Word>()
        UpdateSQLiteHelper.getInstance(context, filePath).use {
            select(SQLiteTables.WORDS.tableName, *SQLiteTable.allColumns(SQLiteWord.values()))
                    .exec {
                        words = parseList(getWordsParser())
                    }
        }
        return words
    }

    fun getAllStatEntries(): List<StatEntry> {
        var stats = listOf<StatEntry>()
        UpdateSQLiteHelper.getInstance(context, filePath).use {
            select(SQLiteTables.STAT_ENTRY.tableName, *SQLiteTable.allColumns(SQLiteStatEntry.values()))
                    .exec {
                        stats = parseList(rowParser(::StatEntry))
                    }
        }
        return stats
    }

    fun getAddedQuizWords(): List<QuizWord> {
        var quizWord = listOf<QuizWord>()
        UpdateSQLiteHelper.getInstance(context, filePath).use {
            select(SQLiteTables.QUIZ_WORD.tableName, *SQLiteTable.allColumns(SQLiteQuizWord.values()))
                    .where("${SQLiteQuizWord.ID.column} > 7504")
                    .exec {
                        quizWord = parseList(rowParser(::QuizWord))
                    }
        }
        return quizWord
    }

    fun getAddedQuizzes(): List<Quiz> {
        var quizzes = listOf<Quiz>()
        UpdateSQLiteHelper.getInstance(context, filePath).use {
            select(SQLiteTables.QUIZ.tableName, *SQLiteTable.allColumns(SQLiteQuiz.values()))
                    .where("${SQLiteQuiz.ID.column} > 96")
                    .exec {
                        quizzes = parseList(rowParser(::Quiz))
                    }
        }
        return quizzes
    }

    fun getAllKanjiSolo(): List<KanjiSolo> {
        val kanjiSolos = mutableListOf<KanjiSolo>()
        try {
            UpdateSQLiteHelper.getInstance(context, filePath).use {
                select(SQLiteTables.KANJI_SOLO.tableName, *SQLiteTable.allColumns(SQLiteKanjiSolo.values()))
                        .exec {
                            while (moveToNext()) {
                                kanjiSolos.add(KanjiSolo(
                                        getLong(0),
                                        getString(1),
                                        getInt(2),
                                        getString(3),
                                        getString(4),
                                        getString(5),
                                        getString(6),
                                        getString(7)))
                            }
                        }
            }
        } catch (e: SQLiteException) {
            e.printStackTrace()
        }

        return kanjiSolos
    }

    fun getAllRadicals(): List<Radical> {
        val radicals = mutableListOf<Radical>()
        try {
            UpdateSQLiteHelper.getInstance(context, filePath).use {
                select(SQLiteTables.RADICALS.tableName, *SQLiteTable.allColumns(SQLiteRadicals.values()))
                        .exec {
                            while (moveToNext()) {
                                radicals.add(Radical(
                                        getLong(0),
                                        getInt(1),
                                        getString(2),
                                        getString(3),
                                        getString(4),
                                        getString(5)))
                            }
                        }
            }
        } catch (e: SQLiteException) {
            e.printStackTrace()
        }
        return radicals
    }

    fun getAllSentences(): List<Sentence> {
        var sentences = listOf<Sentence>()
        UpdateSQLiteHelper.getInstance(context, filePath).use {
            select(SQLiteTables.SENTENCES.tableName, *SQLiteTable.allColumns(SQLiteSentences.values()))
                .exec {
                    sentences = parseList(getSentencesParser())
                }
        }
        return sentences
    }

}