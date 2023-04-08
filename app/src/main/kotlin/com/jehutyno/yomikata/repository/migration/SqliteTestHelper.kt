package com.jehutyno.yomikata.repository.migration

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.sqlite.db.SupportSQLiteDatabase


/**
 * Sqlite test helper
 * Used to test migration from version 12 to version 13. Room schemas started being used from
 * version 13. See MigrationTest.kt
 */
class SqliteTestHelper(context: Context?, databaseName: String?) :
      SQLiteOpenHelper(context, databaseName, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        createAllTables(db)
    }

    private fun createAllTables(db: SQLiteDatabase) {
        db.execSQL(
            """
                CREATE TABLE IF NOT EXISTS words (
                  _id integer PRIMARY KEY AUTOINCREMENT,
                  japanese text NOT NULL,
                  english text NOT NULL,
                  french text NOT NULL,
                  reading text NOT NULL,
                  level integer,
                  count_try integer,
                  count_success integer,
                  count_fail integer,
                  is_kana BOOLEAN,
                  repetition integer (-1),
                  points integer,
                  base_category INTEGER,
                  isSelected INTEGER DEFAULT (0),
                  sentence_id INTEGER DEFAULT (-1)
                )
            """.trimIndent()
        )

        db.execSQL(
            """
                CREATE TABLE IF NOT EXISTS quiz (
                _id INTEGER PRIMARY KEY AUTOINCREMENT,
                name_en TEXT NOT NULL,
                name_fr TEXT NOT NULL,
                category INTEGER,
                isSelected INTEGER
                )
            """.trimIndent()
        )

        db.execSQL(
            """
                CREATE TABLE IF NOT EXISTS quiz_word (
                    _id INTEGER PRIMARY KEY AUTOINCREMENT,
                    quiz_id INTEGER,
                    word_id INTEGER
                )
            """.trimIndent()
        )

        db.execSQL(
            """
                CREATE TABLE IF NOT EXISTS stat_entry (
                    _id INTEGER PRIMARY KEY,
                    "action" INTEGER,
                    associatedId INTEGER,
                    date INTEGER,
                    result INTEGER
                )
            """.trimIndent()
        )

        db.execSQL(
            """
                CREATE TABLE IF NOT EXISTS kanji_solo (
                    _id INTEGER,
                    kanji TEXT,
                    strokes INTEGER,
                    en TEXT,
                    fr TEXT,
                    kunyomi TEXT,
                    onyomi TEXT,
                    radical TEXT,
                    PRIMARY KEY(_id)
                )
            """.trimIndent()
        )

        db.execSQL(
            """
                CREATE TABLE IF NOT EXISTS radicals (
                    _id INTEGER,
                    strokes INTEGER,
                    radical TEXT,
                    reading TEXT,
                    en TEXT,
                    fr TEXT,
                    PRIMARY KEY(_id)
                )
            """.trimIndent()
        )

        db.execSQL(
            """
                CREATE TABLE IF NOT EXISTS sentences (
                    _id INTEGER PRIMARY KEY,
                    jap TEXT,
                    en TEXT,
                    fr TEXT,
                    level INTEGER
                )
            """.trimIndent()
        )
    }

    private fun clearDatabase(db: SQLiteDatabase) {
        val dropTableQueries = listOf(
            "DROP TABLE IF EXISTS NEW_words",
            "DROP TABLE IF EXISTS quiz",
            "DROP TABLE IF EXISTS quiz_word",
            "DROP TABLE IF EXISTS stat_entry",
            "DROP TABLE IF EXISTS kanji_solo",
            "DROP TABLE IF EXISTS radicals",
            "DROP TABLE IF EXISTS sentences"
        )
        dropTableQueries.forEach { query -> db.execSQL(query) }
    }

    private fun insertWord(db: SQLiteDatabase, wordv12: Wordv12) {
        db.execSQL("""
            INSERT INTO words (
                _id, japanese, english, french, reading, level, count_try, count_success, count_fail,
                is_kana, repetition, points, base_category, isSelected, sentence_id
            )
            VALUES (
                ${wordv12.id}, "${wordv12.japanese}", "${wordv12.english}",
                "${wordv12.french}", "${wordv12.reading}", ${wordv12.level}, ${wordv12.countTry},
                ${wordv12.countSuccess}, ${wordv12.countFail}, ${wordv12.isKana}, ${wordv12.repetition},
                ${wordv12.points}, ${wordv12.baseCategory}, ${wordv12.isSelected}, ${wordv12.sentenceId}
            )
        """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Not needed for test which goes from 12 to new version 13 using Room
    }

    override fun onDowngrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        // call this and do nothing to set up tests correctly
    }

    companion object {
        const val DATABASE_VERSION = 12

        fun createAllTables(mSqliteTestHelper: SqliteTestHelper) {
            val closeMe = mSqliteTestHelper.writableDatabase
            mSqliteTestHelper.createAllTables(closeMe)
            closeMe.close()
        }

        fun clearDatabase(mSqliteTestHelper: SqliteTestHelper) {
            val closeMe = mSqliteTestHelper.writableDatabase
            mSqliteTestHelper.clearDatabase(closeMe)
            closeMe.close()
        }

        fun insertWord(mSqliteTestHelper: SqliteTestHelper, wordv12: Wordv12) {
            val closeMe = mSqliteTestHelper.writableDatabase
            mSqliteTestHelper.insertWord(closeMe, wordv12)
            closeMe.close()
        }

    }
}

