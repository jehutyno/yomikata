package com.jehutyno.yomikata.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.ui.theme.AccentOrange
import com.jehutyno.yomikata.ui.theme.BackgroundPrimary
import com.jehutyno.yomikata.ui.theme.BorderDefault
import com.jehutyno.yomikata.ui.theme.SurfacePrimary
import com.jehutyno.yomikata.ui.theme.TextDim
import com.jehutyno.yomikata.ui.theme.TextMuted
import com.jehutyno.yomikata.ui.theme.YomikataTheme

@Composable
fun SettingsScreen(
    versionName: String,
    onDiscord: () -> Unit,
    onFacebook: () -> Unit,
    onPlayStore: () -> Unit,
    onShare: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(BackgroundPrimary)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Social links section header
        Text(
            text = "コミュニティ · " + stringResource(R.string.community).uppercase(),
            color = TextDim,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(
            Modifier
                .fillMaxWidth()
                .background(SurfacePrimary, shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SocialButton(iconRes = R.drawable.ic_discord, label = "Discord", onClick = onDiscord)
            SocialButton(iconRes = R.drawable.ic_facebook, label = "Facebook", onClick = onFacebook)
            SocialButton(iconRes = R.drawable.ic_google_play, label = "Play Store", onClick = onPlayStore)
            SocialButton(iconRes = R.drawable.ic_share, label = stringResource(R.string.action_share), onClick = onShare)
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = BorderDefault, thickness = 1.dp)
        Spacer(Modifier.height(12.dp))

        // Version
        Text(
            text = "v$versionName",
            color = TextMuted,
            fontSize = 12.sp,
            modifier = Modifier.align(Alignment.End)
        )

        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun SocialButton(iconRes: Int, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = label,
            tint = AccentOrange,
            modifier = Modifier.size(28.dp)
        )
        Spacer(Modifier.height(4.dp))
        Text(text = label, color = TextMuted, fontSize = 10.sp)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0E17)
@Composable
private fun SettingsScreenPreviewDark() {
    YomikataTheme {
        SettingsScreen(
            versionName = "3.0.0",
            onDiscord = {},
            onFacebook = {},
            onPlayStore = {},
            onShare = {}
        )
    }
}
