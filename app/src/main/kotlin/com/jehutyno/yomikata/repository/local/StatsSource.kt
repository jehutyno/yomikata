package com.jehutyno.yomikata.repository.local

import com.jehutyno.yomikata.dao.StatsDao
import com.jehutyno.yomikata.model.StatAction
import com.jehutyno.yomikata.model.StatEntry
import com.jehutyno.yomikata.model.StatResult
import com.jehutyno.yomikata.repository.StatsRepository
import com.jehutyno.yomikata.repository.database.RoomStatEntry
import com.jehutyno.yomikata.util.getStartEndOFWeek
import com.jehutyno.yomikata.util.getStartEndOfMonth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar


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

    override fun getStatEntriesOfTimeInterval(start: Long, end: Long): Flow<List<StatEntry>> {
        return statsDao.getStatEntriesOfTimeInterval(start, end).map { list ->
            list.map { it.toStatEntry() }
        }
    }

    override fun getTodayStatEntries() : Flow<List<StatEntry>> {
        val today = Calendar.getInstance()
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        val start = today.timeInMillis

        today.set(Calendar.HOUR_OF_DAY, 23)
        today.set(Calendar.MINUTE, 59)
        val end = today.timeInMillis

        return getStatEntriesOfTimeInterval(start, end)
    }

    override fun getThisWeekStatEntries() : Flow<List<StatEntry>> {
        val today = Calendar.getInstance()
        val startEnd = getStartEndOFWeek(today)
        return getStatEntriesOfTimeInterval(startEnd[0], startEnd[1])
    }

    override fun getThisMonthStatEntries() : Flow<List<StatEntry>> {
        val today = Calendar.getInstance()
        val startEnd = getStartEndOfMonth(today)
        return getStatEntriesOfTimeInterval(startEnd[0], startEnd[1])
    }

    override fun getAllStatEntries() : Flow<List<StatEntry>> {
        return statsDao.getAllStatEntries().map { list ->
            list.map { it.toStatEntry() }
        }
    }

    override suspend fun removeAllStats() {
        statsDao.removeAllStats()
    }
}
