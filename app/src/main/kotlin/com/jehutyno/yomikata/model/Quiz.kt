package com.jehutyno.yomikata.model

import com.jehutyno.yomikata.util.language.AppLanguage
import com.jehutyno.yomikata.util.language.LanguageManager

/**
 * Created by valentin on 27/09/2016.
 */
open class Quiz(val id: Long, var nameEn: String, var nameFr: String, val category: Int, var isSelected: Boolean,
                // Translation columns added in v18 (empty until populated in Phase 3c+)
                var nameDe: String = "", var nameEs: String = "",
                var namePt: String = "", var nameZh: String = "") {

    fun getName(): String {
        return when (LanguageManager.current) {
            AppLanguage.FRENCH     -> nameFr
            AppLanguage.GERMAN     -> nameDe.ifEmpty { nameEn }
            AppLanguage.SPANISH    -> nameEs.ifEmpty { nameEn }
            AppLanguage.PORTUGUESE -> namePt.ifEmpty { nameEn }
            AppLanguage.CHINESE    -> nameZh.ifEmpty { nameEn }
            else                   -> nameEn
        }
    }

}
