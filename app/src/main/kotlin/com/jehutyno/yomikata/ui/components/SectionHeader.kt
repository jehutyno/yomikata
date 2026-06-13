package com.jehutyno.yomikata.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jehutyno.yomikata.ui.theme.BorderSubtle
import com.jehutyno.yomikata.ui.theme.TextDim
import com.jehutyno.yomikata.ui.theme.TypeLabel
import com.jehutyno.yomikata.ui.theme.YomikataTheme

/**
 * Bilingual section header — format "JP · EN".
 * 10sp weight 600 UPPERCASE, letter-spacing 0.14em, TextDim color.
 * Followed by a subtle horizontal divider line.
 */
@Composable
fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = text.uppercase(),
            style = TypeLabel,
            color = TextDim,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(BorderSubtle),
        )
    }
}

@Preview(name = "SectionHeader — dark", showBackground = true, backgroundColor = 0xFF0A0E17)
@Composable
private fun SectionHeaderPreview() {
    YomikataTheme {
        SectionHeader(
            text = "今日 · Today",
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}

@Preview(name = "SectionHeader Composition — dark", showBackground = true, backgroundColor = 0xFF0A0E17)
@Composable
private fun SectionHeaderCompositionPreview() {
    YomikataTheme {
        SectionHeader(
            text = "構成 · Composition",
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}
