package com.jehutyno.yomikata.repository.migration

import android.database.sqlite.SQLiteDatabase


/**
 * Created by valentin on 07/10/2016.
 */
class MigrationSource(private val oldDatabase: SQLiteDatabase, private val newDatabase: SQLiteDatabase) {

    fun getWordTable(tableName: String) : List<WordTable> {
        val wordTables = mutableListOf<WordTable>()
        val query = "SELECT * FROM $tableName"
        oldDatabase.rawQuery(query, arrayOf()).use {
            while (it.moveToNext()) {
                wordTables.add(
                    WordTable(
                        it.getInt(0),
                        it.getString(1),
                        it.getString(2),
                        it.getInt(3),
                        it.getInt(4),
                        it.getInt(5),
                        it.getInt(6),
                    )
                )
            }
        }
        return wordTables
    }

    /**
     * Restore word.
     *
     * Converts a word from the old yomikata format to the new yomikataz format (at version 13)
     *
     * @param word
     * @param pronunciation
     * @param wordTable
     */
    fun restoreWord(word: String, pronunciation: String, wordTable: WordTable) {
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

        val query = "UPDATE words SET level = ?, " +
                "points = ?, " +
                "count_fail = ?, " +
                "count_try = ?, " +
                "count_success = ? " +
                "WHERE japanese = (?) AND reading LIKE '%' || (?) || '%'"

        newDatabase.execSQL(query, arrayOf(
                priority, points, wordTable.counterFail, wordTable.counterTry,
                wordTable.counterSuccess, word, pronunciation
            )
        )
    }
}
