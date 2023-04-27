package com.jehutyno.yomikata.screens.quizzes

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.preference.PreferenceManager
import com.jehutyno.yomikata.model.Quiz
import com.jehutyno.yomikata.model.StatAction
import com.jehutyno.yomikata.model.StatResult
import com.jehutyno.yomikata.repository.QuizRepository
import com.jehutyno.yomikata.repository.StatsRepository
import com.jehutyno.yomikata.util.Categories
import com.jehutyno.yomikata.util.Prefs
import com.jehutyno.yomikata.util.QuizStrategy
import com.jehutyno.yomikata.util.QuizType
import mu.KLogging
import java.util.*


/**
 * Created by valentin on 29/09/2016.
 */
class QuizzesPresenter(
    private val context: Context,
    private val quizRepository: QuizRepository,
    private val statsRepository: StatsRepository,
    private val quizzesView: QuizzesContract.View,
    category: Int) : QuizzesContract.Presenter {

    companion object : KLogging()

    init {
        quizzesView.setPresenter(this)
    }

    private lateinit var selectedTypes: ArrayList<Int>

    // define LiveData
    // from Room
    override val quizList : LiveData<List<Quiz>> = quizRepository.getQuiz(category).asLiveData()

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
        return quizRepository.countWordsForQuizzes(ids)
    }

    override suspend fun countLow(ids: LongArray): Int {
        return quizRepository.countWordsForLevel(ids, 0)
    }

    override suspend fun countMedium(ids: LongArray): Int {
        return quizRepository.countWordsForLevel(ids, 1)
    }

    override suspend fun countHigh(ids: LongArray): Int {
        return quizRepository.countWordsForLevel(ids, 2)
    }

    override suspend fun countMaster(ids: LongArray): Int {
        return quizRepository.countWordsForLevel(ids, 3) + quizRepository.countWordsForLevel(ids, 4)
    }

    override fun initQuizTypes() {
        selectedTypes = getIntArrayFromPrefs(Prefs.SELECTED_QUIZ_TYPES.pref, QuizType.TYPE_AUTO.type)
        selectTypes()
    }

    private fun switchOthers(type: Int) {
        if (!selectedTypes.contains(type)) {
            if (selectedTypes.contains(QuizType.TYPE_AUTO.type))
                selectedTypes.remove(QuizType.TYPE_AUTO.type)
            selectedTypes.add(type)
        } else {
            selectedTypes.remove(type)
            if (selectedTypes.size == 0)
                selectedTypes.add(QuizType.TYPE_AUTO.type)
        }
    }

    override fun pronunciationQcmSwitch() {
        switchOthers(QuizType.TYPE_PRONUNCIATION_QCM.type)
        saveIntArrayInPrefs(Prefs.SELECTED_QUIZ_TYPES.pref, selectedTypes)
        selectTypes()
    }

    override fun pronunciationSwitch() {
        switchOthers(QuizType.TYPE_PRONUNCIATION.type)
        saveIntArrayInPrefs(Prefs.SELECTED_QUIZ_TYPES.pref, selectedTypes)
        selectTypes()
    }

    override fun audioSwitch() {
        switchOthers(QuizType.TYPE_AUDIO.type)
        saveIntArrayInPrefs(Prefs.SELECTED_QUIZ_TYPES.pref, selectedTypes)
        selectTypes()
    }

    override fun enJapSwitch() {
        switchOthers(QuizType.TYPE_EN_JAP.type)
        saveIntArrayInPrefs(Prefs.SELECTED_QUIZ_TYPES.pref, selectedTypes)
        selectTypes()
    }

    override fun japEnSwitch() {
        switchOthers(QuizType.TYPE_JAP_EN.type)
        saveIntArrayInPrefs(Prefs.SELECTED_QUIZ_TYPES.pref, selectedTypes)
        selectTypes()
    }

    override fun autoSwitch() {
        if (selectedTypes.contains(QuizType.TYPE_AUTO.type)) {
            selectedTypes.clear()
            selectedTypes = getIntArrayFromPrefs(Prefs.WAS_SELECTED_QUIZ_TYPES.pref, QuizType.TYPE_PRONUNCIATION.type)
        } else {
            saveIntArrayInPrefs(Prefs.WAS_SELECTED_QUIZ_TYPES.pref, selectedTypes)
            selectedTypes.clear()
            selectedTypes.add(QuizType.TYPE_AUTO.type)
        }
        saveIntArrayInPrefs(Prefs.SELECTED_QUIZ_TYPES.pref, selectedTypes)
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
                QuizType.TYPE_AUTO.type -> quizzesView.selectAuto(true)
                QuizType.TYPE_PRONUNCIATION.type -> quizzesView.selectPronunciation(true)
                QuizType.TYPE_PRONUNCIATION_QCM.type -> quizzesView.selectPronunciationQcm(true)
                QuizType.TYPE_AUDIO.type -> quizzesView.selectAudio(true)
                QuizType.TYPE_EN_JAP.type -> quizzesView.selectEnJap(true)
                QuizType.TYPE_JAP_EN.type -> quizzesView.selectJapEn(true)

            }
        }
    }


    private fun getIntArrayFromPrefs(key: String): ArrayList<Int> {
        return getIntArrayFromPrefs(key, -1)
    }

    private fun getIntArrayFromPrefs(key: String, default: Int): ArrayList<Int> {
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        val savedString = pref.getString(key, "")
        val savedList = ArrayList<Int>(5)
        if (savedString == "" && default != -1) {
            savedList.add(default)
        } else {
            val st = StringTokenizer(savedString, ",")
            for (i in 0 until st.countTokens()) {
                savedList.add(Integer.parseInt(st.nextToken()))
            }
        }

        return savedList
    }

    private fun saveIntArrayInPrefs(key: String, list: ArrayList<Int>) {
        val str = StringBuilder()
        list.forEach {
            str.append(it).append(",")
        }
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        pref.edit().putString(key, str.toString()).apply()
    }

    override suspend fun launchQuizClick(strategy: QuizStrategy, title: String, category: Int) {
        statsRepository.addStatEntry(StatAction.LAUNCH_QUIZ_FROM_CATEGORY, category.toLong(), Calendar.getInstance().timeInMillis, StatResult.OTHER)
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        val cat1 = pref.getInt(Prefs.LATEST_CATEGORY_1.pref, -1)

        if (category != cat1) {
            pref.edit().putInt(Prefs.LATEST_CATEGORY_2.pref, cat1).apply()
            pref.edit().putInt(Prefs.LATEST_CATEGORY_1.pref, category).apply()
        }
        quizzesView.launchQuiz(strategy, selectedTypes.toIntArray(), title)
    }

    override fun getSelectedTypes(): IntArray {
        return selectedTypes.toIntArray()
    }
}
