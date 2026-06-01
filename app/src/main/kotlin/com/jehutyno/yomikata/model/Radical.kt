package com.jehutyno.yomikata.model

import com.jehutyno.yomikata.util.AppLanguage
import com.jehutyno.yomikata.util.LanguageManager

/**
 * Created by valentin on 27/09/2016.
 */
open class Radical(val radical: String, val strokes: Int, val reading: String, val en: String, val fr: String,
                   // Translation columns added in v18 (empty until populated in Phase 3c+)
                   val de: String = "", val es: String = "",
                   val pt: String = "", val zh: String = "") {

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
