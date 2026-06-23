package com.jehutyno.yomikata.screenshot

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.jehutyno.yomikata.ui.components.MasteryBar
import com.jehutyno.yomikata.ui.theme.BackgroundPrimary
import com.jehutyno.yomikata.ui.theme.YomikataTheme
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Smoke + reference test for the Roborazzi + Robolectric screenshot toolchain.
 *
 * Renders the shared [MasteryBar] component on the JVM and captures PNGs — no emulator required.
 * Demonstrates the two axes of the real screenshot matrix:
 *  - component state (in-progress vs fully mastered),
 *  - UI locale (English vs German), via RuntimeEnvironment.setQualifiers — this is the headline
 *    guard for the multi-language release (catches truncated/blank translated strings).
 *
 * Notes for this bleeding-edge stack:
 *  - Robolectric runtime SDK is pinned to 36 (its current max) while the app compiles against 37.
 *  - The stock [Application] is used so YomikataZKApplication.onCreate (Firebase/DI) does not run.
 *  - captureRoboImage's content-lambda renders without an Activity (createComposeRule needs a
 *    launcher Activity Robolectric cannot resolve for the .debug applicationId).
 *  - Baselines live under src/test/screenshots/ and are committed; verifyRoborazziDebug compares.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-night-xxhdpi", application = Application::class)
class MasteryBarScreenshotTest {

    private fun capture(name: String, content: @Composable () -> Unit) {
        captureRoboImage("src/test/screenshots/$name.png") {
            YomikataTheme {
                Box(
                    Modifier
                        .background(BackgroundPrimary)
                        .width(360.dp)
                        .padding(16.dp)
                ) {
                    content()
                }
            }
        }
    }

    @Test
    fun masteryBar_inProgress() {
        capture("mastery_bar_in_progress") { MasteryBar(total = 100, mastered = 61) }
    }

    @Test
    fun masteryBar_allMastered() {
        capture("mastery_bar_all_mastered") { MasteryBar(total = 40, mastered = 40) }
    }

    @Test
    fun masteryBar_german() {
        // Merge the German locale into the current qualifiers so string resources resolve to values-de
        RuntimeEnvironment.setQualifiers("+de")
        capture("mastery_bar_de") { MasteryBar(total = 100, mastered = 61) }
    }
}
