package com.jehutyno.yomikata.model

/**
 * Created by valentin on 27/09/2016.
 */
open class StatEntry(val id: Long, val action: Int, val associatedId: Long, val date: Long, val result: Int)

interface StatValue {
    val value: Int
}

enum class StatAction(override val value: Int) : StatValue {
    LAUNCH_QUIZ_FROM_CATEGORY(0),
    ANSWER_QUESTION(1),
    WORD_SEEN(2)
}

enum class StatTime(override val value: Int) : StatValue {
    TODAY(0),
    THIS_WEEK(1),
    THIS_MONTH(2),
    ALL(3)
}

enum class StatResult(override val value: Int) : StatValue {
    SUCCESS(0),
    FAIL(1),
    OTHER(2)
}