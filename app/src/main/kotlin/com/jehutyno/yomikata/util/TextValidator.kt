package com.jehutyno.yomikata.util

import android.text.Editable
import android.text.TextWatcher
import android.widget.TextView

/**
 * Created by valentin on 12/10/2016.
 */
abstract class TextValidator(textView: TextView): TextWatcher {
    private val textView:TextView
    init{
        this.textView = textView
    }
    abstract fun validate(textView:TextView, text:String)
    override fun afterTextChanged(s: Editable) {
        val text = textView.getText().toString()
        validate(textView, text)
    }
    override fun beforeTextChanged(s:CharSequence, start:Int, count:Int, after:Int) { /* Don't care */}
    override fun onTextChanged(s:CharSequence, start:Int, before:Int, count:Int) { /* Don't care */}
}