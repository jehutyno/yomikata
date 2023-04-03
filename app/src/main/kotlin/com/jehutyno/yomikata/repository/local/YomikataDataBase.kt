package com.jehutyno.yomikata.repository.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase


@Database(entities = [RoomKanjiSolo::class, RoomQuiz::class, RoomSentences::class,
                      RoomStatEntry::class, RoomWords::class, RoomQuizWord::class,
                      RoomRadicals::class],
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
                            .allowMainThreadQueries()   // TODO: remove this after using coroutines/livedata
                            .addMigrations(MIGRATION_8_9(context), MIGRATION_12_13)
                            .build()
                }
            }
            return INSTANCE!!
        }

        ///////// DEFINE MIGRATIONS /////////
        // do not use values or constants that may be changed externally

        // migrate from anko sqlite to room
        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Empty implementation, because the schema isn't changing.
            }
        }

        @Suppress("ClassName")
        private class MIGRATION_8_9(val context: Context) : Migration(8, 9) {
            // STEPS
            // 1. Add sentences table (did not exist before version 9)
            // 2. Alter the words table: new column = sentenceId (+ redefine types??) (all others preserved)
            // 3. Add all of the new sentences
            override fun migrate(database: SupportSQLiteDatabase) {
                // STEP 1
                val queryCreateSentencesTable = "CREATE TABLE sentences (\n" +
                        "   _id         INTEGER     PRIMARY KEY,\n" +
                        "   jap         TEXT,\n" +
                        "   en          TEXT,\n" +
                        "   fr          TEXT,\n" +
                        "   level       INTEGER\n" +
                        ");"

                database.execSQL(queryCreateSentencesTable)

                // STEP 2
                val queryRenameOldTable = "ALTER TABLE words RENAME TO OLD_words"

                val queryCreateNewTable = "CREATE TABLE words (\n" +
                        "    _id           INTEGER      PRIMARY KEY AUTOINCREMENT,\n" +
                        "    japanese      TEXT         NOT NULL,\n" +
                        "    english       TEXT         NOT NULL,\n" +
                        "    french        TEXT         NOT NULL,\n" +
                        "    reading       TEXT         NOT NULL,\n" +
                        "    level         INTEGER,\n" +
                        "    count_try     INTEGER,\n" +
                        "    count_success INTEGER,\n" +
                        "    count_fail    INTEGER,\n" +
                        "    is_kana       BOOLEAN,\n" +
                        "    repetition    INTEGER (-1),\n" +
                        "    points        INTEGER,\n" +
                        "    base_category INTEGER,\n" +
                        "    isSelected    INTEGER      DEFAULT (0),\n" +
                        "    sentence_id   INTEGER      DEFAULT (-1) \n" +
                        ");"

                val queryInsertOldIntoNew = "INSERT INTO words " +
                        "(_id, japanese, english, french, reading, level, count_try, count_success, " +
                        "       count_fail, is_kana, repetition, points, base_category, isSelected) " +
                        "SELECT _id, japanese, english, french, reading, level, count_try, count_success, " +
                        "       count_fail, is_kana, repetition, points, base_category, isSelected " +
                        "FROM OLD_words"

                val queryDropOldTable = "DROP TABLE OLD_words"

                database.use {
                    it.execSQL(queryRenameOldTable)
                    it.execSQL(queryCreateNewTable)
                    it.execSQL(queryInsertOldIntoNew)
                    it.execSQL(queryDropOldTable)
                }

                // STEP 3
                val queryInsertAllSentences = context.assets.open("InsertSentencesVersion9.sql")
                            .bufferedReader().use {
                                it.readText()
                            }
                database.execSQL(queryInsertAllSentences)
            }
        }

    }

}
