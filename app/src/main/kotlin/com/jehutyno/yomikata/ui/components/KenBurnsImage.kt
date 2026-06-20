package com.jehutyno.yomikata.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource

/**
 * Image plein cadre avec effet Ken Burns : zoom lent en boucle (aller-retour).
 *
 * Utilisée en arrière-plan des hero (Home, Study). À combiner avec un scrim
 * par-dessus pour la lisibilité du contenu de premier plan.
 */
@Composable
fun KenBurnsImage(
    @DrawableRes resId: Int,
    modifier: Modifier = Modifier,
    durationMillis: Int = 14000,
    maxScale: Float = 1.18f,
) {
    val transition = rememberInfiniteTransition(label = "ken-burns")
    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = maxScale,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "ken-burns-scale",
    )
    Image(
        painter = painterResource(resId),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
    )
}
