package com.jehutyno.yomikata.model

import com.jehutyno.yomikata.util.language.AppLanguage
import com.jehutyno.yomikata.util.language.LanguageManager
import com.jehutyno.yomikata.util.quiz.getLevelFromPoints
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Verifies the getTrad() / getName() fallback chain for the four languages added in the
 * multi-language release (DE/ES/PT/ZH) across every translatable model: Word, Sentence,
 * KanjiSolo, Radical and Quiz.
 *
 * Two invariants per language:
 *  - when the localized column is populated, that translation is returned;
 *  - when it is empty, getTrad() falls back to the english column (so it is never blank).
 * FRENCH / ENGLISH are always present (no fallback).
 *
 * This guards the headline feature of the release at the model level — a regression here
 * (e.g. a missing `.ifEmpty { english }`) would silently blank out translations in the UI.
 */
class GetTradMultiLanguageTest {

    @After
    fun reset() {
        LanguageManager.current = AppLanguage.DEFAULT
    }

    // Each "full" fixture stores the language code itself as the translation, so the expected
    // value for language L is simply L's short code below.
    private val codes = mapOf(
        AppLanguage.GERMAN to "DE",
        AppLanguage.SPANISH to "ES",
        AppLanguage.PORTUGUESE to "PT",
        AppLanguage.CHINESE to "ZH",
    )

    // --- fixtures: fully translated vs. only english/french present -----------------------------

    private val wordFull = Word(1, "水", "EN", "FR", "みず",
        getLevelFromPoints(0), 0, 0, 0, 0, -1, 0, 3, 0, null,
        german = "DE", spanish = "ES", portuguese = "PT", chinese = "ZH")
    private val wordEmpty = Word(2, "水", "EN", "FR", "みず",
        getLevelFromPoints(0), 0, 0, 0, 0, -1, 0, 3, 0, null)

    private val sentenceFull = Sentence(1, "水", "EN", "FR", 0,
        de = "DE", es = "ES", pt = "PT", zh = "ZH")
    private val sentenceEmpty = Sentence(2, "水", "EN", "FR", 0)

    private val kanjiFull = KanjiSolo("水", 4, "EN", "FR", "みず", "スイ", "水",
        de = "DE", es = "ES", pt = "PT", zh = "ZH")
    private val kanjiEmpty = KanjiSolo("水", 4, "EN", "FR", "みず", "スイ", "水")

    private val radicalFull = Radical("水", 4, "みず", "EN", "FR",
        de = "DE", es = "ES", pt = "PT", zh = "ZH")
    private val radicalEmpty = Radical("水", 4, "みず", "EN", "FR")

    private val quizFull = Quiz(1, "EN", "FR", 7, false,
        nameDe = "DE", nameEs = "ES", namePt = "PT", nameZh = "ZH")
    private val quizEmpty = Quiz(2, "EN", "FR", 7, false)

    // --- tests ----------------------------------------------------------------------------------

    @Test
    fun `populated localized column is returned for each language`() {
        for ((lang, expected) in codes) {
            LanguageManager.current = lang
            assertEquals("Word $lang", expected, wordFull.getTrad())
            assertEquals("Sentence $lang", expected, sentenceFull.getTrad())
            assertEquals("KanjiSolo $lang", expected, kanjiFull.getTrad())
            assertEquals("Radical $lang", expected, radicalFull.getTrad())
            assertEquals("Quiz $lang", expected, quizFull.getName())
        }
    }

    @Test
    fun `empty localized column falls back to english for each language`() {
        for (lang in codes.keys) {
            LanguageManager.current = lang
            assertEquals("Word $lang", "EN", wordEmpty.getTrad())
            assertEquals("Sentence $lang", "EN", sentenceEmpty.getTrad())
            assertEquals("KanjiSolo $lang", "EN", kanjiEmpty.getTrad())
            assertEquals("Radical $lang", "EN", radicalEmpty.getTrad())
            assertEquals("Quiz $lang", "EN", quizEmpty.getName())
        }
    }

    @Test
    fun `french returns the french column`() {
        LanguageManager.current = AppLanguage.FRENCH
        assertEquals("FR", wordFull.getTrad())
        assertEquals("FR", sentenceFull.getTrad())
        assertEquals("FR", kanjiFull.getTrad())
        assertEquals("FR", radicalFull.getTrad())
        assertEquals("FR", quizFull.getName())
    }

    @Test
    fun `english returns the english column`() {
        LanguageManager.current = AppLanguage.ENGLISH
        assertEquals("EN", wordFull.getTrad())
        assertEquals("EN", sentenceFull.getTrad())
        assertEquals("EN", kanjiFull.getTrad())
        assertEquals("EN", radicalFull.getTrad())
        assertEquals("EN", quizFull.getName())
    }
}
