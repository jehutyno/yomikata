package com.jehutyno.yomikata.repository.local

import com.jehutyno.yomikata.repository.KanjiSoloRepository
import com.jehutyno.yomikata.repository.QuizRepository
import com.jehutyno.yomikata.repository.SentenceRepository
import com.jehutyno.yomikata.repository.StatsRepository
import com.jehutyno.yomikata.repository.WordRepository
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.instance
import org.kodein.di.provider


fun repositoryModule() = DI.Module("repositoryModule") {
    bind<QuizRepository>() with provider { QuizSource(instance()) }
    bind<WordRepository>() with provider { WordSource(instance()) }
    bind<StatsRepository>() with provider { StatsSource(instance()) }
    bind<KanjiSoloRepository>() with provider { KanjiSoloSource(instance()) }
    bind<SentenceRepository>() with provider { SentenceSource(instance()) }
}
