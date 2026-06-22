package com.jehutyno.yomikata

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import com.jehutyno.yomikata.repository.database.dao.daoModule
import com.jehutyno.yomikata.presenters.impl.presenterModule
import com.jehutyno.yomikata.repository.database.databaseModule
import com.jehutyno.yomikata.repository.local.repositoryModule
import com.jehutyno.yomikata.util.analytics.Analytics
import com.jehutyno.yomikata.util.language.LanguageManager
import io.github.inflationx.calligraphy3.CalligraphyConfig
import io.github.inflationx.calligraphy3.CalligraphyInterceptor
import io.github.inflationx.viewpump.ViewPump
import org.kodein.di.DI
import org.kodein.di.DIAware


/**
 * Created by jehutyno on 25/09/2016.
 */

class YomikataZKApplication : Application(), DIAware {

    companion object {
        const val APP_PNAME = "com.jehutyno.yomikata"
    }

    override val di: DI by DI.lazy {
        import(applicationModule(this@YomikataZKApplication))
        import(databaseModule())
        import(daoModule())
        import(repositoryModule())
        import(presenterModule())
    }

    override fun onCreate() {
        super.onCreate()

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        // Bootstrap language before any getTrad() call (models read LanguageManager.current)
        LanguageManager.initFromPrefs(prefs)
        // Applique l'état de consentement analytics persisté (OFF tant que pas d'opt-in explicite)
        Analytics.init(this, prefs)
        // App is dark-only: the whole design system is built exclusively for the dark theme.
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)

        ViewPump.init(ViewPump.builder()
                .addInterceptor(CalligraphyInterceptor(
                        CalligraphyConfig.Builder()
                                .setDefaultFontPath("fonts/Roboto-RobotoRegular.ttf")
                                .build()))
                .build())
    }

}
