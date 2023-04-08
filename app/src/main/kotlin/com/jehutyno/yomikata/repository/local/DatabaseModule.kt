package com.jehutyno.yomikata.repository.local

import android.content.Context
import com.jehutyno.yomikata.dao.*
import org.kodein.di.*


fun databaseModule(context: Context) = DI.Module("databaseModule") {
    bind<YomikataDataBase>() with instance(YomikataDataBase.getDatabase(context))
    bind<QuizDao>() with provider { instance<YomikataDataBase>().quizDao() }
    bind<WordDao>() with provider { instance<YomikataDataBase>().wordDao() }
    bind<StatsDao>() with provider { instance<YomikataDataBase>().statsDao() }
    bind<KanjiSoloDao>() with provider { instance<YomikataDataBase>().kanjiSoloDao() }
    bind<SentenceDao>() with provider { instance<YomikataDataBase>().sentenceDao() }
}
