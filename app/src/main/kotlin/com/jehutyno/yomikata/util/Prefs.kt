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
    TTS_RATE("tts_rate"),
    LATEST_CATEGORY_1("latest_category_1"),
    LATEST_CATEGORY_2("latest_category_2"),
    DB_UPDATE_ONGOING("db_update_ongoing"),
    DB_UPDATE_FILE("db_update_file"),
    DB_UPDATE_OLD_VERSION("db_update_old_version"),

    // set to true when starting a restore process
    // from the PrefsActivity, if app unexpectedly closes -> continue the migration at startup
    // if error happens and this is set to true -> restore the local backup
    DB_RESTORE_ONGOING("db_restore_ongoing"),
    VOICE_DOWNLOADED_LEVEL_V("voice_downloaded_level_V"),
    DONT_SHOW_VOICES_POPUP("dont_show_voices_popup"),

    QUIZ_ERROR_SELECTED_RADIO_BUTTON_ID("quiz_error_selected_radio_button_id"),
    QUIZ_FLAWLESS_SELECTED_RADIO_BUTTON_ID("quiz_flawless_selected_radio_button_id"),

    APP_LANGUAGE("app_language"),

    QUIZ_LENGTH("length"),
    QUIZ_SPEED("speed"),
    PLAY_START("play_start"),
    PLAY_END("play_end"),
    FONT_SIZE("font_size"),

    LAST_SELECTED_LEVEL("last_selected_level"),

    LAST_LAUNCH_MODE("last_launch_mode")
}
