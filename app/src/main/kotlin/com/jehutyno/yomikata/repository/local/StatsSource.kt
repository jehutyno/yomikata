package com.jehutyno.yomikata.repository.local

import android.content.ContentValues
import android.content.Context
import com.jehutyno.yomikata.model.StatAction
import com.jehutyno.yomikata.model.StatEntry
import com.jehutyno.yomikata.model.StatResult
import com.jehutyno.yomikata.model.StatTime
import com.jehutyno.yomikata.repository.StatsRepository
import com.jehutyno.yomikata.util.getStartEndOFWeek
import com.jehutyno.yomikata.util.getStartEndOfMonth
import org.jetbrains.anko.db.parseList
import org.jetbrains.anko.db.rowParser
import org.jetbrains.anko.db.select
import java.util.*

/**
 * Created by valentin on 10/01/2017.
 */
class StatsSource(var context: Context) : StatsRepository {

    override fun addStatEntry(action: StatAction, associatedId: Long, date: Long, result: StatResult) {
        context.database.use {
            val values = ContentValues()
            values.put(SQLiteStatEntry.ACTION.column, action.value)
            values.put(SQLiteStatEntry.ASSOCIATED_ID.column, associatedId)
            values.put(SQLiteStatEntry.DATE.column, date)
            values.put(SQLiteStatEntry.RESULT.column, result.value)
            insert(SQLiteTables.STAT_ENTRY.tableName, null, values)
        }
    }

    override fun addStatEntry(entry: StatEntry) {
        context.database.use {
            val values = ContentValues()
            values.put(SQLiteStatEntry.ACTION.column, entry.action)
            values.put(SQLiteStatEntry.ASSOCIATED_ID.column, entry.associatedId)
            values.put(SQLiteStatEntry.DATE.column, entry.date)
            values.put(SQLiteStatEntry.RESULT.column, entry.result)
            insert(SQLiteTables.STAT_ENTRY.tableName, null, values)
        }
    }

    override fun getTodayStatEntries(today: Calendar, callback: StatsRepository.LoadStatsCallback) {
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        val start = today.timeInMillis
        today.set(Calendar.HOUR_OF_DAY, 23)
        today.set(Calendar.MINUTE, 59)
        val end = today.timeInMillis;
        context.database.use {
            select(SQLiteTables.STAT_ENTRY.tableName, *SQLiteTable.allColumns(SQLiteStatEntry.values()))
                .whereArgs("${SQLiteStatEntry.DATE} > $start AND ${SQLiteStatEntry.DATE} < $end")
                .exec {
                    val rowParser = rowParser(::StatEntry)
                    callback.onStatsLoaded(StatTime.TODAY, parseList(rowParser))
                }
        }
    }

    override fun getThisWeekStatEntries(today: Calendar, callback: StatsRepository.LoadStatsCallback) {
        val startEnd = getStartEndOFWeek(today)
        context.database.use {
            select(SQLiteTables.STAT_ENTRY.tableName, *SQLiteTable.allColumns(SQLiteStatEntry.values()))
                .where("${SQLiteStatEntry.DATE} > ${startEnd[0]} AND ${SQLiteStatEntry.DATE} < ${startEnd[1]}")
                .exec {
                    val rowParser = rowParser(::StatEntry)
                    callback.onStatsLoaded(StatTime.THIS_WEEK, parseList(rowParser))
                }
        }
    }

    override fun getThisMonthStatEntries(today: Calendar, callback: StatsRepository.LoadStatsCallback) {
        val startEnd = getStartEndOfMonth(today)
        context.database.use {
            select(SQLiteTables.STAT_ENTRY.tableName, *SQLiteTable.allColumns(SQLiteStatEntry.values()))
                .where("${SQLiteStatEntry.DATE} > ${startEnd[0]} AND ${SQLiteStatEntry.DATE} < ${startEnd[1]}")
                .exec {
                    val rowParser = rowParser(::StatEntry)
                    callback.onStatsLoaded(StatTime.THIS_MONTH, parseList(rowParser))
                }
        }
    }

    override fun getAllStatEntries(callback: StatsRepository.LoadStatsCallback) {
        context.database.use {
            select(SQLiteTables.STAT_ENTRY.tableName, *SQLiteTable.allColumns(SQLiteStatEntry.values()))
                .exec {
                    val rowParser = rowParser(::StatEntry)
                    callback.onStatsLoaded(StatTime.ALL, parseList(rowParser))
                }
        }
    }

    override fun removeAllStats() {
        context.database.use {
            execSQL("delete from ${SQLiteTables.STAT_ENTRY}")
        }
    }
}