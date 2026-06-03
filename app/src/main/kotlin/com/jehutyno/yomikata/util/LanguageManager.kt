package com.jehutyno.yomikata.util

import android.content.SharedPreferences

/**
 * Language manager.
 *
 * Single source of truth for the active translation language.
 *
 * **Architecture note:** model classes (`Word`, `Quiz`, `Sentence`, etc.) call
 * [LanguageManager.current] directly on the companion object so they need no
 * injected dependency. All Android components that need to *change* the language
 * receive the Kodein singleton via DI.
 *
 * **Initialization:** [initFromPrefs] must be called in `Application.onCreate()`
 * before any Kodein binding is resolved, so [current] is correct from the very
 * first [getTrad] call.
 */
class LanguageManager(private val prefs: SharedPreferences) {

    companion object {
        /**
         * Currently active language. Read by all model `getTrad()` methods.
         * Defaults to [AppLanguage.DEFAULT] until [initFromPrefs] is called.
         */
        @Volatile
        var current: AppLanguage = AppLanguage.DEFAULT
            internal set

        /**
         * Bootstrap [current] from SharedPreferences.
         *
         * - If a language was previously saved → restore it.
         * - Otherwise (first launch) → detect from system locale and persist it.
         *
         * Call this once in `Application.onCreate()`, before any UI or DI initialization.
         */
        fun initFromPrefs(prefs: SharedPreferences) {
            val saved = prefs.getString(Prefs.APP_LANGUAGE.pref, null)
            // If the user has explicitly chosen a language in Settings, use it.
            // Otherwise fall back to the system locale — do NOT persist the detected
            // value so that changing the device language is always picked up on restart.
            current = if (saved != null) {
                AppLanguage.fromIsoCode(saved)
            } else {
                AppLanguage.fromSystemLocale()
            }
        }
    }

    /** Returns the currently active language. */
    fun getCurrentLanguage(): AppLanguage = current

    /**
     * Updates the active language in memory and persists it to SharedPreferences.
     * The change takes effect immediately for all subsequent [getTrad] calls.
     * A full app restart is required for Android string resources to reload.
     */
    fun setLanguage(language: AppLanguage) {
        current = language
        prefs.edit().putString(Prefs.APP_LANGUAGE.pref, language.isoCode).apply()
    }
}