// Version of database entries from version 12 used for migration and testing
// --------- DO NOT CHANGE ---------
abstract class BaseGetAllItems<T>(private val constructor: (Cursor) -> T) {
    protected lateinit var tableName: String

    fun getAllItems(database: SQLiteDatabase): List<T> {
        val items = mutableListOf<T>()
        database.rawQuery("""SELECT * FROM $tableName""", arrayOf<String>()).apply {
            while (this.moveToNext()) {
                items.add(constructor.invoke(this))
            }
        }.close()
        return items
    }

    fun getAllItems(database: SupportSQLiteDatabase): List<T> {
        val items = mutableListOf<T>()
        database.query("""SELECT * FROM $tableName""").apply {
            while (this.moveToNext()) {
                items.add(constructor.invoke(this))
            }
        }.close()
        return items
    }

    fun deleteAll(database: SupportSQLiteDatabase) {
        database.execSQL("""DELETE FROM $tableName""")
    }
}

data class Wordv12(var id: Long, var japanese: String, var english: String, var french: String,
              var reading: String, var level: Int, var countTry: Int, var countSuccess: Int,
              var countFail: Int, var isKana: Int, var repetition: Int, var points: Int,
              var baseCategory: Int, var isSelected: Int, var sentenceId: Long) {

    constructor(cursor: Cursor) : this (
        cursor.getLong(cursor.getColumnIndexOrThrow(SQLiteWord.ID.column_name)),
        cursor.getString(cursor.getColumnIndexOrThrow(SQLiteWord.JAPANESE.column_name)),
        cursor.getString(cursor.getColumnIndexOrThrow(SQLiteWord.ENGLISH.column_name)),
        cursor.getString(cursor.getColumnIndexOrThrow(SQLiteWord.FRENCH.column_name)),
        cursor.getString(cursor.getColumnIndexOrThrow(SQLiteWord.READING.column_name)),
        cursor.getInt(cursor.getColumnIndexOrThrow(SQLiteWord.LEVEL.column_name)),
        cursor.getInt(cursor.getColumnIndexOrThrow(SQLiteWord.COUNT_TRY.column_name)),
        cursor.getInt(cursor.getColumnIndexOrThrow(SQLiteWord.COUNT_SUCCESS.column_name)),
        cursor.getInt(cursor.getColumnIndexOrThrow(SQLiteWord.COUNT_FAIL.column_name)),
        cursor.getInt(cursor.getColumnIndexOrThrow(SQLiteWord.IS_KANA.column_name)),
        cursor.getInt(cursor.getColumnIndexOrThrow(SQLiteWord.REPETITION.column_name)),
        cursor.getInt(cursor.getColumnIndexOrThrow(SQLiteWord.POINTS.column_name)),
        cursor.getInt(cursor.getColumnIndexOrThrow(SQLiteWord.BASE_CATEGORY.column_name)),
        cursor.getInt(cursor.getColumnIndexOrThrow(SQLiteWord.IS_SELECTED.column_name)),
        cursor.getLong(cursor.getColumnIndexOrThrow(SQLiteWord.SENTENCE_ID.column_name)),
    )

    companion object : BaseGetAllItems<Wordv12>(::Wordv12) {
        init {
            tableName = "words"
        }

        fun insertWord(database: SupportSQLiteDatabase, newWord: Wordv12, preserve_id: Boolean = false) {
            database.execSQL("""
                INSERT INTO $tableName (
                    ${if (preserve_id) "${SQLiteWord.ID.column_name}," else ""},
                    ${SQLiteWord.JAPANESE.column_name}, ${SQLiteWord.ENGLISH.column_name},
                    ${SQLiteWord.FRENCH.column_name}, ${SQLiteWord.READING.column_name},
                    ${SQLiteWord.LEVEL.column_name}, ${SQLiteWord.COUNT_TRY.column_name},
                    ${SQLiteWord.COUNT_SUCCESS.column_name}, ${SQLiteWord.COUNT_FAIL.column_name},
                    ${SQLiteWord.IS_KANA.column_name}, ${SQLiteWord.REPETITION.column_name},
                    ${SQLiteWord.POINTS.column_name}, ${SQLiteWord.BASE_CATEGORY.column_name},
                    ${SQLiteWord.IS_SELECTED.column_name}, ${SQLiteWord.SENTENCE_ID.column_name}
                )
                VALUES (
                    ${if (preserve_id) "${newWord.id}," else ""}, "${newWord.japanese}", "${newWord.english}",
                    "${newWord.french}", "${newWord.reading}", ${newWord.level}, ${newWord.countTry},
                    ${newWord.countSuccess}, ${newWord.countFail}, ${newWord.isKana}, ${newWord.repetition},
                    ${newWord.points}, ${newWord.baseCategory}, ${newWord.isSelected}, ${newWord.sentenceId}
                )
            """.trimIndent()
            )
        }

        fun updateWord(database: SupportSQLiteDatabase, originalId: Long, newWord: Wordv12) {
            database.execSQL("""
                UPDATE $tableName SET
                    ${SQLiteWord.JAPANESE.column_name} = ?, ${SQLiteWord.ENGLISH.column_name} = ?,
                    ${SQLiteWord.FRENCH.column_name} = ?, ${SQLiteWord.READING.column_name} = ?,
                    ${SQLiteWord.IS_KANA.column_name} = ?, ${SQLiteWord.BASE_CATEGORY.column_name} = ?,
                    ${SQLiteWord.SENTENCE_ID.column_name} = ?
                WHERE ${SQLiteWord.ID.column_name} = ?
            """.trimIndent(), arrayOf<Any>(newWord.japanese, newWord.english, newWord.french,
                newWord.reading, newWord.isKana, newWord.baseCategory, newWord.sentenceId, originalId)
            )
        }
    }

    enum class SQLiteWord(val column_name: String) {
        ID("_id"),
        JAPANESE("japanese"),
        ENGLISH("english"),
        FRENCH("french"),
        READING("reading"),
        LEVEL("level"),
        COUNT_TRY("count_try"),
        COUNT_SUCCESS("count_success"),
        COUNT_FAIL("count_fail"),
        IS_KANA("is_kana"),
        REPETITION("repetition"),
        POINTS("points"),
        BASE_CATEGORY("base_category"),
        IS_SELECTED("isSelected"),
        SENTENCE_ID("sentence_id")
    }
}

