package com.jehutyno.yomikata.util.quiz

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for the category → voice-pack-level mappings in Categories.kt.
 *
 * These pure functions drive the voice download card (which level/size/version to fetch), so a
 * wrong mapping would download the wrong pack or none at all.
 */
class CategoriesTest {

    @Test
    fun `getCategoryLevel maps each category to its voice-pack level`() {
        assertEquals(0, getCategoryLevel(Categories.CATEGORY_HIRAGANA))
        assertEquals(0, getCategoryLevel(Categories.CATEGORY_KATAKANA))
        assertEquals(1, getCategoryLevel(Categories.CATEGORY_KANJI))
        assertEquals(1, getCategoryLevel(Categories.CATEGORY_COUNTERS))
        assertEquals(2, getCategoryLevel(Categories.CATEGORY_JLPT_5))
        assertEquals(3, getCategoryLevel(Categories.CATEGORY_JLPT_4))
        assertEquals(4, getCategoryLevel(Categories.CATEGORY_JLPT_3))
        assertEquals(5, getCategoryLevel(Categories.CATEGORY_JLPT_2))
        assertEquals(6, getCategoryLevel(Categories.CATEGORY_JLPT_1))
    }

    @Test
    fun `getCategoryLevel falls back to 6 for unmapped categories`() {
        assertEquals(6, getCategoryLevel(Categories.CATEGORY_SELECTIONS))
        assertEquals(6, getCategoryLevel(999))
    }

    @Test
    fun `getLevelDownloadSize returns a size per level and 56 beyond the range`() {
        assertEquals(5, getLevelDownloadSize(0))
        assertEquals(6, getLevelDownloadSize(1))
        assertEquals(6, getLevelDownloadSize(2))
        assertEquals(7, getLevelDownloadSize(3))
        assertEquals(35, getLevelDownloadSize(4))
        assertEquals(41, getLevelDownloadSize(5))
        assertEquals(56, getLevelDownloadSize(6))
        assertEquals(56, getLevelDownloadSize(99))
    }

    @Test
    fun `getLevelDownloadVersion matches the per-level pack version`() {
        assertEquals(0, getLevelDownloadVersion(0))
        assertEquals(2, getLevelDownloadVersion(1))
        assertEquals(2, getLevelDownloadVersion(2))
        assertEquals(1, getLevelDownloadVersion(3))
        assertEquals(1, getLevelDownloadVersion(4))
        assertEquals(1, getLevelDownloadVersion(5))
        assertEquals(1, getLevelDownloadVersion(6))
        assertEquals(0, getLevelDownloadVersion(42))
    }

    @Test
    fun `getLevelDownloadUrl returns a download path for levels 0 to 6 and empty otherwise`() {
        for (level in 0..6) {
            assertTrue("level $level should map to a /download path",
                getLevelDownloadUrl(level).endsWith("/download"))
        }
        assertEquals("", getLevelDownloadUrl(7))
        assertEquals("", getLevelDownloadUrl(-1))
    }
}
