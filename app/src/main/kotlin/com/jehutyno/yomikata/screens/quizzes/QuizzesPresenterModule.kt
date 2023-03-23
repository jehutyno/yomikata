package com.jehutyno.yomikata.screens.quizzes

import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.instance

/**
 * Created by valentin on 29/09/2016.
 */
fun quizzesPresenterModule(view: QuizzesContract.View) = DI.Module("quizzesPresenterModule") {
    bind<QuizzesContract.View>() with instance(view)
}