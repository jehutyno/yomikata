package com.jehutyno.yomikata.repository.migration

import androidx.room.Dao
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery


@Dao
interface MigrationDao {
    @RawQuery(observedEntities = [RoomMigrationWordTable::class])
    fun getWordTable(query: SupportSQLiteQuery) : List<RoomMigrationWordTable>
}
