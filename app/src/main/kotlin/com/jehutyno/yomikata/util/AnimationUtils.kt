package com.jehutyno.yomikata.util

import android.animation.ObjectAnimator
import android.view.animation.DecelerateInterpolator
import android.widget.SeekBar

/**
 * Created by valentin on 27/10/2016.
 */
fun animateSeekBar(seekBar: SeekBar, from: Int, to: Int, max: Int) : ObjectAnimator {
    seekBar.max = if (max < 10) max * 10 else max
    seekBar.progress = from
    val animation = ObjectAnimator.ofInt(seekBar, "progress", if (max < 10) to * 10 else to)
    animation.startDelay = 0
    animation.duration = 700
    animation.interpolator = DecelerateInterpolator()
    animation.start()
    seekBar.isEnabled = false
    return animation
}
