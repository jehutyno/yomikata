package com.jehutyno.yomikata.screens.content

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.PagerAdapter
import android.util.SparseArray
import android.view.ViewGroup
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.screens.home.HomeFragment
import com.jehutyno.yomikata.screens.quizzes.QuizzesFragment
import com.jehutyno.yomikata.util.Categories
import com.jehutyno.yomikata.util.Extras
import org.jetbrains.anko.support.v4.withArguments

/**
 * Created by valentin on 19/12/2016.
 */
class QuizzesPagerAdapter(val context: Context, fm: FragmentManager) : FragmentStatePagerAdapter(fm) {

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
    val registered: SparseArray<Fragment> = SparseArray()

    override fun getItem(position: Int): Fragment {
        if (position == 0) {
            val homeFragment = HomeFragment()
            return homeFragment
        } else {
            val quizzesFragment = QuizzesFragment().withArguments(Extras.EXTRA_CATEGORY to categories[position])
            return quizzesFragment
        }
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val fragment = super.instantiateItem(container, position) as Fragment
        registered.put(position, fragment)
        return fragment
    }

    override fun getCount(): Int {
        return categories.size
    }

    override fun getItemPosition(`object`: Any): Int {
        return PagerAdapter.POSITION_NONE
    }

    override fun destroyItem(container: ViewGroup, position: Int, view: Any) {
        registered.remove(position)
        super.destroyItem(container, position, view)
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

    fun categoryFromPosition(position: Int): Int {
        return when (position) {
            0 -> Categories.HOME
            1 -> Categories.CATEGORY_SELECTIONS
            2 -> Categories.CATEGORY_HIRAGANA
            3 -> Categories.CATEGORY_KATAKANA
            4 -> Categories.CATEGORY_KANJI
            5 -> Categories.CATEGORY_COUNTERS
            6 -> Categories.CATEGORY_JLPT_5
            7 -> Categories.CATEGORY_JLPT_4
            8 -> Categories.CATEGORY_JLPT_3
            9 -> Categories.CATEGORY_JLPT_2
            else -> Categories.CATEGORY_JLPT_1
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
