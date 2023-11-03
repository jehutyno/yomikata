package com.jehutyno.yomikata.repository.database

import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.instance
import org.kodein.di.provider


fun databaseModule() = DI.Module("databaseModule") {
    bind<YomikataDatabase>() with provider { YomikataDatabase.getDatabase(instance()) }
}
