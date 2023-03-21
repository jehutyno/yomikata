package com.jehutyno.yomikata.util

import android.animation.ObjectAnimator
import android.widget.SeekBar

/**
 * Used to keep track of the seekBars settings / animations.
 */
class SeekBarsManager(private val seekLow: SeekBar, private val seekMedium: SeekBar,
                      private val seekHigh: SeekBar, private val seekMaster: SeekBar) {

    var seekLowAnimation : ObjectAnimator? = null
    var seekMediumAnimation : ObjectAnimator? = null
    var seekHighAnimation : ObjectAnimator? = null
    var seekMasterAnimation : ObjectAnimator? = null

    var count : Int = 0
    var low : Int = 0
    var medium : Int = 0
    var high : Int = 0
    var master : Int = 0

    fun animateAll() {
        seekLowAnimation = animateSeekBar(seekLow, 0, low, count)
        seekMediumAnimation = animateSeekBar(seekMedium, 0, medium, count)
        seekHighAnimation = animateSeekBar(seekHigh, 0, high, count)
        seekMasterAnimation = animateSeekBar(seekMaster, 0, master, count)
    }

    /**
     * Cancel all animations of the seekBars.
     * Use before manually setting seekBar.progress = value (since the animation may override your value)
     */
    fun cancelAll() {
        seekLowAnimation?.cancel()
        seekMediumAnimation?.cancel()
        seekHighAnimation?.cancel()
        seekMasterAnimation?.cancel()
    }
}