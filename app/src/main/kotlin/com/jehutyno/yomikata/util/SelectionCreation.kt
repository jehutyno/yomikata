package com.jehutyno.yomikata.util

import android.content.Context
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import com.jehutyno.yomikata.R
import splitties.alertdialog.appcompat.alertDialog
import splitties.alertdialog.appcompat.cancelButton
import splitties.alertdialog.appcompat.neutralButton
import splitties.alertdialog.appcompat.okButton
import splitties.alertdialog.appcompat.positiveButton
import splitties.alertdialog.appcompat.titleResource


/**
 * Create new selection dialog
 *
 * Creates and shows a dialog to create a new selection, requiring the user to give a name for
 * the new selection. The OK button will only be enabled if the text validation passes: which
 * requires the given name to not be empty.
 * You can use the okCallback to immediately add some words for example.
 *
 * @param defaultText Text that will be displayed (for the selection name) when dialog is created.
 * @param okCallback Called if the ok button is clicked -> means the user wants to create the selection.
 * Takes the name of the selection as input.
 * @param deleteCallback If null -> no delete button.
 *                       if not null -> called if delete button is clicked.
 */
fun Context.createNewSelectionDialog(
    defaultText: String,
    okCallback: (selectionName: String) -> Unit,
    deleteCallback: (() -> Unit)?
) {
    val input = EditText(this)
    input.setSingleLine()
    input.setText(defaultText)
    input.hint = getString(R.string.selection_name)

    val container = FrameLayout(this)
    val params = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    params.leftMargin = DimensionHelper.getPixelFromDip(this, 20)
    params.rightMargin = DimensionHelper.getPixelFromDip(this, 20)
    params.bottomMargin = DimensionHelper.getPixelFromDip(this, 20)
    input.layoutParams = params
    container.addView(input)

    val dialog = this.alertDialog {
        titleResource =
            if (deleteCallback == null)
                R.string.new_selection
            else
                R.string.selection_edit

        setView(container)

        okButton {
            okCallback(input.text.toString())
        }

        if (deleteCallback != null) {
            neutralButton(R.string.action_delete) {
                this@createNewSelectionDialog.alertDialog {
                    titleResource = R.string.selection_delete_sure
                    okButton {
                        deleteCallback()
                    }
                    cancelButton { }
                }.show()
            }
        }

        cancelButton()
    }
    dialog.show()

    val validator = object : TextValidator() {
        // TODO: also require name to be unique (not equal to other selections)?
        override fun validate(text: String) {
            if (text.isEmpty()) {
                dialog.positiveButton.isEnabled = false
                input.error = getString(R.string.selection_not_empty_name)
            } else {
                dialog.positiveButton.isEnabled = true
                input.error = null
            }
        }
    }
    input.addTextChangedListener(validator)

    // make sure to validate initial text, this is not done by the validator which only checks changes
    validator.validate(defaultText)
}
