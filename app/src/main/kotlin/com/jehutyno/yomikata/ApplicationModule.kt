package com.jehutyno.yomikata

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.jehutyno.yomikata.util.LanguageManager
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.instance
import org.kodein.di.singleton


fun applicationModule(context: Context) = DI.Module("applicationModule") {
    bind<Context>() with instance(context)
    bind<SharedPreferences>() with singleton { PreferenceManager.getDefaultSharedPreferences(instance()) }
    bind<LanguageManager>() with singleton { LanguageManager(instance()) }
}
