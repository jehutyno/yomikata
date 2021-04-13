package com.jehutyno.yomikata.repository.migration

import android.content.Context
import org.jetbrains.anko.db.parseList
import org.jetbrains.anko.db.rowParser
import org.jetbrains.anko.db.select
import java.util.*

/**
 * Created by valentin on 07/10/2016.
 */
class MigrationSource(var context: Context, val database: DatabaseHelper) {

    fun getWordTable(tableName: String) : ArrayList<WordTable> {
        var wordTables = arrayListOf<WordTable>()
        database.use {
            select(tableName, *MigrationTable.allColumns(MigrationWordTable.values()))
                .exec {
                    val rowParser = rowParser(::WordTable)
                    if (count > 0)
                        wordTables.addAll(parseList(rowParser))
                }
        }

        return wordTables
    }
}