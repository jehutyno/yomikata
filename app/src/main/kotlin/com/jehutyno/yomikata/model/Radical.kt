package com.jehutyno.yomikata.model

import com.jehutyno.yomikata.util.AppLanguage
import com.jehutyno.yomikata.util.LanguageManager

/**
 * Created by valentin on 27/09/2016.
 */
open class Radical(val radical: String, val strokes: Int, val reading: String, val en: String, val fr: String) {

    fun getTrad(): String {
        return when (LanguageManager.current) {
            AppLanguage.FRENCH -> fr
            else -> en   // Phase 3a: DE/ES/PT/ZH added in Phase 3b
        }
    }

}
