package com.jehutyno.yomikata.screens.answers

import com.github.salomonbrys.kodein.Kodein
import com.github.salomonbrys.kodein.instance

/**
 * Created by valentin on 25/10/2016.
 */
fun answersPresenterModule(view: AnswersContract.View) = Kodein.Module {
    bind<AnswersContract.View>() with instance(view)
}