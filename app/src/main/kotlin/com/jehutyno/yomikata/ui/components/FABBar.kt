package com.jehutyno.yomikata.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jehutyno.yomikata.ui.theme.AccentOnOrange
import com.jehutyno.yomikata.ui.theme.AccentOrange
import com.jehutyno.yomikata.ui.theme.Correct
import com.jehutyno.yomikata.ui.theme.CorrectOn
import com.jehutyno.yomikata.ui.theme.RadiusXl
import com.jehutyno.yomikata.ui.theme.TypeListTitle
import com.jehutyno.yomikata.ui.theme.YomikataTheme

/**
 * Primary action button shown above the bottom navigation bar.
 *
 * - [FABBarState.Start] / [FABBarState.Continue] / [FABBarState.LaunchSelection]:
 *   orange background (AccentOrange), dark text (AccentOnOrange)
 * - [FABBarState.Next]: green background (Correct), dark text (CorrectOn)
 */
sealed class FABBarState {
    object Start : FABBarState()
    data class Continue(val levelName: String) : FABBarState()
    object LaunchAll : FABBarState()
    data class LaunchSelection(val count: Int) : FABBarState()
    object Next : FABBarState()
}

@Composable
fun FABBar(
    state: FABBarState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (label, containerColor, contentColor) = when (state) {
        is FABBarState.Start -> Triple("Commencer", AccentOrange, AccentOnOrange)
        is FABBarState.Continue -> Triple("Continuer — ${state.levelName}", AccentOrange, AccentOnOrange)
        is FABBarState.LaunchAll -> Triple("Lancer tous les quiz", AccentOrange, AccentOnOrange)
        is FABBarState.LaunchSelection -> Triple("Lancer la sélection (${state.count})", AccentOrange, AccentOnOrange)
        is FABBarState.Next -> Triple("Suivant →", Correct, CorrectOn)
    }

    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
        shape = RoundedCornerShape(RadiusXl),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
    ) {
        Text(
            text = label,
            style = TypeListTitle,
            color = contentColor,
        )
    }
}

@Preview(name = "FABBar Start — dark", showBackground = true, backgroundColor = 0xFF0A0E17)
@Composable
private fun FABBarStartPreview() {
    YomikataTheme {
        FABBar(
            state = FABBarState.Start,
            onClick = {},
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}

@Preview(name = "FABBar Continue — dark", showBackground = true, backgroundColor = 0xFF0A0E17)
@Composable
private fun FABBarContinuePreview() {
    YomikataTheme {
        FABBar(
            state = FABBarState.Continue("N4"),
            onClick = {},
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}

@Preview(name = "FABBar Selection — dark", showBackground = true, backgroundColor = 0xFF0A0E17)
@Composable
private fun FABBarSelectionPreview() {
    YomikataTheme {
        FABBar(
            state = FABBarState.LaunchSelection(3),
            onClick = {},
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}

@Preview(name = "FABBar Next — dark", showBackground = true, backgroundColor = 0xFF0A0E17)
@Composable
private fun FABBarNextPreview() {
    YomikataTheme {
        FABBar(
            state = FABBarState.Next,
            onClick = {},
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}
