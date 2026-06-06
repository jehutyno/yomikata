package com.jehutyno.yomikata.util

import android.content.SharedPreferences
import com.jehutyno.yomikata.model.KanjiSolo
import com.jehutyno.yomikata.model.Quiz
import com.jehutyno.yomikata.model.Radical
import com.jehutyno.yomikata.model.Sentence
import com.jehutyno.yomikata.model.Word
import com.jehutyno.yomikata.util.getLevelFromPoints
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Tests for [LanguageManager], [AppLanguage], and the getTrad() / getName() methods
 * in model classes that depend on [LanguageManager.current].
 */
class LanguageManagerTest {

    private lateinit var prefsMock: SharedPreferences
    private lateinit var editorMock: SharedPreferences.Editor

    @Before
    fun setUp() {
        prefsMock = mockk()
        editorMock = mockk(relaxed = true)
        every { prefsMock.edit() } returns editorMock
        every { editorMock.putString(any(), any()) } returns editorMock
    }

    @After
    fun tearDown() {
        // Reset to default so other tests are not affected
        LanguageManager.current = AppLanguage.DEFAULT
    }

    // ─── AppLanguage ────────────────────────────────────────────────────────

    @Test fun `fromIsoCode returns ENGLISH for en`() =
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromIsoCode("en"))

    @Test fun `fromIsoCode returns FRENCH for fr`() =
        assertEquals(AppLanguage.FRENCH, AppLanguage.fromIsoCode("fr"))

    @Test fun `fromIsoCode returns GERMAN for de`() =
        assertEquals(AppLanguage.GERMAN, AppLanguage.fromIsoCode("de"))

    @Test fun `fromIsoCode returns SPANISH for es`() =
        assertEquals(AppLanguage.SPANISH, AppLanguage.fromIsoCode("es"))

    @Test fun `fromIsoCode returns PORTUGUESE for pt`() =
        assertEquals(AppLanguage.PORTUGUESE, AppLanguage.fromIsoCode("pt"))

    @Test fun `fromIsoCode returns CHINESE for zh`() =
        assertEquals(AppLanguage.CHINESE, AppLanguage.fromIsoCode("zh"))

    @Test fun `fromIsoCode returns DEFAULT for unknown code`() =
        assertEquals(AppLanguage.DEFAULT, AppLanguage.fromIsoCode("xx"))

    @Test fun `fromIsoCode returns DEFAULT for empty string`() =
        assertEquals(AppLanguage.DEFAULT, AppLanguage.fromIsoCode(""))

    // ─── LanguageManager.initFromPrefs ──────────────────────────────────────

    @Test fun `initFromPrefs restores saved language from prefs`() {
        every { prefsMock.getString(Prefs.APP_LANGUAGE.pref, null) } returns "fr"
        LanguageManager.initFromPrefs(prefsMock)
        assertEquals(AppLanguage.FRENCH, LanguageManager.current)
    }

    @Test fun `initFromPrefs falls back to DEFAULT for unknown saved code`() {
        every { prefsMock.getString(Prefs.APP_LANGUAGE.pref, null) } returns "xx"
        LanguageManager.initFromPrefs(prefsMock)
        assertEquals(AppLanguage.DEFAULT, LanguageManager.current)
    }

    @Test fun `initFromPrefs does not persist detected locale on first launch`() {
        every { prefsMock.getString(Prefs.APP_LANGUAGE.pref, null) } returns null
        LanguageManager.initFromPrefs(prefsMock)
        // Auto-detected locale must NOT be saved — device language changes must be picked up on restart
        verify(exactly = 0) { editorMock.putString(eq(Prefs.APP_LANGUAGE.pref), any()) }
    }

    // ─── LanguageManager.setLanguage ────────────────────────────────────────

    @Test fun `setLanguage updates current`() {
        every { prefsMock.getString(any(), any()) } returns "en"
        val manager = LanguageManager(prefsMock)
        manager.setLanguage(AppLanguage.FRENCH)
        assertEquals(AppLanguage.FRENCH, LanguageManager.current)
    }

    @Test fun `setLanguage persists new language to prefs`() {
        every { prefsMock.getString(any(), any()) } returns "en"
        val manager = LanguageManager(prefsMock)
        manager.setLanguage(AppLanguage.GERMAN)
        verify { editorMock.putString(eq(Prefs.APP_LANGUAGE.pref), eq("de")) }
    }

    // ─── Model getTrad() / getName() ────────────────────────────────────────

    private fun makeWord(en: String, fr: String) = Word(
        1L, "日本語", en, fr, "にほんご",
        getLevelFromPoints(0), 0, 0, 0, 0, -1, 0, 3, 0, null
    )

    @Test fun `Word getTrad returns french when language is FRENCH`() {
        LanguageManager.current = AppLanguage.FRENCH
        assertEquals("bonjour", makeWord("hello", "bonjour").getTrad())
    }

    @Test fun `Word getTrad returns english when language is ENGLISH`() {
        LanguageManager.current = AppLanguage.ENGLISH
        assertEquals("hello", makeWord("hello", "bonjour").getTrad())
    }

    @Test fun `Word getTrad falls back to english for unsupported language`() {
        LanguageManager.current = AppLanguage.GERMAN   // no german column yet in Phase 3a
        assertEquals("hello", makeWord("hello", "bonjour").getTrad())
    }

    @Test fun `Sentence getTrad returns correct language`() {
        LanguageManager.current = AppLanguage.FRENCH
        val sentence = Sentence(1L, "こんにちは", "hello", "bonjour", 0)
        assertEquals("bonjour", sentence.getTrad())
    }

    @Test fun `KanjiSolo getTrad returns correct language`() {
        LanguageManager.current = AppLanguage.FRENCH
        val kanji = KanjiSolo("日", 4, "sun; day", "soleil; jour", "ひ", "にち", "日")
        assertEquals("soleil; jour", kanji.getTrad())
    }

    @Test fun `Radical getTrad returns correct language`() {
        LanguageManager.current = AppLanguage.FRENCH
        val radical = Radical("日", 4, "ひ", "sun", "soleil")
        assertEquals("soleil", radical.getTrad())
    }

    @Test fun `Quiz getName returns correct language`() {
        LanguageManager.current = AppLanguage.FRENCH
        val quiz = Quiz(1L, "JLPT N1", "JLPT N1", 3, false)
        // nameEn == nameFr here, but we test the branch
        LanguageManager.current = AppLanguage.ENGLISH
        assertEquals("JLPT N1", quiz.getName())
        LanguageManager.current = AppLanguage.FRENCH
        assertEquals("JLPT N1", quiz.getName())
    }
}
