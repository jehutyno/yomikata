package com.jehutyno.yomikata.screens.quiz

import android.text.method.ScrollingMovementMethod
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.jehutyno.yomikata.databinding.FragmentQuizBinding

class QCMUIController(private val binding: FragmentQuizBinding) {

    private val qcmBinding get() = binding.quizAnswersMultipleChoice

    /** List of QCM tvs for convenience */
    val QCMtvs get() = qcmBinding.let {
        listOf(it.option1Tv, it.option2Tv, it.option3Tv, it.option4Tv)
    }

    /** List of QCM furigana for convenience */
    val QCMfuri get() = qcmBinding.let {
        listOf(it.option1Furi, it.option2Furi, it.option3Furi, it.option4Furi)
    }

    fun setUpOptionClickListeners(clickerFactory: (Int) -> (android.view.View) -> Unit) {
        qcmBinding.option1Container.setOnClickListener(clickerFactory(1))
        qcmBinding.option2Container.setOnClickListener(clickerFactory(2))
        qcmBinding.option3Container.setOnClickListener(clickerFactory(3))
        qcmBinding.option4Container.setOnClickListener(clickerFactory(4))

        QCMtvs.forEachIndexed { i, tv ->
            tv.setOnClickListener(clickerFactory(i + 1))
            tv.movementMethod = ScrollingMovementMethod()
        }
    }

    fun displayQCMTv(tvNum: Int, option: String, colorId: Int) {
        QCMtvs[tvNum - 1].also { tv ->
            tv.text = option
            tv.setTextColor(ContextCompat.getColor(binding.root.context, colorId))
            tv.scrollTo(0, 0)
        }
    }

    fun displayQCMTv(options: List<String>, colorIds: List<Int>) {
        QCMtvs.forEachIndexed { i, tv ->
            tv.text = options[i]
            tv.setTextColor(ContextCompat.getColor(binding.root.context, colorIds[i]))
            tv.scrollTo(0, 0)
        }
    }

    fun displayQCMFuri(furiNum: Int, optionFuri: String, start: Int, end: Int, colorId: Int) {
        val color = ContextCompat.getColor(binding.root.context, colorId)
        QCMfuri[furiNum - 1].text_set(optionFuri, start, end, color)
    }

    fun displayQCMFuri(options: List<String>, starts: List<Int>, ends: List<Int>, colorIds: List<Int>) {
        QCMfuri.forEachIndexed { i, furi ->
            furi.text_set(options[i], starts[i], ends[i], ContextCompat.getColor(binding.root.context, colorIds[i]))
        }
    }

    fun displayQCMNormalTextViews() {
        val pref = PreferenceManager.getDefaultSharedPreferences(binding.root.context)
        val fontSize = (pref.getString("font_size", "23") ?: "23").toFloat()
        QCMtvs.forEach { tv ->
            tv.textSize = fontSize
            tv.visibility = VISIBLE
        }
        QCMfuri.forEach { furi ->
            furi.visibility = GONE
        }
    }

    fun displayQCMFuriTextViews() {
        QCMtvs.forEach { tv ->
            tv.visibility = GONE
        }
        QCMfuri.forEach { furi ->
            furi.visibility = VISIBLE
        }
    }

    fun setOptionsFontSize(fontSize: Float) {
        QCMtvs.forEach { tv ->
            tv.textSize = fontSize
        }
    }

}
