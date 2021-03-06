package com.jehutyno.yomikata.screens.quizzes

import android.content.Context
import android.util.Log
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
import org.jetbrains.anko.defaultSharedPreferences
import java.util.*

/**
 * Created by valentin on 29/09/2016.
 */
class QuizzesPresenter(
    private val context: Context,
    private val quizRepository: QuizRepository,
    private val statsRepository: StatsRepository,
    private val quizzesView: QuizzesContract.View) : QuizzesContract.Presenter {

    companion object : KLogging()

    init {
        quizzesView.setPresenter(this)
    }

    private lateinit var selectedTypes: ArrayList<Int>

    override fun start() {
        Log.i("YomikataZK", "Home Presenter start")
    }

    override fun loadQuizzes(category: Int) {
        quizRepository.getQuiz(category, object : QuizRepository.LoadQuizCallback {
            override fun onQuizLoaded(quizzes: List<Quiz>) {
                quizzesView.displayQuizzes(quizzes)
            }

            override fun onDataNotAvailable() {
                quizzesView.displayNoData()
            }

        })
    }

    override fun createQuiz(quizName: String) {
        quizRepository.saveQuiz(quizName, Categories.CATEGORY_SELECTIONS)
    }

    override fun updateQuizName(quizId: Long, quizName: String) {
        quizRepository.updateQuizName(quizId, quizName)
    }

    override fun updateQuizCheck(id: Long, checked: Boolean) {
        quizRepository.updateQuizSelected(id, checked)
    }

    override fun deleteQuiz(quizId: Long) {
        quizRepository.deleteQuiz(quizId)
    }

    override fun countQuiz(ids: LongArray): Int {
        return quizRepository.countWordsForQuizzes(ids)
    }

    override fun countLow(ids: LongArray): Int {
        return quizRepository.countWordsForLevel(ids, 0)
    }

    override fun countMedium(ids: LongArray): Int {
        return quizRepository.countWordsForLevel(ids, 1)
    }

    override fun countHigh(ids: LongArray): Int {
        return quizRepository.countWordsForLevel(ids, 2)
    }

    override fun countMaster(ids: LongArray): Int {
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
        val savedString = context.defaultSharedPreferences.getString(key, "")
        val savedList = ArrayList<Int>(5)
        if (savedString == "" && default != -1) {
            savedList.add(default)
        } else {
            val st = StringTokenizer(savedString, ",")
            for (i in 0..st.countTokens() - 1) {
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
        context.defaultSharedPreferences.edit().putString(key, str.toString()).apply()
    }

    override fun launchQuizClick(strategy: QuizStrategy, title: String, category: Int) {
        statsRepository.addStatEntry(StatAction.LAUNCH_QUIZ_FROM_CATEGORY, category.toLong(), Calendar.getInstance().timeInMillis, StatResult.OTHER)
        val cat1 = context.defaultSharedPreferences.getInt(Prefs.LATEST_CATEGORY_1.pref, -1)

        if (category != cat1) {
            context.defaultSharedPreferences.edit().putInt(Prefs.LATEST_CATEGORY_2.pref, cat1).apply()
            context.defaultSharedPreferences.edit().putInt(Prefs.LATEST_CATEGORY_1.pref, category).apply()
        }
        quizzesView.launchQuiz(strategy, selectedTypes.toIntArray(), title)
    }

    override fun getSelectedTypes(): IntArray {
        return selectedTypes.toIntArray()
    }
}