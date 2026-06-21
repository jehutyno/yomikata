package com.jehutyno.yomikata.ui.study

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.ui.components.SectionHeader
import com.jehutyno.yomikata.ui.theme.AccentOrange
import com.jehutyno.yomikata.ui.theme.BorderDefault
import com.jehutyno.yomikata.ui.theme.RadiusMd
import com.jehutyno.yomikata.ui.theme.RadiusPill
import com.jehutyno.yomikata.ui.theme.SurfaceAccent
import com.jehutyno.yomikata.ui.theme.SurfacePrimary
import com.jehutyno.yomikata.ui.theme.TextMuted
import com.jehutyno.yomikata.ui.theme.TextPrimary
import com.jehutyno.yomikata.util.quiz.QuizStrategy
import com.jehutyno.yomikata.util.quiz.QuizType

/** Quiz-type chip definition: type + label + check/uncheck icons. */
private data class QuizTypeOption(
    val type: QuizType,
    @StringRes val labelRes: Int,
    @DrawableRes val iconChecked: Int,
    @DrawableRes val iconUnchecked: Int,
)

private val QuizTypeOptions = listOf(
    QuizTypeOption(QuizType.TYPE_AUTO, R.string.quiz_type_auto, R.drawable.ic_auto_check, R.drawable.ic_auto_uncheck),
    QuizTypeOption(QuizType.TYPE_PRONUNCIATION, R.string.quiz_type_pronunciation, R.drawable.ic_pronunciation_check, R.drawable.ic_pronunciation_uncheck),
    QuizTypeOption(QuizType.TYPE_PRONUNCIATION_QCM, R.string.quiz_type_pronunciation_qcm, R.drawable.ic_pronunciation_qcm_check, R.drawable.ic_pronunciation_qcm_uncheck),
    QuizTypeOption(QuizType.TYPE_AUDIO, R.string.quiz_type_audio, R.drawable.ic_sound_qcm_check, R.drawable.ic_sound_qcm_uncheck),
    QuizTypeOption(QuizType.TYPE_EN_JAP, R.string.quiz_type_en_jap, R.drawable.ic_en_jap_check, R.drawable.ic_en_jap_uncheck),
    QuizTypeOption(QuizType.TYPE_JAP_EN, R.string.quiz_type_jap_en, R.drawable.ic_jap_en_check, R.drawable.ic_jap_en_uncheck),
)

/**
 * Launch options bottom sheet shared by the Study and Selections screens.
 *
 * Lets the user pick quiz types (AUTO full-width on top, manual types in a 2-column grid) and a
 * launch mode (Progressive / Straight / Shuffle). Tapping a mode launches the quiz directly.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LaunchOptionsSheet(
    selectedTypes: List<QuizType>,
    lastMode: QuizStrategy?,
    onQuizTypeToggle: (QuizType) -> Unit,
    onModeSelected: (QuizStrategy) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = SurfacePrimary,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
        ) {
            SectionHeader(text = stringResource(R.string.quiz_types_header))
            Spacer(modifier = Modifier.height(10.dp))

            // AUTO stands apart, full width on top: selecting it clears every manual type.
            val autoOption = QuizTypeOptions.first { it.type == QuizType.TYPE_AUTO }
            QuizTypeChip(
                labelRes = autoOption.labelRes,
                iconChecked = autoOption.iconChecked,
                iconUnchecked = autoOption.iconUnchecked,
                isSelected = selectedTypes.contains(autoOption.type),
                onClick = { onQuizTypeToggle(autoOption.type) },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Manual types in a uniform 2-column grid (equal-width chips).
            val manualOptions = QuizTypeOptions.filter { it.type != QuizType.TYPE_AUTO }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                manualOptions.chunked(2).forEach { rowOptions ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        rowOptions.forEach { option ->
                            QuizTypeChip(
                                labelRes = option.labelRes,
                                iconChecked = option.iconChecked,
                                iconUnchecked = option.iconUnchecked,
                                isSelected = selectedTypes.contains(option.type),
                                onClick = { onQuizTypeToggle(option.type) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (rowOptions.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            SectionHeader(text = stringResource(R.string.launch_mode_header))
            Spacer(modifier = Modifier.height(4.dp))
            LaunchModeRow(
                iconRes = R.drawable.ic_progress,
                labelRes = R.string.practice_progressive,
                isHighlighted = lastMode == QuizStrategy.PROGRESSIVE,
                onClick = { onModeSelected(QuizStrategy.PROGRESSIVE) },
            )
            LaunchModeRow(
                iconRes = R.drawable.ic_straight,
                labelRes = R.string.practice_normal,
                isHighlighted = lastMode == QuizStrategy.STRAIGHT,
                onClick = { onModeSelected(QuizStrategy.STRAIGHT) },
            )
            LaunchModeRow(
                iconRes = R.drawable.ic_shuffle_white_24dp,
                labelRes = R.string.practice_random,
                isHighlighted = lastMode == QuizStrategy.SHUFFLE,
                onClick = { onModeSelected(QuizStrategy.SHUFFLE) },
            )
        }
    }
}

@Composable
private fun QuizTypeChip(
    @StringRes labelRes: Int,
    @DrawableRes iconChecked: Int,
    @DrawableRes iconUnchecked: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = if (isSelected) SurfaceAccent else SurfacePrimary
    val border = if (isSelected) AccentOrange else BorderDefault
    val textColor = if (isSelected) AccentOrange else TextMuted

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(RadiusPill))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(RadiusPill))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
    ) {
        Image(
            painter = painterResource(if (isSelected) iconChecked else iconUnchecked),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = stringResource(labelRes),
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = textColor,
            maxLines = 2,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun LaunchModeRow(
    @DrawableRes iconRes: Int,
    @StringRes labelRes: Int,
    isHighlighted: Boolean,
    onClick: () -> Unit,
) {
    val border = if (isHighlighted) AccentOrange else Color.Transparent
    val bg = if (isHighlighted) SurfaceAccent else Color.Transparent

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(RadiusMd))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(RadiusMd))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = AccentOrange,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = stringResource(labelRes),
            fontSize = 15.sp,
            color = TextPrimary,
        )
    }
}
