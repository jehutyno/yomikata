package com.jehutyno.yomikata.screens.quiz

import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.instance

/**
 * Created by valentin on 18/10/2016.
 */
fun quizPresenterModule(view: QuizContract.View) = DI.Module("quizPresenterModule") {
    bind<QuizContract.View>() with instance(view)
}
