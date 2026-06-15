package com.jehutyno.yomikata.ui.home

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jehutyno.yomikata.ui.theme.BorderDefault
import com.jehutyno.yomikata.ui.theme.Correct
import com.jehutyno.yomikata.ui.theme.RadiusMd
import com.jehutyno.yomikata.ui.theme.SurfacePrimary
import com.jehutyno.yomikata.ui.theme.TextDim
import com.jehutyno.yomikata.ui.theme.TextPrimary
import com.jehutyno.yomikata.ui.theme.Wrong
import com.jehutyno.yomikata.ui.theme.YomikataTheme

/**
 * Card 2×2 de la grille stats home.
 * [valueColor] : TextPrimary pour quiz/words, Correct pour bonnes réponses, Wrong pour mauvaises.
 */
@Composable
fun StatCard(
    value: String,
    label: String,
    valueColor: Color = TextPrimary,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = SurfacePrimary,
        shape = RoundedCornerShape(RadiusMd),
        modifier = modifier
            .border(1.dp, BorderDefault, RoundedCornerShape(RadiusMd)),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 12.dp),
        ) {
            Text(
                text = value,
                fontSize = 26.sp,
                fontWeight = FontWeight.W600,
                color = valueColor,
            )
            Text(
                text = label.uppercase(),
                fontSize = 9.sp,
                fontWeight = FontWeight.W600,
                color = TextDim,
                letterSpacing = 0.12.sp,
            )
        }
    }
}

@Preview(name = "StatCard quiz — dark", showBackground = true, backgroundColor = 0xFF0A0E17)
@Composable
private fun StatCardQuizPreview() {
    YomikataTheme {
        StatCard(value = "12", label = "Quiz lancés", modifier = Modifier.padding(8.dp))
    }
}

@Preview(name = "StatCard correct — dark", showBackground = true, backgroundColor = 0xFF0A0E17)
@Composable
private fun StatCardCorrectPreview() {
    YomikataTheme {
        StatCard(value = "87", label = "Bonnes réponses", valueColor = Correct, modifier = Modifier.padding(8.dp))
    }
}

@Preview(name = "StatCard wrong — dark", showBackground = true, backgroundColor = 0xFF0A0E17)
@Composable
private fun StatCardWrongPreview() {
    YomikataTheme {
        StatCard(value = "23", label = "Mauvaises réponses", valueColor = Wrong, modifier = Modifier.padding(8.dp))
    }
}
