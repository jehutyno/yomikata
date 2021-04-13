package com.jehutyno.yomikata.screens.content.word

import com.github.salomonbrys.kodein.Kodein
import com.github.salomonbrys.kodein.instance

/**
 * Created by valentin on 29/09/2016.
 */
fun wordPresenterModule(view: WordContract.View) = Kodein.Module {
    bind<WordContract.View>() with instance(view)
}