package com.jehutyno.yomikata.util

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import com.jehutyno.yomikata.screens.content.ContentFragment
import com.jehutyno.yomikata.screens.content.word.WordDetailDialogFragment
import com.jehutyno.yomikata.screens.content.word.WordDetailFragment
import com.jehutyno.yomikata.screens.quiz.QuizFragment
import org.kodein.di.DI


/**
 * FragmentFactory qui sait instancier les fragments applicatifs porteurs d'une dépendance [DI]
 * au constructeur.
 *
 * Ces fragments n'ont pas de constructeur no-arg : sans cette factory, le [androidx.fragment.app.FragmentManager]
 * tente de les recréer par réflexion (constructeur vide) lors d'une restauration d'état
 * (config change, changement de langue in-app, mort du process) et lève
 * `NoSuchMethodException` → `Fragment$InstantiationException` → crash au démarrage de l'activité
 * (remonté par Crashlytics en 2.0.2).
 *
 * À poser sur le FragmentManager **avant** `super.onCreate(...)` de l'hôte, avec le [DI] attendu
 * par le fragment (subDI étendu pour [QuizFragment], DI de base pour les autres).
 */
class DiFragmentFactory(private val di: DI) : FragmentFactory() {
    override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
        return when (className) {
            QuizFragment::class.java.name             -> QuizFragment(di)
            ContentFragment::class.java.name          -> ContentFragment(di)
            WordDetailFragment::class.java.name       -> WordDetailFragment(di)
            WordDetailDialogFragment::class.java.name -> WordDetailDialogFragment(di)
            else -> super.instantiate(classLoader, className)
        }
    }
}
