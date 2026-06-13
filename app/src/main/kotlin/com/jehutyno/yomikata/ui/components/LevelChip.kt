package com.jehutyno.yomikata.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jehutyno.yomikata.ui.theme.AccentOrange
import com.jehutyno.yomikata.ui.theme.BorderDefault
import com.jehutyno.yomikata.ui.theme.RadiusSm
import com.jehutyno.yomikata.ui.theme.SurfaceAccent
import com.jehutyno.yomikata.ui.theme.SurfacePrimary
import com.jehutyno.yomikata.ui.theme.TextMuted
import com.jehutyno.yomikata.ui.theme.TypeBody
import com.jehutyno.yomikata.ui.theme.YomikataTheme
import com.jehutyno.yomikata.util.quiz.Categories

/** Study level destinations — maps each Category constant to a display label. */
enum class StudyLevel(val categoryId: Int, val label: String) {
    Hiragana(Categories.CATEGORY_HIRAGANA, "Hiragana"),
    Katakana(Categories.CATEGORY_KATAKANA, "Katakana"),
    Kanji(Categories.CATEGORY_KANJI, "漢字"),
    Counters(Categories.CATEGORY_COUNTERS, "数"),
    N5(Categories.CATEGORY_JLPT_5, "N5"),
    N4(Categories.CATEGORY_JLPT_4, "N4"),
    N3(Categories.CATEGORY_JLPT_3, "N3"),
    N2(Categories.CATEGORY_JLPT_2, "N2"),
    N1(Categories.CATEGORY_JLPT_1, "N1"),
}

/**
 * Single selectable chip for a study level.
 *
 * - Active: SurfaceAccent background, AccentOrange border and text
 * - Inactive: SurfacePrimary background, BorderDefault border, TextMuted text
 */
@Composable
fun LevelChip(
    level: StudyLevel,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor = if (isSelected) SurfaceAccent else SurfacePrimary
    val borderColor = if (isSelected) AccentOrange else BorderDefault
    val textColor = if (isSelected) AccentOrange else TextMuted

    Surface(
        onClick = onClick,
        color = containerColor,
        shape = RoundedCornerShape(RadiusSm),
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier,
    ) {
        Text(
            text = level.label,
            style = TypeBody,
            color = textColor,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

/**
 * Horizontal scrollable row of all [StudyLevel] chips.
 */
@Composable
fun LevelChipRow(
    selectedLevel: StudyLevel,
    onLevelSelected: (StudyLevel) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 10.dp, vertical = 9.dp),
    ) {
        StudyLevel.entries.forEach { level ->
            LevelChip(
                level = level,
                isSelected = level == selectedLevel,
                onClick = { onLevelSelected(level) },
            )
        }
    }
}

@Preview(name = "LevelChip active — dark", showBackground = true, backgroundColor = 0xFF0A0E17)
@Composable
private fun LevelChipActivePreview() {
    YomikataTheme {
        LevelChip(
            level = StudyLevel.N4,
            isSelected = true,
            onClick = {},
            modifier = Modifier.padding(14.dp),
        )
    }
}

@Preview(name = "LevelChip inactive — dark", showBackground = true, backgroundColor = 0xFF0A0E17)
@Composable
private fun LevelChipInactivePreview() {
    YomikataTheme {
        LevelChip(
            level = StudyLevel.N4,
            isSelected = false,
            onClick = {},
            modifier = Modifier.padding(14.dp),
        )
    }
}

@Preview(name = "LevelChipRow — dark", showBackground = true, backgroundColor = 0xFF0A0E17)
@Composable
private fun LevelChipRowPreview() {
    YomikataTheme {
        var selected by remember { mutableIntStateOf(StudyLevel.N4.ordinal) }
        LevelChipRow(
            selectedLevel = StudyLevel.entries[selected],
            onLevelSelected = { selected = it.ordinal },
        )
    }
}
