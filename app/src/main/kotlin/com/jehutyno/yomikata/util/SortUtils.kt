package com.jehutyno.yomikata.util

import java.util.*

/**
 * Created by valentin on 21/10/2016.
 */
fun shuffleArray(arr: Array<Any>) : Array<Any> {
    val rg: Random = Random()
    for (i in 0..arr.size - 1) {
        val randomPosition = rg.nextInt(arr.size)
        swap(arr, i, randomPosition)
    }
    return arr
}

fun swap(arr: Array<Any>, i: Int, j: Int) : Array<Any> {
    val tmp = arr[i]
    arr[i] = arr[j]
    arr[j] = tmp
    return arr
}

fun <T>shuffle(items:MutableList<T>):List<T>{
    val rg : Random = Random()
    for (i in 0..items.size - 1) {
        val randomPosition = rg.nextInt(items.size)
        val tmp : T = items[i]
        items[i] = items[randomPosition]
        items[randomPosition] = tmp
    }
    return items
}

/**
 * Creates random array or Int of given length and max value
 * @param length length of an array to be generated
 * @param maxValue
 */
fun randomNumericArray(length: Int, maxValue : Int = 10) : Array<Int>{
    return Array<Int>(length, {i -> Math.round(maxValue * Math.random()).toInt() })
}

