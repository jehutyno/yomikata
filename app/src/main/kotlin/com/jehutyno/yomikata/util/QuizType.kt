package com.jehutyno.yomikata.util

import android.os.Parcel
import android.os.Parcelable


/**
 * Created by jehutyno on 09/10/2016.
 */
const val BASE_POINTS = 50  // default reference points given for any answer
enum class QuizType(val type: Int, val extraPoints: Int): Parcelable {
    // extraPoints should be less than BASE_POINTS (in absolute value), see LevelSystem addPoints
    TYPE_AUTO(0, 0),
    TYPE_PRONUNCIATION(1, 15),
    TYPE_PRONUNCIATION_QCM(2, -10),
    TYPE_AUDIO(3, -5),
    TYPE_EN_JAP(4, 5),
    TYPE_JAP_EN(5, 0);

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(ordinal)
    }

    fun getRandomType(): QuizType {
        return values().random()
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object {
        @Suppress("unused")
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
