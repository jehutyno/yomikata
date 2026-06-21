package com.jehutyno.yomikata.ui.selections

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.jehutyno.yomikata.ui.components.KenBurnsImage
import com.jehutyno.yomikata.ui.components.MasteryBar
import com.jehutyno.yomikata.ui.components.SectionHeader
import com.jehutyno.yomikata.ui.study.LaunchOptionsSheet
import com.jehutyno.yomikata.ui.theme.AccentOrange
import com.jehutyno.yomikata.ui.theme.BackgroundHero
import com.jehutyno.yomikata.ui.theme.BackgroundPrimary
import com.jehutyno.yomikata.ui.theme.BorderAccent
import com.jehutyno.yomikata.ui.theme.BorderDefault
import com.jehutyno.yomikata.ui.theme.RadiusMd
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

/**
 * UI state for the Selections screen.
 *
 * @param selections  user-created word lists (Quiz with category = CATEGORY_SELECTIONS)
 * @param wordCounts  word count per selection id (for the row subtitle)
 * @param quizCount   total words across the checked-or-all selections (mastery bar)
 * @param goodCount   mastered words across the same set (mastery bar)
 */
data class SelectionsUiState(
    val selections: List<Quiz> = emptyList(),
    val wordCounts: Map<Long, Int> = emptyMap(),
    val quizCount: Int = 0,
    val goodCount: Int = 0,
    val selectedTypes: List<QuizType> = listOf(QuizType.TYPE_AUTO),
    val lastMode: QuizStrategy? = null,
)

// MARK: — Hero

@Composable
private fun SelectionsHero(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp)
            .background(BackgroundHero)
            .clipToBounds(),
    ) {
        // Photo de fond avec effet Ken Burns — image dédiée aux sélections
        KenBurnsImage(resId = R.drawable.pic_le_charme)
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
                painter = painterResource(R.drawable.ic_selections_big),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(80.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "選択",
                style = TypeHeroTitle,
                color = AccentOrange,
            )
        }
    }
}

// MARK: — Rows

@Composable
private fun SelectionItem(
    quiz: Quiz,
    wordCount: Int?,
    onChecked: (Boolean) -> Unit,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val name = quiz.getName().split("%")[0]
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
                text = name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = AccentOrange,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (wordCount != null) {
                Text(
                    text = "$wordCount ${stringResource(R.string.word_count_label)}",
                    fontSize = 12.sp,
                    color = TextDim,
                    maxLines = 1,
                )
            }
        }
        Icon(
            painter = painterResource(R.drawable.ic_tooltip_edit),
            contentDescription = stringResource(R.string.selection_edit),
            tint = TextMuted,
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(RadiusMd))
                .clickable(onClick = onEdit)
                .padding(7.dp),
        )
    }
}

@Composable
private fun CreateSelectionCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RadiusMd))
            .background(SurfacePrimary)
            .border(1.dp, BorderAccent, RoundedCornerShape(RadiusMd))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
    ) {
        Image(
            painter = painterResource(R.drawable.ic_add),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.new_selection),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = AccentOrange,
        )
    }
}

// MARK: — Screen

@Composable
fun SelectionsScreen(
    state: SelectionsUiState,
    onSelectionChecked: (quizId: Long, checked: Boolean) -> Unit,
    onSelectionClick: (Quiz) -> Unit,
    onSelectionEdit: (Quiz) -> Unit,
    onCreate: () -> Unit,
    onLaunchQuiz: (QuizStrategy) -> Unit,
    onQuizTypeToggle: (QuizType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedCount = state.selections.count { it.isSelected }
    val fabState = if (selectedCount > 0) FABBarState.LaunchSelection(selectedCount)
                   else FABBarState.LaunchAll
    var showLaunchSheet by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundPrimary),
    ) {
        SelectionsHero()

        // Progress — only meaningful when there is at least one selection
        if (state.selections.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .padding(horizontal = 14.dp)
                    .fillMaxWidth()
                    .background(SurfacePrimary, RoundedCornerShape(RadiusMd))
                    .border(1.dp, BorderDefault, RoundedCornerShape(RadiusMd))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                SectionHeader(text = stringResource(R.string.progress_header))
                MasteryBar(total = state.quizCount, mastered = state.goodCount, showLegend = true)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Create card + list of selections, or empty state
        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 28.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                item(key = "create") {
                    CreateSelectionCard(onClick = onCreate)
                }
                if (state.selections.isEmpty()) {
                    item(key = "empty") {
                        Text(
                            text = stringResource(R.string.no_selections),
                            fontSize = 14.sp,
                            color = TextMuted,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 24.dp),
                        )
                    }
                }
                items(state.selections, key = { it.id }) { quiz ->
                    SelectionItem(
                        quiz = quiz,
                        wordCount = state.wordCounts[quiz.id],
                        onChecked = { checked -> onSelectionChecked(quiz.id, checked) },
                        onClick = { onSelectionClick(quiz) },
                        onEdit = { onSelectionEdit(quiz) },
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

        // FABBar — only when there is something to launch
        if (state.selections.isNotEmpty()) {
            FABBar(
                state = fabState,
                onClick = { showLaunchSheet = true },
                modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 8.dp),
            )
        }
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

@Preview(name = "SelectionsScreen — dark", showBackground = true, backgroundColor = 0xFF0A0E17)
@Composable
private fun SelectionsScreenPreview() {
    val selections = listOf(
        Quiz(1L, "Mes kanji", "Mes kanji", Categories.CATEGORY_SELECTIONS, false),
        Quiz(2L, "Voyage", "Voyage", Categories.CATEGORY_SELECTIONS, true),
        Quiz(3L, "À revoir", "À revoir", Categories.CATEGORY_SELECTIONS, false),
    )
    YomikataTheme {
        SelectionsScreen(
            state = SelectionsUiState(
                selections = selections,
                wordCounts = mapOf(1L to 24, 2L to 8, 3L to 53),
                quizCount = 85,
                goodCount = 40,
            ),
            onSelectionChecked = { _, _ -> },
            onSelectionClick = {},
            onSelectionEdit = {},
            onCreate = {},
            onLaunchQuiz = {},
            onQuizTypeToggle = {},
        )
    }
}

@Preview(name = "SelectionsScreen — empty — dark", showBackground = true, backgroundColor = 0xFF0A0E17)
@Composable
private fun SelectionsScreenEmptyPreview() {
    YomikataTheme {
        SelectionsScreen(
            state = SelectionsUiState(),
            onSelectionChecked = { _, _ -> },
            onSelectionClick = {},
            onSelectionEdit = {},
            onCreate = {},
            onLaunchQuiz = {},
            onQuizTypeToggle = {},
        )
    }
}
