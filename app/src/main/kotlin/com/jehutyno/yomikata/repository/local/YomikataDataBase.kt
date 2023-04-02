package com.jehutyno.yomikata.repository.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase


@Database(entities = [KanjiSoloDao::class, QuizDao::class, SentenceDao::class,
                      QuizDao::class, SentenceDao::class, StatsDao::class,
                      UpdateDao::class, WordDao::class],
          version = 13, exportSchema = true)
abstract class YomikataDataBase : RoomDatabase() {
    abstract fun kanjiSoloDao(): KanjiSoloDao
    abstract fun quizDao(): QuizDao
    abstract fun sentenceDao(): SentenceDao
    abstract fun statsDao(): StatsDao
    abstract fun updateDao(): UpdateDao
    abstract fun wordDao(): WordDao

    companion object {
        private var INSTANCE: YomikataDataBase? = null
        fun getDatabase(context: Context): YomikataDataBase {
            if (INSTANCE == null) {
                synchronized(this) {
                    INSTANCE =
                        Room.databaseBuilder(context, YomikataDataBase::class.java, "yomikataz")
                            .addMigrations(MIGRATION_12_13)
                            .build()
                }
            }
            return INSTANCE!!
        }

        // migrate from anko sqlite to room
        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Empty implementation, because the schema isn't changing.
            }
        }
    }

}
