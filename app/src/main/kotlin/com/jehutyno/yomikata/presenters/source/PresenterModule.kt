package com.jehutyno.yomikata.presenters.source

import com.jehutyno.yomikata.presenters.SelectionsInterface
import com.jehutyno.yomikata.presenters.WordCountInterface
import com.jehutyno.yomikata.presenters.WordInQuizInterface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.factory
import org.kodein.di.instance
import org.kodein.di.provider


fun presenterModule() = DI.Module("presenterModule") {
    bind<SelectionsInterface>() with factory {
        coroutineScope: CoroutineScope -> SelectionsPresenter(instance(), coroutineScope)
    }
    bind<WordCountInterface>() with factory {
        quizIds: LongArray -> WordCountPresenter(instance(), quizIds)
    }
    bind<WordCountInterface>() with factory {
        quizIdsFlow: Flow<LongArray> -> WordCountPresenter(instance(), quizIdsFlow)
    }
    bind<WordInQuizInterface>() with provider { WordInQuizPresenter(instance()) }
}
