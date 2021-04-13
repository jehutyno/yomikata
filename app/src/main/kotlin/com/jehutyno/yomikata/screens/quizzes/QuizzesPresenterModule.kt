package com.jehutyno.yomikata.screens.quizzes

import com.github.salomonbrys.kodein.Kodein
import com.github.salomonbrys.kodein.instance

/**
 * Created by valentin on 29/09/2016.
 */
fun quizzesPresenterModule(view: QuizzesContract.View) = Kodein.Module {
    bind<QuizzesContract.View>() with instance(view)
}