package com.jehutyno.yomikata

import androidx.appcompat.app.AppCompatDelegate
import androidx.multidex.MultiDexApplication
import com.facebook.stetho.Stetho
import com.github.salomonbrys.kodein.*
import com.jehutyno.yomikata.repository.*
import com.jehutyno.yomikata.repository.local.*
import com.jehutyno.yomikata.util.Prefs
import io.github.inflationx.calligraphy3.CalligraphyConfig
import io.github.inflationx.calligraphy3.CalligraphyInterceptor
import io.github.inflationx.viewpump.ViewPump
import org.jetbrains.anko.defaultSharedPreferences


/**
 * Created by jehutyno on 25/09/2016.
 */

class YomikataZKApplication : MultiDexApplication(), KodeinAware {

    companion object {
        val APP_PNAME = "com.jehutyno.yomikata"
    }

    override val kodein: Kodein by Kodein.lazy {
        import(repositoryModule())
        import(applicationModule(this@YomikataZKApplication))
        bind<QuizRepository>() with singleton { QuizSource(instance()) }
        bind<WordRepository>() with singleton { WordSource(instance()) }
        bind<StatsRepository>() with singleton { StatsSource(instance()) }
        bind<KanjiSoloRepository>() with singleton { KanjiSoloSource(instance()) }
        bind<SentenceRepository>() with singleton { SentenceSource(instance()) }
    }

    override fun onCreate() {
        super.onCreate()

        Stetho.initializeWithDefaults(this)
        AppCompatDelegate.setDefaultNightMode(defaultSharedPreferences.getInt(Prefs.DAY_NIGHT_MODE.pref, AppCompatDelegate.MODE_NIGHT_YES))
        ViewPump.init(ViewPump.builder()
                .addInterceptor(CalligraphyInterceptor(
                        CalligraphyConfig.Builder()
                                .setDefaultFontPath("fonts/Roboto-RobotoRegular.ttf")
                                .build()))
                .build())
        database.open()
        database.close()
    }

}