data class Quizv12(val id: Long, var nameEn: String, var nameFr: String,
                   val category: Int, var isSelected: Int) {

    constructor(cursor: Cursor) : this (
        cursor.getLong(cursor.getColumnIndexOrThrow(SQLiteQuiz.ID.column_name)),
        cursor.getString(cursor.getColumnIndexOrThrow(SQLiteQuiz.NAME_EN.column_name)),
        cursor.getString(cursor.getColumnIndexOrThrow(SQLiteQuiz.NAME_FR.column_name)),
        cursor.getInt(cursor.getColumnIndexOrThrow(SQLiteQuiz.CATEGORY.column_name)),
        cursor.getInt(cursor.getColumnIndexOrThrow(SQLiteQuiz.IS_SELECTED.column_name))
    )

    companion object : BaseGetAllItems<Quizv12>(::Quizv12) {
        init {
            tableName = "quiz"
        }

        fun insertQuiz(database: SupportSQLiteDatabase, newQuiz: Quizv12, preserve_id: Boolean = false): Long {
            val cursor = database.query("""
                INSERT INTO $tableName (
                    ${if (preserve_id) "${SQLiteQuiz.ID.column_name}," else ""},
                    ${SQLiteQuiz.NAME_EN.column_name}, ${SQLiteQuiz.NAME_FR.column_name},
                    ${SQLiteQuiz.CATEGORY.column_name}, ${SQLiteQuiz.IS_SELECTED.column_name}
                )
                VALUES (
                    ${if (preserve_id) "${newQuiz.id}," else ""}, ${newQuiz.nameEn}, ${newQuiz.nameFr},
                    ${newQuiz.category}, ${newQuiz.isSelected}
                )
            """.trimIndent()
            )
            cursor.moveToFirst()
            val newIndex = cursor.getLong(0)
            cursor.close()
            return newIndex
        }
    }

    enum class SQLiteQuiz(val column_name: String) {
        ID("_id"),
        NAME_EN("name_en"),
        NAME_FR("name_fr"),
        CATEGORY("category"),
        IS_SELECTED("isSelected")
    }
}

