package com.jehutyno.yomikata.screens.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import com.jehutyno.yomikata.databinding.ActivitySplashBinding
import com.jehutyno.yomikata.repository.local.YomikataDataBase
import com.jehutyno.yomikata.screens.quizzes.QuizzesActivity
import com.jehutyno.yomikata.util.UpdateProgressDialog
import kotlinx.coroutines.*


// TODO: migrate to splashScreen https://developer.android.com/develop/ui/views/launch/splash-screen/migrate
class SplashActivity : AppCompatActivity() {

    // View Binding
    private lateinit var binding: ActivitySplashBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySplashBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        val updateProgressDialogMigrate = UpdateProgressDialog(this)
        updateProgressDialogMigrate.prepare("Migrating your database", "This may take a while...")

        val job = CoroutineScope(Dispatchers.Main).launch {
            // do migration
            YomikataDataBase.updateProgressDialogGetter = {
                updateProgressDialogMigrate
            }
            withContext(Dispatchers.IO) {
                YomikataDataBase.forceLoadDatabase(this@SplashActivity)
            }
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
        // delay showing the progress dialog by a short time to prevent showing it when no migration is needed
        handler.postDelayed({
            if (job.isActive) {
                updateProgressDialogMigrate.show()
            }
        }, 250)
        handler.postDelayed(
            {
                // TODO: handle old yomikata database type
//                if (!pref.getBoolean("migrationYomiDone", false)) {
//                    importYomikata("????")
//                    pref.edit().putBoolean("migrationYomiDone", true).apply()
//                }
                CoroutineScope(Dispatchers.Main).launch {
                    // finish the migration & destroy progress dialog (if it was shown at all)
                    job.join()
                    updateProgressDialogMigrate.destroy()
                    YomikataDataBase.updateProgressDialogGetter = null

                    val intent = Intent(this@SplashActivity, QuizzesActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    this@SplashActivity.finish()
                }
            }, 900)

    }

}
