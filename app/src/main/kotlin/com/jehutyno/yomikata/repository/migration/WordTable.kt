package com.jehutyno.yomikata.repository.migration

/**
 * Created by valentin on 01/12/2016.
 */
class WordTable(var id: Int, var word: String, var pronunciation: String,
                var counterTry: Int, var counterSuccess: Int, var counterFail: Int, var priority: Int)
