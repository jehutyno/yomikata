package com.jehutyno.yomikata.screens.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.TextView
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
import com.jehutyno.yomikata.util.quiz.Categories
import com.jehutyno.yomikata.util.Prefs
import com.jehutyno.yomikata.util.contactDiscord
import com.jehutyno.yomikata.util.contactFacebook
import com.jehutyno.yomikata.util.contactPlayStore
import com.jehutyno.yomikata.util.shareApp
import org.kodein.di.DI
import org.kodein.di.instance
import org.kodein.di.newInstance
import com.jehutyno.yomikata.util.language.AppLanguage
import com.jehutyno.yomikata.util.language.LanguageManager


/**
 * Created by valentin on 26/12/2016.
 */
class HomeFragment(di: DI) : Fragment(), HomeContract.View {

    // kodein
    private val mpresenter: HomeContract.Presenter by di.newInstance {
        HomePresenter(instance())
    }

    private lateinit var newsRef: DatabaseReference

    // View Binding
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!


    override fun onMenuItemClick(category: Int) {

    }

    override fun onStart() {
        // preload for viewPager2
        super.onStart()
        mpresenter.start()
        subscribeStatsDisplay()
        displayLatestCategories()
    }

    override fun onResume() {
        super.onResume()
        mpresenter.start()
        displayLatestCategories()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
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

        binding.expandTextView.text = getString(R.string.news_loading)
        newsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (_binding == null) return
                binding.expandTextView.text = dataSnapshot.getValue(String::class.java)
            }

            override fun onCancelled(error: DatabaseError) {
                if (_binding == null) return
                binding.expandTextView.text = getString(R.string.news_default)
            }
        })

        binding.share.setOnClickListener { shareApp(requireActivity()) }
        binding.facebook.setOnClickListener { contactFacebook(activity) }
        binding.playStore.setOnClickListener { contactPlayStore(requireActivity()) }
        binding.discord.setOnClickListener { contactDiscord(requireActivity()) }

    }

    override fun displayTodayStats(stats: List<StatEntry>) {
        displayStat(stats, binding.todayQuizLaunch, binding.todayWordsSeen, binding.todayGoodAnswer, binding.todayWrongAnswer)
    }

    override fun displayThisWeekStats(stats: List<StatEntry>) {
        displayStat(stats, binding.weekQuizLaunch, binding.weekWordsSeen, binding.weekGoodAnswer, binding.weekWrongAnswer)
    }

    override fun displayThisMonthStats(stats: List<StatEntry>) {
        displayStat(stats, binding.monthQuizLaunch, binding.monthWordsSeen, binding.monthGoodAnswer, binding.monthWrongAnswer)
    }

    override fun displayTotalStats(stats: List<StatEntry>) {
        displayStat(stats, binding.totalQuizLaunch, binding.totalWordsSeen, binding.totalGoodAnswer, binding.totalWrongAnswer)
    }

    private fun displayStat(stats: List<StatEntry>, vararg textViews: TextView) {
        val quizLaunched = mpresenter.getNumberOfLaunchedQuizzes(stats)
        val wordsSeen = mpresenter.getNumberOfWordsSeen(stats)
        val correctAnswer = mpresenter.getNumberOfCorrectAnswers(stats)
        val wrongAnswer = mpresenter.getNumberOfWrongAnswers(stats)

        textViews[0].text = getString(R.string.quiz_launched, quizLaunched)
        textViews[1].text = getString(R.string.words_seen, wordsSeen)
        textViews[2].text = getString(R.string.good_answers, correctAnswer)
        textViews[3].text = getString(R.string.wrong_answers, wrongAnswer)
    }

    /**
     * Subscribe stats display
     *
     * Set subscribers to automatically update stats when database changes
     */
    private fun subscribeStatsDisplay() {
        mpresenter.todayStatList.observe(viewLifecycleOwner) { stats ->
            displayTodayStats(stats)
        }
        mpresenter.thisWeekStatList.observe(viewLifecycleOwner) { stats ->
            displayThisWeekStats(stats)
        }
        mpresenter.thisMonthStatList.observe(viewLifecycleOwner) { stats ->
            displayThisMonthStats(stats)
        }
        mpresenter.totalStatList.observe(viewLifecycleOwner) { stats ->
            displayTotalStats(stats)
        }
    }

    private fun displayLatestCategories() {
        val pref = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val cat1 = pref.getInt(Prefs.LATEST_CATEGORY_1.pref, -1)
        val cat2 = pref.getInt(Prefs.LATEST_CATEGORY_2.pref, -1)

        if (cat1 != -1) {
            binding.lastCategory1.visibility = VISIBLE
            binding.lastCategory1.setImageResource(getCategoryResId(cat1))
            binding.lastCategory1.setOnClickListener {
                (activity as QuizzesActivity).gotoCategory(cat1)
            }
        } else {
            binding.lastCategory1.visibility = GONE
        }

        if (cat2 != -1) {
            binding.lastCategory2.visibility = VISIBLE
            binding.lastCategory2.setImageResource(getCategoryResId(cat2))
            binding.lastCategory2.setOnClickListener {
                (activity as QuizzesActivity).gotoCategory(cat2)
            }
        } else {
            binding.lastCategory2.visibility = GONE
        }
        binding.noCategories.visibility = if (cat1 == -1 && cat2 == -1) VISIBLE else GONE
    }

    private fun getCategoryResId(category: Int): Int {
        return when (category) {
            Categories.CATEGORY_HIRAGANA -> {
                R.drawable.ic_hiragana_big
            }
            Categories.CATEGORY_KATAKANA -> {
                R.drawable.ic_katakana_big
            }
            Categories.CATEGORY_KANJI -> {
                R.drawable.ic_kanji_big
            }
            Categories.CATEGORY_COUNTERS -> {
                R.drawable.ic_counters_big
            }
            Categories.CATEGORY_JLPT_1 -> {
                R.drawable.ic_jlpt1_big
            }
            Categories.CATEGORY_JLPT_2 -> {
                R.drawable.ic_jlpt2_big
            }
            Categories.CATEGORY_JLPT_3 -> {
                R.drawable.ic_jlpt3_big
            }
            Categories.CATEGORY_JLPT_4 -> {
                R.drawable.ic_jlpt4_big
            }
            Categories.CATEGORY_JLPT_5 -> {
                R.drawable.ic_jlpt5_big
            }
            else -> {
                R.drawable.ic_selections_big
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
