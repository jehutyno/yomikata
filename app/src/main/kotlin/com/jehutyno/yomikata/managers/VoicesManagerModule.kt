package com.jehutyno.yomikata.screens.answers

import android.app.Activity
import com.github.salomonbrys.kodein.Kodein
import com.github.salomonbrys.kodein.singleton
import com.jehutyno.yomikata.managers.VoicesManager

/**
 * Created by valentin on 25/10/2016.
 */
fun voicesManagerModule(context: Activity) = Kodein.Module {
    bind<VoicesManager>() with singleton { VoicesManager(context) }
}