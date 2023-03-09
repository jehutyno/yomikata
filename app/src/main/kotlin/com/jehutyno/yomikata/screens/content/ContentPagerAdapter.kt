package com.jehutyno.yomikata.screens.content

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.PagerAdapter
import com.jehutyno.yomikata.model.Quiz
import com.jehutyno.yomikata.util.Extras


/**
 * Created by valentin on 19/12/2016.
 */
class ContentPagerAdapter(val context : Context, fm: FragmentManager, var quizzes: List<Quiz>) : FragmentStatePagerAdapter(fm) {

    override fun getItem(position: Int): Fragment {
        val bundle = Bundle()
        bundle.putLongArray(Extras.EXTRA_QUIZ_IDS, longArrayOf(quizzes[position].id))
        bundle.putString(Extras.EXTRA_QUIZ_TITLE, quizzes[position].getName())
        val contentFragment = ContentFragment()
        contentFragment.arguments = bundle
        return contentFragment
    }

    override fun getCount(): Int {
        return quizzes.size
    }

    override fun getItemPosition(`object`: Any): Int {
        return PagerAdapter.POSITION_NONE
    }

}
