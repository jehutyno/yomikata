package com.jehutyno.yomikata.model

import com.jehutyno.yomikata.util.language.AppLanguage
import com.jehutyno.yomikata.util.language.LanguageManager


/**
 * Created by valentin on 27/09/2016.
 */
open class KanjiSoloRadical(kanji: String, strokes: Int, en: String, fr: String,
                     kunyomi: String, onyomi: String, radical: String, val radStroke: Int, val radReading: String,
                    val radEn: String, val radFr: String,
                    // Translation columns added in v18 (empty until populated in Phase 3c+)
                    de: String = "", es: String = "", pt: String = "", zh: String = "",
                    val radDe: String = "", val radEs: String = "",
                    val radPt: String = "", val radZh: String = ""
): KanjiSolo(kanji, strokes, en, fr, kunyomi, onyomi, radical, de, es, pt, zh) {

    fun getRadTrad(): String {
        return when (LanguageManager.current) {
            AppLanguage.FRENCH     -> radFr
            AppLanguage.GERMAN     -> radDe.ifEmpty { radEn }
            AppLanguage.SPANISH    -> radEs.ifEmpty { radEn }
            AppLanguage.PORTUGUESE -> radPt.ifEmpty { radEn }
            AppLanguage.CHINESE    -> radZh.ifEmpty { radEn }
            else                   -> radEn
        }
    }

}
