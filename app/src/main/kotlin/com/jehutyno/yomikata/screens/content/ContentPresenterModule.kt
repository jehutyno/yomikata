package com.jehutyno.yomikata.screens.content

import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.instance

/**
 * Created by valentin on 29/09/2016.
 */
fun contentPresenterModule(view: ContentContract.View) = DI.Module("contentPresenterModule") {
    bind<ContentContract.View>() with instance(view)
}