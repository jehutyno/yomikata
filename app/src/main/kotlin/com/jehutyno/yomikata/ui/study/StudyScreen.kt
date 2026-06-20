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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.model.Quiz
import com.jehutyno.yomikata.ui.components.FABBar
import com.jehutyno.yomikata.ui.components.FABBarState
import com.jehutyno.yomikata.ui.components.KenBurnsImage
import com.jehutyno.yomikata.ui.components.SectionHeader
import com.jehutyno.yomikata.ui.theme.AccentOrange
import com.jehutyno.yomikata.ui.theme.BackgroundHero
import com.jehutyno.yomikata.ui.theme.BackgroundPrimary
import com.jehutyno.yomikata.ui.theme.BorderDefault
import com.jehutyno.yomikata.ui.theme.Correct
import com.jehutyno.yomikata.ui.theme.RadiusMd
import com.jehutyno.yomikata.ui.theme.RadiusPill
import com.jehutyno.yomikata.ui.theme.SurfaceAccent
import com.jehutyno.yomikata.ui.theme.SurfacePrimary
import com.jehutyno.yomikata.ui.theme.TextDim
import com.jehutyno.yomikata.ui.theme.TextGhost
import com.jehutyno.yomikata.ui.theme.TextMuted
import com.jehutyno.yomikata.ui.theme.TextPrimary
import com.jehutyno.yomikata.ui.theme.TypeHeroTitle
import com.jehutyno.yomikata.ui.theme.Wrong
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
)

/** Quiz-type chip definition: type + label + check/uncheck icons. */
private data class QuizTypeOption(
    val type: QuizType,
    @StringRes val labelRes: Int,
    @DrawableRes val iconChecked: Int,
    @DrawableRes val iconUnchecked: Int,
)

private val QuizTypeOptions = listOf(
    QuizTypeOption(QuizType.TYPE_AUTO, R.string.quiz_type_auto, R.drawable.ic_auto_check, R.drawable.ic_auto_uncheck),
    QuizTypeOption(QuizType.TYPE_PRONUNCIATION, R.string.quiz_type_pronunciation, R.drawable.ic_pronunciation_check, R.drawable.ic_pronunciation_uncheck),
    QuizTypeOption(QuizType.TYPE_PRONUNCIATION_QCM, R.string.quiz_type_pronunciation_qcm, R.drawable.ic_pronunciation_qcm_check, R.drawable.ic_pronunciation_qcm_uncheck),
    QuizTypeOption(QuizType.TYPE_AUDIO, R.string.quiz_type_audio, R.drawable.ic_sound_qcm_check, R.drawable.ic_sound_qcm_uncheck),
    QuizTypeOption(QuizType.TYPE_EN_JAP, R.string.quiz_type_en_jap, R.drawable.ic_en_jap_check, R.drawable.ic_en_jap_uncheck),
    QuizTypeOption(QuizType.TYPE_JAP_EN, R.string.quiz_type_jap_en, R.drawable.ic_jap_en_check, R.drawable.ic_jap_en_uncheck),
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
private fun StudyProgressBars(
    quizCount: Int,
    goodCount: Int,
    wrongCount: Int,
    modifier: Modifier = Modifier,
) {
    val total = quizCount.coerceAtLeast(1).toFloat()
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .fillMaxWidth()
            .background(SurfacePrimary, RoundedCornerShape(RadiusMd))
            .border(1.dp, BorderDefault, RoundedCornerShape(RadiusMd))
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        ProgressRow(label = "Maîtrisés", count = goodCount, total = quizCount, barColor = Correct)
        ProgressRow(label = "À revoir", count = wrongCount, total = quizCount, barColor = Wrong)
        ProgressRow(label = "Total", count = quizCount, total = quizCount, barColor = TextMuted)
    }
}

@Composable
private fun ProgressRow(
    label: String,
    count: Int,
    total: Int,
    barColor: androidx.compose.ui.graphics.Color,
) {
    val fraction = if (total > 0) count.toFloat() / total else 0f
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextDim,
            modifier = Modifier.width(72.dp),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(BorderDefault),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize(fraction.coerceIn(0f, 1f))
                    .background(barColor),
            )
        }
        Text(
            text = count.toString(),
            fontSize = 10.sp,
            color = TextMuted,
            modifier = Modifier.width(28.dp),
        )
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

        // Progress bars
        StudyProgressBars(
            quizCount = state.quizCount,
            goodCount = state.goodCount,
            wrongCount = state.wrongCount,
            modifier = Modifier.padding(horizontal = 14.dp),
        )

        Spacer(modifier = Modifier.height(6.dp))

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

        // FABBar
        FABBar(
            state = fabState,
            onClick = { showLaunchSheet = true },
            modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 8.dp),
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

