package com.jehutyno.yomikata.util

/**
 * Created by jehutyno on 09/10/2016.
 */

enum class Prefs(val pref: String) {
    SELECTED_CATEGORY("prefs_selected_category"),
    TRAD_DISPLAYED("prefs_trad_displayed"),
    FURI_DISPLAYED("prefs_furi_displayed"),
    SELECTED_QUIZ_TYPES("selected_quiz_types"),
    WAS_SELECTED_QUIZ_TYPES("was_selected_quiz_types"),
    DAY_NIGHT_MODE("pref_day_night_mode"),
    TTS_RATE("tts_rate"),
    LATEST_CATEGORY_1("latest_category_1"),
    LATEST_CATEGORY_2("latest_category_2"),
    DB_UPDATE_ONGOING("db_update_ongoing"),
    DB_UPDATE_FILE("db_update_file"),
    DB_UPDATE_OLD_VERSION("db_update_old_version"),
    VOICE_DOWNLOADED_LEVEL_V("voice_downloaded_level_V"),
    DONT_SHOW_VOICES_POPUP("dont_show_voices_popup")
}