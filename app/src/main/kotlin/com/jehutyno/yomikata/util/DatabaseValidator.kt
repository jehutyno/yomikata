package com.jehutyno.yomikata.util

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import com.jehutyno.yomikata.repository.local.DATABASE_VERSION
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
//        checkSchema(db, listOf(1, 2, 3).toIntArray())

        if (isOldYomikata(db)) {
            return DatabaseType.OLD_YOMIKATA
        }
        // check for tables
        val requiredTables = arrayOf("words", "quiz")
        val tableCursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'" +
                                          "AND name IN (?, ?)", requiredTables)
        for (table in requiredTables) {
            if (!tableCursor.moveToNext()) {
                throw IllegalStateException("Database does not contain some table(s)")
            }
        }
        tableCursor.close()

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
    // check for tables
    var tableCursor: Cursor? = null
    val requiredTables = MigrationTable.allTables(MigrationTables.values())
    try {
        val size = requiredTables.size
        // generates a string of the form "?," repeated 'size' times and removes the last comma and space
        val questionMarks = "?, ".repeat(size).removeSuffix(", ")

        tableCursor = database.rawQuery("SELECT name FROM sqlite_master WHERE type='table'" +
                                            "AND name IN ($questionMarks)", requiredTables)
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
