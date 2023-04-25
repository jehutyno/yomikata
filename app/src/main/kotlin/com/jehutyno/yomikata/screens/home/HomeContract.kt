package com.jehutyno.yomikata.screens.home

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
        suspend fun loadAllStats()
    }
}