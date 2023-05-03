package com.jehutyno.yomikata.model

import java.util.Locale

/**
 * Created by valentin on 27/09/2016.
 */
open class KanjiSolo(val kanji: String, val strokes: Int, val en: String, val fr: String,
                     val kunyomi: String, val onyomi: String, val radical: String) {

    fun getTrad(): String {
        return if (Locale.getDefault().language == "fr")
            fr
        else
            en
    }

}
