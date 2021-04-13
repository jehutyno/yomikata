package com.jehutyno.yomikata.repository

import androidx.annotation.NonNull
import com.jehutyno.yomikata.model.StatAction
import com.jehutyno.yomikata.model.StatEntry
import com.jehutyno.yomikata.model.StatResult
import com.jehutyno.yomikata.model.StatTime
import java.util.*

/**
 * Created by valentin on 27/09/2016.
 */
interface StatsRepository {
    interface LoadStatsCallback {
        fun onStatsLoaded(statTime: StatTime, stats:List<StatEntry>)
        fun onDataNotAvailable()
    }
    fun addStatEntry(action: StatAction, associatedId: Long, date: Long, result: StatResult)
    fun getTodayStatEntries(today: Calendar, @NonNull callback: LoadStatsCallback)
    fun getThisWeekStatEntries(today: Calendar, @NonNull callback: LoadStatsCallback)
    fun getThisMonthStatEntries(today: Calendar, @NonNull callback: LoadStatsCallback)
    fun getAllStatEntries(@NonNull callback: StatsRepository.LoadStatsCallback)
    fun removeAllStats()
    fun addStatEntry(entry: StatEntry)
}