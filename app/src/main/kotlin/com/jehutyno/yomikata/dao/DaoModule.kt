package com.jehutyno.yomikata.dao

import com.jehutyno.yomikata.repository.database.YomikataDatabase
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.instance
import org.kodein.di.provider


fun daoModule() = DI.Module("daoModule") {
    bind<QuizDao>() with provider { instance<YomikataDatabase>().quizDao() }
    bind<WordDao>() with provider { instance<YomikataDatabase>().wordDao() }
    bind<StatsDao>() with provider { instance<YomikataDatabase>().statsDao() }
    bind<KanjiSoloDao>() with provider { instance<YomikataDatabase>().kanjiSoloDao() }
    bind<SentenceDao>() with provider { instance<YomikataDatabase>().sentenceDao() }
}
