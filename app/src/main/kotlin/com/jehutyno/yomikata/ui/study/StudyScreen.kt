package com.jehutyno.yomikata.ui.study

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jehutyno.yomikata.model.Quiz
import com.jehutyno.yomikata.ui.components.FABBar
import com.jehutyno.yomikata.ui.components.FABBarState
import com.jehutyno.yomikata.ui.theme.AccentOrange
import com.jehutyno.yomikata.ui.theme.BackgroundPrimary
import com.jehutyno.yomikata.ui.theme.BorderDefault
import com.jehutyno.yomikata.ui.theme.BorderSubtle
import com.jehutyno.yomikata.ui.theme.Correct
import com.jehutyno.yomikata.ui.theme.RadiusMd
import com.jehutyno.yomikata.ui.theme.RadiusPill
import com.jehutyno.yomikata.ui.theme.SurfaceAccent
import com.jehutyno.yomikata.ui.theme.SurfacePrimary
import com.jehutyno.yomikata.ui.theme.TextDim
import com.jehutyno.yomikata.ui.theme.TextGhost
import com.jehutyno.yomikata.ui.theme.TextMuted
import com.jehutyno.yomikata.ui.theme.TextPrimary
import com.jehutyno.yomikata.ui.theme.Wrong
import com.jehutyno.yomikata.ui.theme.YomikataTheme
import com.jehutyno.yomikata.util.quiz.Categories

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
)

// MARK: — Composables

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

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
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
    onLaunchQuiz: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedCount = state.quizzes.count { it.isSelected }
    val fabState = if (selectedCount > 0) FABBarState.LaunchSelection(selectedCount)
                   else FABBarState.LaunchAll

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundPrimary),
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
        ) {
            Text(
                text = "学ぶ",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
            )
            Text(
                text = categoryName(state.selectedCategory),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = AccentOrange,
            )
        }

        HorizontalDivider(color = BorderSubtle, thickness = 1.dp)

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

        Spacer(modifier = Modifier.height(8.dp))

        // Quiz list
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(SurfacePrimary, RoundedCornerShape(topStart = RadiusMd, topEnd = RadiusMd))
                .border(1.dp, BorderDefault, RoundedCornerShape(topStart = RadiusMd, topEnd = RadiusMd)),
        ) {
            itemsIndexed(state.quizzes) { index, quiz ->
                StudyQuizItem(
                    quiz = quiz,
                    onChecked = { checked -> onQuizChecked(quiz.id, checked) },
                    onClick = { onQuizClick(quiz) },
                )
                if (index < state.quizzes.lastIndex) {
                    HorizontalDivider(
                        color = BorderSubtle,
                        thickness = 1.dp,
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                }
            }
        }

        // FABBar
        FABBar(
            state = fabState,
            onClick = onLaunchQuiz,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
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
        )
    }
}
