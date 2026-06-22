package com.jehutyno.yomikata.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.ui.theme.AccentOrange
import com.jehutyno.yomikata.ui.theme.BackgroundPrimary
import com.jehutyno.yomikata.ui.theme.BorderAccent
import com.jehutyno.yomikata.ui.theme.BorderDefault
import com.jehutyno.yomikata.ui.theme.SurfaceAccent
import com.jehutyno.yomikata.ui.theme.SurfacePrimary
import com.jehutyno.yomikata.ui.theme.TextDim
import com.jehutyno.yomikata.ui.theme.TextMuted
import com.jehutyno.yomikata.ui.theme.YomikataTheme

/** Destinations de la barre de navigation principale (Home / Study / Sélections / Réglages). */
enum class BottomNavDestination(
    val labelRes: String,
    @param:DrawableRes val iconRes: Int
) {
    HOME("Home", R.drawable.ic_home),
    STUDY("Study", R.drawable.ic_list),
    SELECTIONS("Selections", R.drawable.ic_star_black_24dp),
    SETTINGS("Settings", R.drawable.ic_settings)
}

/**
 * Hauteur du contenu de la barre flottante (pill + zone de fondu), hors inset système.
 * Les écrans qui passent SOUS la barre ajoutent [floatingNavBarBottomPadding] en bas pour
 * que leur contenu / FABBar ne soit pas masqué par la pill.
 */
val FloatingNavBarHeight: Dp = 82.dp

/** Padding bas à appliquer au contenu d'un écran hébergé derrière la barre flottante. */
@Composable
fun floatingNavBarBottomPadding(): Dp =
    FloatingNavBarHeight + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

/**
 * Barre de navigation principale, style « floating toolbar » Material 3 :
 * une pill large détachée des bords qui flotte au-dessus du contenu, lequel s'estompe
 * derrière elle (dégradé transparent → fond) façon Telegram.
 *
 * L'item actif porte un highlight pill orange translucide (SurfaceAccent + BorderAccent),
 * icône + label en AccentOrange. La transition entre onglets est animée (fondu des
 * couleurs de fond/contenu + expansion du padding de l'item sélectionné).
 *
 * Réutilise l'enum [BottomNavDestination] : aucun impact sur la navigation existante.
 */
@Composable
fun YomikataFloatingNavBar(
    selected: BottomNavDestination,
    onDestinationSelected: (BottomNavDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.Transparent, BackgroundPrimary),
                )
            )
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(top = 20.dp, start = 12.dp, end = 12.dp, bottom = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(PILL),
            color = SurfacePrimary,
            border = BorderStroke(1.dp, BorderDefault),
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 10.dp,
                    shape = RoundedCornerShape(PILL),
                    clip = false
                )
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp)
            ) {
                BottomNavDestination.entries.forEach { destination ->
                    NavItem(
                        destination = destination,
                        isSelected = destination == selected,
                        onClick = { onDestinationSelected(destination) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.NavItem(
    destination: BottomNavDestination,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) AccentOrange else TextMuted,
        label = "navItemContent"
    )
    val labelColor by animateColorAsState(
        targetValue = if (isSelected) AccentOrange else TextDim,
        label = "navItemLabel"
    )
    val background by animateColorAsState(
        targetValue = if (isSelected) SurfaceAccent else Color.Transparent,
        label = "navItemBg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) BorderAccent else Color.Transparent,
        label = "navItemBorder"
    )
    val horizontalPadding by animateDpAsState(
        targetValue = if (isSelected) 10.dp else 4.dp,
        label = "navItemPadding"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier
            .clip(RoundedCornerShape(PILL))
            .background(background)
            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(PILL))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = horizontalPadding, vertical = 5.dp)
    ) {
        Icon(
            painter = painterResource(destination.iconRes),
            contentDescription = destination.labelRes,
            tint = contentColor,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = destination.labelRes,
            fontSize = 10.sp,
            color = labelColor
        )
    }
}

private val PILL = 24.dp

@Preview(
    name = "FloatingNavBar — Study active (dark)",
    showBackground = true,
    backgroundColor = 0xFF0A0E17
)
@Composable
private fun PreviewFloatingNavBarStudy() {
    YomikataTheme {
        YomikataFloatingNavBar(
            selected = BottomNavDestination.STUDY,
            onDestinationSelected = {}
        )
    }
}

@Preview(
    name = "FloatingNavBar — Home active (dark)",
    showBackground = true,
    backgroundColor = 0xFF0A0E17
)
@Composable
private fun PreviewFloatingNavBarHome() {
    YomikataTheme {
        YomikataFloatingNavBar(
            selected = BottomNavDestination.HOME,
            onDestinationSelected = {}
        )
    }
}
