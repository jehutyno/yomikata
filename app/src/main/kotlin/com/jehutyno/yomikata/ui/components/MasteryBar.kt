package com.jehutyno.yomikata.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.ui.theme.BorderDefault
import com.jehutyno.yomikata.ui.theme.Correct
import com.jehutyno.yomikata.ui.theme.MasteryMedium4
import com.jehutyno.yomikata.ui.theme.RadiusPill
import com.jehutyno.yomikata.ui.theme.TextMuted
import com.jehutyno.yomikata.ui.theme.TextPrimary
import com.jehutyno.yomikata.ui.theme.YomikataTheme

/** Color of mastered words (good) — DESIGN.md §1. */
private val MasteredColor = Correct
/** Color of words still to review — amber from the mastery scale (never the red Wrong). */
private val ToReviewColor = MasteryMedium4

/**
 * Single segmented mastery bar shared by Study and the word list.
 *
 * Shows one distribution: how many words are mastered (HIGH+MASTER) versus to review
 * (LOW+MEDIUM, including never-seen words). Replaces the old 3 stacked bars / Material tabs.
 *
 * @param total     total number of words
 * @param mastered  number of mastered words (mastered ≤ total)
 * @param showLegend when true, shows the "● N mastered / ● N to review" legend below the bar
 */
@Composable
fun MasteryBar(
    total: Int,
    mastered: Int,
    modifier: Modifier = Modifier,
    showLegend: Boolean = true,
) {
    val safeMastered = mastered.coerceIn(0, total.coerceAtLeast(0))
    val toReview = (total - safeMastered).coerceAtLeast(0)
    val percent = if (total > 0) safeMastered * 100 / total else 0

    Column(modifier = modifier.fillMaxWidth()) {
        // Top row: "61% mastered" on the left, total on the right.
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "$percent%",
                fontSize = 22.sp,
                fontWeight = FontWeight.W500,
                color = TextPrimary,
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = stringResource(R.string.mastery_percent_label),
                fontSize = 12.sp,
                color = TextMuted,
                modifier = Modifier.padding(bottom = 3.dp),
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "$total ${stringResource(R.string.word_count_label)}",
                fontSize = 12.sp,
                color = TextMuted,
                modifier = Modifier.padding(bottom = 3.dp),
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // The bar: track + two proportional segments.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(9.dp)
                .clip(RoundedCornerShape(RadiusPill))
                .background(BorderDefault),
        ) {
            if (total > 0) {
                Row(modifier = Modifier.fillMaxSize()) {
                    if (safeMastered > 0) {
                        Box(
                            modifier = Modifier
                                .weight(safeMastered.toFloat())
                                .fillMaxSize()
                                .background(MasteredColor),
                        )
                    }
                    if (toReview > 0) {
                        Box(
                            modifier = Modifier
                                .weight(toReview.toFloat())
                                .fillMaxSize()
                                .background(ToReviewColor),
                        )
                    }
                }
            }
        }

        if (showLegend) {
            Spacer(modifier = Modifier.height(9.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LegendEntry(MasteredColor, safeMastered, stringResource(R.string.mastered))
                LegendEntry(ToReviewColor, toReview, stringResource(R.string.mastery_to_review))
            }
        }
    }
}

@Composable
private fun LegendEntry(color: Color, count: Int, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = "$count $label",
            fontSize = 11.sp,
            color = TextMuted,
        )
    }
}

@Preview(name = "MasteryBar — populated", showBackground = true, backgroundColor = 0xFF0A0E17)
@Composable
private fun MasteryBarPreview() {
    YomikataTheme {
        MasteryBar(total = 46, mastered = 28, modifier = Modifier.padding(14.dp))
    }
}

@Preview(name = "MasteryBar — compact (no legend)", showBackground = true, backgroundColor = 0xFF0A0E17)
@Composable
private fun MasteryBarCompactPreview() {
    YomikataTheme {
        MasteryBar(total = 46, mastered = 28, showLegend = false, modifier = Modifier.padding(14.dp))
    }
}

@Preview(name = "MasteryBar — empty", showBackground = true, backgroundColor = 0xFF0A0E17)
@Composable
private fun MasteryBarEmptyPreview() {
    YomikataTheme {
        MasteryBar(total = 0, mastered = 0, modifier = Modifier.padding(14.dp))
    }
}
