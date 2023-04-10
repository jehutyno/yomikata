package com.jehutyno.yomikata.util

import android.app.Activity
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.appcompat.app.AlertDialog
import com.jehutyno.yomikata.R
import splitties.alertdialog.appcompat.*


class UpdateProgressDialog(private val activity: Activity) {

    private val progressBar: ProgressBar = ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal)
    private var progressAlertDialog: AlertDialog? = null

    var finishDialog: AlertDialog? = null
    var finishCallback: (() -> Unit)? = null    // called when the progress finished successfully
    var destroyOnFinish: Boolean = false        // when finished, simply destroy the dialog

    init {
        progressBar.setPadding(40, progressBar.paddingTop, 40, progressBar.paddingBottom)
        progressBar.max = 100   // default max
    }

    fun setMax(max: Int) {
        progressBar.max = max
    }

    fun prepare(title: String?, message: String? = null) {
        progressAlertDialog = activity.alertDialog {
            this.title = title
            this.message = message
            setCancelable(false)
            setView(progressBar)
        }
    }

    /**
     * Show the progress dialog
     */
    fun show() {
        if (progressAlertDialog == null) {
            progressAlertDialog = activity.alertDialog {
                titleResource = R.string.progress_bdd_update_title
                messageResource = R.string.progress_bdd_update_message
                setCancelable(false)
                setView(progressBar)
            }
        }
        progressBar.progress = 0

        progressAlertDialog!!.show()
    }

    /**
     * Update progress
     *
     * If the progress reaches the maximum, finishProgress is automatically called
     *
     * @param newProgress set progressBar.progress to this value
     */
    fun updateProgress(newProgress: Int) {
        progressBar.progress = newProgress
        if (newProgress >= progressBar.max) {
            if (destroyOnFinish)
                destroy()
            else
                finish()
        }
    }

    /**
     * Finish progress
     *
     * Dismiss the dialog
     */
    fun finish() {
        destroy()
        if (finishDialog == null) {
            activity.alertDialog {
                titleResource = R.string.update_success_title
                messageResource = R.string.update_success_message
                okButton()
            }.show()
        } else {
            finishDialog!!.show()
        }
        finishCallback?.invoke()
    }

    /**
     * Destroy
     *
     * Dismiss the dialog without calling any callbacks or showing any confirmations.
     */
    fun destroy() {
        (progressBar.parent as ViewGroup).removeView(progressBar)
        progressAlertDialog!!.dismiss()
        progressAlertDialog = null
    }

    /**
     * Error progress
     *
     * Call this when an error happens. The dialog is dismissed, and a new dialog
     * appears with an error message.
     *
     * @param errorTitle title of the error dialog
     * @param errorMessage message of the error dialog
     */
    fun error(errorTitle: String?, errorMessage: String?) {
        (progressBar.parent as ViewGroup).removeView(progressBar)
        progressAlertDialog!!.dismiss()
        progressAlertDialog = null
        activity.alertDialog {
            title = errorTitle
            message= "The following error occurred:\n$errorMessage"
            setCancelable(false)
            okButton()
        }.show()
    }

}
