package com.jehutyno.yomikata.screens.content

import com.github.salomonbrys.kodein.Kodein
import com.github.salomonbrys.kodein.instance

/**
 * Created by valentin on 29/09/2016.
 */
fun contentPresenterModule(view: ContentContract.View) = Kodein.Module {
    bind<ContentContract.View>() with instance(view)
}