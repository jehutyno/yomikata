package com.jehutyno.yomikata.screens.prefs

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.jehutyno.yomikata.util.quiz.Categories
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.android.closestDI

/**
 * Created by valentin on 30/11/2016.
 */
class PrefsActivity : AppCompatActivity(), DIAware {

    override val di: DI by closestDI()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val fragment = PrefsFragment().also { f ->
            f.onResetTuto = {
                val intent = android.content.Intent()
                intent.putExtra("gotoCategory", Categories.HOME)
                setResult(RESULT_OK, intent)
                finish()
            }
        }
        supportFragmentManager.beginTransaction()
            .replace(android.R.id.content, fragment)
            .commit()
    }
}
