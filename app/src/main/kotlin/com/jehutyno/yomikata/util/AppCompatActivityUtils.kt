package com.jehutyno.yomikata.util

import android.app.Activity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.appcompat.app.AppCompatActivity
import android.view.inputmethod.InputMethodManager


/**
 * Created by valentin on 03/10/2016.
 */

inline fun AppCompatActivity.fragmentTransaction(autocommit: Boolean = true, func: FragmentTransaction.() -> Unit): FragmentTransaction {
    val transaction = supportFragmentManager.beginTransaction()
    transaction.func()
    if (autocommit && !transaction.isEmpty) {
        transaction.commit()
    }
    return transaction
}

fun AppCompatActivity.addOrReplaceFragment(layoutId: Int, fragment: Fragment) {
    fragmentTransaction {
        if (supportFragmentManager.findFragmentById(layoutId) == null) {
            add(layoutId, fragment)
        } else {
            replace(layoutId, fragment)
        }
    }
}

fun Activity.hideSoftKeyboard() {
    val inputMethodManager = this.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    if (this.currentFocus != null)
        inputMethodManager.hideSoftInputFromWindow(this.currentFocus!!.windowToken, 0)
}
