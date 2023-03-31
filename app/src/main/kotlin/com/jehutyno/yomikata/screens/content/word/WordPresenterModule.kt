package com.jehutyno.yomikata.screens.content.word

import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.instance

/**
 * Created by valentin on 29/09/2016.
 */
fun wordPresenterModule(view: WordContract.View) = DI.Module("wordPresenterModule") {
    bind<WordContract.View>() with instance(view)
}