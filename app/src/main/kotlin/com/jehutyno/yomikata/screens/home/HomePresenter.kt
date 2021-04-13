package com.jehutyno.yomikata.screens.home

import android.content.Context
import com.jehutyno.yomikata.model.StatEntry
import com.jehutyno.yomikata.model.StatTime
import com.jehutyno.yomikata.repository.StatsRepository
import mu.KLogging
import java.util.*

/**
 * Created by valentin on 26/12/2016.
 */
class HomePresenter(
    private val context: Context,
    private val statsRepository: StatsRepository,
    private val homeView: HomeContract.View) : HomeContract.Presenter {

    companion object : KLogging()

    init {
        homeView.setPresenter(this)
    }

    override fun start() {

    }

    override fun loadAllStats() {
        val calendar = Calendar.getInstance()
        statsRepository.getTodayStatEntries(calendar, object: StatsRepository.LoadStatsCallback {
            override fun onStatsLoaded(statTime: StatTime, stats: List<StatEntry>) {
                homeView.displayTodayStats(stats)
            }

            override fun onDataNotAvailable() {

            }
        })
        statsRepository.getThisWeekStatEntries(calendar, object: StatsRepository.LoadStatsCallback {
            override fun onStatsLoaded(statTime: StatTime, stats: List<StatEntry>) {
                homeView.displayThisWeekStats(stats)
            }

            override fun onDataNotAvailable() {

            }
        })
        statsRepository.getThisMonthStatEntries(calendar, object: StatsRepository.LoadStatsCallback {
            override fun onStatsLoaded(statTime: StatTime, stats: List<StatEntry>) {
                homeView.displayThisMonthStats(stats)
            }

            override fun onDataNotAvailable() {

            }
        })
        statsRepository.getAllStatEntries(object: StatsRepository.LoadStatsCallback {
            override fun onStatsLoaded(statTime: StatTime, stats: List<StatEntry>) {
                homeView.displayTotalStats(stats)
            }

            override fun onDataNotAvailable() {

            }
        })
    }


}