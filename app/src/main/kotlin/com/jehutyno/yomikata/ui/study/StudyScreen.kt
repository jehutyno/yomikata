package com.jehutyno.yomikata.ui.study

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.model.Quiz
import com.jehutyno.yomikata.ui.components.FABBar
import com.jehutyno.yomikata.ui.components.FABBarState
import com.jehutyno.yomikata.ui.components.floatingNavBarBottomPadding
import com.jehutyno.yomikata.ui.components.KenBurnsImage
import com.jehutyno.yomikata.ui.components.MasteryBar
import com.jehutyno.yomikata.ui.components.SectionHeader
import com.jehutyno.yomikata.ui.theme.AccentOrange
import com.jehutyno.yomikata.ui.theme.BackgroundHero
import com.jehutyno.yomikata.ui.theme.BackgroundPrimary
import com.jehutyno.yomikata.ui.theme.BorderDefault
import com.jehutyno.yomikata.ui.theme.RadiusMd
import com.jehutyno.yomikata.ui.theme.RadiusPill
import com.jehutyno.yomikata.ui.theme.SurfaceAccent
import com.jehutyno.yomikata.ui.theme.SurfacePrimary
import com.jehutyno.yomikata.ui.theme.TextDim
import com.jehutyno.yomikata.ui.theme.TextGhost
import com.jehutyno.yomikata.ui.theme.TextMuted
import com.jehutyno.yomikata.ui.theme.TypeHeroTitle
import com.jehutyno.yomikata.ui.theme.YomikataTheme
import com.jehutyno.yomikata.util.quiz.Categories
import com.jehutyno.yomikata.util.quiz.QuizStrategy
import com.jehutyno.yomikata.util.quiz.QuizType

data class LevelDefinition(
    val category: Int,
    val label: String,
    val name: String,
)

val StudyLevels = listOf(
    LevelDefinition(Categories.CATEGORY_HIRAGANA, "あ", "Hiragana"),
    LevelDefinition(Categories.CATEGORY_KATAKANA, "ア", "Katakana"),
    LevelDefinition(Categories.CATEGORY_KANJI, "漢", "Kanji"),
    LevelDefinition(Categories.CATEGORY_COUNTERS, "数", "Compteurs"),
    LevelDefinition(Categories.CATEGORY_JLPT_5, "N5", "JLPT N5"),
    LevelDefinition(Categories.CATEGORY_JLPT_4, "N4", "JLPT N4"),
    LevelDefinition(Categories.CATEGORY_JLPT_3, "N3", "JLPT N3"),
    LevelDefinition(Categories.CATEGORY_JLPT_2, "N2", "JLPT N2"),
    LevelDefinition(Categories.CATEGORY_JLPT_1, "N1", "JLPT N1"),
)

fun categoryName(category: Int): String =
    StudyLevels.firstOrNull { it.category == category }?.name ?: "Study"

data class StudyUiState(
    val selectedCategory: Int = Categories.CATEGORY_HIRAGANA,
    val quizzes: List<Quiz> = emptyList(),
    val quizCount: Int = 0,
    val goodCount: Int = 0,
    val wrongCount: Int = 0,
    val selectedTypes: List<QuizType> = listOf(QuizType.TYPE_AUTO),
    val lastMode: QuizStrategy? = null,
    val voicesDownloaded: Boolean = false,
    val voiceSizeMb: Int = 0,
    val voiceDownloadProgress: Float? = null,   // null = pas de téléchargement en cours
)

@DrawableRes
private fun categoryHeroDrawable(category: Int): Int = when (category) {
    Categories.CATEGORY_HIRAGANA -> R.drawable.ic_hiragana_big
    Categories.CATEGORY_KATAKANA -> R.drawable.ic_katakana_big
    Categories.CATEGORY_KANJI    -> R.drawable.ic_kanji_big
    Categories.CATEGORY_COUNTERS -> R.drawable.ic_counters_big
    Categories.CATEGORY_JLPT_5   -> R.drawable.ic_jlpt5_big
    Categories.CATEGORY_JLPT_4   -> R.drawable.ic_jlpt4_big
    Categories.CATEGORY_JLPT_3   -> R.drawable.ic_jlpt3_big
    Categories.CATEGORY_JLPT_2   -> R.drawable.ic_jlpt2_big
    else                         -> R.drawable.ic_jlpt1_big
}

