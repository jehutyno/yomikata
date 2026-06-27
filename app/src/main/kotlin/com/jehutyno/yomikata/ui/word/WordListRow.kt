package com.jehutyno.yomikata.ui.word

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.ui.theme.AccentOrange
import com.jehutyno.yomikata.ui.theme.BorderDefault
import com.jehutyno.yomikata.ui.theme.BorderSubtle
import com.jehutyno.yomikata.ui.theme.PosChipText
import com.jehutyno.yomikata.ui.theme.TextDim
import com.jehutyno.yomikata.ui.theme.TextGhost
import com.jehutyno.yomikata.ui.theme.TextMuted
import com.jehutyno.yomikata.ui.theme.TextPrimary
import com.jehutyno.yomikata.ui.theme.TypeCaption
import com.jehutyno.yomikata.ui.theme.YomikataTheme

private val masteryColors = listOf(
    Color(0xFFD22828), Color(0xFFD24328), Color(0xFFD25728), Color(0xFFD26028),
    Color(0xFFD26728), Color(0xFFD26C28), Color(0xFFD27728), Color(0xFFD28E28),
    Color(0xFFD2B028), Color(0xFFD2BB28), Color(0xFFD2C628), Color(0xFFD2D228),
    Color(0xFFD2D228), Color(0xFFBBD228), Color(0xFF99D228), Color(0xFF77D228),
)

private fun masteryDotColor(score: Int): Color =
    if (score > 0) masteryColors[(score - 1).coerceIn(0, 15)] else BorderDefault

/**
 * One row in the word list. Kanji is always TextPrimary — never red (DESIGN.md rule §7).
 *
 * @param japanese   kanji/kana form of the word
 * @param furigana   reading shown above the kanji (9sp, TextDim)
 * @param translation localized translation
 * @param score      mastery score 0..16
 * @param posTokens  POS tokens from Word.getPosTokens() — first token shown as chip
 * @param isFavorite whether the ★ icon is filled (active)
 * @param onFavoriteClick callback for the star button
 * @param onAudioClick    callback for the speaker button
 * @param onClick         callback for tapping the row
 */
@Composable
fun WordListRow(
    japanese: String,
    furigana: String,
    translation: String,
    score: Int,
    posTokens: List<String>,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    onAudioClick: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            // Mastery dot
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(masteryDotColor(score)),
            )

            Spacer(modifier = Modifier.width(10.dp))

            // Furigana + kanji + translation
            Column(modifier = Modifier.weight(1f)) {
                if (furigana.isNotEmpty()) {
                    Text(
                        text = furigana,
                        fontSize = 9.sp,
                        color = TextDim,
                        letterSpacing = 0.06.sp,
                    )
                }
                Text(
                    text = japanese,
                    fontSize = 18.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.W500,
                    color = TextPrimary,
                )
                Text(
                    text = translation,
                    style = TypeCaption,
                    color = TextMuted,
                )
            }

            // POS chip (first token only)
            if (posTokens.isNotEmpty()) {
                val token = posTokens.first()
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(posChipColor(token))
                        .padding(horizontal = 5.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = posChipLabel(token),
                        fontSize = 9.sp,
                        color = PosChipText,
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
            }

            // Action icons
            IconButton(onClick = onFavoriteClick, modifier = Modifier.size(32.dp)) {
                Icon(
                    painter = painterResource(R.drawable.ic_star_black_24dp),
                    contentDescription = stringResource(R.string.action_favorite),
                    tint = if (isFavorite) AccentOrange else TextGhost,
                    modifier = Modifier.size(13.dp),
                )
            }
            IconButton(onClick = onAudioClick, modifier = Modifier.size(32.dp)) {
                Icon(
                    painter = painterResource(R.drawable.ic_volume_up_black_24dp),
                    contentDescription = stringResource(R.string.action_audio),
                    tint = TextGhost,
                    modifier = Modifier.size(13.dp),
                )
            }
        }

        // Divider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(BorderSubtle),
        )
    }
}

@Preview(name = "WordListRow — not studied", showBackground = true, backgroundColor = 0xFF0A0E17)
@Composable
private fun WordListRowNotStudiedPreview() {
    YomikataTheme {
        WordListRow(
            japanese = "食べる",
            furigana = "たべる",
            translation = "to eat",
            score = 0,
            posTokens = listOf("v1"),
            isFavorite = false,
            onFavoriteClick = {},
            onAudioClick = {},
            onClick = {},
        )
    }
}

@Preview(name = "WordListRow — mastered, favorite", showBackground = true, backgroundColor = 0xFF0A0E17)
@Composable
private fun WordListRowMasteredPreview() {
    YomikataTheme {
        WordListRow(
            japanese = "学校",
            furigana = "がっこう",
            translation = "school",
            score = 16,
            posTokens = listOf("n"),
            isFavorite = true,
            onFavoriteClick = {},
            onAudioClick = {},
            onClick = {},
        )
    }
}

@Preview(name = "WordListRow — kana only, no pos", showBackground = true, backgroundColor = 0xFF0A0E17)
@Composable
private fun WordListRowKanaPreview() {
    YomikataTheme {
        WordListRow(
            japanese = "あ",
            furigana = "",
            translation = "a (hiragana)",
            score = 7,
            posTokens = emptyList(),
            isFavorite = false,
            onFavoriteClick = {},
            onAudioClick = {},
            onClick = {},
        )
    }
}
