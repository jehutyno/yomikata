package com.jehutyno.yomikata.screens.answers

import android.app.Activity
import com.jehutyno.yomikata.managers.VoicesManager
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.singleton

/**
 * Created by valentin on 25/10/2016.
 */
fun voicesManagerModule(context: Activity) = DI.Module("voicesManagerModule") {
    bind<VoicesManager>() with singleton { VoicesManager(context) }
}