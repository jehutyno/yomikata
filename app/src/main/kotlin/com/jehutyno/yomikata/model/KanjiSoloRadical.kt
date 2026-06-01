package com.jehutyno.yomikata.model

import com.jehutyno.yomikata.util.AppLanguage
import com.jehutyno.yomikata.util.LanguageManager


/**
 * Created by valentin on 27/09/2016.
 */
open class KanjiSoloRadical(kanji: String, strokes: Int, en: String, fr: String,
                     kunyomi: String, onyomi: String, radical: String, val radStroke: Int, val radReading: String,
                    val radEn: String, val radFr: String): KanjiSolo(kanji, strokes, en, fr, kunyomi, onyomi, radical) {

    fun getRadTrad(): String {
        return when (LanguageManager.current) {
            AppLanguage.FRENCH -> radFr
            else -> radEn   // Phase 3a: DE/ES/PT/ZH added in Phase 3b
        }
    }

}
