package com.jehutyno.yomikata.compose

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jehutyno.yomikata.model.Quiz
import com.jehutyno.yomikata.ui.selections.SelectionsScreen
import com.jehutyno.yomikata.ui.selections.SelectionsUiState
import com.jehutyno.yomikata.ui.theme.YomikataTheme
import com.jehutyno.yomikata.util.quiz.Categories
import com.jehutyno.yomikata.util.quiz.QuizType
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose interaction test for [SelectionsScreen] (couche 4). Selection names are non-localized
 * fixtures, so selectors are locale-independent.
 */
@RunWith(AndroidJUnit4::class)
class SelectionsScreenInteractionTest {

    @get:Rule
    val rule = createComposeRule()

    private val selections = listOf(
        Quiz(101, "JLPT N5 favoris", "JLPT N5 favoris", Categories.CATEGORY_SELECTIONS, false),
        Quiz(102, "Verbes du jour", "Verbes du jour", Categories.CATEGORY_SELECTIONS, true),
    )

    private fun state() = SelectionsUiState(
        selections = selections, wordCounts = mapOf(101L to 24, 102L to 12),
        quizCount = 36, goodCount = 20, selectedTypes = listOf(QuizType.TYPE_AUTO), lastMode = null,
    )

    @Test
    fun tappingASelectionInvokesOnSelectionClick() {
        var clicked: Quiz? = null
        rule.setContent {
            YomikataTheme {
                SelectionsScreen(state(), { _, _ -> }, { clicked = it }, {}, {}, {}, {})
            }
        }
        rule.onNodeWithText("JLPT N5 favoris").performClick()
        assertEquals(101L, clicked?.id)
    }

    @Test
    fun togglingTheFirstCheckboxInvokesOnSelectionChecked() {
        var checkedId = -1L
        var checkedValue: Boolean? = null
        rule.setContent {
            YomikataTheme {
                SelectionsScreen(state(), { id, c -> checkedId = id; checkedValue = c }, {}, {}, {}, {}, {})
            }
        }
        // First toggleable node = first selection's checkbox (the "create" card is not toggleable).
        rule.onAllNodes(isToggleable()).onFirst().performClick()
        assertEquals(101L, checkedId)
        assertEquals(true, checkedValue)
    }

    @Test
    fun bothSelectionsAreRendered() {
        rule.setContent { YomikataTheme { SelectionsScreen(state(), { _, _ -> }, {}, {}, {}, {}, {}) } }
        rule.onNodeWithText("JLPT N5 favoris").assertIsDisplayed()
        rule.onNodeWithText("Verbes du jour").assertIsDisplayed()
    }
}
