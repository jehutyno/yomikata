package com.jehutyno.yomikata.util

import android.Manifest

/**
 * Created by valentin on 04/10/2016.
 */
object Extras {
    @JvmStatic val EXTRA_CATEGORY = "extra_category"
    @JvmStatic val EXTRA_LEVEL = "extra_level"
    @JvmStatic val EXTRA_QUIZ_ID = "extra_quiz_id"
    @JvmStatic val EXTRA_QUIZ_IDS = "extra_quiz_ids"
    @JvmStatic val EXTRA_QUIZ_POSITION = "extra_quiz_position"
    @JvmStatic val EXTRA_QUIZ_TYPE = "extra_quiz_type"
    @JvmStatic val EXTRA_QUIZ_TYPES = "extra_quiz_types"
    @JvmStatic val EXTRA_QUIZ_TITLE = "extra_quiz_title"
    @JvmStatic val EXTRA_QUIZ_STRATEGY = "extra_quiz_strategy"
    @JvmStatic val EXTRA_WORD_ID = "extra_word_id"
    @JvmStatic val EXTRA_WORD_POSITION = "extra_word_position"
    @JvmStatic val EXTRA_SEARCH_STRING = "extra_search_string"
    @JvmStatic val EXTRA_ERRORS = "extra_errors"

    @JvmStatic val REQUEST_PREFS = 33
    @JvmStatic val REQUEST_RESTORE = 55
    @JvmStatic val REQUEST_EXTERNAL_STORAGE_BACKUP = 44
    @JvmStatic val REQUEST_EXTERNAL_STORAGE_RESTORE = 66
    @JvmStatic val PERMISSIONS_STORAGE = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
}