// MARK: — Launch options sheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LaunchOptionsSheet(
    selectedTypes: List<QuizType>,
    lastMode: QuizStrategy?,
    onQuizTypeToggle: (QuizType) -> Unit,
    onModeSelected: (QuizStrategy) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = SurfacePrimary,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
        ) {
            SectionHeader(text = stringResource(R.string.quiz_types_header))
            Spacer(modifier = Modifier.height(10.dp))

            // AUTO stands apart, full width on top: selecting it clears every manual type.
            val autoOption = QuizTypeOptions.first { it.type == QuizType.TYPE_AUTO }
            QuizTypeChip(
                labelRes = autoOption.labelRes,
                iconChecked = autoOption.iconChecked,
                iconUnchecked = autoOption.iconUnchecked,
                isSelected = selectedTypes.contains(autoOption.type),
                onClick = { onQuizTypeToggle(autoOption.type) },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Manual types in a uniform 2-column grid (equal-width chips).
            val manualOptions = QuizTypeOptions.filter { it.type != QuizType.TYPE_AUTO }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                manualOptions.chunked(2).forEach { rowOptions ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        rowOptions.forEach { option ->
                            QuizTypeChip(
                                labelRes = option.labelRes,
                                iconChecked = option.iconChecked,
                                iconUnchecked = option.iconUnchecked,
                                isSelected = selectedTypes.contains(option.type),
                                onClick = { onQuizTypeToggle(option.type) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (rowOptions.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            SectionHeader(text = stringResource(R.string.launch_mode_header))
            Spacer(modifier = Modifier.height(4.dp))
            LaunchModeRow(
                iconRes = R.drawable.ic_progress,
                labelRes = R.string.practice_progressive,
                isHighlighted = lastMode == QuizStrategy.PROGRESSIVE,
                onClick = { onModeSelected(QuizStrategy.PROGRESSIVE) },
            )
            LaunchModeRow(
                iconRes = R.drawable.ic_straight,
                labelRes = R.string.practice_normal,
                isHighlighted = lastMode == QuizStrategy.STRAIGHT,
                onClick = { onModeSelected(QuizStrategy.STRAIGHT) },
            )
            LaunchModeRow(
                iconRes = R.drawable.ic_shuffle_white_24dp,
                labelRes = R.string.practice_random,
                isHighlighted = lastMode == QuizStrategy.SHUFFLE,
                onClick = { onModeSelected(QuizStrategy.SHUFFLE) },
            )
        }
    }
}

@Composable
private fun QuizTypeChip(
    @StringRes labelRes: Int,
    @DrawableRes iconChecked: Int,
    @DrawableRes iconUnchecked: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = if (isSelected) SurfaceAccent else SurfacePrimary
    val border = if (isSelected) AccentOrange else BorderDefault
    val textColor = if (isSelected) AccentOrange else TextMuted

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(RadiusPill))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(RadiusPill))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
    ) {
        Image(
            painter = painterResource(if (isSelected) iconChecked else iconUnchecked),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = stringResource(labelRes),
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = textColor,
            maxLines = 2,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun LaunchModeRow(
    @DrawableRes iconRes: Int,
    @StringRes labelRes: Int,
    isHighlighted: Boolean,
    onClick: () -> Unit,
) {
    val border = if (isHighlighted) AccentOrange else Color.Transparent
    val bg = if (isHighlighted) SurfaceAccent else Color.Transparent

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(RadiusMd))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(RadiusMd))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = AccentOrange,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = stringResource(labelRes),
            fontSize = 15.sp,
            color = TextPrimary,
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
            ),
            onCategorySelected = {},
            onQuizChecked = { _, _ -> },
            onQuizClick = {},
            onLaunchQuiz = {},
            onQuizTypeToggle = {},
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
            ),
            onCategorySelected = {},
            onQuizChecked = { _, _ -> },
            onQuizClick = {},
            onLaunchQuiz = {},
            onQuizTypeToggle = {},
        )
    }
}
