package com.jehutyno.yomikata.screens.search

import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.instance

/**
 * Created by valentin on 13/10/2016.
 */
fun searchResultPresenterModule(view: SearchResultContract.View) = DI.Module("searchResultPresenterModule") {
    bind<SearchResultContract.View>() with instance(view)
}
