package com.jehutyno.yomikata.repository.migration

import java.util.*


/**
 * Created by valentin on 07/10/2016.
 */
class MigrationSource(private val migrationDao: MigrationDao) {

    fun getWordTable(tableName: String) : ArrayList<WordTable> {
        return migrationDao.getWordTable(tableName).map { it.toWordTable() } as ArrayList<WordTable>
    }
}
