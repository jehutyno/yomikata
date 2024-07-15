package com.jehutyno.yomikata.screens.word

import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.jehutyno.yomikata.model.KanjiSoloRadical
import com.jehutyno.yomikata.model.Sentence
import com.jehutyno.yomikata.model.Word
import com.jehutyno.yomikata.util.QuizType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


/**
 * Created by jehutyno on 08/10/2016.
 */
class WordPagerAdapter(
    fragment: Fragment, private val coroutineScope: CoroutineScope,
    var quizType: QuizType?, private val callback: Callback, private val presenter: WordContract.Presenter
) : FragmentStateAdapter(fragment) {

    private val words: ArrayList<Triple<MutableLiveData<Word>,
            MutableLiveData<List<KanjiSoloRadical>?>, MutableLiveData<Sentence?>>> = arrayListOf()

    val count get() = words.count()

    /**
     * Replace data
     *
     * Replace the list of words used by the FragmentStateAdapter.
     *
     * KanjiSoloRadicals and Sentences will be loaded when needed.
     *
     * @param words List of words
     */
    fun replaceData(words: List<Word>) {
        this.words.clear()
        this.words.addAll(
            words.map {
                Triple(
                    MutableLiveData(it), MutableLiveData(null), MutableLiveData(null)
                )
            }
        )
        @Suppress("notifyDataSetChanged")
        notifyDataSetChanged()
    }


    interface Callback {
        fun onSelectionClick(view: View, word: Word)
        fun onReportClick(wordKanjiSentence: Triple<Word, List<KanjiSoloRadical?>, Sentence>)
        fun onWordTTSClick(word: Word)
        fun onSentenceTTSClick(sentence: Sentence)
        fun onLevelUp(word: MutableLiveData<Word>)
        fun onLevelDown(word: MutableLiveData<Word>)
        fun onCloseClick()
    }

    interface InternalCallback: Callback {
        fun onLevelUp()
        fun onLevelDown()
    }

    override fun getItemCount(): Int {
        return words.size
    }

    override fun createFragment(position: Int): Fragment {
        class ExtendedCallback: InternalCallback, Callback by callback {
            override fun onLevelUp() {
                callback.onLevelUp(words[position].first)
            }
            override fun onLevelDown() {
                callback.onLevelDown(words[position].first)
            }
        }

        // load kanjisolo and sentence if not loaded yet
        if (words[position].second.value == null) {
            coroutineScope.launch {
                words[position].second.value = presenter.getKanjiSoloList(words[position].first.value!!)
            }
        }
        if (words[position].third.value == null) {
            coroutineScope.launch {
                words[position].third.value = presenter.getSentence(words[position].first.value!!)
            }
        }

        return WordFragment(words[position], quizType, ExtendedCallback())
    }

}
