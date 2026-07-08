package com.jehutyno.yomikata.screens.quiz

import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.window.OnBackInvokedDispatcher
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import android.content.SharedPreferences
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.databinding.ActivityQuizBinding
import com.jehutyno.yomikata.util.DiFragmentFactory
import com.jehutyno.yomikata.util.Extras
import com.jehutyno.yomikata.util.quiz.Level
import com.jehutyno.yomikata.util.quiz.QuizStrategy
import com.jehutyno.yomikata.util.quiz.QuizType
import com.jehutyno.yomikata.util.addOrReplaceFragment
import com.jehutyno.yomikata.util.getParcelableArrayListExtraHelper
import com.jehutyno.yomikata.util.getParcelableArrayListHelper
import com.jehutyno.yomikata.util.getSerializableExtraHelper
import com.jehutyno.yomikata.util.getSerializableHelper
import io.github.inflationx.viewpump.ViewPumpContextWrapper
import mu.KLogging
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.android.closestDI
import org.kodein.di.bind
import org.kodein.di.factory
import org.kodein.di.instance
import com.jehutyno.yomikata.ui.components.DialogButton
import com.jehutyno.yomikata.ui.components.DialogButtonStyle
import com.jehutyno.yomikata.ui.components.yomikataAlert
import java.util.Random


class QuizActivity : AppCompatActivity(), DIAware {

    companion object : KLogging()

    // kodein
    override val di: DI by closestDI()
    private val subDI by DI.lazy {
        extend(di)
        bind<QuizContract.Presenter>() with factory {
            view: QuizContract.View ->
            QuizPresenter (
                instance(), instance<SharedPreferences>(), instance(), instance(), instance(), view,
                quizIds, quizStrategy, level, quizTypes, Random(),
                instance(arg = lifecycleScope), instance(), lifecycleScope
            )
        }
    }

    private lateinit var binding: ActivityQuizBinding
    private lateinit var quizFragment: QuizFragment

    private lateinit var quizIds: LongArray
    private lateinit var quizStrategy: QuizStrategy
    private var level: Level? = null
    private lateinit var quizTypes: ArrayList<QuizType>

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(ViewPumpContextWrapper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Doit être posé AVANT super.onCreate : c'est là que le FragmentManager restaure
        // QuizFragment (constructeur à DI, pas de no-arg) → sans factory, NoSuchMethodException.
        // On passe subDI (porte le binding factory du présenteur), pas le DI de base.
        supportFragmentManager.fragmentFactory = DiFragmentFactory(subDI)
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityQuizBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if(resources.getBoolean(R.bool.portrait_only)){
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        if (savedInstanceState != null) {
            //Restore the fragment's instance
            quizFragment = supportFragmentManager.getFragment(savedInstanceState, "quizFragment") as QuizFragment
            quizIds = savedInstanceState.getLongArray("quiz_ids")?: longArrayOf()

            // Sans stratégie, aucun quiz ne peut démarrer : fermer proprement plutôt que
            // crasher (l'extra Serializable peut revenir null à la restauration après mort
            // du process). Cf. helpers durcis + crash prod 2.0.2.
            quizStrategy = savedInstanceState.getSerializableHelper("quiz_strategy", QuizStrategy::class.java)
                ?: run { finish(); return }
            level = savedInstanceState.getSerializableHelper("level", Level::class.java)

            quizTypes = savedInstanceState.getParcelableArrayListHelper("quiz_types", QuizType::class.java)?: arrayListOf()
        } else {
            quizIds = intent.getLongArrayExtra(Extras.EXTRA_QUIZ_IDS) ?: longArrayOf()

            // Sans stratégie, aucun quiz ne peut démarrer : fermer proprement plutôt que
            // crasher (l'extra Serializable peut revenir null quand le système redélivre
            // l'Intent après mort du process). Cf. helpers durcis + crash prod 2.0.2.
            quizStrategy = intent.getSerializableExtraHelper(Extras.EXTRA_QUIZ_STRATEGY, QuizStrategy::class.java)
                ?: run { finish(); return }

            level = intent.getSerializableExtraHelper(Extras.EXTRA_LEVEL, Level::class.java)

            quizTypes = intent.getParcelableArrayListExtraHelper(Extras.EXTRA_QUIZ_TYPES, QuizType::class.java) ?: arrayListOf()

            val bundle = Bundle()
            bundle.putLongArray(Extras.EXTRA_QUIZ_IDS, quizIds)
            bundle.putSerializable(Extras.EXTRA_QUIZ_STRATEGY, quizStrategy)
            bundle.putParcelableArrayList(Extras.EXTRA_QUIZ_TYPES, quizTypes)

            quizFragment = QuizFragment(subDI)
            quizFragment.arguments = bundle
        }
        addOrReplaceFragment(R.id.container_content, quizFragment)

        fun askToQuitSession() {
            yomikataAlert(
                message = getString(R.string.quit_quiz),
                onBackKey = { finish() },
                buttons = listOf(
                    DialogButton(getString(android.R.string.cancel), DialogButtonStyle.Muted) {},
                    DialogButton(getString(android.R.string.ok), DialogButtonStyle.Primary) { finish() },
                ),
            ).show()
        }

        // set back button: ask if user wants to quit out of session
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_DEFAULT
            ) {
                askToQuitSession()
            }
        } else {
            onBackPressedDispatcher.addCallback(this /* lifecycle owner */) {
                askToQuitSession()
            }
        }

    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        //Save the fragment's instance
        supportFragmentManager.putFragment(outState, "quizFragment", quizFragment)
        outState.putLongArray("quiz_ids", quizIds)
        outState.putSerializable("quiz_strategy", quizStrategy)
        outState.putSerializable("level", level)
        outState.putParcelableArrayList("quiz_types", quizTypes)
    }

}
