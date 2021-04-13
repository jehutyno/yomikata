package com.jehutyno.yomikata.screens.quiz

import com.github.salomonbrys.kodein.Kodein
import com.github.salomonbrys.kodein.instance

/**
 * Created by valentin on 18/10/2016.
 */
fun quizPresenterModule(view: QuizContract.View) = Kodein.Module {
    bind<QuizContract.View>() with instance(view)
}
