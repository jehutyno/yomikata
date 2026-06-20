package com.jehutyno.yomikata.screens.selections

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.ui.theme.AccentOrange
import com.jehutyno.yomikata.ui.theme.BackgroundHero
import com.jehutyno.yomikata.ui.theme.BackgroundPrimary
import com.jehutyno.yomikata.ui.theme.TextMuted
import com.jehutyno.yomikata.ui.theme.TypeHeroTitle
import com.jehutyno.yomikata.ui.theme.YomikataTheme

class SelectionsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            YomikataTheme {
                SelectionsScreen()
            }
        }
    }
}

@Composable
private fun SelectionsScreen() {
    Column(modifier = Modifier.fillMaxSize().background(BackgroundPrimary)) {
        SelectionsHero()
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "Bientôt disponible",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = TextMuted,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}

@Composable
private fun SelectionsHero() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .background(BackgroundHero),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xA6000A14), Color(0xB3000A14)),
                    )
                ),
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize(),
        ) {
            Image(
                painter = painterResource(R.drawable.ic_selections_big),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(80.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "選択",
                style = TypeHeroTitle,
                color = AccentOrange,
            )
        }
    }
}
