package com.jehutyno.yomikata.util.analytics

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.jehutyno.yomikata.util.Prefs
import com.jehutyno.yomikata.util.quiz.Categories
import com.jehutyno.yomikata.util.quiz.QuizStrategy
import com.jehutyno.yomikata.util.quiz.QuizType

/**
 * Point d'entrée unique pour le suivi d'usage (Firebase Analytics + Crashlytics).
 *
 * Règles RGPD (opt-in strict) :
 *  - La collecte est DÉSACTIVÉE par défaut (cf. meta-data du manifeste). Elle n'est activée
 *    qu'après consentement explicite de l'utilisateur, via [setConsent].
 *  - Aucune donnée personnelle ou identifiante n'est envoyée : uniquement des catégories
 *    stables et indépendantes de la langue (slugs), des enums et des compteurs.
 *  - Tous les noms d'événements/paramètres sont centralisés ici (pas de strings en dur
 *    dispersées dans l'app).
 */
object Analytics {

    // Noms d'événements (stables, snake_case — convention GA4)
    private const val EVENT_QUIZ_LAUNCHED = "quiz_launched"
    private const val EVENT_QUIZ_COMPLETED = "quiz_completed"
    private const val EVENT_SELECTION_CREATED = "selection_created"
    private const val EVENT_SELECTION_DELETED = "selection_deleted"

    // Paramètres
    private const val PARAM_SOURCE = "source"
    private const val PARAM_CATEGORY = "category"
    private const val PARAM_LEVEL = "level"
    private const val PARAM_STRATEGY = "strategy"
    private const val PARAM_QUIZ_TYPES = "quiz_types"
    private const val PARAM_SESSION_LENGTH = "session_length"
    private const val PARAM_ERROR_COUNT = "error_count"

    const val SOURCE_STUDY = "study"
    const val SOURCE_SELECTION = "selection"

    private var firebaseAnalytics: FirebaseAnalytics? = null

    /** Applique l'état de consentement persisté au démarrage de l'app. */
    fun init(context: Context, prefs: SharedPreferences) {
        firebaseAnalytics = FirebaseAnalytics.getInstance(context.applicationContext)
        applyConsent(hasConsent(prefs))
    }

    fun hasConsent(prefs: SharedPreferences): Boolean =
        prefs.getBoolean(Prefs.ANALYTICS_CONSENT.pref, false)

    fun consentAsked(prefs: SharedPreferences): Boolean =
        prefs.getBoolean(Prefs.ANALYTICS_CONSENT_ASKED.pref, false)

    /** Persiste le choix de l'utilisateur (toggle Réglages ou dialogue) et l'applique. */
    fun setConsent(prefs: SharedPreferences, granted: Boolean) {
        prefs.edit()
            .putBoolean(Prefs.ANALYTICS_CONSENT.pref, granted)
            .putBoolean(Prefs.ANALYTICS_CONSENT_ASKED.pref, true)
            .apply()
        applyConsent(granted)
    }

    private fun applyConsent(granted: Boolean) {
        firebaseAnalytics?.setAnalyticsCollectionEnabled(granted)
        FirebaseCrashlytics.getInstance().isCrashlyticsCollectionEnabled = granted
    }

    // --- Événements ---------------------------------------------------------

    fun logQuizLaunched(
        source: String,
        category: Int,
        level: Int?,
        strategy: QuizStrategy,
        types: List<QuizType>,
    ) {
        log(EVENT_QUIZ_LAUNCHED) {
            putString(PARAM_SOURCE, source)
            putString(PARAM_CATEGORY, categorySlug(category))
            level?.let { putLong(PARAM_LEVEL, it.toLong()) }
            putString(PARAM_STRATEGY, strategy.name)
            putString(PARAM_QUIZ_TYPES, types.joinToString(",") { it.name })
        }
    }

    fun logQuizCompleted(
        sessionLength: Int,
        strategy: QuizStrategy,
        errorCount: Int,
    ) {
        log(EVENT_QUIZ_COMPLETED) {
            putLong(PARAM_SESSION_LENGTH, sessionLength.toLong())
            putString(PARAM_STRATEGY, strategy.name)
            putLong(PARAM_ERROR_COUNT, errorCount.toLong())
        }
    }

    fun logSelectionCreated() = log(EVENT_SELECTION_CREATED) {}

    fun logSelectionDeleted() = log(EVENT_SELECTION_DELETED) {}

    /**
     * Le [Bundle] (classe Android) n'est construit que si Firebase est réellement initialisé,
     * ce qui garantit qu'aucun appel n'est fait dans les tests JVM purs (où Bundle n'est pas
     * mocké et lève une RuntimeException). `setAnalyticsCollectionEnabled(false)` bloque de
     * toute façon l'envoi côté SDK quand le consentement n'est pas accordé.
     */
    private inline fun log(event: String, build: Bundle.() -> Unit) {
        val fa = firebaseAnalytics ?: return
        fa.logEvent(event, Bundle().apply(build))
    }

    /** Slug stable et indépendant de la langue pour la catégorie (jamais le nom localisé). */
    private fun categorySlug(category: Int): String = when (category) {
        Categories.CATEGORY_HIRAGANA -> "hiragana"
        Categories.CATEGORY_KATAKANA -> "katakana"
        Categories.CATEGORY_KANJI -> "kanji"
        Categories.CATEGORY_COUNTERS -> "counters"
        Categories.CATEGORY_JLPT_5 -> "jlpt5"
        Categories.CATEGORY_JLPT_4 -> "jlpt4"
        Categories.CATEGORY_JLPT_3 -> "jlpt3"
        Categories.CATEGORY_JLPT_2 -> "jlpt2"
        Categories.CATEGORY_JLPT_1 -> "jlpt1"
        Categories.CATEGORY_SELECTIONS -> "selection"
        else -> "other"
    }
}
