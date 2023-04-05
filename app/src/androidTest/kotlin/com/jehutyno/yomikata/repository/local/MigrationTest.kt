package com.jehutyno.yomikata.repository.local

import androidx.room.Room.databaseBuilder
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException


@RunWith(AndroidJUnit4::class)
@LargeTest
class MigrationTest {

    // do not use Daos in tests since they only work
    // for the latest version which may change in the future

    private val TEST_DB_NAME = "test_database"

    // Helper for creating Room databases and migrations
    @get:Rule
    val mMigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        YomikataDataBase::class.java
    )

    // Helper for creating SQLite database in version 12
    private var mSqliteTestHelper: SqliteTestHelper? = null

    private val sampleWordv12 = Wordv12(65, "草", "grass", "herbe", "くさ",
                                        0, 0, 0, 0, 0,
                                -1, 0, 2, 0, 6647)

    @Before
    fun setUp() {
        // To test migrations from version 12 of the database, we need to create the database
        // with version 12 using SQLite API
        mSqliteTestHelper = SqliteTestHelper(
            ApplicationProvider.getApplicationContext(),
            TEST_DB_NAME
        )
        // We're creating the table for every test, to ensure that the table is in the correct state
        SqliteTestHelper.createAllTables(mSqliteTestHelper!!)
    }

    @After
    fun tearDown() {
        // Clear the database after every test
        SqliteTestHelper.clearDatabase(mSqliteTestHelper!!)
    }

    @Test
    @Throws(IOException::class)
    fun migrationFrom12To13_containsCorrectData() {
        // Create the database with the version 12 schema and insert some rows

        SqliteTestHelper.insertWord(mSqliteTestHelper!!, sampleWordv12)


        mMigrationTestHelper.runMigrationsAndValidate(
            TEST_DB_NAME, 13, true,
            YomikataDataBase.MIGRATION_12_13
        )
        // Get the latest, migrated, version of the database
        val latestDb: YomikataDataBase = getMigratedRoomDatabase()

        // Check that the correct data is in the database
        val wordCusror = latestDb.query(
            """SELECT * FROM words""", arrayOf<Any>()
        )
        assert ( wordCusror.moveToFirst() )
        assert ( wordCusror.getLong(0) == sampleWordv12.id )
    }

    @Test
    @Throws(IOException::class)
    fun startInVersion13_containsCorrectData() {
        // Create the database with version 13
        val db = mMigrationTestHelper.createDatabase(TEST_DB_NAME, 13)

        // db has schema version 13. insert some data
        db.execSQL("""
            INSERT INTO words (
                "_id", "japanese", "english", "french", "reading", "level",
                "count_try", "count_success", "count_fail", "is_kana", "repetition", "points",
                "base_category", "isSelected", "sentence_id"
                )
            VALUES (
                '489', '安い', 'cheap; inexpensive', 'bon marché; pas cher',
                'やすい', '0', '0', '0', '0', '0', '-1', '0', '7', '0', '6819'
                )
        """.trimIndent()
        )

        db.close()

        // open the db with Room
        val database: YomikataDataBase = getMigratedRoomDatabase()

        // verify that the data is correct
        val wordCursor = database.query("""SELECT * FROM words""", arrayOf<Any>())
        assert (wordCursor.moveToFirst() )
        val l : Long = 489
        assert ( wordCursor.getLong(0) == l )
    }

    private fun getMigratedRoomDatabase(): YomikataDataBase {
        val database: YomikataDataBase = databaseBuilder(
            ApplicationProvider.getApplicationContext(),
            YomikataDataBase::class.java, TEST_DB_NAME
        )
            .addMigrations(YomikataDataBase.MIGRATION_12_13)
            .build()
        // close the database and release any stream resources when the test finishes
        mMigrationTestHelper.closeWhenFinished(database)
        return database
    }

//    private fun insertUser(user: User, db: SupportSQLiteDatabase) {
//        val values = ContentValues()
//        values.put("userid", user.getId())
//        values.put("username", user.getUserName())
//        db.insert("users", SQLiteDatabase.CONFLICT_REPLACE, values)
//    }

}
