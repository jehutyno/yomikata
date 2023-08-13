package com.jehutyno.yomikata.screens.quiz

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.jehutyno.yomikata.model.Word
import com.jehutyno.yomikata.presenters.source.SelectionsPresenter
import com.jehutyno.yomikata.presenters.source.WordInQuizPresenter
import com.jehutyno.yomikata.repository.SentenceRepository
import com.jehutyno.yomikata.repository.StatsRepository
import com.jehutyno.yomikata.repository.WordRepository
import com.jehutyno.yomikata.repository.database.RoomWords
import com.jehutyno.yomikata.util.Prefs
import com.jehutyno.yomikata.util.QuizStrategy
import com.jehutyno.yomikata.util.QuizType
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.Random


// indices for choosing from multiple choice
private const val CORRECT_CHOICE = 1
private const val WRONG_CHOICE = 2

@RunWith(Parameterized::class)
class QuizPresenterTest(private val length: Int, private val type: QuizType) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data() = listOf(
            arrayOf(5, QuizType.TYPE_JAP_EN),
            arrayOf(10, QuizType.TYPE_JAP_EN),
            arrayOf(100, QuizType.TYPE_EN_JAP),
            arrayOf(1000, QuizType.TYPE_AUDIO),
            arrayOf(-1, QuizType.TYPE_PRONUNCIATION_QCM)
        )
    }

    private lateinit var wordRepoMock: WordRepository
    private lateinit var sentenceRepoMock: SentenceRepository
    private lateinit var statsRepoMock: StatsRepository
    private lateinit var quizViewMock: QuizContract.View
    private lateinit var selectionsMock: SelectionsPresenter
    private lateinit var wordInQuizMock: WordInQuizPresenter
    private lateinit var scope: CoroutineScope
    private lateinit var quizPresenter: QuizPresenter

    private fun createWord(id: Long, correct: Boolean = true): Word {
        val str = if (correct) "correct" else "wrong"
        return RoomWords(id, str, str, str, str, 0, 0, 0,
            0, 0, 0, 0, 0, 0, null).toWord()
    }

    private val maxWordsIndex = 500L
    private val words = (0..maxWordsIndex).map { createWord(it, true) }.toList()
    /** used for other, incorrect options in a quiz */
    private val otherWords = (1..3).map { createWord(maxWordsIndex + it, false) }
                                         .toList() as ArrayList

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setUp() {
        // mocks for repositories
        wordRepoMock = mockk(relaxed = true)
        sentenceRepoMock = mockk(relaxed = true)
        statsRepoMock = mockk(relaxed = true)
        quizViewMock = mockk(relaxed = true)
        selectionsMock = mockk(relaxed = true)
        wordInQuizMock = mockk(relaxed = true)

        every {
            quizViewMock.showAlertNonProgressiveSessionEnd(any())
        } answers { runBlocking {
            quizPresenter.onContinueAfterNonProgressiveSessionEnd()
        }}

        every { wordRepoMock.getWordsByLevel(any(), any()) } returns flow {
            emit(
                words
            )
        }
        coEvery { wordRepoMock.getRandomWords(any(), any(), any(), any(), any()) } returns otherWords

        // you cannot use the default Dispatchers.Main in a unit test,
        // create a testDispatcher instead
        val testScheduler = TestCoroutineScheduler()
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)
        scope = MainScope()

        // mock for preferences
        val sharedPrefMock = mockk<SharedPreferences>()
        every { sharedPrefMock.getBoolean("play_start", any()) } returns false
        every { sharedPrefMock.getBoolean("play_end", any()) } returns false
        every { sharedPrefMock.getString("length", any()) } returns length.toString()
        every { sharedPrefMock.getString("speed", any()) } returns "2"
        every { sharedPrefMock.getBoolean(Prefs.FURI_DISPLAYED.pref, any()) } returns false

        val context = mockk<Context>(relaxed = true)
        every { PreferenceManager.getDefaultSharedPreferences(context) } returns sharedPrefMock

        // always insert correct answer into option 1 (index 0)
        val rngMock = mockk<Random>()
        every { rngMock.nextInt(4) } returns 0

        quizPresenter = QuizPresenter(context, wordRepoMock, sentenceRepoMock,
            statsRepoMock, quizViewMock, longArrayOf(), QuizStrategy.STRAIGHT, null,
            arrayListOf(type), rngMock, selectionsMock, wordInQuizMock, scope
        )
    }

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun init_and_answer() = runBlocking {
        quizPresenter.run {
            initQuiz()
            onOptionClick(CORRECT_CHOICE)
        }
        assert ( !quizPresenter.previousAnswerWrong() )
    }

    @Test
    fun session_end_no_errors() = runBlocking {
        if (length >= words.size) {
            // quiz would end before session, not part of test case
            return@runBlocking
        }
        quizPresenter.initQuiz()
        (0 until length).forEach { _ ->
            quizPresenter.onOptionClick(CORRECT_CHOICE)
            quizPresenter.onNextWord()
        }
        // should be called if finite session, otherwise not called
        val sessionEndCallTimes = if (length == -1) 0 else 1
        verify(exactly = sessionEndCallTimes) {
            quizViewMock.showAlertNonProgressiveSessionEnd(false)
        }
    }

    @Test
    fun session_end_with_errors() = runBlocking {
        if (length >= words.size) {
            // quiz would end before session, not part of test case
            return@runBlocking
        }
        quizPresenter.initQuiz()
        (0 until length).forEach { _ ->
            quizPresenter.onOptionClick(WRONG_CHOICE)
            quizPresenter.onNextWord()
        }
        // should be called if finite session, otherwise not called
        val sessionEndCallTimes = if (length == -1) 0 else 1
        verify(exactly = sessionEndCallTimes) {
            quizViewMock.showAlertNonProgressiveSessionEnd(true)
        }
    }

    @Test
    fun quiz_end_no_errors() = runBlocking {
        quizPresenter.initQuiz()
        words.indices.forEach { _ ->
            quizPresenter.onOptionClick(CORRECT_CHOICE)
            quizPresenter.onNextWord()
        }
        verify(exactly = 1) { quizViewMock.showAlertQuizEnd(false) }
    }

    @Test
    fun quiz_end_with_errors() = runBlocking {
        quizPresenter.initQuiz()
        words.indices.forEach { _ ->
            quizPresenter.onOptionClick(WRONG_CHOICE)
            quizPresenter.onNextWord()
        }
        verify(exactly = 1) { quizViewMock.showAlertQuizEnd(true) }
    }

}
