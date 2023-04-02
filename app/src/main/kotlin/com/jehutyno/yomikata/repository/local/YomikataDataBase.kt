package com.jehutyno.yomikata.repository.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase


@Database(entities = [KanjiSoloDao::class, QuizDao::class, SentenceDao::class,
                      QuizDao::class, SentenceDao::class, StatsDao::class,
                      UpdateDao::class, WordDao::class],
          version = 13, exportSchema = true)
abstract class YomikataDataBase : RoomDatabase() {
    abstract fun kanjiSoloDao(): KanjiSoloDao
    abstract fun quizDao(): QuizDao
    abstract fun sentenceDao(): SentenceDao
    abstract fun statsDao(): StatsDao
    abstract fun updateDao(): UpdateDao
    abstract fun wordDao(): WordDao

    companion object {
        private var INSTANCE: YomikataDataBase? = null
        fun getDatabase(context: Context): YomikataDataBase {
            if (INSTANCE == null) {
                synchronized(this) {
                    INSTANCE =
                        Room.databaseBuilder(context, YomikataDataBase::class.java, "yomikataz")
                            .addMigrations(MIGRATION_8_9, MIGRATION_12_13)
                            .build()
                }
            }
            return INSTANCE!!
        }

        // migrate from anko sqlite to room
        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Empty implementation, because the schema isn't changing.
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
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

                database.use {
                    it.execSQL(query0)
                    it.execSQL(query1)
                    it.execSQL(query2)
                    it.execSQL(query3)
                }
            }
        }

    }

}
