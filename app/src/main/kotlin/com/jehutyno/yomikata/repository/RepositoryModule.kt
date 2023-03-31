package com.jehutyno.yomikata.repository

import com.jehutyno.yomikata.repository.local.*
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.instance
import org.kodein.di.singleton

fun repositoryModule() = DI.Module("repositoryModule") {
    bind<QuizRepository>("Local") with singleton { QuizSource(instance()) }
    bind<WordRepository>("Local") with singleton { WordSource(instance()) }
    bind<StatsRepository>("Local") with singleton { StatsSource(instance()) }
    bind<KanjiSoloRepository>("Local") with singleton { KanjiSoloSource(instance()) }
    bind<SentenceRepository>("Local") with singleton { SentenceSource(instance()) }
}
