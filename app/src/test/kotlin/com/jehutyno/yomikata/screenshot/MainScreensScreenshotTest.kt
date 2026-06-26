package com.jehutyno.yomikata.screenshot

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.jehutyno.yomikata.model.Quiz
import com.jehutyno.yomikata.model.Sentence
import com.jehutyno.yomikata.model.Word
import com.jehutyno.yomikata.ui.home.HomeScreen
import com.jehutyno.yomikata.ui.home.HomeUiState
import com.jehutyno.yomikata.ui.quiz.AnswerButtonState
import com.jehutyno.yomikata.ui.quiz.AnswerMode
import com.jehutyno.yomikata.ui.quiz.QcmOption
import com.jehutyno.yomikata.ui.quiz.QuizScreen
import com.jehutyno.yomikata.ui.quiz.QuizUiState
import com.jehutyno.yomikata.ui.quiz.SegmentState
import com.jehutyno.yomikata.ui.study.StudyScreen
import com.jehutyno.yomikata.ui.study.StudyUiState
import com.jehutyno.yomikata.ui.theme.AccentOrange
import com.jehutyno.yomikata.ui.theme.Correct
import com.jehutyno.yomikata.ui.theme.YomikataTheme
import com.jehutyno.yomikata.util.quiz.Categories
import com.jehutyno.yomikata.util.quiz.Level
import com.jehutyno.yomikata.util.quiz.QuizType
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Screenshot regression baselines for the priority screens: Home, Study, Quiz.
 *
 * Each screen is rendered from a fixed [*UiState] fixture (no DB/network) and captured on the JVM.
 * The German variants exercise the UI-string i18n axis via RuntimeEnvironment.setQualifiers("+de"),
 * catching truncated/blank translated strings before release. See [MasteryBarScreenshotTest] for the
 * stack-specific Robolectric/Roborazzi configuration notes.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-night-xxhdpi", application = Application::class)
class MainScreensScreenshotTest {

    private fun capture(name: String, content: @Composable () -> Unit) {
        captureRoboImage("src/test/screenshots/$name.png") {
            // LocalInspectionMode = true freezes infinite animations (KenBurns zoom, hero photo
            // cycling) so Roborazzi does not spin advancing the clock forever.
            CompositionLocalProvider(LocalInspectionMode provides true) {
                YomikataTheme {
                    // Bound the capture to a fixed phone viewport. Screens use fillMaxSize and some
                    // contain a LazyColumn/weight that needs a bounded parent height — without this
                    // the content host measures unbounded and the bitmap blows the heap (Study OOM).
                    Box(Modifier.size(411.dp, 891.dp)) { content() }
                }
            }
        }
    }

    // ── Fixtures ────────────────────────────────────────────────────────────────

    private fun homeState() = HomeUiState(
        quizLaunched = 12, wordsSeen = 340, correctAnswers = 280, wrongAnswers = 60,
        lastSessionLevel = "JLPT N5", newsText = "Yomikata Z 2.0 — 6 langues !",
        newsLoading = false, sponsorAvailable = true, sponsorUrl = "https://example.org",
    )

    private fun studyState() = StudyUiState(
        selectedCategory = Categories.CATEGORY_HIRAGANA,
        quizzes = listOf(
            Quiz(1, "Vowels%あ い う え お", "Voyelles%あ い う え お", Categories.CATEGORY_HIRAGANA, false),
            Quiz(2, "K line%か き く け こ", "Ligne K%か き く け こ", Categories.CATEGORY_HIRAGANA, true),
            Quiz(3, "S line%さ し す せ そ", "Ligne S%さ し す せ そ", Categories.CATEGORY_HIRAGANA, false),
        ),
        quizCount = 150, goodCount = 92, wrongCount = 58,
        selectedTypes = listOf(QuizType.TYPE_AUTO), lastMode = null,
        voicesDownloaded = false, voiceSizeMb = 5, voiceDownloadProgress = null,
    )

    private val quizWord = Word(
        1L, "食べる", "to eat", "manger", "たべる", Level.LOW, 5, 3, 2, 0, 0, 0, 0, 0, null,
    )
    private val quizSentence = Sentence(
        jap = "毎日%食べる%のが好きです", en = "I like eating every day", fr = "J'aime manger tous les jours",
    )

    private fun quizState(revealed: Boolean, correct: Boolean) = QuizUiState(
        title = "Quiz",
        segments = listOf(
            SegmentState.Correct, SegmentState.Wrong, SegmentState.Current,
            SegmentState.Pending, SegmentState.Pending,
        ),
        answerMode = AnswerMode.QCM,
        qcmOptions = listOf(
            QcmOption("たべる", buttonState = if (correct) AnswerButtonState.Correct else AnswerButtonState.Default),
            QcmOption("のむ", buttonState = AnswerButtonState.Default),
            QcmOption("かく", buttonState = AnswerButtonState.Default),
            QcmOption("みる", buttonState = AnswerButtonState.Default),
        ),
        hintText = "Give hiragana reading",
        isRevealed = revealed,
        currentIndex = 0,
        words = listOf(Pair(quizWord, QuizType.TYPE_JAP_EN)),
        sentence = quizSentence,
        wordHighlightColor = if (correct) Correct.toArgb() else AccentOrange.toArgb(),
    )

    @Composable
    private fun QuizFixture(state: QuizUiState) {
        QuizScreen(
            uiState = state,
            onClose = {}, onTtsSettings = {}, onDisplayAnswers = {}, onOptionClick = {},
            onNextWord = {}, onFuriToggle = {}, onTradToggle = {}, onItemClick = {},
            onSelectionClick = {}, onReportClick = {}, onSentenceTts = {}, onSoundClick = {},
            onEditTextChange = {}, onEditBeforeTextChange = {}, onEditSubmit = {}, onEditAction = {},
        )
    }

    // ── Home ────────────────────────────────────────────────────────────────────

    @Test
    fun home_en() = capture("home_en") { HomeScreen(homeState(), {}, {}) }

    @Test
    fun home_de() {
        RuntimeEnvironment.setQualifiers("+de")
        capture("home_de") { HomeScreen(homeState(), {}, {}) }
    }

    // ── Study ───────────────────────────────────────────────────────────────────

    @Test
    fun study_en() = capture("study_en") {
        StudyScreen(studyState(), {}, { _, _ -> }, {}, {}, {}, {})
    }

    @Test
    fun study_de() {
        RuntimeEnvironment.setQualifiers("+de")
        capture("study_de") { StudyScreen(studyState(), {}, { _, _ -> }, {}, {}, {}, {}) }
    }

    // ── Quiz ────────────────────────────────────────────────────────────────────

    @Test
    fun quiz_qcm_before_en() = capture("quiz_qcm_before_en") {
        QuizFixture(quizState(revealed = false, correct = false))
    }

    @Test
    fun quiz_qcm_correct_en() = capture("quiz_qcm_correct_en") {
        QuizFixture(quizState(revealed = true, correct = true))
    }

    @Test
    fun quiz_qcm_before_de() {
        RuntimeEnvironment.setQualifiers("+de")
        capture("quiz_qcm_before_de") { QuizFixture(quizState(revealed = false, correct = false)) }
    }
}
