package com.jehutyno.yomikata.repository.local

import com.jehutyno.yomikata.dao.StatsDao
import com.jehutyno.yomikata.model.StatAction
import com.jehutyno.yomikata.model.StatEntry
import com.jehutyno.yomikata.model.StatResult
import com.jehutyno.yomikata.model.StatTime
import com.jehutyno.yomikata.repository.StatsRepository
import com.jehutyno.yomikata.util.getStartEndOFWeek
import com.jehutyno.yomikata.util.getStartEndOfMonth
import java.util.*


/**
 * Created by valentin on 10/01/2017.
 */
class StatsSource(private val statsDao: StatsDao) : StatsRepository {

    override suspend fun addStatEntry(action: StatAction, associatedId: Long, date: Long, result: StatResult) {
        addStatEntry(StatEntry(0, action.value, associatedId, date, result.value))
    }

    override suspend fun addStatEntry(entry: StatEntry) {
        statsDao.addStatEntry(RoomStatEntry.from(entry))
    }

    override suspend fun getStatEntriesOfTimeInterval(start: Long, end: Long): List<StatEntry> {
        return statsDao.getStatEntriesOfTimeInterval(start, end).map { it.toStatEntry() }
    }

    override suspend fun getTodayStatEntries(today: Calendar, callback: StatsRepository.LoadStatsCallback) {
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        val start = today.timeInMillis

        today.set(Calendar.HOUR_OF_DAY, 23)
        today.set(Calendar.MINUTE, 59)
        val end = today.timeInMillis

        val statEntryList = getStatEntriesOfTimeInterval(start, end)
        callback.onStatsLoaded(StatTime.TODAY, statEntryList)
    }

    override suspend fun getThisWeekStatEntries(today: Calendar, callback: StatsRepository.LoadStatsCallback) {
        val startEnd = getStartEndOFWeek(today)
        val statEntryList = getStatEntriesOfTimeInterval(startEnd[0], startEnd[1])
        callback.onStatsLoaded(StatTime.THIS_WEEK, statEntryList)
    }

    override suspend fun getThisMonthStatEntries(today: Calendar, callback: StatsRepository.LoadStatsCallback) {
        val startEnd = getStartEndOfMonth(today)
        val statEntryList = getStatEntriesOfTimeInterval(startEnd[0], startEnd[1])
        callback.onStatsLoaded(StatTime.THIS_MONTH, statEntryList)
    }

    override suspend fun getAllStatEntries(callback: StatsRepository.LoadStatsCallback) {
        val statEntryList = statsDao.getAllStatEntries().map { it.toStatEntry() }
        callback.onStatsLoaded(StatTime.ALL, statEntryList)
    }

    override suspend fun removeAllStats() {
        statsDao.removeAllStats()
    }
}
