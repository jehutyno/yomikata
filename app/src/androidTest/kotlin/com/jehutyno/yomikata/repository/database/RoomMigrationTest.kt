package com.jehutyno.yomikata.repository.database

import androidx.collection.arraySetOf
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.jehutyno.yomikata.repository.database.YomikataDatabase
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
        YomikataDatabase::class.java
    )

    // add new migrations to this list
    private val ALL_MIGRATIONS = YomikataDatabase.let {
        arrayOf (
            it.MIGRATION_14_15, it.MIGRATION_15_16
        )
    }

    @Test
    fun migrate14To15() {
        // test values for points and levels
        class Values(val id: Long, val level: Int, val points: Int, val newLevel: Int, val newPoints: Int) {
            fun toArray(): Array<Any> {
                return arrayOf(id, level, points)
            }
        }
        val values = listOf(
            Values(3658, 0, 0, 0, 0),
            Values(6328, 1, 0, 1, 200),
            Values(47, 1, 25, 1, 250),
            Values(93, 4, 100, 4, 850)
        )

        @Suppress("VARIABLE_WITH_REDUNDANT_INITIALIZER")
        var database = migrationHelper.createDatabase(TEST_DB_NAME, 14).apply {

            // insert some data to test
            execSQL("""
                INSERT INTO words
                VALUES (?, '重力', '(n) gravity;(P)', '(n) gravité;pesanteur', 'じゅうりょく',
                ?, '0', '0', '0', '0', '-1', ?, '4', '0', NULL);
            """.trimIndent(), values[0].toArray())
            execSQL("""
                INSERT INTO words
                VALUES (?, '感触',
                '(n,vs) feel (i.e. tactile sensation);touch;feeling;sensation;texture (e.g. food, cloth);(P)',
                '(n) toucher (sens);sensation', 'かんしょく', ?, '0', '0', '0', '0', '-1', ?, '3', '0', NULL);
            """.trimIndent(), values[1].toArray())
            execSQL("""
                INSERT INTO words
                VALUES (?, '目', 'eye', 'oeil', 'め', ?, '0', '0', '0', '0', '-1', ?, '2', '0', NULL);
            """.trimIndent(), values[2].toArray())
            execSQL("""
                INSERT INTO words
                VALUES (?, '小さい', 'small', 'petit', 'ちいさい', ?, '0', '0', '0', '0', '-1', ?, '2', '0', NULL);
            """.trimIndent(), values[3].toArray())

            close()
        }

        database = migrationHelper.runMigrationsAndValidate(TEST_DB_NAME, 15,
                            true, YomikataDatabase.MIGRATION_14_15)

        // test that inserted data was processed properly
        val cursor = database.query("""SELECT _id, level, points FROM words""")

        val foundIds = arraySetOf<Long>()
        while (cursor.moveToNext()) {
            val id = cursor.getLong(cursor.getColumnIndex("_id"))
            foundIds.add(id)
            val value = values.find { it.id == id }!!
            assert ( cursor.getInt(cursor.getColumnIndex("level")) == value.newLevel )
            assert ( cursor.getInt(cursor.getColumnIndex("points")) == value.newPoints )
        }
        // make sure all inserted rows were found
        assert ( foundIds == values.map { it.id }.toSet() )
    }

    @Test
    fun migrate15To16() {

        class Values(val id: Long, val english: String, val newEnglish: String) {
            fun toArray(): Array<Any> {
                return arrayOf(id, english)
            }
        }

        val englishValues = arrayOf(
            Values(3854,
            "(n) (1) lighthouse;(2) old-fashioned interior light fixture comprising a wooden pole with an oil-filled dish and a wick atop it;(P)",
            "(n) (1) lighthouse;(2) old-fashioned interior light fixture comprising a wooden pole with an oil-filled dish and a wick atop it"
            ),
            Values(94,
            "warm (spring)",
            "warm (spring)"
            )
        )

        @Suppress("VARIABLE_WITH_REDUNDANT_INITIALIZER")
        var database = migrationHelper.createDatabase(TEST_DB_NAME, 15).apply {

            execSQL("""
                INSERT INTO words
                VALUES (?, '灯台', ?, '(n) phare', 'とうだい', '0', '0', '0',
                '0', '0', '-1', '0', '4', '0', NULL);
            """.trimIndent(), englishValues[0].toArray())

            execSQL("""
                INSERT INTO words
                VALUES (?, '暖かい', ?, 'chaud (printemps)', 'あたたかい', '0', '0', '0', '0', '0',
                '-1', '0', '2', '0', NULL);
            """.trimIndent(), englishValues[1].toArray())

            close()
        }

        database = migrationHelper.runMigrationsAndValidate(TEST_DB_NAME, 16,
                            true, YomikataDatabase.MIGRATION_15_16)


        val cursor = database.query("""SELECT _id, english FROM words""")

        val foundIds = arraySetOf<Long>()
        while (cursor.moveToNext()) {
            val id = cursor.getLong(cursor.getColumnIndex("_id"))
            foundIds.add(id)
            val value = englishValues.find { it.id == id }!!
            val newEnglish = cursor.getString(cursor.getColumnIndex("english"))
            assert( newEnglish == value.newEnglish )
        }
        assert( foundIds == englishValues.map{ it.id }.toSet() )
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
            YomikataDatabase::class.java,
            TEST_DB_NAME
        ).addMigrations(*ALL_MIGRATIONS).build().apply {
            openHelper.writableDatabase.close()
        }
    }

}
