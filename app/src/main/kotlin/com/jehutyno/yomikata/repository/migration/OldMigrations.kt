package com.jehutyno.yomikata.repository.migration

import android.content.Context
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.jehutyno.yomikata.util.UpdateProgressDialog


/**
 * Old migrations.
 *
 * For migrations up to version 13.
 * See YomikataDatabase.kt for migrations using Room schemas from version 14 upwards.
 */
object OldMigrations {

    fun getOldMigrations(): Array<Migration> {
        return arrayOf(
            MIGRATION_1_2, MIGRATION_2_3,
            MIGRATION_3_4, MIGRATION_4_5,
            MIGRATION_5_6, MIGRATION_6_7,
            MIGRATION_7_8, MIGRATION_8_9,
            MIGRATION_9_10, MIGRATION_10_11,
            MIGRATION_11_12
        )
    }

    // used to show progress off migration 12 -> 13
    var updateProgressDialogGetter: () -> UpdateProgressDialog? = { null }

    @Suppress("ClassName")
    class MIGRATION_12_13(private val context: Context) : Migration(12, 13) {

        // This performs all migrations from 1 to 13 (excluding the operations in
        // MIGRATION_8_9). The old database is synchronized with a static copy of version 13
        override fun migrate(database: SupportSQLiteDatabase) {
            updateOldDBtoVersion13(database, context, updateProgressDialogGetter.invoke())
        }

    }

    val MIGRATION_11_12 = object : Migration(11, 12) {
        override fun migrate(database: SupportSQLiteDatabase) {}
    }

    val MIGRATION_10_11 = object : Migration(10, 11) {
        override fun migrate(database: SupportSQLiteDatabase) {}
    }

    val MIGRATION_9_10 = object : Migration(9, 10) {
        override fun migrate(database: SupportSQLiteDatabase) {}
    }

    val MIGRATION_8_9 = object : Migration(8, 9) {
        // STEPS
        // 1. Add sentences table
        // 2. Alter the words table: new column = sentenceId (all others preserved)
        // 3. A̶d̶d̶ ̶a̶l̶l̶ ̶o̶f̶ ̶t̶h̶e̶ ̶n̶e̶w̶ ̶s̶e̶n̶t̶e̶n̶c̶e̶s̶ This is now done when migrating from 12 -> 13
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

            database.run {
                this.execSQL(queryRenameOldTable)
                this.execSQL(queryCreateNewTable)
                this.execSQL(queryInsertOldIntoNew)
                this.execSQL(queryDropOldTable)
            }
        }
    }

    val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(database: SupportSQLiteDatabase) {}
    }

    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(database: SupportSQLiteDatabase) {}
    }

    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(database: SupportSQLiteDatabase) {}
    }

    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(database: SupportSQLiteDatabase) {}
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {}
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {}
    }

    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {}
    }
}
