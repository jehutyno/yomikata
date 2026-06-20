package com.jehutyno.yomikata.util.quiz

import android.content.SharedPreferences
import com.jehutyno.yomikata.util.Prefs
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [QuizTypePrefs] — the stateless quiz-type selection logic extracted from
 * QuizzesPresenter. Mirrors the toggle cases of QuizzesPresenterTest but drives the helper
 * directly (no LiveData / InstantTaskExecutorRule needed).
 */
class QuizTypePrefsTest {

    private lateinit var prefsMock: SharedPreferences
    private lateinit var editorMock: SharedPreferences.Editor

    @Before
    fun setUp() {
        prefsMock = mockk()
        editorMock = mockk(relaxed = true)
        every { prefsMock.edit() } returns editorMock
        every { editorMock.putString(any(), any()) } returns editorMock
    }

    private fun stubPrefs(selected: String = "", wasSelected: String = "") {
        every { prefsMock.getString(Prefs.SELECTED_QUIZ_TYPES.pref, "") } returns selected
        every { prefsMock.getString(Prefs.WAS_SELECTED_QUIZ_TYPES.pref, "") } returns wasSelected
    }

    // ─── loadSelectedTypes ──────────────────────────────────────────────────

    @Test
    fun `loadSelectedTypes with empty prefs defaults to TYPE_AUTO`() {
        stubPrefs(selected = "")
        assertEquals(listOf(QuizType.TYPE_AUTO), QuizTypePrefs.loadSelectedTypes(prefsMock))
    }

    @Test
    fun `loadSelectedTypes restores saved types`() {
        stubPrefs(selected = "${QuizType.TYPE_JAP_EN.type},${QuizType.TYPE_EN_JAP.type},")
        assertEquals(
            listOf(QuizType.TYPE_JAP_EN, QuizType.TYPE_EN_JAP),
            QuizTypePrefs.loadSelectedTypes(prefsMock),
        )
    }

    // ─── toggle: concrete types ─────────────────────────────────────────────

    @Test
    fun `selecting nonAuto when AUTO is active removes AUTO and adds the type`() {
        stubPrefs()
        val result = QuizTypePrefs.toggle(prefsMock, arrayListOf(QuizType.TYPE_AUTO), QuizType.TYPE_JAP_EN)
        assertFalse(result.contains(QuizType.TYPE_AUTO))
        assertTrue(result.contains(QuizType.TYPE_JAP_EN))
        assertEquals(1, result.size)
    }

    @Test
    fun `selecting a second nonAuto type appends it`() {
        stubPrefs()
        val result = QuizTypePrefs.toggle(prefsMock, arrayListOf(QuizType.TYPE_JAP_EN), QuizType.TYPE_EN_JAP)
        assertTrue(result.contains(QuizType.TYPE_JAP_EN))
        assertTrue(result.contains(QuizType.TYPE_EN_JAP))
        assertEquals(2, result.size)
    }

    @Test
    fun `deselecting a type keeps the others`() {
        stubPrefs()
        val result = QuizTypePrefs.toggle(
            prefsMock,
            arrayListOf(QuizType.TYPE_JAP_EN, QuizType.TYPE_EN_JAP),
            QuizType.TYPE_JAP_EN,
        )
        assertFalse(result.contains(QuizType.TYPE_JAP_EN))
        assertTrue(result.contains(QuizType.TYPE_EN_JAP))
        assertEquals(1, result.size)
    }

    @Test
    fun `deselecting the last nonAuto type falls back to TYPE_AUTO and saves WAS_SELECTED`() {
        stubPrefs()
        val result = QuizTypePrefs.toggle(prefsMock, arrayListOf(QuizType.TYPE_JAP_EN), QuizType.TYPE_JAP_EN)
        assertEquals(listOf(QuizType.TYPE_AUTO), result)
        verify { editorMock.putString(eq(Prefs.WAS_SELECTED_QUIZ_TYPES.pref), any()) }
    }

    // ─── toggle: AUTO ───────────────────────────────────────────────────────

    @Test
    fun `switching to AUTO saves current types and selects AUTO`() {
        stubPrefs()
        val result = QuizTypePrefs.toggle(prefsMock, arrayListOf(QuizType.TYPE_JAP_EN), QuizType.TYPE_AUTO)
        assertEquals(listOf(QuizType.TYPE_AUTO), result)
        verify { editorMock.putString(eq(Prefs.WAS_SELECTED_QUIZ_TYPES.pref), any()) }
    }

    @Test
    fun `unselecting AUTO restores WAS_SELECTED`() {
        stubPrefs(wasSelected = "${QuizType.TYPE_JAP_EN.type},")
        val result = QuizTypePrefs.toggle(prefsMock, arrayListOf(QuizType.TYPE_AUTO), QuizType.TYPE_AUTO)
        assertEquals(listOf(QuizType.TYPE_JAP_EN), result)
    }

    @Test
    fun `unselecting AUTO with empty WAS_SELECTED falls back to TYPE_PRONUNCIATION`() {
        stubPrefs(wasSelected = "")
        val result = QuizTypePrefs.toggle(prefsMock, arrayListOf(QuizType.TYPE_AUTO), QuizType.TYPE_AUTO)
        assertEquals(listOf(QuizType.TYPE_PRONUNCIATION), result)
    }

    // ─── persistence ────────────────────────────────────────────────────────

    @Test
    fun `toggle always persists to SELECTED_QUIZ_TYPES`() {
        stubPrefs()
        QuizTypePrefs.toggle(prefsMock, arrayListOf(QuizType.TYPE_AUTO), QuizType.TYPE_JAP_EN)
        verify { editorMock.putString(eq(Prefs.SELECTED_QUIZ_TYPES.pref), any()) }
    }
}
