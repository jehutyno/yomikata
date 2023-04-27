package com.jehutyno.yomikata.repository

import com.jehutyno.yomikata.model.StatAction
import com.jehutyno.yomikata.model.StatEntry
import com.jehutyno.yomikata.model.StatResult
import kotlinx.coroutines.flow.Flow


/**
 * Created by valentin on 27/09/2016.
 */
interface StatsRepository {
    suspend fun addStatEntry(action: StatAction, associatedId: Long, date: Long, result: StatResult)
    suspend fun addStatEntry(entry: StatEntry)
    fun getStatEntriesOfTimeInterval(start: Long, end: Long): Flow<List<StatEntry>>
    fun getTodayStatEntries(): Flow<List<StatEntry>>
    fun getThisWeekStatEntries(): Flow<List<StatEntry>>
    fun getThisMonthStatEntries(): Flow<List<StatEntry>>
    fun getAllStatEntries(): Flow<List<StatEntry>>
    suspend fun removeAllStats()
}
