package com.jehutyno.yomikata.screens.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import androidx.preference.PreferenceManager
import com.jehutyno.yomikata.databinding.ActivitySplashBinding
import com.jehutyno.yomikata.screens.quizzes.QuizzesActivity
import com.jehutyno.yomikata.util.Prefs
import com.jehutyno.yomikata.util.migrateFromYomikata
import com.jehutyno.yomikata.util.updateBDD


// TODO: migrate to splashScreen https://developer.android.com/develop/ui/views/launch/splash-screen/migrate
class SplashActivity : AppCompatActivity() {

    // View Binding
    private lateinit var binding: ActivitySplashBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySplashBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        val pref = PreferenceManager.getDefaultSharedPreferences(this)
        if (pref.getBoolean(Prefs.DB_UPDATE_ONGOING.pref, false))
            pref.getString(Prefs.DB_UPDATE_FILE.pref, "")?.let {
                updateBDD(null, it,
                    pref.getInt(Prefs.DB_UPDATE_OLD_VERSION.pref, -1))
            }

        binding.pathView.useNaturalColors()
        binding.pathView.pathAnimator
            .delay(100)
            .duration(500)
            .listenerEnd {
                binding.logo.visibility = View.VISIBLE
            }
            .start()

        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed(
            {
                if (!pref.getBoolean("migrationYomiDone", false)) {
                    migrateFromYomikata()
                    pref.edit().putBoolean("migrationYomiDone", true).apply()
                }
                val intent = Intent(this@SplashActivity, QuizzesActivity::class.java)
                startActivity(intent)
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                this@SplashActivity.finish()
            }, 900)

    }

}
