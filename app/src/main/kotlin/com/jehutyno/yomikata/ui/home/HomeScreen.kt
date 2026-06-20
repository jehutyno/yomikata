package com.jehutyno.yomikata.ui.home

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.ui.components.SectionHeader
import com.jehutyno.yomikata.ui.theme.AccentOrange
import com.jehutyno.yomikata.ui.theme.AccentWarm
import com.jehutyno.yomikata.ui.theme.BackgroundHeroWarm
import com.jehutyno.yomikata.ui.theme.BackgroundPrimary
import com.jehutyno.yomikata.ui.theme.BorderAccent
import com.jehutyno.yomikata.ui.theme.BorderDefault
import com.jehutyno.yomikata.ui.theme.Correct
import com.jehutyno.yomikata.ui.theme.RadiusMd
import com.jehutyno.yomikata.ui.theme.RadiusXl
import com.jehutyno.yomikata.ui.theme.SurfacePrimary
import com.jehutyno.yomikata.ui.theme.TextMuted
import com.jehutyno.yomikata.ui.theme.TextPrimary
import com.jehutyno.yomikata.ui.theme.TypeHeroTitle
import com.jehutyno.yomikata.ui.theme.TypeListTitle
import com.jehutyno.yomikata.ui.theme.Wrong
import com.jehutyno.yomikata.ui.theme.YomikataTheme
import kotlinx.coroutines.delay

data class HomeUiState(
    val quizLaunched: Int = 0,
    val wordsSeen: Int = 0,
    val correctAnswers: Int = 0,
    val wrongAnswers: Int = 0,
    val lastSessionLevel: String? = null,
    val newsText: String = "",
    val newsLoading: Boolean = true,
)

@Composable
fun HomeScreen(
    state: HomeUiState,
    onContinueClick: () -> Unit,
    onSupportClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundPrimary)
            .verticalScroll(rememberScrollState()),
    ) {
        HomeHero()
        Spacer(modifier = Modifier.height(18.dp))
        SectionHeader(
            text = "今日 · Today",
            modifier = Modifier.padding(horizontal = 14.dp),
        )
        Spacer(modifier = Modifier.height(8.dp))
        StatsGrid(state = state, modifier = Modifier.padding(horizontal = 14.dp))

        if (state.lastSessionLevel != null) {
            Spacer(modifier = Modifier.height(18.dp))
            SectionHeader(
                text = "続ける · Continue",
                modifier = Modifier.padding(horizontal = 14.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            ContinueCard(
                levelName = state.lastSessionLevel,
                onContinueClick = onContinueClick,
                modifier = Modifier.padding(horizontal = 14.dp),
            )
        }

        Spacer(modifier = Modifier.height(18.dp))
        SectionHeader(
            text = "ニュース · News",
            modifier = Modifier.padding(horizontal = 14.dp),
        )
        Spacer(modifier = Modifier.height(8.dp))
        NewsCard(
            text = if (state.newsLoading) "Chargement…" else state.newsText,
            modifier = Modifier.padding(horizontal = 14.dp),
        )

        Spacer(modifier = Modifier.height(18.dp))
        SupportCard(
            onSupportClick = onSupportClick,
            modifier = Modifier.padding(horizontal = 14.dp),
        )
        Spacer(modifier = Modifier.height(18.dp))
    }
}

/** Photos cyclées en fond du hero (effet Ken Burns : zoom lent + crossfade). */
private val HERO_IMAGES = listOf(
    R.drawable.pic_fujisan,
    R.drawable.pic_hanami,
    R.drawable.pic_miyajima,
    R.drawable.pic_geisha,
    R.drawable.pic_hokusai,
)

@Composable
private fun HomeHero(modifier: Modifier = Modifier) {
    var index by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(7000)
            index = (index + 1) % HERO_IMAGES.size
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp)
            .background(BackgroundHeroWarm)
            .clipToBounds(),
    ) {
        Crossfade(
            targetState = index,
            animationSpec = tween(durationMillis = 1500),
            label = "hero-crossfade",
        ) { i ->
            KenBurnsImage(resId = HERO_IMAGES[i])
        }

        // Scrim chaud pour lisibilité du logo/titre
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x99130A02),
                            Color(0xCC0A0500),
                        ),
                    )
                ),
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize(),
        ) {
            Image(
                painter = painterResource(R.drawable.yomi_logo_home),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(96.dp),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Yomikata Z",
                style = TypeHeroTitle,
                color = AccentOrange,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "学ぶ · 読む · 理解する",
                fontSize = 12.sp,
                fontWeight = FontWeight.W400,
                color = AccentWarm,
                letterSpacing = 0.05.sp,
            )
        }
    }
}

