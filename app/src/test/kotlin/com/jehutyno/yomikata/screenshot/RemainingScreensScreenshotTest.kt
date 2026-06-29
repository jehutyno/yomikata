package com.jehutyno.yomikata.screenshot

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.jehutyno.yomikata.model.Answer
import com.jehutyno.yomikata.model.KanjiSoloRadical
import com.jehutyno.yomikata.model.Quiz
import com.jehutyno.yomikata.model.Sentence
import com.jehutyno.yomikata.model.Word
import com.jehutyno.yomikata.ui.answers.AnswerReviewScreen
import com.jehutyno.yomikata.ui.answers.AnswerReviewUiState
import com.jehutyno.yomikata.ui.selections.SelectionsScreen
import com.jehutyno.yomikata.ui.selections.SelectionsUiState
import com.jehutyno.yomikata.ui.settings.SettingsScreen
import com.jehutyno.yomikata.ui.theme.YomikataTheme
import com.jehutyno.yomikata.ui.word.WordDetailScreen
import com.jehutyno.yomikata.ui.word.WordDetailUiState
import com.jehutyno.yomikata.ui.wordlist.WordListScreen
import com.jehutyno.yomikata.ui.wordlist.WordListUiState
import com.jehutyno.yomikata.util.quiz.Categories
import com.jehutyno.yomikata.util.quiz.QuizType
import com.jehutyno.yomikata.util.quiz.getLevelFromPoints
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Screenshot baselines for the remaining screens: Settings, Selections, WordList, WordDetail,
 * AnswerReview. Same approach and stack notes as [MainScreensScreenshotTest] / [MasteryBarScreenshotTest].
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-night-xxhdpi", application = Application::class)
class RemainingScreensScreenshotTest {

    private fun capture(name: String, content: @Composable () -> Unit) {
        captureRoboImage("src/test/screenshots/$name.png") {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                YomikataTheme {
                    Box(Modifier.size(411.dp, 891.dp)) { content() }
                }
            }
        }
    }

    // ── Fixtures ────────────────────────────────────────────────────────────────

    private fun word(id: Long, jp: String, en: String, fr: String, reading: String, points: Int) = Word(
        id, jp, en, fr, reading, getLevelFromPoints(points), 5, 3, 2, 0, 0, points, 7, 0, null,
    )

    private val listWords = listOf(
        word(1, "水", "water", "eau", "みず", 700),
        word(2, "火", "fire", "feu", "ひ", 50),
        word(3, "木", "tree", "arbre", "き", 250),
        word(4, "金", "gold; money", "or; argent", "きん", 450),
        word(5, "土", "earth", "terre", "つち", 0),
    )

    private fun wordListState() = WordListUiState(
        words = listWords, title = "JLPT N5%日本語", quizCount = listWords.size,
        masterCount = 1, highCount = 1, mediumCount = 1, lowCount = 2,
        selectedTab = 0, searchQuery = "", isGrid = false, selectedWordIds = setOf(1L, 3L),
    )

    private fun selectionsState() = SelectionsUiState(
        selections = listOf(
            Quiz(101, "JLPT N5 favoris", "JLPT N5 favoris", Categories.CATEGORY_SELECTIONS, false),
            Quiz(102, "Verbes du jour", "Verbes du jour", Categories.CATEGORY_SELECTIONS, true),
        ),
        wordCounts = mapOf(101L to 24, 102L to 12),
        quizCount = 36, goodCount = 20,
        selectedTypes = listOf(QuizType.TYPE_AUTO), lastMode = null,
    )

    private fun wordDetailState() = WordDetailUiState(
        words = listOf(
            Triple(
                word(1, "食べる", "to eat", "manger", "たべる", 250),
                emptyList<KanjiSoloRadical?>(),
                Sentence(jap = "毎日%食べる%のが好きです", en = "I like eating every day", fr = "J'aime manger tous les jours"),
            ),
        ),
        currentIndex = 0, isFavorite = true,
    )

    private fun answerReviewState() = AnswerReviewUiState(
        items = listOf(
            Triple(
                Answer(1, "たべる", 1L, 1L, QuizType.TYPE_JAP_EN),
                word(1, "食べる", "to eat", "manger", "たべる", 250),
                Sentence(jap = "毎日%食べる%のが好きです", en = "I like eating every day", fr = "J'aime manger tous les jours"),
            ),
            Triple(
                Answer(0, "みず", 2L, 2L, QuizType.TYPE_JAP_EN),
                word(2, "飲む", "to drink", "boire", "のむ", 50),
                Sentence(jap = "水を%飲む%", en = "to drink water", fr = "boire de l'eau"),
            ),
        ),
        selectionSheet = null,
    )

    @Composable private fun Settings() = SettingsScreen("2.0.0", {}, {}, {}, {})
    @Composable private fun Selections() =
        SelectionsScreen(selectionsState(), { _, _ -> }, {}, {}, {}, {}, {})
    @Composable private fun WordList() =
        WordListScreen(wordListState(), {}, {}, {}, {}, {}, {}, {})
    @Composable private fun WordDetail() =
        WordDetailScreen(wordDetailState(), "食べる", {}, {}, {}, {}, {}, {}, {}, {}, {}, {})
    @Composable private fun AnswerReview() =
        AnswerReviewScreen(answerReviewState(), "結果 · Results", {}, {}, {}, {}, {}, { _, _ -> }, {}, {})

    // ── EN ──────────────────────────────────────────────────────────────────────

    @Test fun settings_en() = capture("settings_en") { Settings() }
    @Test fun selections_en() = capture("selections_en") { Selections() }
    @Test fun wordlist_en() = capture("wordlist_en") { WordList() }
    @Test fun worddetail_en() = capture("worddetail_en") { WordDetail() }
    @Test fun answerreview_en() = capture("answerreview_en") { AnswerReview() }

    // ── DE ──────────────────────────────────────────────────────────────────────

    @Test fun settings_de() { RuntimeEnvironment.setQualifiers("+de"); capture("settings_de") { Settings() } }
    @Test fun selections_de() { RuntimeEnvironment.setQualifiers("+de"); capture("selections_de") { Selections() } }
    @Test fun wordlist_de() { RuntimeEnvironment.setQualifiers("+de"); capture("wordlist_de") { WordList() } }
    @Test fun worddetail_de() { RuntimeEnvironment.setQualifiers("+de"); capture("worddetail_de") { WordDetail() } }
    @Test fun answerreview_de() { RuntimeEnvironment.setQualifiers("+de"); capture("answerreview_de") { AnswerReview() } }

    // ── Marketing locales (FR/ES/PT/ZH) — Play Store listing screenshots ──────────

    private fun marketingLocale(lang: String) {
        RuntimeEnvironment.setQualifiers("+$lang")
        capture("selections_$lang") { Selections() }
        capture("wordlist_$lang") { WordList() }
        capture("worddetail_$lang") { WordDetail() }
    }

    @Test fun marketing_fr() = marketingLocale("fr")
    @Test fun marketing_es() = marketingLocale("es")
    @Test fun marketing_pt() = marketingLocale("pt")
    @Test fun marketing_zh() = marketingLocale("zh")
}
