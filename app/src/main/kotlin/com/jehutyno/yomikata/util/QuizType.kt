package com.jehutyno.yomikata.util

import android.os.Parcel
import android.os.Parcelable
import java.util.*

/**
 * Created by jehutyno on 09/10/2016.
 */

enum class QuizType(val type: Int, val points: Int): Parcelable {
    TYPE_AUTO(0, 0),
    TYPE_PRONUNCIATION(1, 50),
    TYPE_PRONUNCIATION_QCM(2, 50),
    TYPE_AUDIO(3, 50),
    TYPE_EN_JAP(4, 50),
    TYPE_JAP_EN(5, 50);

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(ordinal)
    }

    fun getRandomType(): QuizType {
        val random = Random()
        return values()[random.nextInt(values().size)]
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object {
        @JvmField val CREATOR: Parcelable.Creator<QuizType> = object : Parcelable.Creator<QuizType> {
            override fun createFromParcel(source: Parcel): QuizType {
                return QuizType.values()[source.readInt()]
            }

            override fun newArray(size: Int): Array<QuizType?> {
                return arrayOfNulls(size)
            }
        }
    }

}