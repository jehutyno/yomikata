package com.jehutyno.yomikata.screens.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.databinding.FragmentHomeBinding
import com.jehutyno.yomikata.model.StatEntry
import com.jehutyno.yomikata.screens.quizzes.QuizzesActivity
import com.jehutyno.yomikata.ui.home.HomeScreen
import com.jehutyno.yomikata.ui.home.HomeUiState
import com.jehutyno.yomikata.ui.study.categoryName
import com.jehutyno.yomikata.ui.theme.YomikataTheme
import com.jehutyno.yomikata.util.Prefs
import com.jehutyno.yomikata.util.quiz.Categories
import com.jehutyno.yomikata.util.language.AppLanguage
import com.jehutyno.yomikata.util.language.LanguageManager
import org.kodein.di.DI
import org.kodein.di.instance
import org.kodein.di.newInstance


class HomeFragment(di: DI) : Fragment(), HomeContract.View {

    private val mpresenter: HomeContract.Presenter by di.newInstance {
        HomePresenter(instance())
    }

    private lateinit var newsRef: DatabaseReference

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private var uiState by mutableStateOf(HomeUiState())

    override fun onMenuItemClick(category: Int) {}

    override fun onStart() {
        super.onStart()
        mpresenter.start()
        subscribeStatsDisplay()
        refreshLastSession()
    }

    override fun onResume() {
        super.onResume()
        mpresenter.start()
        refreshLastSession()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        binding.homeComposeView.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                YomikataTheme {
                    HomeScreen(
                        state = uiState,
                        onFabClick = { handleFabClick() },
                    )
                }
            }
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val database = FirebaseDatabase.getInstance()
        val newsNode = when (LanguageManager.current) {
            AppLanguage.FRENCH     -> "news_fr"
            AppLanguage.GERMAN     -> "news_de"
            AppLanguage.SPANISH    -> "news_es"
            AppLanguage.PORTUGUESE -> "news_pt"
            AppLanguage.CHINESE    -> "news_zh"
            else                   -> "news_en"
        }
        newsRef = database.getReference(newsNode)
        uiState = uiState.copy(newsLoading = true)

        newsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (_binding == null) return
                uiState = uiState.copy(
                    newsText = dataSnapshot.getValue(String::class.java) ?: "",
                    newsLoading = false,
                )
            }

            override fun onCancelled(error: DatabaseError) {
                if (_binding == null) return
                uiState = uiState.copy(
                    newsText = getString(R.string.news_default),
                    newsLoading = false,
                )
            }
        })
    }

    override fun displayTodayStats(stats: List<StatEntry>) {
        uiState = uiState.copy(
            quizLaunched = mpresenter.getNumberOfLaunchedQuizzes(stats),
            wordsSeen = mpresenter.getNumberOfWordsSeen(stats),
            correctAnswers = mpresenter.getNumberOfCorrectAnswers(stats),
            wrongAnswers = mpresenter.getNumberOfWrongAnswers(stats),
        )
    }

    override fun displayThisWeekStats(stats: List<StatEntry>) {}
    override fun displayThisMonthStats(stats: List<StatEntry>) {}
    override fun displayTotalStats(stats: List<StatEntry>) {}

    private fun subscribeStatsDisplay() {
        mpresenter.todayStatList.observe(viewLifecycleOwner) { stats ->
            displayTodayStats(stats)
        }
    }

    private fun refreshLastSession() {
        val pref = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val lastCategory = pref.getInt(Prefs.LAST_SELECTED_LEVEL.pref, -1)
        uiState = if (lastCategory != -1) {
            uiState.copy(lastSessionLevel = categoryName(lastCategory))
        } else {
            uiState.copy(lastSessionLevel = null)
        }
    }

    private fun handleFabClick() {
        val pref = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val lastCategory = pref.getInt(Prefs.LAST_SELECTED_LEVEL.pref, Categories.CATEGORY_HIRAGANA)
        (activity as? QuizzesActivity)?.navigateToCategory(lastCategory)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
