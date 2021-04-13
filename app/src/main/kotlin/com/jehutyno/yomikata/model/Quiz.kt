package com.jehutyno.yomikata.model

import java.util.*

/**
 * Created by valentin on 27/09/2016.
 */
open class Quiz(val id: Long, var nameEn: String, var nameFr: String, val category: Int, var isSelected: Int) {

    fun getName(): String {
        if (Locale.getDefault().language == "fr")
            return nameFr
        else
            return nameEn
    }

}