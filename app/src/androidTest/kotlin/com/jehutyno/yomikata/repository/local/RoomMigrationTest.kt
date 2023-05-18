package com.jehutyno.yomikata.repository.local

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


/**
 * Room migration test
 *
 * For testing migrations starting from version 14
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class RoomMigrationTest {

    // do not use Daos in tests since they only work
    // for the latest version which may change in the future

    private val TEST_DB_NAME = "test_database_room"

    // Helper for creating Room databases and migrations
    @get:Rule
    val migrationHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        YomikataDataBase::class.java
    )

    // add new migrations to this list
    private val ALL_MIGRATIONS = YomikataDataBase.let {
        arrayOf (
            it.MIGRATION_14_15
        )
    }

    @Test
    fun migrate14To15() {
        @Suppress("VARIABLE_WITH_REDUNDANT_INITIALIZER")
        var database = migrationHelper.createDatabase(TEST_DB_NAME, 14).apply {

            // insert some data to test
            execSQL("""
                INSERT INTO "words" ("_id", "japanese", "english", "french", "reading",
                "level", "count_try", "count_success", "count_fail", "is_kana", "repetition",
                "points", "base_category", "isSelected", "sentence_id")
                VALUES ('3658', '重力', '(n) gravity;(P)', '(n) gravité;pesanteur', 'じゅうりょく',
                '0', '0', '0', '0', '0', '-1', '0', '4', '0', NULL);
            """.trimIndent())

            close()
        }

        database = migrationHelper.runMigrationsAndValidate(TEST_DB_NAME, 15,
                            true, YomikataDataBase.MIGRATION_14_15)

        // test that inserted data was processed properly
        val cursor = database.query("""SELECT level, points FROM words""")
        assert ( cursor.moveToNext() )  // one entity should exist
        assert ( cursor.getInt(cursor.getColumnIndex("level")) == 0 )
        assert ( cursor.getInt(cursor.getColumnIndex("points")) == 0 )
    }

    @Test
    fun migrateAll() {
        // Create version at 14 (earliest version with a Room schema)
        migrationHelper.createDatabase(TEST_DB_NAME, 14).apply {
            close()
        }

        // Open latest version of the database. Room validates the schema once all migrations execute.
        Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            YomikataDataBase::class.java,
            TEST_DB_NAME
        ).addMigrations(*ALL_MIGRATIONS).build().apply {
            openHelper.writableDatabase.close()
        }
    }

}
