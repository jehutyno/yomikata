package com.jehutyno.yomikata.screens.quizzes

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
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
import com.jehutyno.yomikata.util.Prefs
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
        val types = getQuizTypeArrayFromPrefs(Prefs.SELECTED_QUIZ_TYPES.pref, QuizType.TYPE_AUTO)
        _selectedTypes.value = types
    }

    @Synchronized
    private fun switchOthers(type: QuizType) {
        val newSelectedTypes = ArrayList(selectedTypes.value!!) // makes copy
        selectedTypes.value!!.also { types ->
            if (!types.contains(type)) {
                if (types.contains(QuizType.TYPE_AUTO)) {
                    // auto cannot be selected simultaneously with other types -> unselect it
                    newSelectedTypes.remove(QuizType.TYPE_AUTO)
                }
                newSelectedTypes.add(type)
            } else {
                newSelectedTypes.remove(type)
                if (newSelectedTypes.size == 0) {
                    // if no types are selected -> automatically select the auto type
                    saveQuizTypeArrayInPrefs(Prefs.WAS_SELECTED_QUIZ_TYPES.pref, types)
                    newSelectedTypes.add(QuizType.TYPE_AUTO)
                }
            }
        }
        _selectedTypes.value = newSelectedTypes
    }

    @Synchronized
    private fun switchAuto() {
        val newSelectedTypes =
            selectedTypes.value!!.let { types ->
                if (types.contains(QuizType.TYPE_AUTO)) {
                    // if auto is unselected -> get saved selection from preferences
                    getQuizTypeArrayFromPrefs(Prefs.WAS_SELECTED_QUIZ_TYPES.pref, QuizType.TYPE_PRONUNCIATION)
                } else {
                    // if auto is selected -> save the current preferences to be restored later
                    saveQuizTypeArrayInPrefs(Prefs.WAS_SELECTED_QUIZ_TYPES.pref, types)
                    arrayListOf(QuizType.TYPE_AUTO)
                }
            }
        _selectedTypes.value = newSelectedTypes
    }

    override fun quizTypeSwitch(quizType: QuizType) {
        if (quizType == QuizType.TYPE_AUTO) {
            switchAuto()
        } else {
            switchOthers(quizType)
        }
        // save new selection in preferences
        saveQuizTypeArrayInPrefs(Prefs.SELECTED_QUIZ_TYPES.pref, selectedTypes.value!!)
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
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        val cat1 = pref.getInt(Prefs.LATEST_CATEGORY_1.pref, -1)

        if (category != cat1) {
            pref.edit().putInt(Prefs.LATEST_CATEGORY_2.pref, cat1).apply()
            pref.edit().putInt(Prefs.LATEST_CATEGORY_1.pref, category).apply()
        }
    }

    override fun getSelectedTypes(): ArrayList<QuizType> {
        return selectedTypes.value!!
    }
}
