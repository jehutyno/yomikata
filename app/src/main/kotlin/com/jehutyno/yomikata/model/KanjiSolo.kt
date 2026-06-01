package com.jehutyno.yomikata.model

import com.jehutyno.yomikata.util.AppLanguage
import com.jehutyno.yomikata.util.LanguageManager

/**
 * Created by valentin on 27/09/2016.
 */
open class KanjiSolo(val kanji: String, val strokes: Int, val en: String, val fr: String,
                     val kunyomi: String, val onyomi: String, val radical: String) {

    fun getTrad(): String {
        return when (LanguageManager.current) {
            AppLanguage.FRENCH -> fr
            else -> en   // Phase 3a: DE/ES/PT/ZH added in Phase 3b
        }
    }

}
