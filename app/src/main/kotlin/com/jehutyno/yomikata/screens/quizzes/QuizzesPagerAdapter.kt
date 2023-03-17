package com.jehutyno.yomikata.screens.content

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.screens.home.HomeFragment
import com.jehutyno.yomikata.screens.quizzes.QuizzesFragment
import com.jehutyno.yomikata.util.Categories
import com.jehutyno.yomikata.util.Extras


/**
 * Created by valentin on 19/12/2016.
 */
class QuizzesPagerAdapter(val context: Context, fm: FragmentManager, lifecycle: Lifecycle) : FragmentStateAdapter(fm, lifecycle) {

    val categories = intArrayOf(Categories.HOME,
        Categories.CATEGORY_SELECTIONS,
        Categories.CATEGORY_HIRAGANA,
        Categories.CATEGORY_KATAKANA,
        Categories.CATEGORY_KANJI,
        Categories.CATEGORY_COUNTERS,
        Categories.CATEGORY_JLPT_5,
        Categories.CATEGORY_JLPT_4,
        Categories.CATEGORY_JLPT_3,
        Categories.CATEGORY_JLPT_2,
        Categories.CATEGORY_JLPT_1)
//    val registered: SparseArray<Fragment> = SparseArray()

    override fun getItemCount(): Int {
        return categories.size
    }

    override fun createFragment(position: Int): Fragment {
        return if (categories[position] == Categories.HOME) {
            HomeFragment()
        } else {
            val bundle = Bundle()
            bundle.putInt(Extras.EXTRA_CATEGORY, categories[position])
            val quizzesFragment = QuizzesFragment()
            quizzesFragment.arguments = bundle
            quizzesFragment
        }
    }

    fun positionFromCategory(selectedCategory: Int): Int {
        return when (selectedCategory) {
            Categories.HOME -> 0
            Categories.CATEGORY_SELECTIONS -> 1
            Categories.CATEGORY_HIRAGANA -> 2
            Categories.CATEGORY_KATAKANA -> 3
            Categories.CATEGORY_KANJI -> 4
            Categories.CATEGORY_COUNTERS -> 5
            Categories.CATEGORY_JLPT_5 -> 6
            Categories.CATEGORY_JLPT_4 -> 7
            Categories.CATEGORY_JLPT_3 -> 8
            Categories.CATEGORY_JLPT_2 -> 9
            else -> 10
        }
    }

    fun getMenuItemFromPosition(position: Int): Int {
        return when (position) {
            0 -> R.id.home
            1 -> R.id.your_selections_item
            2 -> R.id.hiragana_item
            3 -> R.id.katakana_item
            4 -> R.id.kanji_item
            5 -> R.id.counters_item
            6 -> R.id.jlpt5_item
            7 -> R.id.jlpt4_item
            8 -> R.id.jlpt3_item
            9 -> R.id.jlpt2_item
            else -> R.id.jlpt1_item
        }
    }

}