@Composable
private fun KenBurnsImage(resId: Int) {
    val transition = rememberInfiniteTransition(label = "ken-burns")
    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "ken-burns-scale",
    )
    Image(
        painter = painterResource(resId),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
    )
}

@Composable
private fun StatsGrid(state: HomeUiState, modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        StatCard(
            value = state.quizLaunched.toString(),
            label = "Quiz lancés",
            modifier = Modifier.weight(1f),
        )
        StatCard(
            value = state.wordsSeen.toString(),
            label = "Mots vus",
            modifier = Modifier.weight(1f),
        )
    }
    Spacer(modifier = Modifier.height(8.dp))
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        StatCard(
            value = state.correctAnswers.toString(),
            label = "Bonnes réponses",
            valueColor = Correct,
            modifier = Modifier.weight(1f),
        )
        StatCard(
            value = state.wrongAnswers.toString(),
            label = "Mauvaises rép.",
            valueColor = Wrong,
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * Bouton d'action discret (pill outline orange) — variante sobre du FABBar,
 * utilisé à l'intérieur des cards de la home.
 */
@Composable
private fun DiscreetActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(RadiusXl),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderAccent),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentOrange),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = 16.dp,
            vertical = 8.dp,
        ),
        modifier = modifier,
    ) {
        Text(text = text, style = TypeListTitle, color = AccentOrange)
    }
}

@Composable
private fun ContinueCard(
    levelName: String,
    onContinueClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = SurfacePrimary,
        shape = RoundedCornerShape(RadiusMd),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, BorderAccent, RoundedCornerShape(RadiusMd)),
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(
                text = levelName,
                fontSize = 14.sp,
                fontWeight = FontWeight.W500,
                color = AccentOrange,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Reprendre là où vous vous êtes arrêté",
                fontSize = 12.sp,
                color = TextMuted,
            )
            Spacer(modifier = Modifier.height(12.dp))
            DiscreetActionButton(
                text = "Continuer — $levelName",
                onClick = onContinueClick,
            )
        }
    }
}

@Composable
private fun NewsCard(text: String, modifier: Modifier = Modifier) {
    Surface(
        color = SurfacePrimary,
        shape = RoundedCornerShape(RadiusMd),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, BorderDefault, RoundedCornerShape(RadiusMd)),
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            color = TextPrimary,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun SupportCard(
    onSupportClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = SurfacePrimary,
        shape = RoundedCornerShape(RadiusMd),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, BorderDefault, RoundedCornerShape(RadiusMd)),
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(
                text = "Soutenir le projet",
                fontSize = 13.sp,
                fontWeight = FontWeight.W500,
                color = AccentOrange,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Yomikata Z est gratuit et sans publicité. Si vous l'appréciez, vous pouvez soutenir son développement.",
                fontSize = 12.sp,
                color = TextMuted,
            )
            Spacer(modifier = Modifier.height(12.dp))
            DiscreetActionButton(
                text = "♥ Soutenir sur GitHub",
                onClick = onSupportClick,
            )
        }
    }
}

@Preview(name = "HomeScreen — dark", showBackground = true, backgroundColor = 0xFF0A0E17)
@Composable
private fun HomeScreenPreview() {
    YomikataTheme {
        HomeScreen(
            state = HomeUiState(
                quizLaunched = 5,
                wordsSeen = 42,
                correctAnswers = 38,
                wrongAnswers = 4,
                lastSessionLevel = "JLPT N4",
                newsText = "Nouvelle mise à jour disponible avec des traductions en mandarin complètes.",
                newsLoading = false,
            ),
            onContinueClick = {},
            onSupportClick = {},
        )
    }
}

@Preview(name = "HomeScreen no session — dark", showBackground = true, backgroundColor = 0xFF0A0E17)
@Composable
private fun HomeScreenNoSessionPreview() {
    YomikataTheme {
        HomeScreen(
            state = HomeUiState(newsLoading = false, newsText = "Bienvenue !"),
            onContinueClick = {},
            onSupportClick = {},
        )
    }
}
