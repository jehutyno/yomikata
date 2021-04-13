package com.jehutyno.yomikata.screens.home

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.TextView
import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.model.StatAction
import com.jehutyno.yomikata.model.StatEntry
import com.jehutyno.yomikata.model.StatResult
import com.jehutyno.yomikata.screens.quizzes.QuizzesActivity
import com.jehutyno.yomikata.util.*
import kotlinx.android.synthetic.main.fragment_home.*
import org.jetbrains.anko.support.v4.defaultSharedPreferences
import java.util.*


/**
 * Created by valentin on 26/12/2016.
 */
class HomeFragment : Fragment(), HomeContract.View {

    private var mpresenter: HomeContract.Presenter? = null

    override fun onMenuItemClick(category: Int) {

    }

    override fun setPresenter(presenter: HomeContract.Presenter) {
        mpresenter = presenter
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        mpresenter!!.start()
        mpresenter!!.loadAllStats()
        displayLatestCategories()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (mpresenter == null) {
            mpresenter = HomePresenter(activity!!.appKodein.invoke().instance(), context!!.appKodein.invoke().instance(), this)
        }

        val database = FirebaseDatabase.getInstance()
        val newsRef = if (Locale.getDefault().language == "fr")
            database.getReference("news_fr")
        else
            database.getReference("news_en")

        expand_text_view.text = getString(R.string.news_loading)

        newsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val value = dataSnapshot.getValue(String::class.java)
                expand_text_view?.text = value
            }

            override fun onCancelled(error: DatabaseError) {
                expand_text_view?.text = getString(R.string.news_default)
            }
        })

        share.setOnClickListener { shareApp(activity!!) }
        facebook.setOnClickListener { contactFacebook(activity) }
        play_store.setOnClickListener { contactPlayStore(activity!!) }
        discord.setOnClickListener { contactDiscord(activity!!) }

    }

    override fun displayTodayStats(stats: List<StatEntry>) {
        displayStat(stats, today_quiz_launch, today_words_seen, today_good_answer, today_wrong_answer)
    }

    override fun displayThisWeekStats(stats: List<StatEntry>) {
        displayStat(stats, week_quiz_launch, week_words_seen, week_good_answer, week_wrong_answer)
    }

    override fun displayThisMonthStats(stats: List<StatEntry>) {
        displayStat(stats, month_quiz_launch, month_words_seen, month_good_answer, month_wrong_answer)
    }

    override fun displayTotalStats(stats: List<StatEntry>) {
        displayStat(stats, total_quiz_launch, total_words_seen, total_good_answer, total_wrong_answer)
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
        val cat1 = defaultSharedPreferences.getInt(Prefs.LATEST_CATEGORY_1.pref, -1)
        val cat2 = defaultSharedPreferences.getInt(Prefs.LATEST_CATEGORY_2.pref, -1)

        if (cat1 != -1) {
            last_category_1.visibility = VISIBLE
            last_category_1.setImageResource(getCategoryResId(cat1))
            last_category_1.setOnClickListener {
                (activity as QuizzesActivity).gotoCategory(cat1)
            }
        } else {
            last_category_1.visibility = GONE
        }

        if (cat2 != -1) {
            last_category_2.visibility = VISIBLE
            last_category_2.setImageResource(getCategoryResId(cat2))
            last_category_2.setOnClickListener {
                (activity as QuizzesActivity).gotoCategory(cat2)
            }
        } else {
            last_category_2.visibility = GONE
        }
        no_categories.visibility = if (cat1 == -1 && cat2 == -1) VISIBLE else GONE
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
}