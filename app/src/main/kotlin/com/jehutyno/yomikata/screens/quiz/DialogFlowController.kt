package com.jehutyno.yomikata.screens.quiz

import android.content.SharedPreferences
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.util.Prefs
import kotlinx.coroutines.launch
import splitties.alertdialog.appcompat.*

class DialogFlowController(
    private val fragment: Fragment,
    private val prefs: SharedPreferences,
    private val presenter: QuizContract.Presenter,
) {

    private enum class ErrorReviewOption(val preferenceId: Int) {
        Show(0), AutoReview(1), Skip(2)
    }

    private enum class FlawlessOption(val preferenceId: Int) {
        Show(0), Skip(1)
    }

    private fun errorReviewOption(): ErrorReviewOption {
        val id = prefs.getInt(Prefs.QUIZ_ERROR_SELECTED_RADIO_BUTTON_ID.pref, ErrorReviewOption.Show.preferenceId)
        return ErrorReviewOption.entries.firstOrNull { it.preferenceId == id } ?: ErrorReviewOption.Show
    }

    private fun flawlessOption(): FlawlessOption {
        val id = prefs.getInt(Prefs.QUIZ_FLAWLESS_SELECTED_RADIO_BUTTON_ID.pref, FlawlessOption.Show.preferenceId)
        return FlawlessOption.entries.firstOrNull { it.preferenceId == id } ?: FlawlessOption.Show
    }

    fun showAlertSessionEnd(wordCount: Int, isProgressive: Boolean, proposeErrors: Boolean) {
        val dialog = fragment.requireContext().alertDialog {
            message = fragment.getString(R.string.alert_session_finished, wordCount)
            positiveButton(R.string.alert_continue) {
                fragment.lifecycleScope.launch {
                    if (isProgressive) presenter.onLaunchNextProgressiveSession()
                    else presenter.onContinueAfterNonProgressiveSessionEnd()
                }
            }
            if (proposeErrors) {
                negativeButton(R.string.alert_review_session_errors) {
                    fragment.lifecycleScope.launch { presenter.onLaunchErrorSession() }
                }
            }
            neutralButton(R.string.alert_quit) { fragment.requireActivity().finish() }
            setCancelable(false)
        }
        dialog.create()

        if (proposeErrors) {
            when (errorReviewOption()) {
                ErrorReviewOption.Show -> dialog.show()
                ErrorReviewOption.AutoReview -> dialog.negativeButton.callOnClick()
                ErrorReviewOption.Skip -> dialog.positiveButton.callOnClick()
            }
        } else {
            when (flawlessOption()) {
                FlawlessOption.Show -> dialog.show()
                FlawlessOption.Skip -> dialog.positiveButton.callOnClick()
            }
        }
    }

    fun showAlertErrorSessionEnd(quizEnded: Boolean, isProgressive: Boolean) {
        val dialog = fragment.requireContext().alertDialog {
            if (!quizEnded) {
                messageResource = R.string.alert_error_review_session_message
                positiveButton(R.string.alert_continue_quiz) {
                    fragment.lifecycleScope.launch { presenter.onContinueQuizAfterErrorSession() }
                }
            } else {
                messageResource = R.string.alert_error_review_quiz_message
                val positiveText = if (isProgressive) R.string.alert_continue_quiz else R.string.alert_restart
                positiveButton(positiveText) {
                    fragment.lifecycleScope.launch { presenter.onRestartQuiz(!isProgressive) }
                }
            }
            neutralButton(R.string.alert_quit) { fragment.requireActivity().finish() }
            setCancelable(false)
        }
        dialog.create()

        if (!quizEnded || isProgressive) {
            when (flawlessOption()) {
                FlawlessOption.Show -> dialog.show()
                FlawlessOption.Skip -> dialog.positiveButton.callOnClick()
            }
        } else {
            dialog.show()
        }
    }

    fun showAlertQuizEnd(proposeErrors: Boolean) {
        fragment.requireContext().alertDialog {
            messageResource = R.string.alert_quiz_finished
            positiveButton(R.string.alert_restart) {
                fragment.lifecycleScope.launch { presenter.onRestartQuiz(true) }
            }
            if (proposeErrors) {
                negativeButton(R.string.alert_review_quiz_errors) {
                    fragment.lifecycleScope.launch { presenter.onLaunchErrorSession() }
                }
            }
            neutralButton(R.string.alert_quit) { fragment.requireActivity().finish() }
            setCancelable(false)
        }.show()
    }
}
