package com.jehutyno.yomikata.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jehutyno.yomikata.ui.components.FABBar
import com.jehutyno.yomikata.ui.components.FABBarState
import com.jehutyno.yomikata.ui.components.SectionHeader
import com.jehutyno.yomikata.ui.theme.AccentOrange
import com.jehutyno.yomikata.ui.theme.AccentWarm
import com.jehutyno.yomikata.ui.theme.BackgroundHeroWarm
import com.jehutyno.yomikata.ui.theme.BackgroundPrimary
import com.jehutyno.yomikata.ui.theme.BorderAccent
import com.jehutyno.yomikata.ui.theme.BorderDefault
import com.jehutyno.yomikata.ui.theme.Correct
import com.jehutyno.yomikata.ui.theme.RadiusMd
import com.jehutyno.yomikata.ui.theme.SurfacePrimary
import com.jehutyno.yomikata.ui.theme.TextDim
import com.jehutyno.yomikata.ui.theme.TextMuted
import com.jehutyno.yomikata.ui.theme.TextPrimary
import com.jehutyno.yomikata.ui.theme.TextWarm
import com.jehutyno.yomikata.ui.theme.TypeHeroTitle
import com.jehutyno.yomikata.ui.theme.Wrong
import com.jehutyno.yomikata.ui.theme.YomikataTheme

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
    onFabClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fabState = if (state.lastSessionLevel != null)
        FABBarState.Continue(state.lastSessionLevel)
    else
        FABBarState.Start

    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
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
            SupportCard(modifier = Modifier.padding(horizontal = 14.dp))
            Spacer(modifier = Modifier.height(8.dp))
        }

        FABBar(
            state = fabState,
            onClick = onFabClick,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun HomeHero(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp)
            .background(BackgroundHeroWarm),
    ) {
        // Scrim gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xA60A0500),
                            Color(0xB30A0500),
                        ),
                    )
                ),
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize(),
        ) {
            // Logo circle
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(64.dp)
                    .background(AccentOrange.copy(alpha = 0.15f), CircleShape)
                    .border(1.5.dp, AccentOrange.copy(alpha = 0.5f), CircleShape),
            ) {
                Text(
                    text = "読",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.W300,
                    color = AccentOrange,
                )
            }
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

@Composable
private fun ContinueCard(levelName: String, modifier: Modifier = Modifier) {
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
private fun SupportCard(modifier: Modifier = Modifier) {
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
            onFabClick = {},
        )
    }
}

@Preview(name = "HomeScreen no session — dark", showBackground = true, backgroundColor = 0xFF0A0E17)
@Composable
private fun HomeScreenNoSessionPreview() {
    YomikataTheme {
        HomeScreen(
            state = HomeUiState(newsLoading = false, newsText = "Bienvenue !"),
            onFabClick = {},
        )
    }
}
