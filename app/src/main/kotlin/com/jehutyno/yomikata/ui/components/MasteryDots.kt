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
import com.jehutyno.yomikata.util.quiz.Level
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.jehutyno.yomikata.model.getWordColor

// Dot i lights up when points >= threshold[i]
private val dotThresholds = listOf(Level.LOW.minPoints, Level.MEDIUM.minPoints, Level.HIGH.minPoints, Level.MASTER.minPoints)

/**
 * 5 mastery dots (7×7dp pill) for a word card or detail view.
 *
 * @param points mastery level 0..16 (0 = not studied, 1-16 = mastery scale)
 * @param label optional text shown to the right of the dots
 */
@Composable
fun MasteryDots(
    points: Int,
    modifier: Modifier = Modifier,
    label: String? = null,
) {
    val context = LocalContext.current
    val dotColor = Color(getWordColor(context, points))

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
                    .background(if (points >= threshold) dotColor else BorderDefault),
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
        MasteryDots(points = 0, label = "Not studied", modifier = Modifier.padding(14.dp))
    }
}

@Preview(name = "MasteryDots — Low", showBackground = true, backgroundColor = 0xFF0A0E17)
@Composable
private fun MasteryDotsLowPreview() {
    YomikataTheme {
        MasteryDots(points = Level.LOW.minPoints, label = "Low", modifier = Modifier.padding(14.dp))
    }
}

@Preview(name = "MasteryDots — Medium", showBackground = true, backgroundColor = 0xFF0A0E17)
@Composable
private fun MasteryDotsMediumPreview() {
    YomikataTheme {
        MasteryDots(points = Level.MEDIUM.minPoints, label = "Medium", modifier = Modifier.padding(14.dp))
    }
}

@Preview(name = "MasteryDots — High", showBackground = true, backgroundColor = 0xFF0A0E17)
@Composable
private fun MasteryDotsHighPreview() {
    YomikataTheme {
        MasteryDots(points = Level.HIGH.minPoints, label = "High", modifier = Modifier.padding(14.dp))
    }
}

@Preview(name = "MasteryDots — Master", showBackground = true, backgroundColor = 0xFF0A0E17)
@Composable
private fun MasteryDotsMasterPreview() {
    YomikataTheme {
        MasteryDots(points = Level.MASTER.minPoints, label = "Master", modifier = Modifier.padding(14.dp))
    }
}

@Preview(name = "MasteryDots — Master MAX_POINTS", showBackground = true, backgroundColor = 0xFF0A0E17)
@Composable
private fun MasteryDotsMasterMaxPreview() {
    YomikataTheme {
        MasteryDots(points = 850, label = "Master", modifier = Modifier.padding(14.dp))
    }
}
