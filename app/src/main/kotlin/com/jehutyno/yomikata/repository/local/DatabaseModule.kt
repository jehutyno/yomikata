package com.jehutyno.yomikata.repository.local

import android.content.Context
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.instance


fun databaseModule(context: Context) = DI.Module("databaseModule") {
    bind<YomikataDataBase>() with instance(YomikataDataBase.getDatabase(context))
}
