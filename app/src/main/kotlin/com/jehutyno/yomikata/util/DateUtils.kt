package com.jehutyno.yomikata.util

import java.util.*


/**
 * Created by valentin on 12/01/2017.
 */

fun getStartEndOFWeek(now: Calendar): LongArray {
    val enterWeek = now.get(Calendar.WEEK_OF_YEAR)
    val enterYear = now.get(Calendar.YEAR)

    val calendar = Calendar.getInstance()
    calendar.clear()
    calendar.set(Calendar.WEEK_OF_YEAR, enterWeek)
    calendar.set(Calendar.YEAR, enterYear)
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    val startDate = calendar.timeInMillis

    calendar.add(Calendar.DATE, 6)
    calendar.set(Calendar.HOUR_OF_DAY, 23)
    calendar.set(Calendar.MINUTE, 59)
    val endDate = calendar.timeInMillis

    return longArrayOf(startDate, endDate)
}

fun getStartEndOfMonth(now: Calendar): LongArray {
    now.set(Calendar.DAY_OF_MONTH, 1)
    now.set(Calendar.HOUR_OF_DAY, 0)
    now.set(Calendar.MINUTE, 0)
    val startDate = now.timeInMillis

    now.set(Calendar.DAY_OF_MONTH, now.getActualMaximum(Calendar.DAY_OF_MONTH))
    now.set(Calendar.HOUR_OF_DAY, 23)
    now.set(Calendar.MINUTE, 59)
    val endDate = now.timeInMillis

    return longArrayOf(startDate, endDate)
}