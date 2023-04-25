package com.jehutyno.yomikata.repository

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
        fun onStatsLoaded(statTime: StatTime, stats: List<StatEntry>)
        fun onDataNotAvailable()
    }
    suspend fun addStatEntry(action: StatAction, associatedId: Long, date: Long, result: StatResult)
    suspend fun getTodayStatEntries(today: Calendar, callback: LoadStatsCallback)
    suspend fun getThisWeekStatEntries(today: Calendar, callback: LoadStatsCallback)
    suspend fun getThisMonthStatEntries(today: Calendar, callback: LoadStatsCallback)
    suspend fun getAllStatEntries(callback: LoadStatsCallback)
    suspend fun removeAllStats()
    suspend fun addStatEntry(entry: StatEntry)
    suspend fun getStatEntriesOfTimeInterval(start: Long, end: Long): List<StatEntry>
}
