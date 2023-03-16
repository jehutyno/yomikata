package com.jehutyno.yomikata.screens.content

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.jehutyno.yomikata.model.Quiz
import com.jehutyno.yomikata.util.Extras


/**
 * Created by valentin on 19/12/2016.
 */
class ContentPagerAdapter(val context : Context, fm: FragmentManager, var quizzes: List<Quiz>,
                          lifecycle: Lifecycle) : FragmentStateAdapter (fm, lifecycle) {

//    override fun getItemPosition(`object`: Any): Int {
//        return PagerAdapter.POSITION_NONE
//    }

    override fun getItemCount(): Int {
        return quizzes.size
    }

    override fun createFragment(position: Int): Fragment {
        val bundle = Bundle()
        bundle.putLongArray(Extras.EXTRA_QUIZ_IDS, longArrayOf(quizzes[position].id))
        bundle.putString(Extras.EXTRA_QUIZ_TITLE, quizzes[position].getName())
        val contentFragment = ContentFragment()
        contentFragment.arguments = bundle
        return contentFragment
    }

}
