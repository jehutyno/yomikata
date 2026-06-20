package com.jehutyno.yomikata.screens.quiz

import android.content.SharedPreferences
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.ui.components.DialogButton
import com.jehutyno.yomikata.ui.components.DialogButtonStyle
import com.jehutyno.yomikata.ui.components.DialogIcon
import com.jehutyno.yomikata.ui.components.yomikataAlert
import com.jehutyno.yomikata.util.Prefs
import kotlinx.coroutines.launch

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
        val continueAction: () -> Unit = {
            fragment.lifecycleScope.launch {
                if (isProgressive) presenter.onLaunchNextProgressiveSession()
                else presenter.onContinueAfterNonProgressiveSessionEnd()
            }
        }
        val reviewAction: () -> Unit = {
            fragment.lifecycleScope.launch { presenter.onLaunchErrorSession() }
        }
        val quitAction: () -> Unit = { fragment.requireActivity().finish() }

        if (proposeErrors) {
            when (errorReviewOption()) {
                ErrorReviewOption.Show -> fragment.requireContext().yomikataAlert(
                    message = fragment.getString(R.string.alert_session_finished, wordCount),
                    icon = DialogIcon.Success,
                    cancelable = false,
                    buttons = listOf(
                        DialogButton(fragment.getString(R.string.alert_continue), DialogButtonStyle.Primary, continueAction),
                        DialogButton(fragment.getString(R.string.alert_review_session_errors), DialogButtonStyle.Muted, reviewAction),
                        DialogButton(fragment.getString(R.string.alert_quit), DialogButtonStyle.Muted, quitAction),
                    ),
                ).show()
                ErrorReviewOption.AutoReview -> reviewAction()
                ErrorReviewOption.Skip -> continueAction()
            }
        } else {
            when (flawlessOption()) {
                FlawlessOption.Show -> fragment.requireContext().yomikataAlert(
                    message = fragment.getString(R.string.alert_session_finished, wordCount),
                    icon = DialogIcon.Success,
                    cancelable = false,
                    buttons = listOf(
                        DialogButton(fragment.getString(R.string.alert_continue), DialogButtonStyle.Primary, continueAction),
                        DialogButton(fragment.getString(R.string.alert_quit), DialogButtonStyle.Muted, quitAction),
                    ),
                ).show()
                FlawlessOption.Skip -> continueAction()
            }
        }
    }

    fun showAlertErrorSessionEnd(quizEnded: Boolean, isProgressive: Boolean) {
        val quitAction: () -> Unit = { fragment.requireActivity().finish() }

        val (messageRes, positiveText, positiveAction) = if (!quizEnded) {
            Triple(
                R.string.alert_error_review_session_message,
                R.string.alert_continue_quiz,
                { fragment.lifecycleScope.launch { presenter.onContinueQuizAfterErrorSession() }; Unit },
            )
        } else {
            val positiveText = if (isProgressive) R.string.alert_continue_quiz else R.string.alert_restart
            Triple(
                R.string.alert_error_review_quiz_message,
                positiveText,
                { fragment.lifecycleScope.launch { presenter.onRestartQuiz(!isProgressive) }; Unit },
            )
        }

        val buttons = listOf(
            DialogButton(fragment.getString(positiveText), DialogButtonStyle.Primary, positiveAction),
            DialogButton(fragment.getString(R.string.alert_quit), DialogButtonStyle.Muted, quitAction),
        )

        // Skip the dialog when the user opted out of "flawless" prompts, except on a fully
        // finished non-progressive quiz where the prompt is always shown.
        if (!quizEnded || isProgressive) {
            when (flawlessOption()) {
                FlawlessOption.Show -> showErrorSessionDialog(messageRes, buttons)
                FlawlessOption.Skip -> positiveAction()
            }
        } else {
            showErrorSessionDialog(messageRes, buttons)
        }
    }

    private fun showErrorSessionDialog(messageRes: Int, buttons: List<DialogButton>) {
        fragment.requireContext().yomikataAlert(
            message = fragment.getString(messageRes),
            cancelable = false,
            buttons = buttons,
        ).show()
    }

    fun showAlertQuizEnd(proposeErrors: Boolean) {
        val buttons = buildList {
            add(DialogButton(fragment.getString(R.string.alert_restart), DialogButtonStyle.Primary) {
                fragment.lifecycleScope.launch { presenter.onRestartQuiz(true) }
            })
            if (proposeErrors) {
                add(DialogButton(fragment.getString(R.string.alert_review_quiz_errors), DialogButtonStyle.Muted) {
                    fragment.lifecycleScope.launch { presenter.onLaunchErrorSession() }
                })
            }
            add(DialogButton(fragment.getString(R.string.alert_quit), DialogButtonStyle.Muted) {
                fragment.requireActivity().finish()
            })
        }
        fragment.requireContext().yomikataAlert(
            message = fragment.getString(R.string.alert_quiz_finished),
            icon = DialogIcon.Success,
            cancelable = false,
            buttons = buttons,
        ).show()
    }
}
