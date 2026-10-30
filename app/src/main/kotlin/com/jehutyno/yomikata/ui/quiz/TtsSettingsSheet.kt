package com.jehutyno.yomikata.ui.quiz

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.ui.components.SectionHeader
import com.jehutyno.yomikata.ui.theme.AccentOrange
import com.jehutyno.yomikata.ui.theme.BorderDefault
import com.jehutyno.yomikata.ui.theme.SurfacePrimary
import com.jehutyno.yomikata.ui.theme.TextPrimary
import com.jehutyno.yomikata.ui.theme.YomikataTheme
import kotlin.math.roundToInt

/**
 * Panneau de réglages vocaux du quiz (volume de lecture + vitesse de parole TTS), rétabli après
 * la migration Compose. Composant « pur » : l'état et les effets (AudioManager, Prefs, TextToSpeech)
 * sont gérés par l'hôte via les callbacks.
 *
 * @param volume Volume courant du flux musique (0..[maxVolume]).
 * @param maxVolume Volume maximum du flux musique.
 * @param speechRate Vitesse TTS courante (0..250, = Prefs.TTS_RATE).
 * @param showSpeechRate Masqué quand des voix MP3 sont disponibles (leur débit n'est pas réglable) ;
 *   visible seulement pour le repli TTS.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TtsSettingsSheet(
    volume: Int,
    maxVolume: Int,
    speechRate: Int,
    warnIfVolumeTooLow: Boolean,
    showSpeechRate: Boolean,
    onVolumeChange: (Int) -> Unit,
    onSpeechRateChange: (Int) -> Unit,
    onWarnIfVolumeTooLowChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = SurfacePrimary,
    ) {
        TtsSettingsSheetContent(
            volume = volume,
            maxVolume = maxVolume,
            speechRate = speechRate,
            warnIfVolumeTooLow = warnIfVolumeTooLow,
            showSpeechRate = showSpeechRate,
            onVolumeChange = onVolumeChange,
            onSpeechRateChange = onSpeechRateChange,
            onWarnIfVolumeTooLowChange = onWarnIfVolumeTooLowChange,
        )
    }
}

/**
 * Contenu du panneau de réglages vocaux, sans le [ModalBottomSheet].
 *
 * Séparé du wrapper pour pouvoir être prévisualisé isolément dans la preview Compose
 * (les modal bottom sheet ne se rendent pas dans les previews isolées).
 */
@Composable
private fun TtsSettingsSheetContent(
    volume: Int,
    maxVolume: Int,
    speechRate: Int,
    warnIfVolumeTooLow: Boolean,
    showSpeechRate: Boolean,
    onVolumeChange: (Int) -> Unit,
    onSpeechRateChange: (Int) -> Unit,
    onWarnIfVolumeTooLowChange: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp),
    ) {
        SectionHeader(text = stringResource(R.string.tts_settings_title))
        Spacer(modifier = Modifier.height(12.dp))

        SliderLabel(stringResource(R.string.tts_volume))
        Slider(
            value = volume.toFloat(),
            onValueChange = { onVolumeChange(it.roundToInt()) },
            valueRange = 0f..maxVolume.coerceAtLeast(1).toFloat(),
            steps = (maxVolume - 1).coerceAtLeast(0),
            colors = orangeSliderColors(),
            modifier = Modifier.fillMaxWidth(),
        )

        if (showSpeechRate) {
            Spacer(modifier = Modifier.height(12.dp))
            SliderLabel(stringResource(R.string.tts_rate))
            Slider(
                value = speechRate.toFloat(),
                onValueChange = { onSpeechRateChange(it.roundToInt()) },
                valueRange = 0f..250f,
                colors = orangeSliderColors(),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            SliderLabel(stringResource(R.string.settings_warn_volume_too_low))
            Spacer(modifier = Modifier.weight(1f))
            Checkbox(
                checked = warnIfVolumeTooLow,
                onCheckedChange = onWarnIfVolumeTooLowChange
            )
        }
    }
}

@Composable
private fun SliderLabel(text: String) {
    Text(
        text = text,
        color = TextPrimary,
        fontSize = 14.sp,
        fontWeight = FontWeight.W600,
    )
}

@Composable
private fun orangeSliderColors() = SliderDefaults.colors(
    thumbColor = AccentOrange,
    activeTrackColor = AccentOrange,
    inactiveTrackColor = BorderDefault,
)

@Preview(name = "TTS settings — dark", showBackground = true, backgroundColor = 0xFF0A0E17)
@Composable
private fun TtsSettingsPreview() {
    YomikataTheme {
        // ModalBottomSheet ne se rend pas en preview isolée : on prévisualise le contenu,
        // pas le wrapper ModalBottomSheet.
        TtsSettingsSheetContent(
            volume = 6,
            maxVolume = 10,
            speechRate = 4,
            warnIfVolumeTooLow = false,
            showSpeechRate = true,
            onVolumeChange = {},
            onSpeechRateChange = {},
            onWarnIfVolumeTooLowChange = {},
        )
    }
}
