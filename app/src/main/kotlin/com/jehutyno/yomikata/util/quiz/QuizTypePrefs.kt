package com.jehutyno.yomikata.util.quiz

import android.content.SharedPreferences
import com.jehutyno.yomikata.util.Prefs
import java.util.StringTokenizer


/**
 * Stateless helper carrying the quiz-type selection logic shared between the Study screen
 * ([com.jehutyno.yomikata.screens.quizzes.QuizzesFragment]) and [com.jehutyno.yomikata.screens.quizzes.QuizzesPresenter].
 *
 * Selection is persisted as a CSV of [QuizType.type] ints under [Prefs.SELECTED_QUIZ_TYPES]
 * (format kept identical to legacy data). [QuizType.TYPE_AUTO] is mutually exclusive with the
 * concrete types: when AUTO is enabled the prior selection is saved to [Prefs.WAS_SELECTED_QUIZ_TYPES]
 * so it can be restored when AUTO is turned off again.
 */
object QuizTypePrefs {

    /** Current selection, defaulting to [QuizType.TYPE_AUTO] when nothing is stored. */
    fun loadSelectedTypes(prefs: SharedPreferences): ArrayList<QuizType> =
        getQuizTypeArrayFromPrefs(prefs, Prefs.SELECTED_QUIZ_TYPES.pref, QuizType.TYPE_AUTO)

    /**
     * Toggle [tapped] in [current], applying AUTO mutual-exclusion, persist the result in
     * [Prefs.SELECTED_QUIZ_TYPES] and return the new selection. Never returns an empty list.
     */
    fun toggle(
        prefs: SharedPreferences,
        current: ArrayList<QuizType>,
        tapped: QuizType,
    ): ArrayList<QuizType> {
        val next: ArrayList<QuizType> = if (tapped == QuizType.TYPE_AUTO) {
            if (current.contains(QuizType.TYPE_AUTO)) {
                // AUTO unselected -> restore the previously saved selection
                getQuizTypeArrayFromPrefs(prefs, Prefs.WAS_SELECTED_QUIZ_TYPES.pref, QuizType.TYPE_PRONUNCIATION)
            } else {
                // AUTO selected -> save current selection to restore it later
                saveQuizTypeArrayInPrefs(prefs, Prefs.WAS_SELECTED_QUIZ_TYPES.pref, current)
                arrayListOf(QuizType.TYPE_AUTO)
            }
        } else {
            val copy = ArrayList(current)
            if (!current.contains(tapped)) {
                // AUTO cannot coexist with concrete types -> unselect it
                copy.remove(QuizType.TYPE_AUTO)
                copy.add(tapped)
            } else {
                copy.remove(tapped)
                if (copy.isEmpty()) {
                    // no concrete type left -> fall back to AUTO
                    saveQuizTypeArrayInPrefs(prefs, Prefs.WAS_SELECTED_QUIZ_TYPES.pref, current)
                    copy.add(QuizType.TYPE_AUTO)
                }
            }
            copy
        }
        saveQuizTypeArrayInPrefs(prefs, Prefs.SELECTED_QUIZ_TYPES.pref, next)
        return next
    }

    private fun getQuizTypeArrayFromPrefs(
        prefs: SharedPreferences,
        key: String,
        default: QuizType,
    ): ArrayList<QuizType> {
        val savedString = prefs.getString(key, "")
        val savedList = ArrayList<Int>(5)
        if (savedString.isNullOrEmpty()) {
            savedList.add(default.type)
        } else {
            val st = StringTokenizer(savedString, ",")
            repeat(st.countTokens()) { savedList.add(Integer.parseInt(st.nextToken())) }
        }
        return savedList.map { it.toQuizType() } as ArrayList<QuizType>
    }

    private fun saveQuizTypeArrayInPrefs(
        prefs: SharedPreferences,
        key: String,
        types: ArrayList<QuizType>,
    ) {
        val str = StringBuilder()
        types.forEach { str.append(it.type).append(",") }
        prefs.edit().putString(key, str.toString()).apply()
    }
}
