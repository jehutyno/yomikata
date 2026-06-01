package com.jehutyno.yomikata.screens.quizzes

import android.content.SharedPreferences
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.jehutyno.yomikata.presenters.WordCountInterface
import com.jehutyno.yomikata.repository.QuizRepository
import com.jehutyno.yomikata.repository.StatsRepository
import com.jehutyno.yomikata.util.Prefs
import com.jehutyno.yomikata.util.QuizType
import io.mockk.coJustRun
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.setMain
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for [QuizzesPresenter].
 *
 * Validates quiz-type selection logic (initQuizTypes, quizTypeSwitch) and
 * preference persistence for latest-category tracking.
 *
 * SharedPreferences is injected directly — no mockkStatic needed.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class QuizzesPresenterTest {

    /** Makes LiveData.setValue() work on the test thread. */
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var prefsMock: SharedPreferences
    private lateinit var editorMock: SharedPreferences.Editor
    private lateinit var quizRepoMock: QuizRepository
    private lateinit var statsRepoMock: StatsRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())

        prefsMock = mockk()
        editorMock = mockk(relaxed = true)
        every { prefsMock.edit() } returns editorMock
        every { editorMock.putString(any(), any()) } returns editorMock
        every { editorMock.putInt(any(), any()) } returns editorMock

        quizRepoMock = mockk(relaxed = true)
        every { quizRepoMock.getQuiz(any()) } returns flowOf(emptyList())

        statsRepoMock = mockk(relaxed = true)
        coJustRun { statsRepoMock.addStatEntry(any(), any(), any(), any()) }
    }

    /**
     * Creates a presenter with the given stub values for prefs.
     */
    private fun makePresenter(
        selectedTypesStr: String = "",
        wasSelectedTypesStr: String = "",
        latestCat1: Int = -1
    ): QuizzesPresenter {
        every { prefsMock.getString(Prefs.SELECTED_QUIZ_TYPES.pref, "") } returns selectedTypesStr
        every { prefsMock.getString(Prefs.WAS_SELECTED_QUIZ_TYPES.pref, "") } returns wasSelectedTypesStr
        every { prefsMock.getInt(Prefs.LATEST_CATEGORY_1.pref, -1) } returns latestCat1
        return QuizzesPresenter(
            prefsMock, quizRepoMock, statsRepoMock,
            mockk<WordCountInterface>(relaxed = true), 0
        )
    }

    // ─── initQuizTypes ──────────────────────────────────────────────────────

    @Test
    fun `initQuizTypes with empty prefs selects TYPE_AUTO by default`() {
        val p = makePresenter(selectedTypesStr = "")
        p.initQuizTypes()
        assertEquals(listOf(QuizType.TYPE_AUTO), p.selectedTypes.value)
    }

    @Test
    fun `initQuizTypes with saved single type restores it`() {
        val p = makePresenter(selectedTypesStr = "${QuizType.TYPE_JAP_EN.type},")
        p.initQuizTypes()
        assertEquals(listOf(QuizType.TYPE_JAP_EN), p.selectedTypes.value)
    }

    @Test
    fun `initQuizTypes with two saved types restores both`() {
        val p = makePresenter(selectedTypesStr = "${QuizType.TYPE_JAP_EN.type},${QuizType.TYPE_EN_JAP.type},")
        p.initQuizTypes()
        assertEquals(listOf(QuizType.TYPE_JAP_EN, QuizType.TYPE_EN_JAP), p.selectedTypes.value)
    }

    // ─── quizTypeSwitch: selecting non-auto types ────────────────────────────

    @Test
    fun `selecting nonAuto when AUTO is active removes AUTO and adds the type`() {
        val p = makePresenter(selectedTypesStr = "")
        p.initQuizTypes() // → TYPE_AUTO
        p.quizTypeSwitch(QuizType.TYPE_JAP_EN)
        val result = p.selectedTypes.value!!
        assertFalse("AUTO should be removed", result.contains(QuizType.TYPE_AUTO))
        assertTrue("JAP_EN should be added", result.contains(QuizType.TYPE_JAP_EN))
    }

    @Test
    fun `selecting a second nonAuto type appends it to the current selection`() {
        val p = makePresenter(selectedTypesStr = "${QuizType.TYPE_JAP_EN.type},")
        p.initQuizTypes()
        p.quizTypeSwitch(QuizType.TYPE_EN_JAP)
        val result = p.selectedTypes.value!!
        assertTrue(result.contains(QuizType.TYPE_JAP_EN))
        assertTrue(result.contains(QuizType.TYPE_EN_JAP))
        assertEquals(2, result.size)
    }

    @Test
    fun `deselecting a type that was selected removes it, keeps others`() {
        val p = makePresenter(selectedTypesStr = "${QuizType.TYPE_JAP_EN.type},${QuizType.TYPE_EN_JAP.type},")
        p.initQuizTypes()
        p.quizTypeSwitch(QuizType.TYPE_JAP_EN)
        val result = p.selectedTypes.value!!
        assertFalse(result.contains(QuizType.TYPE_JAP_EN))
        assertTrue(result.contains(QuizType.TYPE_EN_JAP))
        assertEquals(1, result.size)
    }

    @Test
    fun `deselecting the last nonAuto type falls back to TYPE_AUTO`() {
        val p = makePresenter(selectedTypesStr = "${QuizType.TYPE_JAP_EN.type},")
        p.initQuizTypes()
        p.quizTypeSwitch(QuizType.TYPE_JAP_EN)
        assertEquals(listOf(QuizType.TYPE_AUTO), p.selectedTypes.value)
    }

    @Test
    fun `deselecting last type saves the old selection to WAS_SELECTED before fallback`() {
        val p = makePresenter(selectedTypesStr = "${QuizType.TYPE_JAP_EN.type},")
        p.initQuizTypes()
        p.quizTypeSwitch(QuizType.TYPE_JAP_EN) // last type → fallback to AUTO
        verify { editorMock.putString(eq(Prefs.WAS_SELECTED_QUIZ_TYPES.pref), any()) }
    }

    // ─── quizTypeSwitch: AUTO ────────────────────────────────────────────────

    @Test
    fun `switching to AUTO when nonAuto active saves current types and selects AUTO`() {
        val p = makePresenter(selectedTypesStr = "${QuizType.TYPE_JAP_EN.type},")
        p.initQuizTypes()
        p.quizTypeSwitch(QuizType.TYPE_AUTO)
        assertEquals(listOf(QuizType.TYPE_AUTO), p.selectedTypes.value)
        verify { editorMock.putString(eq(Prefs.WAS_SELECTED_QUIZ_TYPES.pref), any()) }
    }

    @Test
    fun `unselecting AUTO restores types saved in WAS_SELECTED`() {
        val p = makePresenter(
            selectedTypesStr = "",
            wasSelectedTypesStr = "${QuizType.TYPE_JAP_EN.type},"
        )
        p.initQuizTypes() // → AUTO
        p.quizTypeSwitch(QuizType.TYPE_AUTO) // should restore WAS_SELECTED
        assertEquals(listOf(QuizType.TYPE_JAP_EN), p.selectedTypes.value)
    }

    @Test
    fun `unselecting AUTO with empty WAS_SELECTED falls back to TYPE_PRONUNCIATION`() {
        val p = makePresenter(selectedTypesStr = "", wasSelectedTypesStr = "")
        p.initQuizTypes() // → AUTO
        p.quizTypeSwitch(QuizType.TYPE_AUTO)
        assertEquals(listOf(QuizType.TYPE_PRONUNCIATION), p.selectedTypes.value)
    }

    // ─── prefs persistence ───────────────────────────────────────────────────

    @Test
    fun `quizTypeSwitch always saves updated selection to SELECTED_QUIZ_TYPES`() {
        val p = makePresenter(selectedTypesStr = "")
        p.initQuizTypes()
        p.quizTypeSwitch(QuizType.TYPE_JAP_EN)
        verify { editorMock.putString(eq(Prefs.SELECTED_QUIZ_TYPES.pref), any()) }
    }

    // ─── onLaunchQuizClick ───────────────────────────────────────────────────

    @Test
    fun `onLaunchQuizClick with new category pushes old cat1 to cat2 and saves new cat1`() =
        runBlocking {
            val p = makePresenter(latestCat1 = 3)
            p.onLaunchQuizClick(5)
            verify { editorMock.putInt(eq(Prefs.LATEST_CATEGORY_1.pref), eq(5)) }
            verify { editorMock.putInt(eq(Prefs.LATEST_CATEGORY_2.pref), eq(3)) }
        }

    @Test
    fun `onLaunchQuizClick with same category does not update prefs`() = runBlocking {
        val p = makePresenter(latestCat1 = 5)
        p.onLaunchQuizClick(5) // same → no-op
        verify(exactly = 0) { editorMock.putInt(eq(Prefs.LATEST_CATEGORY_1.pref), any()) }
        verify(exactly = 0) { editorMock.putInt(eq(Prefs.LATEST_CATEGORY_2.pref), any()) }
    }
}
