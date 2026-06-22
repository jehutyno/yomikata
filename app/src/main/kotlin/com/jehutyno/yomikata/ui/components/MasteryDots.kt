package com.jehutyno.yomikata.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jehutyno.yomikata.ui.theme.BorderDefault
import com.jehutyno.yomikata.ui.theme.TextGhost
import com.jehutyno.yomikata.ui.theme.TypeMicro
import com.jehutyno.yomikata.ui.theme.YomikataTheme
import androidx.compose.ui.graphics.Color

// Ordered from lowest (index 0) to highest (index 15) mastery
private val masteryColors = listOf(
    Color(0xFFD22828), Color(0xFFD24328), Color(0xFFD25728), Color(0xFFD26028), // Low 1-4
    Color(0xFFD26728), Color(0xFFD26C28), Color(0xFFD27728), Color(0xFFD28E28), // Medium 1-4
    Color(0xFFD2B028), Color(0xFFD2BB28), Color(0xFFD2C628), Color(0xFFD2D228), // High 1-4
    Color(0xFFD2D228), Color(0xFFBBD228), Color(0xFF99D228), Color(0xFF77D228), // Master 1-4
)

// Dot i lights up when score >= threshold[i]
private val dotThresholds = listOf(1, 4, 7, 10, 13)

/**
 * 5 mastery dots (7×7dp pill) for a word card or detail view.
 *
 * @param score mastery level 0..16 (0 = not studied, 1-16 = mastery scale)
 * @param label optional text shown to the right of the dots
 */
@Composable
fun MasteryDots(
    score: Int,
    modifier: Modifier = Modifier,
    label: String? = null,
) {
    val dotColor = if (score > 0) masteryColors[(score - 1).coerceIn(0, 15)] else BorderDefault

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        modifier = modifier,
    ) {
        dotThresholds.forEach { threshold ->
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(if (score >= threshold) dotColor else BorderDefault),
            )
        }
        if (label != null) {
            Text(
                text = label,
                style = TypeMicro,
                color = TextGhost,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }
}

@Preview(name = "MasteryDots — not studied", showBackground = true, backgroundColor = 0xFF0A0E17)
@Composable
private fun MasteryDotsNotStudiedPreview() {
    YomikataTheme {
        MasteryDots(score = 0, label = "Non étudié", modifier = Modifier.padding(14.dp))
    }
}

@Preview(name = "MasteryDots — low (score 2)", showBackground = true, backgroundColor = 0xFF0A0E17)
@Composable
private fun MasteryDotsLowPreview() {
    YomikataTheme {
        MasteryDots(score = 2, label = "Débutant", modifier = Modifier.padding(14.dp))
    }
}

@Preview(name = "MasteryDots — master (score 16)", showBackground = true, backgroundColor = 0xFF0A0E17)
@Composable
private fun MasteryDotsMasterPreview() {
    YomikataTheme {
        MasteryDots(score = 16, label = "Maîtrisé", modifier = Modifier.padding(14.dp))
    }
}
