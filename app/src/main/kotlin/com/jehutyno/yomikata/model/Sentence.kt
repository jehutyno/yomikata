package com.jehutyno.yomikata.model

import android.os.Parcel
import android.os.Parcelable
import com.jehutyno.yomikata.util.AppLanguage
import com.jehutyno.yomikata.util.LanguageManager

/**
 * Created by valentinlanfranchi on 19/05/2017.
 */
open class Sentence(var id: Long = -1, val jap: String = "", val en: String = "",
                    val fr: String = "", val level: Int = -1,
                    // Translation columns added in v18 (empty until populated in Phase 3c+)
                    val de: String = "", val es: String = "",
                    val pt: String = "", val zh: String = ""): Parcelable {

    constructor(source: Parcel): this(source.readLong(), source.readString()!!,
        source.readString()!!, source.readString()!!, source.readInt(),
        source.readString()!!, source.readString()!!, source.readString()!!, source.readString()!!)

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeLong(id)
        dest.writeString(jap)
        dest.writeString(en)
        dest.writeString(fr)
        dest.writeInt(level)
        dest.writeString(de)
        dest.writeString(es)
        dest.writeString(pt)
        dest.writeString(zh)
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
            AppLanguage.FRENCH     -> fr
            AppLanguage.GERMAN     -> de.ifEmpty { en }
            AppLanguage.SPANISH    -> es.ifEmpty { en }
            AppLanguage.PORTUGUESE -> pt.ifEmpty { en }
            AppLanguage.CHINESE    -> zh.ifEmpty { en }
            else                   -> en
        }
    }

}