package com.jehutyno.yomikata.screens.answers

import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.instance

/**
 * Created by valentin on 25/10/2016.
 */
fun answersPresenterModule(view: AnswersContract.View) = DI.Module("answersPresenterModule") {
    bind<AnswersContract.View>() with instance(view)
}