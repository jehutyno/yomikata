package com.jehutyno.yomikata.screens.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.databinding.ActivitySplashBinding
import com.jehutyno.yomikata.screens.quizzes.QuizzesActivity
import com.jehutyno.yomikata.util.Prefs
import com.jehutyno.yomikata.util.migrateFromYomikata
import com.jehutyno.yomikata.util.updateBDD
import org.jetbrains.anko.defaultSharedPreferences

class SplashActivity : AppCompatActivity() {

    // View Binding
    private lateinit var binding: ActivitySplashBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySplashBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        if (defaultSharedPreferences.getBoolean(Prefs.DB_UPDATE_ONGOING.pref, false))
            defaultSharedPreferences.getString(Prefs.DB_UPDATE_FILE.pref, "")?.let {
                updateBDD(null, it,
                    defaultSharedPreferences.getInt(Prefs.DB_UPDATE_OLD_VERSION.pref, -1))
            }

        binding.pathView.useNaturalColors()
        binding.pathView.pathAnimator
            .delay(100)
            .duration(500)
            .listenerEnd {
                binding.logo.visibility = View.VISIBLE
            }
            .start()

        val handler = Handler()
        handler.postDelayed(
            {
                if (!defaultSharedPreferences.getBoolean("migrationYomiDone", false)) {
                    migrateFromYomikata()
                    defaultSharedPreferences.edit().putBoolean("migrationYomiDone", true).apply()
                }
                val intent = Intent(this@SplashActivity, QuizzesActivity::class.java)
                startActivity(intent)
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                this@SplashActivity.finish()
            }, 900)

    }

}
