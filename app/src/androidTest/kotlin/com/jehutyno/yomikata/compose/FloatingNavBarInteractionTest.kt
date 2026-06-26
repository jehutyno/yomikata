package com.jehutyno.yomikata.compose

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jehutyno.yomikata.ui.components.BottomNavDestination
import com.jehutyno.yomikata.ui.components.YomikataFloatingNavBar
import com.jehutyno.yomikata.ui.theme.YomikataTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose interaction test for [YomikataFloatingNavBar] (couche 4).
 *
 * Verifies the wiring screenshots can't: tapping a destination invokes the callback with the right
 * enum value, and all four destinations are rendered. Labels are non-localized literals, so the
 * selectors are locale-independent.
 */
@RunWith(AndroidJUnit4::class)
class FloatingNavBarInteractionTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun tappingADestinationInvokesCallbackWithThatDestination() {
        var clicked: BottomNavDestination? = null
        rule.setContent {
            YomikataTheme {
                YomikataFloatingNavBar(
                    selected = BottomNavDestination.HOME,
                    onDestinationSelected = { clicked = it },
                )
            }
        }

        rule.onNodeWithText("Study").performClick()
        assertEquals(BottomNavDestination.STUDY, clicked)

        rule.onNodeWithText("Settings").performClick()
        assertEquals(BottomNavDestination.SETTINGS, clicked)

        rule.onNodeWithText("Selections").performClick()
        assertEquals(BottomNavDestination.SELECTIONS, clicked)
    }

    @Test
    fun allFourDestinationsAreRendered() {
        rule.setContent {
            YomikataTheme {
                YomikataFloatingNavBar(selected = BottomNavDestination.HOME, onDestinationSelected = {})
            }
        }
        listOf("Home", "Study", "Selections", "Settings").forEach { label ->
            // Each label appears once (merged node carries both the text and the icon description).
            rule.onAllNodesWithText(label).assertCountEquals(1)
        }
    }
}
