package com.jehutyno.yomikata.repository.database

import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream


/**
 * Integrity test for the bundled asset database `yomikataz.db`.
 *
 * Unlike [RoomMigrationTest] (which validates schema transitions on synthetic data), this test
 * opens the **real shipped asset** read-only and asserts the invariants that the manual multi-language
 * audit used to check by hand:
 *  - schema version / sqlite integrity,
 *  - 100 % translation coverage (no empty localized columns in words / sentences / quiz),
 *  - no `?` (0x3F) encoding corruption in CJK columns (the documented PowerShell pitfall),
 *  - Room identity hash matches the exported schema (`app/schemas/.../21.json`), else the app
 *    crashes at startup with "Migration Error",
 *  - the `pos` column is populated and french translations no longer carry leading POS prefixes.
 *
 * This turns a tedious 4-language × 7 500-word manual review into a single green/red assertion,
 * and is the most important release gate for the multi-language data.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class DatabaseIntegrityTest {

    private companion object {
        const val ASSET_NAME = "yomikataz.db"
        const val EXPECTED_USER_VERSION = 21
        // identityHash from app/schemas/<db>/21.json — must match room_master_table in the asset
        const val EXPECTED_IDENTITY_HASH = "0cfad3fce2dbe5205b3b7894ddef7dbf"
        // Documented coverage floors (CLAUDE.md): 7 503 words, ~7 425 sentences, 96 quiz.
        // Floors (not exact) so legitimate data edits don't false-fail; the zero-empty checks
        // below are the real guarantee of 100 % coverage.
        const val MIN_WORDS = 7_000
        const val MIN_SENTENCES = 7_000
        const val MIN_QUIZ = 90
    }

    private lateinit var db: SQLiteDatabase
    private lateinit var tempFile: File

    @Before
    fun openAsset() {
        // The asset ships inside the app under test (targetContext), not the test apk.
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        tempFile = File(context.cacheDir, "integrity_check_$ASSET_NAME")
        context.assets.open(ASSET_NAME).use { input ->
            FileOutputStream(tempFile).use { output -> input.copyTo(output) }
        }
        db = SQLiteDatabase.openDatabase(tempFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
    }

    @After
    fun closeAsset() {
        if (this::db.isInitialized) db.close()
        if (this::tempFile.isInitialized) tempFile.delete()
    }

    // ---- helpers -------------------------------------------------------------------------------

    private fun queryInt(sql: String): Int =
        db.rawQuery(sql, null).use { if (it.moveToFirst()) it.getInt(0) else -1 }

    private fun queryString(sql: String): String =
        db.rawQuery(sql, null).use { if (it.moveToFirst()) it.getString(0) ?: "" else "" }

    /** Count of rows where any of [columns] is NULL or empty string. */
    private fun countEmpty(table: String, columns: List<String>): Int {
        val where = columns.joinToString(" OR ") { "$it IS NULL OR $it = ''" }
        return queryInt("SELECT COUNT(*) FROM $table WHERE $where")
    }

    /** Count of rows where [column] contains the [pattern] (literal '?' is not a LIKE wildcard). */
    private fun countLike(table: String, column: String, pattern: String): Int =
        queryInt("SELECT COUNT(*) FROM $table WHERE $column LIKE '$pattern'")

    // ---- schema & integrity --------------------------------------------------------------------

    @Test
    fun userVersionIs21() {
        assertEquals("asset PRAGMA user_version must match DATABASE_VERSION",
            EXPECTED_USER_VERSION, queryInt("PRAGMA user_version"))
    }

    @Test
    fun integrityCheckOk() {
        assertEquals("PRAGMA integrity_check must be ok",
            "ok", queryString("PRAGMA integrity_check"))
    }

    @Test
    fun identityHashMatchesExportedSchema() {
        // room_master_table holds the identity hash Room compares at startup; a mismatch
        // crashes the app with "Migration Error". Keep it in sync with schemas/21.json.
        val hash = queryString("SELECT identity_hash FROM room_master_table WHERE id = 42")
        assertEquals("room_master_table identity_hash must match schemas/21.json identityHash",
            EXPECTED_IDENTITY_HASH, hash)
    }

    // ---- coverage: row counts ------------------------------------------------------------------

    @Test
    fun rowCountsAboveFloor() {
        assertTrue("words count below floor", queryInt("SELECT COUNT(*) FROM words") >= MIN_WORDS)
        assertTrue("sentences count below floor", queryInt("SELECT COUNT(*) FROM sentences") >= MIN_SENTENCES)
        assertTrue("quiz count below floor", queryInt("SELECT COUNT(*) FROM quiz") >= MIN_QUIZ)
    }

    // ---- coverage: no empty translations -------------------------------------------------------

    @Test
    fun everyWordHasAllTranslations() {
        // Core fields + the 4 added languages (full names in `words`). english/french assumed present.
        val empty = countEmpty("words",
            listOf("japanese", "reading", "english", "french",
                   "german", "spanish", "portuguese", "chinese"))
        assertEquals("words with an empty core/translation column", 0, empty)
    }

    @Test
    fun everySentenceHasAllTranslations() {
        // sentences use ISO codes: en/fr/de/es/pt/zh + japanese in `jap`.
        val empty = countEmpty("sentences",
            listOf("jap", "en", "fr", "de", "es", "pt", "zh"))
        assertEquals("sentences with an empty translation column", 0, empty)
    }

    @Test
    fun everyQuizHasAllNameTranslations() {
        val empty = countEmpty("quiz",
            listOf("name_en", "name_fr", "name_de", "name_es", "name_pt", "name_zh"))
        assertEquals("quiz with an empty localized name", 0, empty)
    }

    // ---- encoding corruption -------------------------------------------------------------------

    @Test
    fun noEncodingCorruptionInCjkColumns() {
        // A '?' (0x3F) standing in for a CJK glyph means lost bytes (the documented PowerShell pipe
        // pitfall). The signal differs by column type:
        //  - word glosses / quiz names are short and never legitimately contain ASCII '?' → any '?'.
        //  - full sentences DO legitimately end with a half-width '?' (e.g. 你想一起去吗?), so only a
        //    RUN of '??' (multiple glyphs collapsed) indicates real corruption there.
        val violations = buildList {
            countLike("words", "japanese", "%?%").let { if (it > 0) add("words.japanese: $it") }
            countLike("words", "chinese", "%?%").let { if (it > 0) add("words.chinese: $it") }
            countLike("quiz", "name_zh", "%?%").let { if (it > 0) add("quiz.name_zh: $it") }
            countLike("sentences", "jap", "%??%").let { if (it > 0) add("sentences.jap (??): $it") }
            countLike("sentences", "zh", "%??%").let { if (it > 0) add("sentences.zh (??): $it") }
        }
        assertTrue("CJK columns show 0x3F corruption -> $violations", violations.isEmpty())
    }

    // ---- POS column ----------------------------------------------------------------------------

    @Test
    fun posColumnIsPopulated() {
        // The bulk of words carry a JMdict POS tag; assert a healthy majority is non-empty
        // (some entries legitimately have no POS).
        val total = queryInt("SELECT COUNT(*) FROM words")
        val withPos = queryInt("SELECT COUNT(*) FROM words WHERE pos IS NOT NULL AND pos != ''")
        assertTrue("pos column looks unpopulated ($withPos / $total)", withPos > total / 2)
    }

    @Test
    fun frenchHasNoLeadingPosPrefix() {
        // After the fr-pos-cleanup session, no french translation should start with a POS-only
        // paren group like "(n)", "(adj-na)", "(v5r)". Spot-check the most common tokens.
        val leadingPos = queryInt("""
            SELECT COUNT(*) FROM words WHERE
                french LIKE '(n)%' OR french LIKE '(adj-%' OR french LIKE '(adv%' OR
                french LIKE '(v1%' OR french LIKE '(v5%' OR french LIKE '(vs%' OR
                french LIKE '(vt%' OR french LIKE '(vi%' OR french LIKE '(exp%' OR
                french LIKE '(pn%' OR french LIKE '(n,%' OR french LIKE '(n-%'
        """.trimIndent())
        assertEquals("french translations still carry a leading POS prefix", 0, leadingPos)
    }
}
