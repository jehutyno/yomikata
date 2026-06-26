package com.jehutyno.yomikata.compose

import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.jehutyno.yomikata.screens.quizzes.QuizzesActivity
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Smoke E2E (couche 4): launches the real launcher activity end-to-end and asserts it reaches
 * RESUMED without crashing. Unlike the isolated Compose tests, this exercises the full integration
 * — Kodein DI, asset DB load (forceLoadDatabase), Firebase init, splash, and the Compose host —
 * so it catches wiring/startup regressions that unit and screenshot tests can't.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class AppLaunchSmokeTest {

    @Test
    fun mainActivityLaunchesAndReachesResumed() {
        ActivityScenario.launch(QuizzesActivity::class.java).use { scenario ->
            assertEquals(Lifecycle.State.RESUMED, scenario.state)
        }
    }

    @Test
    fun mainActivitySurvivesRecreation() {
        // Recreation reproduces the config/locale-change path (AppCompatDelegate.setApplicationLocales)
        // that once left Home blank / crashed. The activity must come back up cleanly.
        ActivityScenario.launch(QuizzesActivity::class.java).use { scenario ->
            scenario.recreate()
            assertEquals(Lifecycle.State.RESUMED, scenario.state)
        }
    }
}
