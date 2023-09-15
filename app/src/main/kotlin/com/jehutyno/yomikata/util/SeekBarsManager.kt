package com.jehutyno.yomikata.util

import android.animation.ObjectAnimator
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData


/**
 * Used to keep track of the seekBars settings / animations.
 */
class SeekBarsManager(private val seekLow: SeekBar, private val seekMedium: SeekBar,
                      private val seekHigh: SeekBar, private val seekMaster: SeekBar) {

    private var seekLowAnimation : ObjectAnimator? = null
    private var seekMediumAnimation : ObjectAnimator? = null
    private var seekHighAnimation : ObjectAnimator? = null
    private var seekMasterAnimation : ObjectAnimator? = null

    private var lowView: TextView? = null
    private var mediumView: TextView? = null
    private var highView: TextView? = null
    private var masterView: TextView? = null

    private var lowPlay: View? = null
    private var mediumPlay: View? = null
    private var highPlay: View? = null
    private var masterPlay: View? = null

    var count : Int = 0
    var low : Int = 0
    var medium : Int = 0
    var high : Int = 0
    var master : Int = 0

    fun setTextViews(low: TextView, medium: TextView, high: TextView, master: TextView) {
        lowView = low
        mediumView = medium
        highView = high
        masterView = master
    }

    fun setPlay(low: View, medium: View, high: View, master: View) {
        lowPlay = low
        mediumPlay = medium
        highPlay = high
        masterPlay = master
    }

    fun setObservers(liveCount: LiveData<Int>, liveLow: LiveData<Int>, liveMedium: LiveData<Int>,
                     liveHigh: LiveData<Int>, liveMaster: LiveData<Int>, viewLifecycleOwner: LifecycleOwner) {
        liveCount.observe(viewLifecycleOwner) {
            count = it
            animateAll()
        }
        liveLow.observe(viewLifecycleOwner) {
            low = it
            lowView?.text = low.toString()
            lowPlay?.visibility = if (low > 0) View.VISIBLE else View.INVISIBLE
            animateAll()
        }
        liveMedium.observe(viewLifecycleOwner) {
            medium = it
            mediumView?.text = medium.toString()
            mediumPlay?.visibility = if (medium > 0) View.VISIBLE else View.INVISIBLE
            animateAll()
        }
        liveHigh.observe(viewLifecycleOwner) {
            high = it
            highView?.text = high.toString()
            highPlay?.visibility = if (high > 0) View.VISIBLE else View.INVISIBLE
            animateAll()
        }
        liveMaster.observe(viewLifecycleOwner) {
            master = it
            masterView?.text = master.toString()
            masterPlay?.visibility = if (master > 0) View.VISIBLE else View.INVISIBLE
            animateAll()
        }
    }

    fun animateAll() {
        cancelAll() // cancel current animation
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

    fun instantUpdate() {
        cancelAll()
        seekLow.progress = low
        seekMedium.progress = medium
        seekHigh.progress = high
        seekMaster.progress = master
    }

    /**
     * Reset all
     *
     * Cancels animation and sets all values to zero.
     */
    fun resetAll() {
        cancelAll()
        seekLow.progress = 0
        seekMedium.progress = 0
        seekHigh.progress = 0
        seekMaster.progress = 0
    }
}
