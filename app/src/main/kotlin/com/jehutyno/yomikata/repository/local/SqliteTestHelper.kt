package com.jehutyno.yomikata.repository.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper


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

// Version of database entries from version 12 used for testing   --- DO NOT CHANGE ---
class Wordv12(var id: Long, var japanese: String, var english: String, var french: String,
              var reading: String, var level: Int, var countTry: Int, var countSuccess: Int,
              var countFail: Int, var isKana: Int, var repetition: Int, var points: Int,
              var baseCategory: Int, var isSelected: Int, var sentenceId: Long)
