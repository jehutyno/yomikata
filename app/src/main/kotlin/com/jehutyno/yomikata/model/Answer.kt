package com.jehutyno.yomikata.model

import android.os.Parcel
import android.os.Parcelable
import com.jehutyno.yomikata.util.QuizType
import com.jehutyno.yomikata.util.readParcelableHelper
import java.io.Serializable


/**
 * Created by valentin on 25/10/2016.
 */
open class Answer(val result: Int, var answer: String, val wordId: Long, val sentenceId: Long, val quizType: QuizType) : Parcelable, Serializable {

    constructor(source: Parcel) : this(source.readInt(), source.readString()!!, source.readLong(), source.readLong(),
                source.readParcelableHelper(QuizType::class.java.classLoader, QuizType::class.java)!!)

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(result)
        dest.writeString(answer)
        dest.writeLong(wordId)
        dest.writeLong(sentenceId)
        dest.writeParcelable(quizType, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object {
        @Suppress("UNUSED")
        @JvmField val CREATOR: Parcelable.Creator<Answer> = object : Parcelable.Creator<Answer> {
            override fun createFromParcel(source: Parcel): Answer {
                return Answer(source)
            }

            override fun newArray(size: Int): Array<Answer?> {
                return arrayOfNulls(size)
            }
        }
    }

}
