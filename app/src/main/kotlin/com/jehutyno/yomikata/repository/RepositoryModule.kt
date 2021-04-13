package com.jehutyno.yomikata.repository

import com.github.salomonbrys.kodein.Kodein
import com.github.salomonbrys.kodein.instance
import com.github.salomonbrys.kodein.singleton
import com.jehutyno.yomikata.repository.local.*

fun repositoryModule() = Kodein.Module {
    bind<QuizRepository>("Local") with singleton { QuizSource(instance()) }
    bind<WordRepository>("Local") with singleton { WordSource(instance()) }
    bind<StatsRepository>("Local") with singleton { StatsSource(instance()) }
    bind<KanjiSoloRepository>("Local") with singleton { KanjiSoloSource(instance()) }
    bind<SentenceRepository>("Local") with singleton { SentenceSource(instance()) }
}
