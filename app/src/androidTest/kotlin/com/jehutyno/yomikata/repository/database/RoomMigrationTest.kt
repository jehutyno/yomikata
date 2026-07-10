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

    // Shorthand for the instrumentation context (available at any point during tests)
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    // add new migrations to this list
    private val ALL_MIGRATIONS get() = arrayOf(
        YomikataDatabase.MIGRATION_13_14,
        YomikataDatabase.MIGRATION_14_15,
        YomikataDatabase.MIGRATION_15_16,
        YomikataDatabase.MIGRATION_16_21,
        // Chaîne granulaire 17→21 : doit refléter exactement le builder de getDatabase()
        // (sinon une base v17-20 n'a aucun chemin vers 21 → "Migration error").
        YomikataDatabase.MIGRATION_17_18,
        YomikataDatabase.createMigration18to19(context),
        YomikataDatabase.MIGRATION_19_20,
        YomikataDatabase.MIGRATION_20_21
    )

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
            Values(4519, 3, 50,  3,725),
            Values(93, 4, 666, 3, 850)
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
                VALUES (?, '炒る', '(v5r,vt) to parch;to fry;to fire;to broil;to roast;to boil down (in oil)',
                '(v5r,vt) frire; faire bouillir (dans l''huile); rôtir; cuire', 'いる', ?, '0', '0', '0', '0', '-1', ?, '4', '0', '4646');
            """.trimIndent(), values[3].toArray())
            execSQL("""
                INSERT INTO words
                VALUES (?, '小さい', 'small', 'petit', 'ちいさい', ?, '0', '0', '0', '0', '-1', ?, '2', '0', NULL);
            """.trimIndent(), values[4].toArray())

            close()
        }

        database = migrationHelper.runMigrationsAndValidate(TEST_DB_NAME, 15,
                            true, YomikataDatabase.MIGRATION_14_15)

        // test that inserted data was processed properly
        val cursor = database.query("""SELECT _id, level, points FROM words""")

        val foundIds = arraySetOf<Long>()
        while (cursor.moveToNext()) {
            val id = cursor.getLong(cursor.getColumnIndexOrThrow("_id"))
            foundIds.add(id)
            val value = values.find { it.id == id }!!
            assert ( cursor.getInt(cursor.getColumnIndexOrThrow("level")) == value.newLevel )
            assert ( cursor.getInt(cursor.getColumnIndexOrThrow("points")) == value.newPoints )
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
    fun migrate16To17() {
        // Insert test data in v16 schema
        val database = migrationHelper.createDatabase(TEST_DB_NAME, 16).apply {

            // phantom word — all fields empty/invalid, should be deleted
            execSQL("""
                INSERT INTO words
                VALUES (3537, '', '', '', '', -1, 0, 0, 0, -1, -1, 0, -1, 0, NULL)
            """.trimIndent())

            // word with double space in english
            execSQL("""
                INSERT INTO words
                VALUES (1, '有難う', 'thank  you', 'merci', 'ありがとう', 0, 0, 0, 0, 0, -1, 0, 7, 0, NULL)
            """.trimIndent())

            // word with leading space in french
            execSQL("""
                INSERT INTO words
                VALUES (2, '月', 'moon; month', ' lune; mois', 'つき', 0, 0, 0, 0, 0, -1, 0, 2, 0, NULL)
            """.trimIndent())

            // word with trailing space in english
            execSQL("""
                INSERT INTO words
                VALUES (3, '猫', 'cat ', 'chat', 'ねこ', 0, 0, 0, 0, 0, -1, 0, 2, 0, NULL)
            """.trimIndent())

            // sentence with double space in en and trailing space in fr
            execSQL("""
                INSERT INTO sentences
                VALUES (1, '猫がいる', 'There  is a cat', 'Il y a un chat ', 0)
            """.trimIndent())

            close()
        }

        val migratedDb = migrationHelper.runMigrationsAndValidate(
            TEST_DB_NAME, 17, true, YomikataDatabase.MIGRATION_16_17
        )

        // phantom word must be gone
        migratedDb.query("SELECT _id FROM words WHERE _id = 3537").use {
            assertEquals("phantom word 3537 should be deleted", 0, it.count)
        }

        // verify all words are cleaned
        val wordCursor = migratedDb.query("SELECT _id, english, french FROM words ORDER BY _id")
        wordCursor.use {
            assertTrue(it.moveToNext())
            assertEquals(1L, it.getLong(0))
            assertEquals("thank you", it.getString(1))   // double space removed
            assertEquals("merci", it.getString(2))        // unchanged

            assertTrue(it.moveToNext())
            assertEquals(2L, it.getLong(0))
            assertEquals("moon; month", it.getString(1))  // unchanged
            assertEquals("lune; mois", it.getString(2))   // leading space removed

            assertTrue(it.moveToNext())
            assertEquals(3L, it.getLong(0))
            assertEquals("cat", it.getString(1))          // trailing space removed
            assertEquals("chat", it.getString(2))          // unchanged

            assertFalse("No more rows expected", it.moveToNext())
        }

        // verify sentence is cleaned
        migratedDb.query("SELECT en, fr FROM sentences WHERE _id = 1").use {
            assertTrue(it.moveToNext())
            assertEquals("There is a cat", it.getString(0))  // double space removed
            assertEquals("Il y a un chat", it.getString(1))   // trailing space removed
        }
    }

    @Test
    fun migrate17To18() {
        // Create a v17 database with sample rows
        migrationHelper.createDatabase(TEST_DB_NAME, 17).apply {
            execSQL("INSERT INTO words VALUES (1,'犬','dog','chien','いぬ',0,0,0,0,0,-1,0,2,0,NULL)")
            execSQL("INSERT INTO sentences VALUES (1,'犬がいる','There is a dog','Il y a un chien',0)")
            execSQL("INSERT INTO kanji_solo VALUES ('犬',4,'dog','chien','いぬ','ケン','大')")
            execSQL("INSERT INTO radicals VALUES ('大',3,'おおがい','big','grand')")
            execSQL("INSERT INTO quiz VALUES (1,'JLPT N5','JLPT N5',7,0)")
            close()
        }

        val db = migrationHelper.runMigrationsAndValidate(
            TEST_DB_NAME, 18, true, YomikataDatabase.MIGRATION_17_18
        )

        // All new columns should exist with empty-string defaults
        db.query("SELECT german, spanish, portuguese, chinese FROM words WHERE _id = 1").use {
            assertTrue(it.moveToFirst())
            assertEquals("", it.getString(0))   // german
            assertEquals("", it.getString(1))   // spanish
            assertEquals("", it.getString(2))   // portuguese
            assertEquals("", it.getString(3))   // chinese
        }
        db.query("SELECT de, es, pt, zh FROM sentences WHERE _id = 1").use {
            assertTrue(it.moveToFirst())
            assertEquals("", it.getString(0))
            assertEquals("", it.getString(1))
            assertEquals("", it.getString(2))
            assertEquals("", it.getString(3))
        }
        db.query("SELECT de, es, pt, zh FROM kanji_solo WHERE kanji = '犬'").use {
            assertTrue(it.moveToFirst())
            assertEquals("", it.getString(0))
        }
        db.query("SELECT de, es, pt, zh FROM radicals WHERE radical = '大'").use {
            assertTrue(it.moveToFirst())
            assertEquals("", it.getString(0))
        }
        db.query("SELECT name_de, name_es, name_pt, name_zh FROM quiz WHERE _id = 1").use {
            assertTrue(it.moveToFirst())
            assertEquals("", it.getString(0))
            assertEquals("", it.getString(1))
            assertEquals("", it.getString(2))
            assertEquals("", it.getString(3))
        }
    }

    @Test
    fun migrate18To19() {
        // Create a v18 database with words and quiz entries that have empty translation columns
        migrationHelper.createDatabase(TEST_DB_NAME, 18).apply {
            // Word _id=1 (ア — "Asia") is known to be in the asset with all translations filled.
            // sentence_id=NULL to avoid FK constraint issues in the test environment.
            execSQL("""
                INSERT INTO words
                VALUES (1,'ア','Asia','Asie','a',0,0,0,0,2,-1,0,1,0,NULL,'','','','')
            """.trimIndent())
            // Quiz _id=1 is known to be in the asset with all name translations filled.
            execSQL("""
                INSERT INTO quiz
                VALUES (1,'Katakana - Vowels%ア　イ　ウ　エ　オ　ン',
                          'Katakana - Les voyelles%ア イ ウ エ オ ン',1,0,'','','','')
            """.trimIndent())
            close()
        }

        val db = migrationHelper.runMigrationsAndValidate(
            TEST_DB_NAME, 19, true, YomikataDatabase.createMigration18to19(context)
        )

        // Migration 18→19 is intentionally a no-op (just a version bump): translations are populated
        // by populateTranslationsIfNeeded in the onOpen callback, which runMigrationsAndValidate does
        // not invoke. Call it explicitly here to exercise the real population path.
        YomikataDatabase.populateTranslationsIfNeeded(context, db)

        // Verify that word translations were populated from the asset database
        db.query("SELECT german, spanish, portuguese, chinese FROM words WHERE _id = 1").use {
            assertTrue("word should be found", it.moveToFirst())
            // Asset has german="Asien", spanish="Asia", portuguese="Ásia", chinese="亚洲" for _id=1
            assertTrue("german should be populated after migration", it.getString(0).isNotEmpty())
            assertTrue("spanish should be populated after migration", it.getString(1).isNotEmpty())
            assertTrue("portuguese should be populated after migration", it.getString(2).isNotEmpty())
            assertTrue("chinese should be populated after migration", it.getString(3).isNotEmpty())
        }

        // Verify that quiz name translations were populated from the asset database
        db.query("SELECT name_de, name_es, name_pt, name_zh FROM quiz WHERE _id = 1").use {
            assertTrue("quiz row should be found", it.moveToFirst())
            // Asset has name_de="Katakana - Vokale%…", name_zh="片假名 - 元音%…" for _id=1
            assertTrue("name_de should be populated after migration", it.getString(0).isNotEmpty())
            assertTrue("name_es should be populated after migration", it.getString(1).isNotEmpty())
            assertTrue("name_pt should be populated after migration", it.getString(2).isNotEmpty())
            assertTrue("name_zh should be populated after migration", it.getString(3).isNotEmpty())
        }
    }

    @Test
    fun migrate19To20() {
        // v19 words: english contains POS prefix "(n)", "(v1,vt)", no POS ("eye")
        migrationHelper.createDatabase(TEST_DB_NAME, 19).apply {
            execSQL("""
                INSERT INTO words
                VALUES (1,'重力','(n) gravity;weight','(n) gravité;pesanteur','じゅうりょく',0,0,0,0,0,-1,0,4,0,NULL,'Schwerkraft','gravedad','gravidade','重力')
            """.trimIndent())
            execSQL("""
                INSERT INTO words
                VALUES (2,'炒る','(v5r,vt) to parch;to fry;to roast','(adj-na)(n) frire','いる',0,0,0,0,0,-1,0,4,0,NULL,'rösten','tostar','torrar','烘')
            """.trimIndent())
            execSQL("""
                INSERT INTO words
                VALUES (3,'目','eye','oeil (organe)','め',0,0,0,0,0,-1,0,2,0,NULL,'Auge','ojo','olho','眼')
            """.trimIndent())
            close()
        }

        val db = migrationHelper.runMigrationsAndValidate(
            TEST_DB_NAME, 20, true, YomikataDatabase.MIGRATION_19_20
        )

        // pos column must exist and be populated; english must be stripped of POS tokens;
        // french must have its leading POS groups stripped while keeping mid-string content parens
        db.query("SELECT _id, english, pos, french FROM words ORDER BY _id").use {
            assertTrue(it.moveToNext())
            assertEquals(1L, it.getLong(0))
            assertEquals("gravity;weight", it.getString(1))   // english POS stripped
            assertEquals("n", it.getString(2))                 // POS extracted
            assertEquals("gravité;pesanteur", it.getString(3)) // french leading "(n)" stripped

            assertTrue(it.moveToNext())
            assertEquals(2L, it.getLong(0))
            assertEquals("to parch;to fry;to roast", it.getString(1))
            val pos2 = it.getString(2)
            assertTrue("pos should contain v5r", pos2.contains("v5r"))
            assertTrue("pos should contain vt", pos2.contains("vt"))
            assertEquals("frire", it.getString(3))             // french "(adj-na)(n)" stripped

            assertTrue(it.moveToNext())
            assertEquals(3L, it.getLong(0))
            assertEquals("eye", it.getString(1))              // no POS to strip
            assertEquals("", it.getString(2))                 // no POS tokens
            assertEquals("oeil (organe)", it.getString(3))    // content paren preserved
        }
    }

    @Test
    fun migrate20To21() {
        // v20 word with pos already set (must not be overwritten)
        // v20 word with pos empty (must be left empty — populatePosIfNeeded runs in onOpen, not here)
        migrationHelper.createDatabase(TEST_DB_NAME, 20).apply {
            execSQL("""
                INSERT INTO words
                VALUES (1,'重力','gravity;weight','gravité','じゅうりょく',0,0,0,0,0,-1,0,4,0,NULL,'Schwerkraft','gravedad','gravidade','重力','n')
            """.trimIndent())
            execSQL("""
                INSERT INTO words
                VALUES (2,'目','eye','oeil','め',0,0,0,0,0,-1,0,2,0,NULL,'Auge','ojo','olho','眼','')
            """.trimIndent())
            close()
        }

        // validateDroppedTables = false: MIGRATION_20_21 creates the Room 2.7 internal table
        // room_table_modification_log, which the exported schema does not list, so strict
        // dropped-table validation would flag it as "unexpected" (false positive).
        val db = migrationHelper.runMigrationsAndValidate(
            TEST_DB_NAME, 21, false, YomikataDatabase.MIGRATION_20_21
        )

        // Existing pos must be preserved
        db.query("SELECT pos FROM words WHERE _id = 1").use {
            assertTrue(it.moveToFirst())
            assertEquals("n", it.getString(0))
        }
        // Empty pos stays empty after migration (populatePosIfNeeded runs in onOpen)
        db.query("SELECT pos FROM words WHERE _id = 2").use {
            assertTrue(it.moveToFirst())
            assertEquals("", it.getString(0))
        }
        // room_table_modification_log must exist (required by TriggerBasedInvalidationTracker in Room 2.7+)
        db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='room_table_modification_log'").use {
            assertTrue("room_table_modification_log table must exist after migration 20→21", it.moveToFirst())
        }
    }

    @Test
    fun migrate16To21() {
        // Simulates the production upgrade path: prod APK code 65 has DB v16.
        migrationHelper.createDatabase(TEST_DB_NAME, 16).apply {
            // Insert a word with POS prefix in english AND french (both should be stripped)
            execSQL("""
                INSERT INTO words VALUES
                (1,'走る','(v5r,vi) to run','(v5r,vi) courir','はしる',0,0,0,0,0,-1,0,2,0,NULL)
            """.trimIndent())
            // Insert the phantom word that must be deleted
            execSQL("""
                INSERT INTO words VALUES
                (3537,'','','','',0,0,0,0,0,-1,0,0,0,NULL)
            """.trimIndent())
            // Insert a word without POS prefix (french content paren must be preserved)
            execSQL("""
                INSERT INTO words VALUES
                (2,'目','eye','oeil (organe)','め',0,0,0,0,0,-1,0,2,0,NULL)
            """.trimIndent())
            // Insert the malformed-prefix word (missing ')') fixed explicitly by the migration
            execSQL("""
                INSERT INTO words VALUES
                (6526,'優勢','dominance','(adj-na,nà prédominance; ascendant; supériorité','ゆうせい',0,0,0,0,0,-1,0,4,0,NULL)
            """.trimIndent())
            // Word 4954 (先程) shipped with an empty english gloss — must be filled by the migration
            execSQL("""
                INSERT INTO words VALUES
                (4954,'先程','','tout à l''heure','さきほど',0,0,0,0,0,-1,0,4,0,NULL)
            """.trimIndent())
            close()
        }

        // validateDroppedTables = false: MIGRATION_16_21 creates the Room 2.7 internal table
        // room_table_modification_log (not listed in the exported schema), which strict
        // dropped-table validation would otherwise flag as "unexpected".
        val db = migrationHelper.runMigrationsAndValidate(
            TEST_DB_NAME, 21, false, YomikataDatabase.MIGRATION_16_21
        )

        // Phantom word must be gone
        db.query("SELECT COUNT(*) FROM words WHERE _id = 3537").use {
            assertTrue(it.moveToFirst())
            assertEquals(0, it.getInt(0))
        }
        // POS must be extracted and english cleaned; french POS prefix stripped too
        db.query("SELECT english, pos, french FROM words WHERE _id = 1").use {
            assertTrue(it.moveToFirst())
            assertEquals("to run", it.getString(0))
            assertEquals("v5r,vi", it.getString(1))
            assertEquals("courir", it.getString(2))           // french "(v5r,vi)" stripped
        }
        // Word without POS: english unchanged, pos empty, french content paren preserved
        db.query("SELECT english, pos, french FROM words WHERE _id = 2").use {
            assertTrue(it.moveToFirst())
            assertEquals("eye", it.getString(0))
            assertEquals("", it.getString(1))
            assertEquals("oeil (organe)", it.getString(2))    // mid-string paren kept
        }
        // Malformed french POS prefix (missing ')') must be fixed explicitly
        db.query("SELECT french FROM words WHERE _id = 6526").use {
            assertTrue(it.moveToFirst())
            assertEquals("à prédominance; ascendant; supériorité", it.getString(0))
        }
        // Word 4954's empty english gloss must be filled by the migration
        db.query("SELECT english FROM words WHERE _id = 4954").use {
            assertTrue(it.moveToFirst())
            assertEquals("a little while ago; just now", it.getString(0))
        }
        // Multilingual columns must exist (spot-check on words table)
        db.query("SELECT german, spanish, portuguese, chinese FROM words WHERE _id = 2").use {
            assertTrue(it.moveToFirst())
        }
        // room_table_modification_log must exist
        db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='room_table_modification_log'").use {
            assertTrue("room_table_modification_log must exist", it.moveToFirst())
        }
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
