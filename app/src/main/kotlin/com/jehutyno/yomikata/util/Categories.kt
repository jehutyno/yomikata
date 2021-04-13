package com.jehutyno.yomikata.util

import com.jehutyno.yomikata.util.Categories.CATEGORY_COUNTERS
import com.jehutyno.yomikata.util.Categories.CATEGORY_HIRAGANA
import com.jehutyno.yomikata.util.Categories.CATEGORY_JLPT_1
import com.jehutyno.yomikata.util.Categories.CATEGORY_JLPT_2
import com.jehutyno.yomikata.util.Categories.CATEGORY_JLPT_3
import com.jehutyno.yomikata.util.Categories.CATEGORY_JLPT_4
import com.jehutyno.yomikata.util.Categories.CATEGORY_JLPT_5
import com.jehutyno.yomikata.util.Categories.CATEGORY_KANJI
import com.jehutyno.yomikata.util.Categories.CATEGORY_KATAKANA

/**
 * Created by valentin on 04/10/2016.
 */
object Categories {
    @JvmStatic val HOME = -1
    @JvmStatic val CATEGORY_HIRAGANA = 0
    @JvmStatic val CATEGORY_KATAKANA = 1
    @JvmStatic val CATEGORY_KANJI = 2
    @JvmStatic val CATEGORY_COUNTERS = 9
    @JvmStatic val CATEGORY_JLPT_1 = 3
    @JvmStatic val CATEGORY_JLPT_2 = 4
    @JvmStatic val CATEGORY_JLPT_3 = 5
    @JvmStatic val CATEGORY_JLPT_4 = 6
    @JvmStatic val CATEGORY_JLPT_5 = 7
    @JvmStatic val CATEGORY_SELECTIONS = 8
}

fun getCateogryLevel(category: Int): Int {
    return when (category) {
        CATEGORY_HIRAGANA -> 0
        CATEGORY_KATAKANA -> 0
        CATEGORY_KANJI -> 1
        CATEGORY_COUNTERS -> 1
        CATEGORY_JLPT_5 -> 2
        CATEGORY_JLPT_4 -> 3
        CATEGORY_JLPT_3 -> 4
        CATEGORY_JLPT_2 -> 5
        CATEGORY_JLPT_1 -> 6
        else -> 6
    }
}

fun getLevelDownloadUrl(level: Int): String {
    return when (level) {
        0 -> "TuyCO1IZXDmGav7/download"
        1 -> "6YanOT2hfYqjXbm/download"
        2 -> "vIlPQaF7vrpKdwD/download"
        3 -> "wzYwrP3TzXiwIsa/download"
        4 -> "HDSc5EtIVCn2lJD/download"
        5 -> "AFrQRr4UiFLDIzh/download"
        6 -> "HQucZsVJKAzKRnr/download"
        else -> ""
    }
}

fun getLevelDonwloadSize(level: Int): Int {
    return when (level) {
        0 -> 5
        1 -> 6
        2 -> 6
        3 -> 7
        4 -> 35
        5 -> 41
        else -> 56
    }
}

fun getLevelDownloadVersion(level: Int): Int {
    return  when (level) {
        0 -> 0
        1 -> 2
        2 -> 2
        3 -> 1
        4 -> 1
        5 -> 1
        6 -> 1
        else -> 0
    }
}