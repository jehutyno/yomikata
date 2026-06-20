package com.jehutyno.yomikata.screens.quizzes

import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.distinctUntilChanged
import com.jehutyno.yomikata.model.Quiz
import com.jehutyno.yomikata.model.StatAction
import com.jehutyno.yomikata.model.StatResult
import com.jehutyno.yomikata.presenters.WordCountInterface
import com.jehutyno.yomikata.repository.QuizRepository
import com.jehutyno.yomikata.repository.StatsRepository
import com.jehutyno.yomikata.util.quiz.Categories
import com.jehutyno.yomikata.util.Prefs
import com.jehutyno.yomikata.util.quiz.QuizType
import com.jehutyno.yomikata.util.quiz.QuizTypePrefs
import kotlinx.coroutines.flow.first
import mu.KLogging
import java.util.Calendar


/**
 * Created by valentin on 29/09/2016.
 */
class QuizzesPresenter(
    private val prefs: SharedPreferences,
    private val quizRepository: QuizRepository,
    private val statsRepository: StatsRepository,
    wordCountInterface: WordCountInterface,
    category: Int) : QuizzesContract.Presenter, WordCountInterface by wordCountInterface {

    companion object : KLogging()


    private var _selectedTypes = MutableLiveData<ArrayList<QuizType>>()
    /** the quizTypes that are selected by the user */
    override val selectedTypes: LiveData<ArrayList<QuizType>>
        get() = _selectedTypes

    // define LiveData
    // from Room
    override val quizList : LiveData<List<Quiz>> = quizRepository.getQuiz(category)
                                                                 .asLiveData().distinctUntilChanged()

    override fun start() {
        Log.i("YomikataZK", "Home Presenter start")
    }

    override suspend fun createQuiz(quizName: String) {
        quizRepository.saveQuiz(quizName, Categories.CATEGORY_SELECTIONS)
    }

    override suspend fun updateQuizName(quizId: Long, quizName: String) {
        quizRepository.updateQuizName(quizId, quizName)
    }

    override suspend fun updateQuizCheck(id: Long, checked: Boolean) {
        quizRepository.updateQuizSelected(id, checked)
    }

    override suspend fun deleteQuiz(quizId: Long) {
        quizRepository.deleteQuiz(quizId)
    }

    override suspend fun countQuiz(ids: LongArray): Int {
        return quizRepository.countWordsForQuizzes(ids).first()
    }

    override fun initQuizTypes() {
        _selectedTypes.value = QuizTypePrefs.loadSelectedTypes(prefs)
    }

    override fun quizTypeSwitch(quizType: QuizType) {
        val current = requireNotNull(selectedTypes.value) { "selectedTypes not initialized" }
        _selectedTypes.value = QuizTypePrefs.toggle(prefs, current, quizType)
    }

    /**
     * On launch quiz click
     *
     * Call this when a new quiz is launched to update the database stat entries, and the latest
     * category preferences.
     *
     * @param category Category of quiz that is being launched
     */
    override suspend fun onLaunchQuizClick(category: Int) {
        statsRepository.addStatEntry(StatAction.LAUNCH_QUIZ_FROM_CATEGORY, category.toLong(), Calendar.getInstance().timeInMillis, StatResult.OTHER)
        val pref = prefs
        val cat1 = pref.getInt(Prefs.LATEST_CATEGORY_1.pref, -1)

        if (category != cat1) {
            pref.edit().putInt(Prefs.LATEST_CATEGORY_2.pref, cat1).apply()
            pref.edit().putInt(Prefs.LATEST_CATEGORY_1.pref, category).apply()
        }
    }

    override fun getSelectedTypes(): ArrayList<QuizType> {
        return requireNotNull(selectedTypes.value) { "selectedTypes not initialized" }
    }
}