/** Photo de fond du hero, distincte par niveau (effet Ken Burns). */
@DrawableRes
private fun categoryHeroPhoto(category: Int): Int = when (category) {
    Categories.CATEGORY_HIRAGANA -> R.drawable.pic_hanami
    Categories.CATEGORY_KATAKANA -> R.drawable.pic_miyajima
    Categories.CATEGORY_KANJI    -> R.drawable.pic_hokusai
    Categories.CATEGORY_COUNTERS -> R.drawable.pic_toit
    Categories.CATEGORY_JLPT_5   -> R.drawable.pic_fujisan
    Categories.CATEGORY_JLPT_4   -> R.drawable.pic_geisha
    Categories.CATEGORY_JLPT_3   -> R.drawable.pic_dragon
    Categories.CATEGORY_JLPT_2   -> R.drawable.pic_monk
    else                         -> R.drawable.pic_fujiyoshida
}

// MARK: — Composables

@Composable
private fun StudyHero(selectedCategory: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp)
            .background(BackgroundHero)
            .clipToBounds(),
    ) {
        Crossfade(
            targetState = selectedCategory,
            animationSpec = tween(durationMillis = 1000),
            label = "study-hero-crossfade",
        ) { category ->
            KenBurnsImage(resId = categoryHeroPhoto(category))
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xA6000A14), Color(0xCC000A14)),
                    )
                ),
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize(),
        ) {
            Image(
                painter = painterResource(categoryHeroDrawable(selectedCategory)),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(80.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = categoryName(selectedCategory),
                style = TypeHeroTitle,
                color = AccentOrange,
            )
        }
    }
}

@Composable
private fun LevelChip(
    level: LevelDefinition,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = if (isSelected) SurfaceAccent else SurfacePrimary
    val border = if (isSelected) AccentOrange else BorderDefault
    val textColor = if (isSelected) AccentOrange else TextMuted

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(RadiusPill))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(RadiusPill))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        Text(
            text = level.label,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = textColor,
        )
    }
}

@Composable
private fun StudyProgress(
    quizCount: Int,
    goodCount: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .background(SurfacePrimary, RoundedCornerShape(RadiusMd))
            .border(1.dp, BorderDefault, RoundedCornerShape(RadiusMd))
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        SectionHeader(text = stringResource(R.string.progress_header))
        MasteryBar(total = quizCount, mastered = goodCount, showLegend = true)
    }
}

@Composable
private fun StudyQuizItem(
    quiz: Quiz,
    onChecked: (Boolean) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val localizedName = quiz.getName().split("%")[0]
    val japanesePart = quiz.nameEn.split("%").getOrElse(1) { "" }

    val cardBg = if (quiz.isSelected) SurfaceAccent else SurfacePrimary
    val cardBorder = if (quiz.isSelected) AccentOrange else BorderDefault

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RadiusMd))
            .background(cardBg)
            .border(1.dp, cardBorder, RoundedCornerShape(RadiusMd))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        Checkbox(
            checked = quiz.isSelected,
            onCheckedChange = onChecked,
            colors = CheckboxDefaults.colors(
                checkedColor = AccentOrange,
                uncheckedColor = TextGhost,
                checkmarkColor = BackgroundPrimary,
            ),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = localizedName,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = AccentOrange,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (japanesePart.isNotEmpty()) {
                Text(
                    text = japanesePart,
                    fontSize = 12.sp,
                    color = TextDim,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = TextGhost,
            modifier = Modifier.size(16.dp),
        )
    }
}

// MARK: — StudyScreen

