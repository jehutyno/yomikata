package com.jehutyno.yomikata.model

import java.util.*

/**
 * Created by valentin on 27/09/2016.
 */
open class KanjiSoloRadical(id: Long, kanji: String, strokes: Int, en: String, fr: String,
                     kunyomi: String, onyomi: String, radical: String, val radStroke: Int, val radReading: String,
                            val radEn: String, val radFr: String): KanjiSolo(id, kanji, strokes, en, fr, kunyomi, onyomi, radical){

    fun getRadTrad(): String {
        return if (Locale.getDefault().language == "fr")
            radFr
        else
            radEn
    }

}