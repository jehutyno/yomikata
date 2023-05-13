package com.jehutyno.yomikata.util

import android.text.Editable
import android.text.TextWatcher


/**
 * Created by valentin on 12/10/2016.
 */
abstract class TextValidator: TextWatcher {
    abstract fun validate(text: String)
    override fun afterTextChanged(s: Editable) {
        val text = s.toString()
        validate(text)
    }
    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) { /* Don't care */}
    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) { /* Don't care */}
}
