package com.jehutyno.yomikata.screens.quizzes

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.preference.PreferenceManager
import com.jehutyno.yomikata.model.Quiz
import com.jehutyno.yomikata.model.StatAction
import com.jehutyno.yomikata.model.StatResult
import com.jehutyno.yomikata.presenters.WordCountInterface
import com.jehutyno.yomikata.repository.QuizRepository
import com.jehutyno.yomikata.repository.StatsRepository
import com.jehutyno.yomikata.util.Categories
import com.jehutyno.yomikata.util.Level
import com.jehutyno.yomikata.util.Prefs
import com.jehutyno.yomikata.util.QuizStrategy
import com.jehutyno.yomikata.util.QuizType
import com.jehutyno.yomikata.util.toQuizType
import kotlinx.coroutines.flow.first
import mu.KLogging
import java.util.Calendar
import java.util.StringTokenizer


/**
 * Created by valentin on 29/09/2016.
 */
class QuizzesPresenter(
    private val context: Context,
    private val quizRepository: QuizRepository,
    private val statsRepository: StatsRepository,
    private val quizzesView: QuizzesContract.View,
    wordCountInterface: WordCountInterface,
    category: Int) : QuizzesContract.Presenter, WordCountInterface by wordCountInterface {

    companion object : KLogging()


    private lateinit var selectedTypes: ArrayList<QuizType>

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
        selectedTypes = getQuizTypeArrayFromPrefs(Prefs.SELECTED_QUIZ_TYPES.pref, QuizType.TYPE_AUTO)
        selectTypes()
    }

    private fun switchOthers(type: QuizType) {
        if (!selectedTypes.contains(type)) {
            if (selectedTypes.contains(QuizType.TYPE_AUTO))
                selectedTypes.remove(QuizType.TYPE_AUTO)
            selectedTypes.add(type)
        } else {
            selectedTypes.remove(type)
            if (selectedTypes.size == 0)
                selectedTypes.add(QuizType.TYPE_AUTO)
        }
    }

    override fun pronunciationQcmSwitch() {
        switchOthers(QuizType.TYPE_PRONUNCIATION_QCM)
        saveQuizTypeArrayInPrefs(Prefs.SELECTED_QUIZ_TYPES.pref, selectedTypes)
        selectTypes()
    }

    override fun pronunciationSwitch() {
        switchOthers(QuizType.TYPE_PRONUNCIATION)
        saveQuizTypeArrayInPrefs(Prefs.SELECTED_QUIZ_TYPES.pref, selectedTypes)
        selectTypes()
    }

    override fun audioSwitch() {
        switchOthers(QuizType.TYPE_AUDIO)
        saveQuizTypeArrayInPrefs(Prefs.SELECTED_QUIZ_TYPES.pref, selectedTypes)
        selectTypes()
    }

    override fun enJapSwitch() {
        switchOthers(QuizType.TYPE_EN_JAP)
        saveQuizTypeArrayInPrefs(Prefs.SELECTED_QUIZ_TYPES.pref, selectedTypes)
        selectTypes()
    }

    override fun japEnSwitch() {
        switchOthers(QuizType.TYPE_JAP_EN)
        saveQuizTypeArrayInPrefs(Prefs.SELECTED_QUIZ_TYPES.pref, selectedTypes)
        selectTypes()
    }

    override fun autoSwitch() {
        if (selectedTypes.contains(QuizType.TYPE_AUTO)) {
            selectedTypes.clear()
            selectedTypes = getQuizTypeArrayFromPrefs(Prefs.WAS_SELECTED_QUIZ_TYPES.pref, QuizType.TYPE_PRONUNCIATION)
        } else {
            saveQuizTypeArrayInPrefs(Prefs.WAS_SELECTED_QUIZ_TYPES.pref, selectedTypes)
            selectedTypes.clear()
            selectedTypes.add(QuizType.TYPE_AUTO)
        }
        saveQuizTypeArrayInPrefs(Prefs.SELECTED_QUIZ_TYPES.pref, selectedTypes)
        selectTypes()
    }

    private fun selectTypes() {
        quizzesView.selectAuto(false)
        quizzesView.selectPronunciation(false)
        quizzesView.selectPronunciationQcm(false)
        quizzesView.selectAudio(false)
        quizzesView.selectEnJap(false)
        quizzesView.selectJapEn(false)
        selectedTypes.forEach {
            when (it) {
                QuizType.TYPE_AUTO -> quizzesView.selectAuto(true)
                QuizType.TYPE_PRONUNCIATION -> quizzesView.selectPronunciation(true)
                QuizType.TYPE_PRONUNCIATION_QCM -> quizzesView.selectPronunciationQcm(true)
                QuizType.TYPE_AUDIO -> quizzesView.selectAudio(true)
                QuizType.TYPE_EN_JAP -> quizzesView.selectEnJap(true)
                QuizType.TYPE_JAP_EN -> quizzesView.selectJapEn(true)

            }
        }
    }

    private fun getQuizTypeArrayFromPrefs(key: String, default: QuizType): ArrayList<QuizType> {
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        val savedString = pref.getString(key, "")
        val savedList = ArrayList<Int>(5)
        if (savedString == "") {
            savedList.add(default.type)
        } else {
            val st = StringTokenizer(savedString, ",")
            for (i in 0 until st.countTokens()) {
                savedList.add(Integer.parseInt(st.nextToken()))
            }
        }

        return savedList.map { it.toQuizType() } as ArrayList<QuizType>
    }

    private fun saveQuizTypeArrayInPrefs(key: String, types: ArrayList<QuizType>) {
        val list = types.map { it.type }
        val str = StringBuilder()
        list.forEach {
            str.append(it).append(",")
        }
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        pref.edit().putString(key, str.toString()).apply()
    }

    override suspend fun launchQuizClick(strategy: QuizStrategy, level: Level?, title: String, category: Int) {
        statsRepository.addStatEntry(StatAction.LAUNCH_QUIZ_FROM_CATEGORY, category.toLong(), Calendar.getInstance().timeInMillis, StatResult.OTHER)
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        val cat1 = pref.getInt(Prefs.LATEST_CATEGORY_1.pref, -1)

        if (category != cat1) {
            pref.edit().putInt(Prefs.LATEST_CATEGORY_2.pref, cat1).apply()
            pref.edit().putInt(Prefs.LATEST_CATEGORY_1.pref, category).apply()
        }
        quizzesView.launchQuiz(strategy, level, selectedTypes, title)
    }

    override fun getSelectedTypes(): ArrayList<QuizType> {
        return selectedTypes
    }
}
