package com.jehutyno.yomikata.ui.word

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jehutyno.yomikata.model.KanjiSolo
import com.jehutyno.yomikata.ui.theme.BorderDefault
import com.jehutyno.yomikata.ui.theme.RadiusMd
import com.jehutyno.yomikata.ui.theme.SurfacePrimary
import com.jehutyno.yomikata.ui.theme.TextDim
import com.jehutyno.yomikata.ui.theme.TextGhost
import com.jehutyno.yomikata.ui.theme.TextMuted
import com.jehutyno.yomikata.ui.theme.TextPrimary
import com.jehutyno.yomikata.ui.theme.YomikataTheme

/**
 * Card showing a single kanji component (stroke count, readings, radical).
 * Layout: [kanji 42dp] | [separator 1dp] | [info flex]
 */
@Composable
fun KanjiComponentCard(
    kanji: KanjiSolo,
    meaning: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(RadiusMd))
            .background(SurfacePrimary)
            .border(1.dp, BorderDefault, RoundedCornerShape(RadiusMd))
            .padding(12.dp)
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Kanji glyph + stroke count
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(42.dp),
        ) {
            Text(
                text = kanji.kanji,
                fontSize = 32.sp,
                fontWeight = FontWeight.W300,
                color = TextPrimary,
            )
            Text(
                text = "${kanji.strokes}画",
                fontSize = 8.sp,
                color = TextGhost,
                modifier = Modifier.padding(top = 3.dp),
            )
        }

        // Vertical separator
        Box(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .width(1.dp)
                .fillMaxHeight()
                .background(BorderDefault),
        )

        // Info column
        Column {
            Text(
                text = meaning,
                fontSize = 12.sp,
                fontWeight = FontWeight.W500,
                color = TextPrimary,
            )
            Spacer(modifier = Modifier.height(6.dp))
            if (kanji.kunyomi.isNotEmpty()) {
                ReadingRow(label = "訓", value = kanji.kunyomi)
            }
            if (kanji.onyomi.isNotEmpty()) {
                ReadingRow(label = "音", value = kanji.onyomi)
            }
            if (kanji.radical.isNotEmpty()) {
                Text(
                    text = "radical: ${kanji.radical}",
                    fontSize = 9.sp,
                    color = TextGhost,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun ReadingRow(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            fontSize = 9.sp,
            color = TextDim,
            modifier = Modifier.width(20.dp),
        )
        Text(
            text = value,
            fontSize = 10.sp,
            color = TextMuted,
        )
    }
}

@Preview(name = "KanjiComponentCard — 食", showBackground = true, backgroundColor = 0xFF0A0E17)
@Composable
private fun KanjiComponentCardPreview() {
    YomikataTheme {
        KanjiComponentCard(
            kanji = KanjiSolo(
                kanji = "食",
                strokes = 9,
                en = "eat, food",
                fr = "manger, nourriture",
                kunyomi = "た.べる、く.う",
                onyomi = "ショク、ジキ",
                radical = "食",
            ),
            meaning = "eat, food",
            modifier = Modifier.padding(14.dp),
        )
    }
}

@Preview(name = "KanjiComponentCard — 学", showBackground = true, backgroundColor = 0xFF0A0E17)
@Composable
private fun KanjiComponentCardSchoolPreview() {
    YomikataTheme {
        KanjiComponentCard(
            kanji = KanjiSolo(
                kanji = "学",
                strokes = 8,
                en = "study, learning",
                fr = "apprendre, étude",
                kunyomi = "まな.ぶ",
                onyomi = "ガク",
                radical = "子",
            ),
            meaning = "study, learning",
            modifier = Modifier.padding(14.dp),
        )
    }
}
