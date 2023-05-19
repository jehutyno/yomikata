package com.jehutyno.yomikata.util

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import com.jehutyno.yomikata.repository.database.DATABASE_VERSION
import com.jehutyno.yomikata.repository.migration.MigrationTable
import com.jehutyno.yomikata.repository.migration.MigrationTables
import java.io.File


enum class DatabaseType {
    UP_TO_DATE,     // the latest database version
    OLD_VERSION,    // a previous version
    OLD_YOMIKATA    // the old yomikata database schema
}

/**
 * Validate database
 *
 * Validates a database file
 *
 * @param databaseFile File of the database to validate. Must be openable with SQLitDatabase.openDatabase
 * @return The type of database depending on the version
 */
fun validateDatabase(databaseFile: File): DatabaseType {
    var db : SQLiteDatabase? = null
    try {
        // attempt to open the database file
        db = SQLiteDatabase.openDatabase(databaseFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)

        // optional schema check
//        checkSchema(db, listOf(???).toIntArray())

        if (isOldYomikata(db)) {
            return DatabaseType.OLD_YOMIKATA
        }
        // sentences table is not required since it did not exist yet in versions <= 8
        val requiredTables = arrayOf("words", "quiz", "quiz_word", "kanji_solo", "radicals", "stat_entry")
        if (!containsTables(db, requiredTables)) {
            throw IllegalStateException("Database does not contain some table(s)")
        }

        val version = db.version
        return if (version == DATABASE_VERSION)
            DatabaseType.UP_TO_DATE
        else
            DatabaseType.OLD_VERSION

    } catch (e: SQLiteException) {
        throw IllegalStateException("Unable to open database file, please verify it is a database " +
                "file ending in .db", e)
    } finally {
        db?.close()
    }
}

/**
 * Is old yomikata.
 *
 * @param database SQLiteDatabase
 * @return True if Old type of yomikata database
 */
fun isOldYomikata(database: SQLiteDatabase): Boolean {
    val requiredTables = MigrationTable.allTables(MigrationTables.values())
    return containsTables(database, requiredTables)
}

/**
 * Contains tables
 *
 * check if the database contains all the given tables
 *
 * @param database SQLiteDatabase
 * @param requiredTables Strings of table names
 * @return true if contains all tables, false otherwise
 */
private fun containsTables(database: SQLiteDatabase, requiredTables: Array<String>): Boolean {
    val size = requiredTables.size
    // generates a string of the form "?," repeated 'size' times and removes the last comma and space
    val questionMarks = "?, ".repeat(size).removeSuffix(", ")

    var tableCursor: Cursor? = null
    try {
        tableCursor = database.rawQuery(
            """
                SELECT name FROM sqlite_master WHERE type='table'
                AND name IN ($questionMarks)
            """.trimIndent(),
            requiredTables
        )
        for (table in requiredTables) {
            if (!tableCursor.moveToNext()) {
                return false
            }
        }
    } finally {
        tableCursor?.close()
    }
    return true
}

/**
 * Check schema
 *
 * Throws error if the schema version is wrong
 *
 * @param database SQLiteDatabase to check for schema versions
 */
private fun checkSchema(database: SQLiteDatabase, validSchemaVersions: IntArray) {
    // check the database schema version
    var cursor: Cursor? = null
    try {
        cursor = database.rawQuery("PRAGMA schema_version", null)
        if (cursor.moveToFirst()) {
            val schemaVersion = cursor.getInt(0)
            if (!validSchemaVersions.contains(schemaVersion)) {
                throw IllegalStateException("Unexpected database schema version: $schemaVersion")
            }
        } else {
            throw IllegalStateException("Unable to retrieve database schema version")
        }
    } finally {
        cursor?.close()
    }
}
