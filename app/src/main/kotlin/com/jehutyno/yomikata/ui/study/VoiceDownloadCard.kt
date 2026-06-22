package com.jehutyno.yomikata.ui.study

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.ui.components.SectionHeader
import com.jehutyno.yomikata.ui.theme.AccentOrange
import com.jehutyno.yomikata.ui.theme.BorderAccent
import com.jehutyno.yomikata.ui.theme.BorderDefault
import com.jehutyno.yomikata.ui.theme.Correct
import com.jehutyno.yomikata.ui.theme.RadiusMd
import com.jehutyno.yomikata.ui.theme.RadiusPill
import com.jehutyno.yomikata.ui.theme.RadiusXl
import com.jehutyno.yomikata.ui.theme.SurfacePrimary
import com.jehutyno.yomikata.ui.theme.TextMuted
import com.jehutyno.yomikata.ui.theme.TextPrimary
import com.jehutyno.yomikata.ui.theme.TypeListTitle
import com.jehutyno.yomikata.ui.theme.YomikataTheme

/**
 * Card affichée en haut de Study, sous la progression. Pour la catégorie sélectionnée,
 * indique si le pack de voix japonaises est téléchargé et permet de le télécharger.
 * La progression du téléchargement s'affiche directement dans la card (pas de dialog).
 *
 * @param downloaded le pack du niveau courant est déjà présent sur l'appareil
 * @param sizeMb taille du pack en Mo (affichée sur le bouton de téléchargement)
 * @param progress fraction 0f..1f si un téléchargement est en cours, null sinon
 */
@Composable
fun VoiceDownloadCard(
    downloaded: Boolean,
    sizeMb: Int,
    progress: Float?,
    onDownloadClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .background(SurfacePrimary, RoundedCornerShape(RadiusMd))
            .border(1.dp, BorderDefault, RoundedCornerShape(RadiusMd))
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        SectionHeader(text = stringResource(R.string.voices_card_title))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_volume_up_black_24dp),
                contentDescription = null,
                tint = if (downloaded) Correct else TextMuted,
                modifier = Modifier.size(22.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))

            when {
                progress != null -> {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.voices_downloading),
                            fontSize = 13.sp,
                            color = TextPrimary,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { progress.coerceIn(0f, 1f) },
                            color = AccentOrange,
                            trackColor = BorderDefault,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(RadiusPill)),
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "${(progress * 100).toInt()} %",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AccentOrange,
                    )
                }

                downloaded -> {
                    Text(
                        text = stringResource(R.string.voices_downloaded),
                        fontSize = 13.sp,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = Correct,
                        modifier = Modifier.size(20.dp),
                    )
                }

                else -> {
                    Text(
                        text = stringResource(R.string.voices_card_subtitle),
                        fontSize = 13.sp,
                        color = TextMuted,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    OutlinedButton(
                        onClick = onDownloadClick,
                        shape = RoundedCornerShape(RadiusXl),
                        border = BorderStroke(1.dp, BorderAccent),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentOrange),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.download_voices_action, sizeMb),
                            style = TypeListTitle,
                            color = AccentOrange,
                        )
                    }
                }
            }
        }
    }
}

// MARK: — Previews

@Preview(name = "VoiceCard — à télécharger", showBackground = true, backgroundColor = 0xFF0A0E17)
@Composable
private fun VoiceDownloadCardPreview() {
    YomikataTheme {
        VoiceDownloadCard(
            downloaded = false,
            sizeMb = 5,
            progress = null,
            onDownloadClick = {},
            modifier = Modifier.padding(14.dp),
        )
    }
}

@Preview(name = "VoiceCard — téléchargé", showBackground = true, backgroundColor = 0xFF0A0E17)
@Composable
private fun VoiceDownloadCardDownloadedPreview() {
    YomikataTheme {
        VoiceDownloadCard(
            downloaded = true,
            sizeMb = 5,
            progress = null,
            onDownloadClick = {},
            modifier = Modifier.padding(14.dp),
        )
    }
}

@Preview(name = "VoiceCard — en cours", showBackground = true, backgroundColor = 0xFF0A0E17)
@Composable
private fun VoiceDownloadCardProgressPreview() {
    YomikataTheme {
        VoiceDownloadCard(
            downloaded = false,
            sizeMb = 5,
            progress = 0.42f,
            onDownloadClick = {},
            modifier = Modifier.padding(14.dp),
        )
    }
}
