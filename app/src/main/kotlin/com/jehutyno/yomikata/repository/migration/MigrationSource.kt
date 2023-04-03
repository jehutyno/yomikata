package com.jehutyno.yomikata.repository.migration

import androidx.sqlite.db.SimpleSQLiteQuery
import java.util.*


/**
 * Created by valentin on 07/10/2016.
 */
class MigrationSource(private val migrationDao: MigrationDao) {

    fun getWordTable(tableName: String) : ArrayList<WordTable> {
        val query = "SELECT * FROM $tableName"
        val supportQuery = SimpleSQLiteQuery(query)
        val roomWordTable = migrationDao.getWordTable(supportQuery)
        return roomWordTable.map { it.toWordTable() } as ArrayList<WordTable>
    }
}
