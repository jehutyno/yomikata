package com.jehutyno.yomikata.screens.content

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.PagerAdapter
import com.jehutyno.yomikata.model.Quiz
import com.jehutyno.yomikata.util.Extras
import org.jetbrains.anko.support.v4.withArguments

/**
 * Created by valentin on 19/12/2016.
 */
class ContentPagerAdapter(val context : Context, fm: FragmentManager, var quizzes: List<Quiz>) : FragmentStatePagerAdapter(fm) {

    override fun getItem(position: Int): Fragment {
        val contentFragment = ContentFragment().withArguments(Extras.EXTRA_QUIZ_IDS to longArrayOf(quizzes[position].id), Extras.EXTRA_QUIZ_TITLE to quizzes[position].getName())
        return contentFragment
    }

    override fun getCount(): Int {
        return quizzes.size
    }

    override fun getItemPosition(`object`: Any): Int {
        return PagerAdapter.POSITION_NONE
    }

}
