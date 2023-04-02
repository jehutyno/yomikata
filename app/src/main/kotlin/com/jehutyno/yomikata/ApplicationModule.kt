package com.jehutyno.yomikata

import android.content.Context
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.instance


fun applicationModule(context: Context) = DI.Module("applicationModule") {
    bind<Context>() with instance(context)
}
