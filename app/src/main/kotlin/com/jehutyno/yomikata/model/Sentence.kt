package com.jehutyno.yomikata.model

import android.os.Parcel
import android.os.Parcelable
import com.jehutyno.yomikata.util.AppLanguage
import com.jehutyno.yomikata.util.LanguageManager

/**
 * Created by valentinlanfranchi on 19/05/2017.
 */
open class Sentence(var id: Long = -1, val jap: String = "", val en: String = "", val fr: String = "", val level: Int = -1): Parcelable {

    constructor(source: Parcel): this(source.readLong(), source.readString()!!,
        source.readString()!!, source.readString()!!, source.readInt())

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeLong(id)
        dest.writeString(jap)
        dest.writeString(en)
        dest.writeString(fr)
        dest.writeInt(level)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object {
        @Suppress("UNUSED")
        @JvmField val CREATOR: Parcelable.Creator<Sentence> = object : Parcelable.Creator<Sentence> {
            override fun createFromParcel(source: Parcel): Sentence{
                return Sentence(source)
            }

            override fun newArray(size: Int): Array<Sentence?> {
                return arrayOfNulls(size)
            }
        }
    }


    fun getTrad(): String {
        return when (LanguageManager.current) {
            AppLanguage.FRENCH -> fr
            else -> en   // Phase 3a: DE/ES/PT/ZH added in Phase 3b
        }
    }

}