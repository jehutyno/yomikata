package com.jehutyno.yomikata.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jehutyno.yomikata.model.Sentence
import com.jehutyno.yomikata.model.Word
import com.jehutyno.yomikata.ui.quiz.AnswerMode
import com.jehutyno.yomikata.ui.quiz.QcmOption
import com.jehutyno.yomikata.ui.quiz.QuizScreen
import com.jehutyno.yomikata.ui.quiz.QuizUiState
import com.jehutyno.yomikata.ui.quiz.SegmentState
import com.jehutyno.yomikata.ui.theme.AccentOrange
import com.jehutyno.yomikata.ui.theme.YomikataTheme
import com.jehutyno.yomikata.util.quiz.Level
import com.jehutyno.yomikata.util.quiz.QuizType
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose interaction test for [QuizScreen] in QCM mode (couche 4).
 *
 * Verifies the answering wiring: tapping a QCM option invokes onOptionClick with the option's
 * index. QCM labels are non-localized (kana), so selectors are locale-independent.
 */
@RunWith(AndroidJUnit4::class)
class QuizScreenInteractionTest {

    @get:Rule
    val rule = createComposeRule()

    private val word = Word(
        1L, "食べる", "to eat", "manger", "たべる", Level.LOW, 5, 3, 2, 0, 0, 0, 0, 0, null,
    )

    private fun qcmState() = QuizUiState(
        answerMode = AnswerMode.QCM,
        qcmOptions = listOf(
            QcmOption("たべる"), QcmOption("のむ"), QcmOption("かく"), QcmOption("みる"),
        ),
        segments = listOf(SegmentState.Current),
        words = listOf(Pair(word, QuizType.TYPE_JAP_EN)),
        sentence = Sentence(jap = "毎日%食べる%のが好きです", en = "I like eating every day", fr = "J'aime manger"),
        currentIndex = 0,
        isRevealed = false,
        wordHighlightColor = AccentOrange.toArgb(),
    )

    @Composable
    private fun Quiz(state: QuizUiState, onOptionClick: (Int) -> Unit) {
        QuizScreen(
            uiState = state,
            onClose = {}, onTtsSettings = {}, onDisplayAnswers = {}, onOptionClick = onOptionClick,
            onNextWord = {}, onFuriToggle = {}, onTradToggle = {}, onItemClick = {},
            onSelectionClick = {}, onReportClick = {}, onSentenceTts = {}, onSoundClick = {},
            onEditTextChange = {}, onEditBeforeTextChange = {}, onEditSubmit = {}, onEditAction = {},
        )
    }

    @Test
    fun tappingAQcmOptionInvokesOnOptionClickWithItsIndex() {
        var clickedIndex = -1
        rule.setContent { YomikataTheme { Quiz(qcmState()) { clickedIndex = it } } }

        rule.onNodeWithText("のむ").assertIsDisplayed()
        rule.onNodeWithText("のむ").performClick()
        assertEquals(1, clickedIndex)
    }

    @Test
    fun allFourOptionsAreDisplayed() {
        rule.setContent { YomikataTheme { Quiz(qcmState()) {} } }
        listOf("たべる", "のむ", "かく", "みる").forEach {
            rule.onNodeWithText(it).assertIsDisplayed()
        }
    }
}
