package com.jehutyno.yomikata.repository.local

import android.content.Context
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.instance
import org.kodein.di.singleton


fun databaseModule(context: Context) = DI.Module("databaseModule") {
    bind<YomikataDataBase>() with instance(YomikataDataBase.getDatabase(context))
    bind<QuizDao>() with singleton { instance<YomikataDataBase>().quizDao() }
    bind<WordDao>() with singleton { instance<YomikataDataBase>().wordDao() }
    bind<StatsDao>() with singleton { instance<YomikataDataBase>().statsDao() }
    bind<KanjiSoloDao>() with singleton { instance<YomikataDataBase>().kanjiSoloDao() }
    bind<SentenceDao>() with singleton { instance<YomikataDataBase>().sentenceDao() }
}
