package com.jehutyno.yomikata.screens.home

import androidx.lifecycle.LiveData
import com.jehutyno.yomikata.BasePresenter
import com.jehutyno.yomikata.BaseView
import com.jehutyno.yomikata.model.StatEntry


/**
 * Created by valentin on 27/09/2016.
 */
interface HomeContract {

    interface View : BaseView<Presenter> {
        fun onMenuItemClick(category: Int)

        fun displayTodayStats(stats: List<StatEntry>)
        fun displayThisWeekStats(stats: List<StatEntry>)
        fun displayThisMonthStats(stats: List<StatEntry>)
        fun displayTotalStats(stats: List<StatEntry>)
    }

    interface Presenter : BasePresenter {
        val todayStatList: LiveData<List<StatEntry>>
        val thisWeekStatList: LiveData<List<StatEntry>>
        val thisMonthStatList: LiveData<List<StatEntry>>
        val totalStatList: LiveData<List<StatEntry>>

        fun getNumberOfLaunchedQuizzes(stats: List<StatEntry>): Int
        fun getNumberOfWordsSeen(stats: List<StatEntry>): Int
        fun getNumberOfCorrectAnswers(stats: List<StatEntry>): Int
        fun getNumberOfWrongAnswers(stats: List<StatEntry>): Int
    }
}
