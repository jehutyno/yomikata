package com.jehutyno.yomikata.model

import com.jehutyno.yomikata.util.AppLanguage
import com.jehutyno.yomikata.util.LanguageManager

/**
 * Created by valentin on 27/09/2016.
 */
open class Quiz(val id: Long, var nameEn: String, var nameFr: String, val category: Int, var isSelected: Boolean) {

    fun getName(): String {
        return when (LanguageManager.current) {
            AppLanguage.FRENCH -> nameFr
            else -> nameEn   // Phase 3a: DE/ES/PT/ZH added in Phase 3b
        }
    }

}
