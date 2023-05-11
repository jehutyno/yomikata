package com.jehutyno.yomikata

import androidx.appcompat.app.AppCompatDelegate
import androidx.multidex.MultiDexApplication
import androidx.preference.PreferenceManager
import com.facebook.stetho.Stetho
import com.jehutyno.yomikata.presenters.presenterModule
import com.jehutyno.yomikata.repository.KanjiSoloRepository
import com.jehutyno.yomikata.repository.QuizRepository
import com.jehutyno.yomikata.repository.SentenceRepository
import com.jehutyno.yomikata.repository.StatsRepository
import com.jehutyno.yomikata.repository.WordRepository
import com.jehutyno.yomikata.repository.local.KanjiSoloSource
import com.jehutyno.yomikata.repository.local.QuizSource
import com.jehutyno.yomikata.repository.local.SentenceSource
import com.jehutyno.yomikata.repository.local.StatsSource
import com.jehutyno.yomikata.repository.local.WordSource
import com.jehutyno.yomikata.repository.local.databaseModule
import com.jehutyno.yomikata.util.Prefs
import io.github.inflationx.calligraphy3.CalligraphyConfig
import io.github.inflationx.calligraphy3.CalligraphyInterceptor
import io.github.inflationx.viewpump.ViewPump
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.bind
import org.kodein.di.instance
import org.kodein.di.provider


/**
 * Created by jehutyno on 25/09/2016.
 */

class YomikataZKApplication : MultiDexApplication(), DIAware {

    companion object {
        val APP_PNAME = "com.jehutyno.yomikata"
    }

    override val di: DI by DI.lazy {
        import(applicationModule(this@YomikataZKApplication))
        import(databaseModule(this@YomikataZKApplication))
        bind<QuizRepository>() with provider { QuizSource(instance()) }
        bind<WordRepository>() with provider { WordSource(instance()) }
        bind<StatsRepository>() with provider { StatsSource(instance()) }
        bind<KanjiSoloRepository>() with provider { KanjiSoloSource(instance()) }
        bind<SentenceRepository>() with provider { SentenceSource(instance()) }
        import(presenterModule())
    }

    override fun onCreate() {
        super.onCreate()

        Stetho.initializeWithDefaults(this)

        val nightModePref = PreferenceManager.getDefaultSharedPreferences(this)
        val mode = nightModePref.getInt(Prefs.DAY_NIGHT_MODE.pref, AppCompatDelegate.MODE_NIGHT_YES)
        AppCompatDelegate.setDefaultNightMode(mode)

        ViewPump.init(ViewPump.builder()
                .addInterceptor(CalligraphyInterceptor(
                        CalligraphyConfig.Builder()
                                .setDefaultFontPath("fonts/Roboto-RobotoRegular.ttf")
                                .build()))
                .build())
    }

}
