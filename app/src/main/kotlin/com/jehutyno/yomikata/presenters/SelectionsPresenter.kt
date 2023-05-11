package com.jehutyno.yomikata.presenters

import com.jehutyno.yomikata.model.Quiz
import com.jehutyno.yomikata.repository.QuizRepository
import com.jehutyno.yomikata.util.Categories
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch


/**
 * Selections presenter
 *
 * Used for basic selections operations: create, add word, delete word, getSelections.
 * Will start a thread upon creation to get selections, and allow retrieval at later times.
 *
 * @param quizRepository QuizRepository instance
 * @param coroutineScope CoroutineScope of the activity/fragment, typically LifecycleScope
 */
class SelectionsPresenter(private val quizRepository: QuizRepository,
                               coroutineScope: CoroutineScope): SelectionsInterface {

    // use a StateFlow, since LiveData only collects once it is observed
    private val job: Job
    private lateinit var selectionsStateFlow: StateFlow<List<Quiz>>

    init {
        job = coroutineScope.launch {
            selectionsStateFlow = quizRepository.getQuiz(Categories.CATEGORY_SELECTIONS).stateIn(coroutineScope)
        }
    }

    /**
     * Get selections
     *
     * Suspends until the first value is emitted. Should no longer suspend once value(s) are emitted.
     *
     * @return The latest value of a StateFlow emitting the selection quizzes from the database.
     * This is not necessarily the latest value from the database, but should be in most cases.
     */
    override suspend fun getSelections(): List<Quiz> {
        job.join()
        return selectionsStateFlow.value
    }

    /**
     * Create selection
     *
     * Used for new user created selections.
     *
     * @param quizName Name of the selection
     * @return Id of the new Quiz in the database
     */
    override suspend fun createSelection(quizName: String): Long {
        return quizRepository.saveQuiz(quizName, Categories.CATEGORY_SELECTIONS)
    }

    // TODO: add check to see if quizId corresponds to a selection?
    override suspend fun addWordToSelection(wordId: Long, quizId: Long) {
        quizRepository.addWordToQuiz(wordId, quizId)
    }

    override suspend fun deleteWordFromSelection(wordId: Long, selectionId: Long) {
        quizRepository.deleteWordFromQuiz(wordId, selectionId)
    }

}
