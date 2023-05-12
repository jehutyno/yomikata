package com.jehutyno.yomikata.screens.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.distinctUntilChanged
import com.jehutyno.yomikata.model.StatAction
import com.jehutyno.yomikata.model.StatEntry
import com.jehutyno.yomikata.model.StatResult
import com.jehutyno.yomikata.repository.StatsRepository
import mu.KLogging


/**
 * Created by valentin on 26/12/2016.
 */
class HomePresenter(
    statsRepository: StatsRepository) : HomeContract.Presenter {

    companion object : KLogging()

    // define livedata
    // from Room database & exposed to fragment:
    override val todayStatList: LiveData<List<StatEntry>> = statsRepository.getTodayStatEntries()
                                                                .asLiveData().distinctUntilChanged()
    override val thisWeekStatList: LiveData<List<StatEntry>> = statsRepository.getThisWeekStatEntries()
                                                                .asLiveData().distinctUntilChanged()
    override val thisMonthStatList: LiveData<List<StatEntry>> = statsRepository.getThisMonthStatEntries()
                                                                .asLiveData().distinctUntilChanged()
    override val totalStatList: LiveData<List<StatEntry>> = statsRepository.getAllStatEntries()
                                                                .asLiveData().distinctUntilChanged()

    override fun start() {

    }

    /**
     * Get number of launched quizzes
     *
     * @param stats List of StatEntries
     * @return Number of actions in the list of stat entries that correspond to a quiz launch
     */
    override fun getNumberOfLaunchedQuizzes(stats: List<StatEntry>): Int {
        return stats.count { it.action == StatAction.LAUNCH_QUIZ_FROM_CATEGORY.value }
    }

    /**
     * Get number of words seen
     *
     * @param stats List of StatEntries
     * @return Number of actions in the list of stat entries that correspond to a word seen
     */
    override fun getNumberOfWordsSeen(stats: List<StatEntry>): Int {
        return stats.count { it.action == StatAction.WORD_SEEN.value }
    }

    /**
     * Get number of correct answers
     *
     * @param stats List of StatEntries
     * @return Number of actions in the list of stat entries that correspond to an answer given
     * with a successful result
     */
    override fun getNumberOfCorrectAnswers(stats: List<StatEntry>): Int {
        return stats.count { it.action == StatAction.ANSWER_QUESTION.value && it.result == StatResult.SUCCESS.value }
    }

    /**
     * Get number of wrong answers
     *
     * @param stats List of StatEntries
     * @return Number of actions in the list of stat entries that correspond to an answer given
     * with a failure result
     */
    override fun getNumberOfWrongAnswers(stats: List<StatEntry>): Int {
        return stats.count { it.action == StatAction.ANSWER_QUESTION.value && it.result == StatResult.FAIL.value }
    }

}
