package com.jehutyno.yomikata.util

import android.app.Activity
import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.jehutyno.yomikata.R

enum class TutoId(val key: String) {
    WELCOME("tuto_welcome"),
    CATEGORIES("tuto_categories"),
    QUIZ_TYPE("tuto_quiz_type"),
    PROGRESS("tuto_progress"),
    PART_SELECTION("tuto_part_selection"),
    PRONUNCIATION_MCQ("tuto_pronunciation_mcq"),
    PRONUNCIATION("tuto_pronunciation"),
    AUDIO("tuto_audio"),
    EN_JP("tuto_en_jp"),
    JP_EN("tuto_jp_en"),
    AUTO("tuto_auto"),
}

fun resetAllTutos(prefs: SharedPreferences) {
    prefs.edit().apply {
        TutoId.values().forEach { remove(it.key) }
    }.apply()
}

/**
 * Always shows the overlay (used for the welcome step).
 * Does NOT track state — caller is responsible for sequencing.
 */
fun showTutoAlways(
    activity: Activity,
    target: View,
    title: String,
    message: String,
    onDismiss: () -> Unit
) {
    attachOverlay(activity, target, title, message, onDismiss)
}

/**
 * Shows the overlay only once per [id]. Subsequent calls are no-ops.
 * [onDismiss] is called when the user taps to dismiss (not if already shown).
 */
fun showTutoOnce(
    prefs: SharedPreferences,
    id: TutoId,
    activity: Activity,
    target: View?,
    title: String,
    message: String,
    onDismiss: () -> Unit
) {
    if (target == null || prefs.getBoolean(id.key, false)) return
    attachOverlay(activity, target, title, message) {
        prefs.edit().putBoolean(id.key, true).apply()
        onDismiss()
    }
}

private fun attachOverlay(
    activity: Activity,
    target: View,
    title: String,
    message: String,
    onDismiss: () -> Unit
) {
    val rect = Rect()
    target.getGlobalVisibleRect(rect)
    if (rect.isEmpty) return
    val overlay = TutoOverlayView(activity, rect, title, message, onDismiss)
    (activity.window.decorView as ViewGroup).addView(
        overlay, ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
    )
}

private class TutoOverlayView(
    activity: Activity,
    private val targetRect: Rect,
    title: String,
    message: String,
    private val onDismiss: () -> Unit
) : FrameLayout(activity) {

    private val dp = resources.displayMetrics.density

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xDD000000.toInt()
        style = Paint.Style.FILL
    }
    private val clearPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }
    private val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(activity, R.color.colorAccent)
        style = Paint.Style.STROKE
        strokeWidth = 3f * dp
    }

    init {
        setWillNotDraw(false)
        setLayerType(LAYER_TYPE_SOFTWARE, null)

        val tvTitle = TextView(activity).apply {
            text = title
            textSize = 21f
            setTextColor(ContextCompat.getColor(activity, R.color.colorAccent))
            setPadding((16 * dp).toInt(), 0, (16 * dp).toInt(), (8 * dp).toInt())
        }
        val tvMessage = TextView(activity).apply {
            text = message
            textSize = 16f
            setTextColor(ContextCompat.getColor(activity, R.color.spotlight_subhead))
            setPadding((16 * dp).toInt(), 0, (16 * dp).toInt(), 0)
        }
        val container = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            addView(tvTitle)
            addView(tvMessage)
        }

        val screenHeight = resources.displayMetrics.heightPixels
        val isUpperHalf = targetRect.centerY() < screenHeight / 2
        val margin = (80 * dp).toInt()
        val side = (16 * dp).toInt()

        val lp = LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
            leftMargin = side
            rightMargin = side
            if (isUpperHalf) {
                topMargin = targetRect.bottom + margin
                gravity = Gravity.TOP
            } else {
                bottomMargin = screenHeight - targetRect.top + margin
                gravity = Gravity.BOTTOM
            }
        }
        addView(container, lp)
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)
        val cx = targetRect.exactCenterX()
        val cy = targetRect.exactCenterY()
        val radius = maxOf(targetRect.width(), targetRect.height()) / 2f + 24f * dp
        canvas.drawCircle(cx, cy, radius, clearPaint)
        canvas.drawCircle(cx, cy, radius, accentPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            (parent as? ViewGroup)?.removeView(this)
            onDismiss()
        }
        return true
    }
}