@Composable
fun StudyScreen(
    state: StudyUiState,
    onCategorySelected: (Int) -> Unit,
    onQuizChecked: (quizId: Long, checked: Boolean) -> Unit,
    onQuizClick: (Quiz) -> Unit,
    onLaunchQuiz: (QuizStrategy) -> Unit,
    onQuizTypeToggle: (QuizType) -> Unit,
    onDownloadVoices: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedCount = state.quizzes.count { it.isSelected }
    val fabState = if (selectedCount > 0) FABBarState.LaunchSelection(selectedCount)
                   else FABBarState.LaunchAll
    var showLaunchSheet by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundPrimary),
    ) {
        // Hero
        StudyHero(selectedCategory = state.selectedCategory)

        // Level chips
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 10.dp, vertical = 9.dp),
        ) {
            StudyLevels.forEach { level ->
                LevelChip(
                    level = level,
                    isSelected = level.category == state.selectedCategory,
                    onClick = { onCategorySelected(level.category) },
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
        }

        // Progress — single mastery bar
        StudyProgress(
            quizCount = state.quizCount,
            goodCount = state.goodCount,
            modifier = Modifier.padding(horizontal = 14.dp),
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Voice pack — masquée une fois le pack du niveau téléchargé (sauf DL en cours)
        val showVoiceCard = !state.voicesDownloaded || state.voiceDownloadProgress != null
        if (showVoiceCard) {
            VoiceDownloadCard(
                downloaded = state.voicesDownloaded,
                sizeMb = state.voiceSizeMb,
                progress = state.voiceDownloadProgress,
                onDownloadClick = onDownloadVoices,
                modifier = Modifier.padding(horizontal = 14.dp),
            )

            Spacer(modifier = Modifier.height(6.dp))
        }

        // Quiz list — each item is a separated card; a bottom fade softens the cut to the FAB.
        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 28.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(state.quizzes, key = { it.id }) { quiz ->
                    StudyQuizItem(
                        quiz = quiz,
                        onChecked = { checked -> onQuizChecked(quiz.id, checked) },
                        onClick = { onQuizClick(quiz) },
                    )
                }
            }
            // Bottom fade-out gradient
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(28.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, BackgroundPrimary),
                        )
                    ),
            )
        }

        // FABBar — décalé au-dessus de la barre de navigation flottante
        FABBar(
            state = fabState,
            onClick = { showLaunchSheet = true },
            modifier = Modifier.padding(
                start = 14.dp,
                end = 14.dp,
                bottom = floatingNavBarBottomPadding(),
            ),
        )
    }

    if (showLaunchSheet) {
        LaunchOptionsSheet(
            selectedTypes = state.selectedTypes,
            lastMode = state.lastMode,
            onQuizTypeToggle = onQuizTypeToggle,
            onModeSelected = { strategy ->
                showLaunchSheet = false
                onLaunchQuiz(strategy)
            },
            onDismiss = { showLaunchSheet = false },
        )
    }
}

// MARK: — Previews

@Preview(name = "StudyScreen — dark", showBackground = true, backgroundColor = 0xFF0A0E17)
@Composable
private fun StudyScreenPreview() {
    val quizzes = listOf(
        Quiz(1L, "Hiragana A%あ行", "Hiragana A%あ行", Categories.CATEGORY_HIRAGANA, false),
        Quiz(2L, "Hiragana K%か行", "Hiragana K%か行", Categories.CATEGORY_HIRAGANA, true),
        Quiz(3L, "Hiragana S%さ行", "Hiragana S%さ行", Categories.CATEGORY_HIRAGANA, false),
        Quiz(4L, "Hiragana T%た行", "Hiragana T%た行", Categories.CATEGORY_HIRAGANA, false),
    )
    YomikataTheme {
        StudyScreen(
            state = StudyUiState(
                selectedCategory = Categories.CATEGORY_HIRAGANA,
                quizzes = quizzes,
                quizCount = 46,
                goodCount = 28,
                wrongCount = 12,
                voiceSizeMb = 5,
            ),
            onCategorySelected = {},
            onQuizChecked = { _, _ -> },
            onQuizClick = {},
            onLaunchQuiz = {},
            onQuizTypeToggle = {},
            onDownloadVoices = {},
        )
    }
}

@Preview(name = "StudyScreen N4 — dark", showBackground = true, backgroundColor = 0xFF0A0E17)
@Composable
private fun StudyScreenN4Preview() {
    YomikataTheme {
        StudyScreen(
            state = StudyUiState(
                selectedCategory = Categories.CATEGORY_JLPT_4,
                quizzes = emptyList(),
                quizCount = 0,
                goodCount = 0,
                wrongCount = 0,
                voicesDownloaded = true,
                voiceSizeMb = 7,
            ),
            onCategorySelected = {},
            onQuizChecked = { _, _ -> },
            onQuizClick = {},
            onLaunchQuiz = {},
            onQuizTypeToggle = {},
            onDownloadVoices = {},
        )
    }
}
