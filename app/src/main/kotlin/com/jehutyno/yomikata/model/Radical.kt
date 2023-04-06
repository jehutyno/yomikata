package com.jehutyno.yomikata.model

import java.util.Locale

/**
 * Created by valentin on 27/09/2016.
 */
open class Radical(val id: Long, val strokes: Int, val radical: String, val reading: String, val en: String, val fr: String) {

    fun getTrad(): String {
        return if (Locale.getDefault().language == "fr")
            fr
        else
            en
    }

}
