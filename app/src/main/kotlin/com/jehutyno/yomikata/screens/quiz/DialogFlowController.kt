package com.jehutyno.yomikata.screens.quiz

import android.content.SharedPreferences
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.databinding.FragmentQuizBinding
import com.jehutyno.yomikata.util.Prefs
import kotlinx.coroutines.launch
import splitties.alertdialog.appcompat.*

class DialogFlowController(
    private val fragment: Fragment,
    private val prefs: SharedPreferences,
    private val presenter: QuizContract.Presenter,
    private val binding: FragmentQuizBinding
) {

    private enum class ErrorReviewOption(val preferenceId: Int) {
        Show(0),
        AutoReview(1),
        Skip(2)
    }

    private enum class FlawlessOption(val preferenceId: Int) {
        Show(0),
        Skip(1)
    }

    private val errorReviewIdMap = mapOf(
        Pair(R.id.radio_button_show, ErrorReviewOption.Show),
        Pair(R.id.radio_button_auto_error, ErrorReviewOption.AutoReview),
        Pair(R.id.radio_button_error_no_show, ErrorReviewOption.Skip)
    )

    private val flawlessIdMap = mapOf(
        Pair(R.id.flawless_radio_button_show, FlawlessOption.Show),
        Pair(R.id.flawless_radio_button_no_show, FlawlessOption.Skip)
    )

    fun setUpRadioButtons() {
        val defaultErrorReview = ErrorReviewOption.Show.preferenceId
        val defaultFlawless = FlawlessOption.Show.preferenceId

        val errorReviewSelected = prefs.getInt(
            Prefs.QUIZ_ERROR_SELECTED_RADIO_BUTTON_ID.pref,
            defaultErrorReview
        )
        val flawlessSelected = prefs.getInt(
            Prefs.QUIZ_FLAWLESS_SELECTED_RADIO_BUTTON_ID.pref,
            defaultFlawless
        )

        val errorSelectedId = errorReviewIdMap.filterValues {
            it.preferenceId == errorReviewSelected
        }.keys.toList().getOrElse(0) { defaultErrorReview }
        val flawlessSelectedId = flawlessIdMap.filterValues {
            it.preferenceId == flawlessSelected
        }.keys.toList().getOrElse(0) { defaultFlawless }

        binding.errorReviewRadioGroup.check(errorSelectedId)
        binding.flawlessRadioGroup.check(flawlessSelectedId)

        // set shared preferences
        binding.errorReviewRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            prefs.edit().putInt(
                Prefs.QUIZ_ERROR_SELECTED_RADIO_BUTTON_ID.pref,
                requireNotNull(errorReviewIdMap[checkedId]) { "Unknown radio button: $checkedId" }.preferenceId
            ).apply()
        }
        binding.flawlessRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            prefs.edit().putInt(
                Prefs.QUIZ_FLAWLESS_SELECTED_RADIO_BUTTON_ID.pref,
                requireNotNull(flawlessIdMap[checkedId]) { "Unknown radio button: $checkedId" }.preferenceId
            ).apply()
        }
    }

    fun showAlertSessionEnd(wordCount: Int, isProgressive: Boolean, proposeErrors: Boolean) {
        val dialog = fragment.requireContext().alertDialog {
            message = fragment.getString(R.string.alert_session_finished, wordCount)
            positiveButton(R.string.alert_continue) {
                fragment.lifecycleScope.launch {
                    if (isProgressive)
                        presenter.onLaunchNextProgressiveSession()
                    else
                        presenter.onContinueAfterNonProgressiveSessionEnd()
                }
            }
            if (proposeErrors) {
                negativeButton(R.string.alert_review_session_errors) {
                    fragment.lifecycleScope.launch {
                        presenter.onLaunchErrorSession()
                    }
                }
            }
            neutralButton(R.string.alert_quit) {
                fragment.requireActivity().finish()
            }
            setCancelable(false)    // avoid accidental click out of session
        }
        dialog.create()

        if (proposeErrors) {
            when (requireNotNull(errorReviewIdMap[binding.errorReviewRadioGroup.checkedRadioButtonId]) { "Unknown radio button: ${binding.errorReviewRadioGroup.checkedRadioButtonId}" }) {
                ErrorReviewOption.Show -> { dialog.show() }
                ErrorReviewOption.AutoReview -> { dialog.negativeButton.callOnClick() }
                ErrorReviewOption.Skip -> { dialog.positiveButton.callOnClick() }
            }
        } else {
            when (requireNotNull(flawlessIdMap[binding.flawlessRadioGroup.checkedRadioButtonId]) { "Unknown radio button: ${binding.flawlessRadioGroup.checkedRadioButtonId}" }) {
                FlawlessOption.Show -> { dialog.show() }
                FlawlessOption.Skip -> { dialog.positiveButton.callOnClick() }
            }
        }
    }

    fun showAlertErrorSessionEnd(quizEnded: Boolean, isProgressive: Boolean) {
        val dialog = fragment.requireContext().alertDialog {
            messageResource = R.string.alert_error_review_finished

            if (!quizEnded) {
                messageResource = R.string.alert_error_review_session_message
                positiveButton(R.string.alert_continue_quiz) {
                    fragment.lifecycleScope.launch {
                        presenter.onContinueQuizAfterErrorSession()
                    }
                }
            } else {
                messageResource = R.string.alert_error_review_quiz_message
                val positiveButtonText =
                    if (isProgressive)
                        R.string.alert_continue_quiz    // progressive doesn't really end
                    else
                        R.string.alert_restart
                positiveButton(positiveButtonText) {
                    fragment.lifecycleScope.launch {
                        presenter.onRestartQuiz(!isProgressive)
                    }
                }
            }
            neutralButton(R.string.alert_quit) {
                fragment.requireActivity().finish()
            }
            setCancelable(false)    // avoid accidental click out of session
        }
        dialog.create()

        if (!quizEnded || isProgressive) {
            when (requireNotNull(flawlessIdMap[binding.flawlessRadioGroup.checkedRadioButtonId]) { "Unknown radio button: ${binding.flawlessRadioGroup.checkedRadioButtonId}" }) {
                FlawlessOption.Show -> { dialog.show() }
                FlawlessOption.Skip -> { dialog.positiveButton.callOnClick() }
            }
        } else {
            dialog.show()
        }
    }

    fun showAlertQuizEnd(proposeErrors: Boolean) {
        fragment.requireContext().alertDialog {
            messageResource = R.string.alert_quiz_finished
            positiveButton(R.string.alert_restart) {
                fragment.lifecycleScope.launch {
                    presenter.onRestartQuiz(true)
                }
            }
            if (proposeErrors) {
                negativeButton(R.string.alert_review_quiz_errors) {
                    fragment.lifecycleScope.launch {
                        presenter.onLaunchErrorSession()
                    }
                }
            }
            neutralButton(R.string.alert_quit) {
                fragment.requireActivity().finish()
            }
            setCancelable(false)    // avoid accidental click out of session
        }.show()
    }

}
