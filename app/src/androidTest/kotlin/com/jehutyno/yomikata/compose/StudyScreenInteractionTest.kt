package com.jehutyno.yomikata.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jehutyno.yomikata.model.Quiz
import com.jehutyno.yomikata.ui.study.StudyScreen
import com.jehutyno.yomikata.ui.study.StudyUiState
import com.jehutyno.yomikata.ui.theme.YomikataTheme
import com.jehutyno.yomikata.util.quiz.Categories
import com.jehutyno.yomikata.util.quiz.QuizType
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose interaction test for [StudyScreen] (couche 4).
 *
 * Level-chip labels (あ/ア/漢/数/N5…) and the fixture quiz names are non-localized, so selectors are
 * locale-independent. The content is rendered under LocalInspectionMode = true to freeze the
 * infinite KenBurns hero animation (so the test clock can go idle).
 */
@RunWith(AndroidJUnit4::class)
class StudyScreenInteractionTest {

    @get:Rule
    val rule = createComposeRule()

    private fun state() = StudyUiState(
        selectedCategory = Categories.CATEGORY_HIRAGANA,
        quizzes = listOf(
            Quiz(1, "Vowels%あ い う え お", "Voyelles%あ い う え お", Categories.CATEGORY_HIRAGANA, false),
            Quiz(2, "K line%か き く け こ", "Ligne K%か き く け こ", Categories.CATEGORY_HIRAGANA, false),
        ),
        quizCount = 100, goodCount = 60, wrongCount = 40,
        selectedTypes = listOf(QuizType.TYPE_AUTO), lastMode = null,
        voicesDownloaded = false, voiceSizeMb = 5, voiceDownloadProgress = null,
    )

    @Composable
    private fun Study(onCat: (Int) -> Unit = {}, onQuiz: (Quiz) -> Unit = {}) {
        CompositionLocalProvider(LocalInspectionMode provides true) {
            YomikataTheme {
                StudyScreen(state(), onCat, { _, _ -> }, onQuiz, {}, {}, {})
            }
        }
    }

    @Test
    fun tappingALevelChipInvokesOnCategorySelected() {
        var category = -99
        rule.setContent { Study(onCat = { category = it }) }
        rule.onNodeWithText("N3").performScrollTo().performClick()
        assertEquals(Categories.CATEGORY_JLPT_3, category)
    }

    @Test
    fun tappingAQuizCardInvokesOnQuizClick() {
        var clicked: Quiz? = null
        rule.setContent { Study(onQuiz = { clicked = it }) }
        rule.onNodeWithText("K line").performClick()
        assertEquals(2L, clicked?.id)
    }

    @Test
    fun quizNamesAreRendered() {
        rule.setContent { Study() }
        rule.onNodeWithText("Vowels").assertIsDisplayed()
        rule.onNodeWithText("K line").assertIsDisplayed()
    }
}
