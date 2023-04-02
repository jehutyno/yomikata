package com.jehutyno.yomikata.repository.migration

import androidx.room.Dao
import androidx.room.Query
import java.util.ArrayList


@Dao
interface MigrationDao {
    @Query("SELECT * FROM :tableName")
    fun getWordTable(tableName: String) : ArrayList<RoomMigrationWordTable>
}
