package com.jehutyno.yomikata.presenters

import androidx.lifecycle.LiveData


interface WordCountInterface {
    val quizCount: LiveData<Int>
    val lowCount: LiveData<Int>
    val mediumCount: LiveData<Int>
    val highCount: LiveData<Int>
    val masterCount: LiveData<Int>
}