data class KanjiSolov12(val id: Long, val kanji: String, val strokes: Int, val en: String, val fr: String,
                     val kunyomi: String, val onyomi: String, val radical: String) {

    constructor(cursor: Cursor) : this (
        cursor.getLong(cursor.getColumnIndexOrThrow(SQLiteKanjiSolo.ID.column_name)),
        cursor.getString(cursor.getColumnIndexOrThrow(SQLiteKanjiSolo.KANJI.column_name)),
        cursor.getInt(cursor.getColumnIndexOrThrow(SQLiteKanjiSolo.STROKES.column_name)),
        cursor.getString(cursor.getColumnIndexOrThrow(SQLiteKanjiSolo.EN.column_name)),
        cursor.getString(cursor.getColumnIndexOrThrow(SQLiteKanjiSolo.FR.column_name)),
        cursor.getString(cursor.getColumnIndexOrThrow(SQLiteKanjiSolo.KUNYOMI.column_name)),
        cursor.getString(cursor.getColumnIndexOrThrow(SQLiteKanjiSolo.ONYOMI.column_name)),
        cursor.getString(cursor.getColumnIndexOrThrow(SQLiteKanjiSolo.RADICAL.column_name))
    )

    companion object : BaseGetAllItems<KanjiSolov12>(::KanjiSolov12) {
        init {
            tableName = "kanji_solo"
        }

        fun addKanjiSolo(database: SupportSQLiteDatabase, newKanjiSolo: KanjiSolov12,
                         preserve_id: Boolean = false) {
            database.execSQL("""
                INSERT INTO $tableName ( ${if (preserve_id) "${SQLiteKanjiSolo.ID.column_name}, " else ""}
                    ${SQLiteKanjiSolo.KANJI.column_name}, ${SQLiteKanjiSolo.STROKES.column_name},
                    ${SQLiteKanjiSolo.EN.column_name}, ${SQLiteKanjiSolo.FR.column_name},
                    ${SQLiteKanjiSolo.KUNYOMI.column_name}, ${SQLiteKanjiSolo.ONYOMI.column_name},
                    ${SQLiteKanjiSolo.RADICAL.column_name}
                )
                VALUES ( ${if (preserve_id) "${newKanjiSolo.id}, " else ""}
                        ?, ?, ?, ?, ?, ?, ?
                )
            """.trimIndent(), arrayOf<Any>(newKanjiSolo.kanji, newKanjiSolo.strokes, newKanjiSolo.en,
                    newKanjiSolo.fr, newKanjiSolo.kunyomi, newKanjiSolo.onyomi, newKanjiSolo.radical)
            )
        }
    }

    enum class SQLiteKanjiSolo(val column_name: String) {
        ID("_id"),
        KANJI("kanji"),
        STROKES("strokes"),
        EN("en"),
        FR("fr"),
        KUNYOMI("kunyomi"),
        ONYOMI("onyomi"),
        RADICAL("radical")
    }
}

data class Radicalv12(val id: Long, val strokes: Int, val radical: String,
                   val reading: String, val en: String, val fr: String) {

    constructor(cursor: Cursor) : this (
        cursor.getLong(cursor.getColumnIndexOrThrow(SQLiteRadicals.ID.column_name)),
        cursor.getInt(cursor.getColumnIndexOrThrow(SQLiteRadicals.STROKES.column_name)),
        cursor.getString(cursor.getColumnIndexOrThrow(SQLiteRadicals.RADICAL.column_name)),
        cursor.getString(cursor.getColumnIndexOrThrow(SQLiteRadicals.READING.column_name)),
        cursor.getString(cursor.getColumnIndexOrThrow(SQLiteRadicals.EN.column_name)),
        cursor.getString(cursor.getColumnIndexOrThrow(SQLiteRadicals.FR.column_name))
    )

    companion object : BaseGetAllItems<Radicalv12>(::Radicalv12) {
        init {
            tableName = "radicals"
        }

        fun addRadical(database: SupportSQLiteDatabase, newRadical: Radicalv12,
                       preserve_id: Boolean = false) {
            database.execSQL("""
                INSERT INTO $tableName ( ${if (preserve_id) "${SQLiteRadicals.ID.column_name}, " else ""}
                    ${SQLiteRadicals.STROKES.column_name}, ${SQLiteRadicals.RADICAL.column_name},
                    ${SQLiteRadicals.READING.column_name},
                    ${SQLiteRadicals.EN.column_name}, ${SQLiteRadicals.FR.column_name}
                )
                VALUES ( ${if (preserve_id) "${newRadical.id}, " else ""}
                        ?, ?, ?, ?, ?
                )
            """.trimIndent(), arrayOf<Any>(newRadical.strokes, newRadical.radical,
                                           newRadical.reading, newRadical.en, newRadical.fr)
            )
        }
    }

    enum class SQLiteRadicals(val column_name: String) {
        ID("_id"),
        STROKES("strokes"),
        RADICAL("radical"),
        READING("reading"),
        EN("en"),
        FR("fr")
    }
}

