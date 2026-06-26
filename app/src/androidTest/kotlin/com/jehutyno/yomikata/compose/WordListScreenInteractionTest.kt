package com.jehutyno.yomikata.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jehutyno.yomikata.model.Word
import com.jehutyno.yomikata.ui.theme.YomikataTheme
import com.jehutyno.yomikata.ui.wordlist.WordListScreen
import com.jehutyno.yomikata.ui.wordlist.WordListUiState
import com.jehutyno.yomikata.util.quiz.getLevelFromPoints
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose interaction test for [WordListScreen] (couche 4). The kanji is a plain Compose Text and
 * the favorite is an IconButton with a non-localized contentDescription ("Favori"), so selectors
 * are locale-independent.
 */
@RunWith(AndroidJUnit4::class)
class WordListScreenInteractionTest {

    @get:Rule
    val rule = createComposeRule()

    private fun word(id: Long, jp: String, en: String, reading: String, points: Int) = Word(
        id, jp, en, en, reading, getLevelFromPoints(points), 5, 3, 2, 0, 0, points, 7, 0, null,
    )

    private val words = listOf(
        word(1, "水", "water", "みず", 700),
        word(2, "火", "fire", "ひ", 50),
        word(3, "木", "tree", "き", 250),
    )

    private fun state() = WordListUiState(
        words = words, title = "JLPT N5%日本語", quizCount = 3,
        masterCount = 1, highCount = 0, mediumCount = 1, lowCount = 1,
        selectedTab = 0, searchQuery = "", isGrid = false, selectedWordIds = emptySet(),
    )

    @Composable
    private fun WordList(
        onSearch: (String) -> Unit = {},
        onWord: (Word) -> Unit = {},
        onFav: (Word) -> Unit = {},
    ) {
        WordListScreen(state(), {}, {}, onSearch, {}, onWord, onFav, {})
    }

    @Test
    fun typingInTheSearchFieldInvokesOnSearchQueryChanged() {
        var query = ""
        rule.setContent { YomikataTheme { WordList(onSearch = { query = it }) } }
        rule.onNode(hasSetTextAction()).performTextInput("火")
        assertTrue("expected query to contain 火 but was '$query'", query.contains("火"))
    }

    @Test
    fun tappingAWordRowInvokesOnWordClick() {
        var clicked: Word? = null
        rule.setContent { YomikataTheme { WordList(onWord = { clicked = it }) } }
        rule.onNodeWithText("火").performClick()
        assertEquals(2L, clicked?.id)
    }

    @Test
    fun tappingTheFirstFavoriteInvokesOnFavoriteClick() {
        var favorited: Word? = null
        rule.setContent { YomikataTheme { WordList(onFav = { favorited = it }) } }
        rule.onAllNodesWithContentDescription("Favori").onFirst().performClick()
        assertEquals(1L, favorited?.id)
    }

    @Test
    fun wordsAreRendered() {
        rule.setContent { YomikataTheme { WordList() } }
        rule.onNodeWithText("水").assertIsDisplayed()
        rule.onNodeWithText("火").assertIsDisplayed()
    }
}
