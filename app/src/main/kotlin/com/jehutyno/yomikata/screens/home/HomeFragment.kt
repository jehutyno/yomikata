package com.jehutyno.yomikata.screens.home

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.TextView
import androidx.preference.PreferenceManager
import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.databinding.FragmentHomeBinding
import com.jehutyno.yomikata.model.StatAction
import com.jehutyno.yomikata.model.StatEntry
import com.jehutyno.yomikata.model.StatResult
import com.jehutyno.yomikata.screens.quizzes.QuizzesActivity
import com.jehutyno.yomikata.util.*

import java.util.*


/**
 * Created by valentin on 26/12/2016.
 */
class HomeFragment : Fragment(), HomeContract.View {

    private var mpresenter: HomeContract.Presenter? = null

    // View Binding
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!


    override fun onMenuItemClick(category: Int) {

    }

    override fun setPresenter(presenter: HomeContract.Presenter) {
        mpresenter = presenter
    }

    override fun onResume() {
        super.onResume()
        mpresenter!!.start()
        mpresenter!!.loadAllStats()
        displayLatestCategories()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (mpresenter == null) {
            mpresenter = HomePresenter(requireActivity().appKodein.invoke().instance(), requireContext().appKodein.invoke().instance(), this)
        }

        val database = FirebaseDatabase.getInstance()
        val newsRef = if (Locale.getDefault().language == "fr")
            database.getReference("news_fr")
        else
            database.getReference("news_en")

        binding.expandTextView.text = getString(R.string.news_loading)

        newsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val value = dataSnapshot.getValue(String::class.java)
                binding.expandTextView.text = value
            }

            override fun onCancelled(error: DatabaseError) {
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

    fun displayStat(stats: List<StatEntry>, vararg textViews: TextView) {
        val quizLaunched = stats.filter { it.action == StatAction.LAUNCH_QUIZ_FROM_CATEGORY.value }.count()
        val wordsSeen = stats.filter { it.action == StatAction.WORD_SEEN.value }.count()
        val goodAnswer = stats.filter { it.action == StatAction.ANSWER_QUESTION.value && it.result == StatResult.SUCCESS.value }.count()
        val wrongAnswer = stats.filter { it.action == StatAction.ANSWER_QUESTION.value && it.result == StatResult.FAIL.value }.count()

        textViews[0].text = getString(R.string.quiz_launched, quizLaunched)
        textViews[1].text = getString(R.string.words_seen, wordsSeen)
        textViews[2].text = getString(R.string.good_answers, goodAnswer)
        textViews[3].text = getString(R.string.wrong_answers, wrongAnswer)
    }

    fun displayLatestCategories() {
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
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

    fun getCategoryResId(category: Int): Int {
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