data class QuizWordv12(val id: Long, var quizId: Long, var wordId: Long) {

    constructor(cursor: Cursor) : this (
        cursor.getLong(cursor.getColumnIndexOrThrow(SQLiteQuizWord.ID.column_name)),
        cursor.getLong(cursor.getColumnIndexOrThrow(SQLiteQuizWord.QUIZ_ID.column_name)),
        cursor.getLong(cursor.getColumnIndexOrThrow(SQLiteQuizWord.WORD_ID.column_name))
    )

    companion object : BaseGetAllItems<QuizWordv12>(::QuizWordv12) {
        init {
            tableName = "quiz_word"
        }

        fun quizWordExists(database: SupportSQLiteDatabase, quizId: Long, wordId: Long) : Boolean {
            val cursor = database.query("""
                SELECT COUNT(*) FROM $tableName
                WHERE ${SQLiteQuizWord.QUIZ_ID.column_name} = ?
                AND   ${SQLiteQuizWord.WORD_ID.column_name} = ?
            """.trimIndent(), arrayOf<Any>(quizId, wordId)
            )
            cursor.moveToFirst()
            val exists = cursor.getInt(0)
            cursor.close()
            return exists != 0
        }

        fun addQuizWord(database: SupportSQLiteDatabase, quizID : Long, wordId: Long) {
            database.execSQL("""
                INSERT INTO $tableName
                ( ${SQLiteQuizWord.QUIZ_ID.column_name}, ${SQLiteQuizWord.WORD_ID.column_name} )
                VALUES (?, ?)
            """.trimIndent(), arrayOf<Any>(quizID, wordId)
            )
        }
    }

    enum class SQLiteQuizWord(val column_name: String) {
        ID("_id"),
        QUIZ_ID("quiz_id"),
        WORD_ID("word_id")
    }
}

data class Sentencev12(var id: Long = -1, val jap: String = "",
                       val en: String = "", val fr: String = "", val level: Int = -1) {

    constructor(cursor: Cursor) : this (
        cursor.getLong(cursor.getColumnIndexOrThrow(SQLiteSentences.ID.column_name)),
        cursor.getString(cursor.getColumnIndexOrThrow(SQLiteSentences.JAP.column_name)),
        cursor.getString(cursor.getColumnIndexOrThrow(SQLiteSentences.EN.column_name)),
        cursor.getString(cursor.getColumnIndexOrThrow(SQLiteSentences.FR.column_name)),
        cursor.getInt(cursor.getColumnIndexOrThrow(SQLiteSentences.LEVEL.column_name))
    )

    companion object : BaseGetAllItems<Sentencev12>(::Sentencev12) {
        init {
            tableName = "sentences"
        }

        fun addSentence(database: SupportSQLiteDatabase, newSentence: Sentencev12,
                        preserve_id: Boolean = false) {
            database.execSQL("""
                INSERT INTO $tableName ( ${if (preserve_id) "${SQLiteSentences.ID.column_name}, " else ""}
                    ${SQLiteSentences.JAP.column_name}, ${SQLiteSentences.EN.column_name},
                    ${SQLiteSentences.FR.column_name}, ${SQLiteSentences.LEVEL.column_name}
                )
                VALUES ( ${if (preserve_id) "${newSentence.id}, " else ""}
                        ?, ?, ?, ?
                )
            """.trimIndent(), arrayOf<Any>(newSentence.jap, newSentence.en,
                                           newSentence.fr, newSentence.level)
            )
        }
    }

    enum class SQLiteSentences(val column_name: String) {
        ID("_id"),
        JAP("jap"),
        EN("en"),
        FR("fr"),
        LEVEL("level")
    }
}
