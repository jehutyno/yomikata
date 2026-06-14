package com.jehutyno.yomikata.ui.quiz

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jehutyno.yomikata.ui.theme.BackgroundCorrect
import com.jehutyno.yomikata.ui.theme.BackgroundWrong
import com.jehutyno.yomikata.ui.theme.BorderCorrect
import com.jehutyno.yomikata.ui.theme.BorderDefault
import com.jehutyno.yomikata.ui.theme.BorderWrong
import com.jehutyno.yomikata.ui.theme.Correct
import com.jehutyno.yomikata.ui.theme.RadiusLg
import com.jehutyno.yomikata.ui.theme.SurfacePrimary
import com.jehutyno.yomikata.ui.theme.TextDim
import com.jehutyno.yomikata.ui.theme.TextPrimary
import com.jehutyno.yomikata.ui.theme.Wrong
import com.jehutyno.yomikata.ui.theme.YomikataTheme

enum class AnswerButtonState { Default, Correct, Wrong }

@Composable
fun AnswerButton(
    label: String,
    indexLetter: Char,
    state: AnswerButtonState,
    isRevealed: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dimmed = isRevealed && state == AnswerButtonState.Default

    val bgColor = when (state) {
        AnswerButtonState.Default -> SurfacePrimary
        AnswerButtonState.Correct -> BackgroundCorrect
        AnswerButtonState.Wrong -> if (isSelected) BackgroundWrong else SurfacePrimary
    }
    val borderColor = when (state) {
        AnswerButtonState.Default -> BorderDefault
        AnswerButtonState.Correct -> BorderCorrect
        AnswerButtonState.Wrong -> if (isSelected) BorderWrong else BorderDefault
    }
    val textColor = when (state) {
        AnswerButtonState.Default -> TextPrimary
        AnswerButtonState.Correct -> Correct
        AnswerButtonState.Wrong -> if (isSelected) Wrong else TextPrimary
    }
    val letterBg = when (state) {
        AnswerButtonState.Correct -> Color(0xFF1A3A1A)
        AnswerButtonState.Wrong -> if (isSelected) Color(0xFF3A1A1A) else Color(0xFF1E2B3A)
        else -> Color(0xFF1E2B3A)
    }
    val letterTextColor = when (state) {
        AnswerButtonState.Correct -> Correct
        AnswerButtonState.Wrong -> if (isSelected) Wrong else TextDim
        else -> TextDim
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .alpha(if (dimmed) 0.4f else 1f)
            .clip(RoundedCornerShape(RadiusLg))
            .background(bgColor)
            .border(
                width = if (state != AnswerButtonState.Default) 1.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(RadiusLg)
            )
            .clickable(enabled = !isRevealed, onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(letterBg),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = indexLetter.toString(),
                fontSize = 10.sp,
                fontWeight = FontWeight.W600,
                color = letterTextColor
            )
        }
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.W400,
            color = textColor,
            modifier = Modifier.weight(1f)
        )
        if (state == AnswerButtonState.Correct) {
            Text(text = "✓", fontSize = 14.sp, color = Correct)
        }
    }
}

// ─── Segment colors ───────────────────────────────────────────────────────────

enum class SegmentState { Pending, Current, Correct, Wrong }

private val SegmentCorrectColor = Correct
private val SegmentWrongColor = Wrong
private val SegmentCurrentColor = Color(0xFFFB8C00)
private val SegmentPendingColor = Color(0xFF1E2B3A)

@Composable
fun ProgressSegmentBar(
    segments: List<SegmentState>,
    modifier: Modifier = Modifier
) {
    val correctCount = segments.count { it == SegmentState.Correct }
    val wrongCount = segments.count { it == SegmentState.Wrong }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            segments.forEach { seg ->
                val color = when (seg) {
                    SegmentState.Correct -> SegmentCorrectColor
                    SegmentState.Wrong -> SegmentWrongColor
                    SegmentState.Current -> SegmentCurrentColor
                    SegmentState.Pending -> SegmentPendingColor
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(color)
                )
            }
        }
        if (correctCount > 0 || wrongCount > 0) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (correctCount > 0) {
                    Text(
                        text = "$correctCount ✓",
                        fontSize = 10.sp,
                        color = Correct
                    )
                }
                if (wrongCount > 0) {
                    if (correctCount > 0) Spacer(Modifier.width(8.dp))
                    Text(
                        text = "$wrongCount ✗",
                        fontSize = 10.sp,
                        color = Wrong
                    )
                }
            }
        }
    }
}

// ─── Previews ─────────────────────────────────────────────────────────────────

@Preview(showBackground = true, backgroundColor = 0xFF0A0E17, name = "AnswerButton — Default")
@Composable
private fun PreviewAnswerButtonDefault() {
    YomikataTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AnswerButton(
                label = "食べる (to eat)",
                indexLetter = 'A',
                state = AnswerButtonState.Default,
                isRevealed = false,
                isSelected = false,
                onClick = {}
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0E17, name = "AnswerButton — Correct")
@Composable
private fun PreviewAnswerButtonCorrect() {
    YomikataTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AnswerButton(
                label = "食べる (to eat)",
                indexLetter = 'B',
                state = AnswerButtonState.Correct,
                isRevealed = true,
                isSelected = true,
                onClick = {}
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0E17, name = "AnswerButton — Wrong (all states)")
@Composable
private fun PreviewAnswerButtonWrong() {
    YomikataTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Wrong selected
            AnswerButton(
                label = "飲む (to drink)",
                indexLetter = 'A',
                state = AnswerButtonState.Wrong,
                isRevealed = true,
                isSelected = true,
                onClick = {}
            )
            // Wrong non-selected (dimmed)
            AnswerButton(
                label = "書く (to write)",
                indexLetter = 'C',
                state = AnswerButtonState.Wrong,
                isRevealed = true,
                isSelected = false,
                onClick = {}
            )
            // Correct revealed alongside wrong
            AnswerButton(
                label = "食べる (to eat)",
                indexLetter = 'B',
                state = AnswerButtonState.Correct,
                isRevealed = true,
                isSelected = false,
                onClick = {}
            )
            // Default non-selected (dimmed)
            AnswerButton(
                label = "見る (to see)",
                indexLetter = 'D',
                state = AnswerButtonState.Wrong,
                isRevealed = true,
                isSelected = false,
                onClick = {}
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0E17, name = "ProgressSegmentBar — 10 segments")
@Composable
private fun PreviewProgressSegmentBar() {
    YomikataTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            ProgressSegmentBar(
                segments = listOf(
                    SegmentState.Correct,
                    SegmentState.Correct,
                    SegmentState.Wrong,
                    SegmentState.Correct,
                    SegmentState.Current,
                    SegmentState.Pending,
                    SegmentState.Pending,
                    SegmentState.Pending,
                    SegmentState.Pending,
                    SegmentState.Pending,
                )
            )
        }
    }
}
