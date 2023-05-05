package com.jehutyno.yomikata.util

import kotlin.math.roundToInt

/**
 * Created by valentin on 21/10/2016.
 */

fun swap(arr: Array<Any>, i: Int, j: Int) : Array<Any> {
    val tmp = arr[i]
    arr[i] = arr[j]
    arr[j] = tmp
    return arr
}


/**
 * Creates random array or Int of given length and max value
 * @param length length of an array to be generated
 * @param maxValue
 */
fun randomNumericArray(length: Int, maxValue : Int = 10) : Array<Int>{
    return Array(length) { (maxValue * Math.random()).roundToInt() }
}

