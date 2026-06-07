package com.jehutyno.yomikata.util.language

import java.util.Locale

/**
 * Supported translation languages for the app.
 *
 * @property isoCode    ISO 639-1 code — used as SharedPreferences value and
 *                      matched against [Locale.getDefault().language].
 * @property displayName Human-readable name shown in the language selector.
 */
enum class AppLanguage(val isoCode: String, val displayName: String) {
    ENGLISH("en", "English"),
    FRENCH("fr", "Français"),
    GERMAN("de", "Deutsch"),
    SPANISH("es", "Español"),
    PORTUGUESE("pt", "Português"),
    CHINESE("zh", "中文");

    companion object {
        /** Language used when no supported match is found. */
        val DEFAULT: AppLanguage = ENGLISH

        /** Returns the [AppLanguage] whose [isoCode] matches [code], or [DEFAULT]. */
        fun fromIsoCode(code: String): AppLanguage =
            entries.find { it.isoCode == code } ?: DEFAULT

        /** Returns the best [AppLanguage] matching the current system locale. */
        fun fromSystemLocale(): AppLanguage =
            fromIsoCode(Locale.getDefault().language)
    }
}
