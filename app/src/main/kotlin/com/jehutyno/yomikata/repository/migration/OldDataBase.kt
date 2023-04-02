package com.jehutyno.yomikata.repository.migration

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase


@Database(entities = [MigrationDao::class], version = 1)
abstract class OldDataBase : RoomDatabase() {
    abstract fun migrationDao(): MigrationDao

    companion object {
        private var INSTANCE: OldDataBase? = null
        fun getDatabase(context: Context): OldDataBase {
            if (INSTANCE == null) {
                synchronized(this) {
                    INSTANCE =
                        Room.databaseBuilder(context, OldDataBase::class.java, "Japanese")
                            .build()
                }
            }
            return INSTANCE!!
        }

    }

}
