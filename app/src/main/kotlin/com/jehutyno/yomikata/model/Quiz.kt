package com.jehutyno.yomikata.model

import java.util.Locale

/**
 * Created by valentin on 27/09/2016.
 */
open class Quiz(val id: Long, var nameEn: String, var nameFr: String, val category: Int, var isSelected: Boolean) {

    fun getName(): String {
        return if (Locale.getDefault().language == "fr")
            nameFr
        else
            nameEn
    }

}
