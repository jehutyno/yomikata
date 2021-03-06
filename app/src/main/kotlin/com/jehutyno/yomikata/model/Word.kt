package com.jehutyno.yomikata.model

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import androidx.core.content.ContextCompat
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.util.Categories
import java.io.Serializable
import java.util.*

/**
 * Created by valentin on 26/09/2016.
 */
open class Word(var id: Long, var japanese: String, var english: String, var french: String,
                var reading: String, var level: Int, var countTry: Int, var countSuccess: Int,
                var countFail: Int, var isKana: Int, var repetition: Int, var points: Int,
                var baseCategory: Int, var isSelected: Int, var sentenceId: Long) : Parcelable, Serializable {

    constructor(source: Parcel): this(source.readLong(), source.readString()!!,
        source.readString()!!, source.readString()!!, source.readString()!!, source.readInt(),
        source.readInt(), source.readInt(), source.readInt(), source.readInt(), source.readInt(),
        source.readInt(), source.readInt(), source.readInt(), source.readLong())

    override fun writeToParcel(dest: Parcel?, flags: Int) {
        dest?.writeLong(id)
        dest?.writeString(japanese)
        dest?.writeString(english)
        dest?.writeString(french)
        dest?.writeString(reading)
        dest?.writeInt(level)
        dest?.writeInt(countTry)
        dest?.writeInt(countSuccess)
        dest?.writeInt(countFail)
        dest?.writeInt(isKana)
        dest?.writeInt(repetition)
        dest?.writeInt(points)
        dest?.writeInt(baseCategory)
        dest?.writeInt(isSelected)
        dest?.writeLong(sentenceId)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object {
        @JvmField val CREATOR: Parcelable.Creator<Word> = object : Parcelable.Creator<Word> {
            override fun createFromParcel(source: Parcel): Word{
                return Word(source)
            }

            override fun newArray(size: Int): Array<Word?> {
                return arrayOfNulls(size)
            }
        }
    }

    fun getTrad(): String {
        if (Locale.getDefault().language == "fr")
            return french
        else
            return english
    }
}

fun getCategoryIcon(category: Int): Int {
    when (category) {
        Categories.CATEGORY_HIRAGANA -> {
            return R.drawable.ic_hiragana
        }
        Categories.CATEGORY_KATAKANA -> {
            return R.drawable.ic_katakana
        }
        Categories.CATEGORY_COUNTERS -> {
            return R.drawable.ic_counters
        }
        Categories.CATEGORY_JLPT_1 -> {
            return R.drawable.ic_jlpt1
        }
        Categories.CATEGORY_JLPT_2 -> {
            return R.drawable.ic_jlpt2
        }
        Categories.CATEGORY_JLPT_3 -> {
            return R.drawable.ic_jlpt3
        }
        Categories.CATEGORY_JLPT_4 -> {
            return R.drawable.ic_jlpt4
        }
        Categories.CATEGORY_JLPT_5 -> {
            return R.drawable.ic_jlpt5
        }
        else -> {
            return R.drawable.ic_kanji
        }
    }
}

fun getWordColor(context: Context, level: Int, points: Int): Int {
    val color = when (level) {
        0 -> {
            if (points < 25)
                R.color.level_low_1
            else if (points < 50)
                R.color.level_low_2
            else if (points < 75)
                R.color.level_low_3
            else
                R.color.level_low_4
        }
        1 -> {
            if (points < 25)
                R.color.level_medium_1
            else if (points < 50)
                R.color.level_medium_2
            else if (points < 75)
                R.color.level_medium_3
            else
                R.color.level_medium_4
        }
        2 -> {
            if (points < 25)
                R.color.level_high_1
            else if (points < 50)
                R.color.level_high_2
            else if (points < 75)
                R.color.level_high_3
            else
                R.color.level_high_4
        }
        else -> {
            if (points < 25)
                R.color.level_master_1
            else if (points < 50)
                R.color.level_master_2
            else if (points < 75)
                R.color.level_master_3
            else
                R.color.level_master_4
        }
    }

    return ContextCompat.getColor(context, color)
}