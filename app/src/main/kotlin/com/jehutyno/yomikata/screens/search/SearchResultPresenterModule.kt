package com.jehutyno.yomikata.screens.search

import com.github.salomonbrys.kodein.Kodein
import com.github.salomonbrys.kodein.instance

/**
 * Created by valentin on 13/10/2016.
 */
fun searchResultPresenterModule(view: SearchResultContract.View) = Kodein.Module {
    bind<SearchResultContract.View>() with instance(view)
}
