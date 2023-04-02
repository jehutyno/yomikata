package com.jehutyno.yomikata

import androidx.appcompat.app.AppCompatDelegate
import androidx.multidex.MultiDexApplication
import androidx.preference.PreferenceManager
import com.facebook.stetho.Stetho
import com.jehutyno.yomikata.repository.*
import com.jehutyno.yomikata.repository.local.*
import com.jehutyno.yomikata.util.Prefs
import io.github.inflationx.calligraphy3.CalligraphyConfig
import io.github.inflationx.calligraphy3.CalligraphyInterceptor
import io.github.inflationx.viewpump.ViewPump
import org.kodein.di.*


/**
 * Created by jehutyno on 25/09/2016.
 */

class YomikataZKApplication : MultiDexApplication(), DIAware {

    companion object {
        val APP_PNAME = "com.jehutyno.yomikata"
    }

    override val di: DI by DI.lazy {
        import(repositoryModule())
        import(applicationModule(this@YomikataZKApplication))
        import(databaseModule(this@YomikataZKApplication))
        bind<QuizRepository>() with singleton { QuizSource(instance<YomikataDataBase>().quizDao()) }
        bind<WordRepository>() with singleton { WordSource(instance<YomikataDataBase>().wordDao()) }
        bind<StatsRepository>() with singleton { StatsSource(instance<YomikataDataBase>().statsDao()) }
        bind<KanjiSoloRepository>() with singleton { KanjiSoloSource(instance<YomikataDataBase>().kanjiSoloDao()) }
        bind<SentenceRepository>() with singleton { SentenceSource(instance<YomikataDataBase>().sentenceDao()) }
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
        database.open()
        database.close()
    }

}

