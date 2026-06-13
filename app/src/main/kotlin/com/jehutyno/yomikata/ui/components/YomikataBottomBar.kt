package com.jehutyno.yomikata.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.ui.theme.AccentOrange
import com.jehutyno.yomikata.ui.theme.BackgroundNav
import com.jehutyno.yomikata.ui.theme.BorderDefault
import com.jehutyno.yomikata.ui.theme.TextDim
import com.jehutyno.yomikata.ui.theme.TextGhost
import com.jehutyno.yomikata.ui.theme.YomikataTheme

enum class BottomNavDestination(
    val labelRes: String,
    @param:DrawableRes val iconRes: Int
) {
    HOME("Home", R.drawable.ic_home),
    STUDY("Study", R.drawable.ic_list),
    SELECTIONS("Selections", R.drawable.ic_star_black_24dp),
    SETTINGS("Settings", R.drawable.ic_settings)
}

@Composable
fun YomikataBottomBar(
    selected: BottomNavDestination,
    onDestinationSelected: (BottomNavDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier
            .drawBehind {
                drawLine(
                    color = BorderDefault,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .windowInsetsPadding(WindowInsets.navigationBars),
        containerColor = BackgroundNav,
        tonalElevation = 0.dp
    ) {
        BottomNavDestination.entries.forEach { destination ->
            val isSelected = destination == selected
            NavigationBarItem(
                selected = isSelected,
                onClick = { onDestinationSelected(destination) },
                icon = {
                    Icon(
                        painter = painterResource(destination.iconRes),
                        contentDescription = destination.labelRes
                    )
                },
                label = {
                    Text(
                        text = destination.labelRes,
                        fontSize = 9.sp
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = AccentOrange,
                    selectedTextColor = AccentOrange,
                    unselectedIconColor = TextGhost,
                    unselectedTextColor = TextDim,
                    indicatorColor = BackgroundNav
                )
            )
        }
    }
}

@Preview(
    name = "BottomBar — Home active (dark)",
    showBackground = true,
    backgroundColor = 0xFF0A0E17
)
@Composable
private fun PreviewBottomBarHome() {
    YomikataTheme {
        YomikataBottomBar(
            selected = BottomNavDestination.HOME,
            onDestinationSelected = {}
        )
    }
}

@Preview(
    name = "BottomBar — Study active (dark)",
    showBackground = true,
    backgroundColor = 0xFF0A0E17
)
@Composable
private fun PreviewBottomBarStudy() {
    YomikataTheme {
        YomikataBottomBar(
            selected = BottomNavDestination.STUDY,
            onDestinationSelected = {}
        )
    }
}

@Preview(
    name = "BottomBar — Selections active (dark)",
    showBackground = true,
    backgroundColor = 0xFF0A0E17
)
@Composable
private fun PreviewBottomBarSelections() {
    YomikataTheme {
        YomikataBottomBar(
            selected = BottomNavDestination.SELECTIONS,
            onDestinationSelected = {}
        )
    }
}
