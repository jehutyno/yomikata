package com.jehutyno.yomikata.ui.word

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.ui.theme.AccentOrange
import com.jehutyno.yomikata.ui.theme.BorderDefault
import com.jehutyno.yomikata.ui.theme.RadiusMd
import com.jehutyno.yomikata.ui.theme.SurfacePrimary
import com.jehutyno.yomikata.ui.theme.TextDim
import com.jehutyno.yomikata.ui.theme.TextGhost
import com.jehutyno.yomikata.ui.theme.TypeMicro
import com.jehutyno.yomikata.ui.theme.YomikataTheme

/**
 * Horizontal action bar for the word detail screen.
 * 4 actions: Favorite | Audio | Copy | Report — separated by 1dp vertical dividers.
 *
 * @param isFavorite   whether the word is starred
 * @param isAudioPlaying whether audio is actively playing (icon tinted orange)
 */
@Composable
fun WordActionBar(
    isFavorite: Boolean,
    isAudioPlaying: Boolean,
    onFavoriteClick: () -> Unit,
    onAudioClick: () -> Unit,
    onCopyClick: () -> Unit,
    onReportClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RadiusMd))
            .background(SurfacePrimary)
            .border(1.dp, BorderDefault, RoundedCornerShape(RadiusMd))
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ActionItem(
            iconRes = R.drawable.ic_star_black_24dp,
            label = stringResource(R.string.action_favorite),
            tint = if (isFavorite) AccentOrange else TextGhost,
            onClick = onFavoriteClick,
            modifier = Modifier.weight(1f),
        )
        VerticalDivider()
        ActionItem(
            iconRes = R.drawable.ic_volume_up_black_24dp,
            label = stringResource(R.string.action_audio),
            tint = if (isAudioPlaying) AccentOrange else TextGhost,
            onClick = onAudioClick,
            modifier = Modifier.weight(1f),
        )
        VerticalDivider()
        ActionItem(
            iconRes = R.drawable.ic_copy,
            label = stringResource(R.string.action_copy),
            tint = TextGhost,
            onClick = onCopyClick,
            modifier = Modifier.weight(1f),
        )
        VerticalDivider()
        ActionItem(
            iconRes = R.drawable.ic_report_black_24dp,
            label = stringResource(R.string.action_report),
            tint = TextGhost,
            onClick = onReportClick,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ActionItem(
    iconRes: Int,
    label: String,
    tint: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.padding(vertical = 8.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = label,
                style = TypeMicro,
                color = TextDim,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
    }
}

@Composable
private fun VerticalDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .fillMaxHeight()
            .background(BorderDefault),
    )
}

@Preview(name = "WordActionBar — default", showBackground = true, backgroundColor = 0xFF0A0E17)
@Composable
private fun WordActionBarDefaultPreview() {
    YomikataTheme {
        WordActionBar(
            isFavorite = false,
            isAudioPlaying = false,
            onFavoriteClick = {},
            onAudioClick = {},
            onCopyClick = {},
            onReportClick = {},
            modifier = Modifier.padding(14.dp),
        )
    }
}

@Preview(name = "WordActionBar — favorite + audio playing", showBackground = true, backgroundColor = 0xFF0A0E17)
@Composable
private fun WordActionBarActivePreview() {
    YomikataTheme {
        WordActionBar(
            isFavorite = true,
            isAudioPlaying = true,
            onFavoriteClick = {},
            onAudioClick = {},
            onCopyClick = {},
            onReportClick = {},
            modifier = Modifier.padding(14.dp),
        )
    }
}
