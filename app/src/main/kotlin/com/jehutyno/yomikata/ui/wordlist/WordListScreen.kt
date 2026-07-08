package com.jehutyno.yomikata.ui.wordlist

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.model.Word
import com.jehutyno.yomikata.ui.components.MasteryBar
import com.jehutyno.yomikata.ui.theme.AccentOrange
import com.jehutyno.yomikata.ui.theme.BackgroundPrimary
import com.jehutyno.yomikata.ui.theme.BorderDefault
import com.jehutyno.yomikata.ui.theme.MasteryHigh4
import com.jehutyno.yomikata.ui.theme.MasteryLow1
import com.jehutyno.yomikata.ui.theme.MasteryMaster4
import com.jehutyno.yomikata.ui.theme.MasteryMedium4
import com.jehutyno.yomikata.ui.theme.RadiusPill
import com.jehutyno.yomikata.ui.theme.RadiusSm
import com.jehutyno.yomikata.ui.theme.SurfaceAccent
import com.jehutyno.yomikata.ui.theme.SurfacePrimary
import com.jehutyno.yomikata.ui.theme.TextDim
import com.jehutyno.yomikata.ui.theme.TextGhost
import com.jehutyno.yomikata.ui.theme.TextMuted
import com.jehutyno.yomikata.ui.theme.TextPrimary
import com.jehutyno.yomikata.ui.theme.YomikataTheme
import com.jehutyno.yomikata.ui.word.WordListRow
import com.jehutyno.yomikata.util.quiz.Level

data class WordListUiState(
    val words: List<Word> = emptyList(),
    val title: String = "",
    val quizCount: Int = 0,
    val masterCount: Int = 0,
    val highCount: Int = 0,
    val mediumCount: Int = 0,
    val lowCount: Int = 0,
    val selectedTab: Int = 0,
    val searchQuery: String = "",
    val isGrid: Boolean = false,
    /** Ids of words present in at least one user selection (drives the orange star). */
    val selectedWordIds: Set<Long> = emptySet(),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordListScreen(
    state: WordListUiState,
    onBack: () -> Unit,
    onTabSelected: (Int) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onToggleGrid: () -> Unit,
    onWordClick: (Word) -> Unit,
    onFavoriteClick: (Word) -> Unit,
    onAudioClick: (Word) -> Unit,
    modifier: Modifier = Modifier,
) {
    val filteredWords = remember(state.words, state.selectedTab, state.searchQuery) {
        val level = tabToLevel(state.selectedTab)
        val byTab: List<Word> =
            if (level == null) state.words
            else state.words.filter { it.level == level }
        if (state.searchQuery.isBlank()) byTab
        else byTab.filter { w ->
            w.japanese.contains(state.searchQuery, ignoreCase = true) ||
            w.getTrad().contains(state.searchQuery, ignoreCase = true) ||
            w.reading.contains(state.searchQuery, ignoreCase = true)
        }
    }

    // Title: strip the "%EN" part of "JP%EN" quiz names
    val displayTitle = state.title.substringBefore("%")

    val masteredCount = state.highCount + state.masterCount
    val subtitle = "${state.quizCount} ${stringResource(R.string.word_count_label)}"

    Scaffold(
        modifier = modifier,
        containerColor = BackgroundPrimary,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = displayTitle,
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.W600,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (state.quizCount > 0) {
                            Text(
                                text = subtitle,
                                color = TextMuted,
                                fontSize = 11.sp,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                            tint = TextPrimary,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onToggleGrid) {
                        Icon(
                            painter = painterResource(
                                if (state.isGrid) R.drawable.ic_list else R.drawable.ic_grid
                            ),
                            contentDescription = if (state.isGrid) stringResource(R.string.view_mode_list) else stringResource(R.string.view_mode_grid),
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundPrimary),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // Mastery bar (compact — counts live in the filter pills below)
            MasteryBar(
                total = state.quizCount,
                mastered = masteredCount,
                showLegend = false,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            )

            // Filtres : « Tous » (texte) + 4 niveaux (rond de couleur + compteur), pour retrouver
            // le tri par couleur de réussite de l'ancienne version.
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp),
            ) {
                MasteryFilterChip(
                    label = stringResource(R.string.mastery_all),
                    count = state.quizCount,
                    isSelected = state.selectedTab == 0,
                    onClick = { onTabSelected(0) },
                )
                LevelFilterChip(
                    dotColor = MasteryLow1,
                    count = state.lowCount,
                    isSelected = state.selectedTab == 1,
                    contentDescription = stringResource(R.string.red_review),
                    onClick = { onTabSelected(1) },
                )
                LevelFilterChip(
                    dotColor = MasteryMedium4,
                    count = state.mediumCount,
                    isSelected = state.selectedTab == 2,
                    contentDescription = stringResource(R.string.orange_review),
                    onClick = { onTabSelected(2) },
                )
                LevelFilterChip(
                    dotColor = MasteryHigh4,
                    count = state.highCount,
                    isSelected = state.selectedTab == 3,
                    contentDescription = stringResource(R.string.yellow_review),
                    onClick = { onTabSelected(3) },
                )
                LevelFilterChip(
                    dotColor = MasteryMaster4,
                    count = state.masterCount,
                    isSelected = state.selectedTab == 4,
                    contentDescription = stringResource(R.string.green_review),
                    onClick = { onTabSelected(4) },
                )
            }

            // Search field
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = onSearchQueryChanged,
                placeholder = {
                    Text(stringResource(R.string.search_hint), color = TextGhost, fontSize = 13.sp)
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = TextDim,
                        modifier = Modifier.size(18.dp),
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(RadiusSm),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SurfacePrimary,
                    unfocusedContainerColor = SurfacePrimary,
                    focusedBorderColor = BorderDefault,
                    unfocusedBorderColor = BorderDefault,
                    cursorColor = AccentOrange,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            )

            // Word list or grid
            if (state.isGrid) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    itemsIndexed(filteredWords) { _, word ->
                        WordListRow(
                            japanese = word.japanese,
                            furigana = word.reading,
                            translation = word.getTrad(),
                            score = word.points,
                            posTokens = word.getPosTokens(),
                            isFavorite = word.id in state.selectedWordIds,
                            onFavoriteClick = { onFavoriteClick(word) },
                            onAudioClick = { onAudioClick(word) },
                            onClick = { onWordClick(word) },
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 80.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    itemsIndexed(filteredWords) { _, word ->
                        WordListRow(
                            japanese = word.japanese,
                            furigana = word.reading,
                            translation = word.getTrad(),
                            score = word.points,
                            posTokens = word.getPosTokens(),
                            isFavorite = word.id in state.selectedWordIds,
                            onFavoriteClick = { onFavoriteClick(word) },
                            onAudioClick = { onAudioClick(word) },
                            onClick = { onWordClick(word) },
                        )
                    }
                }
            }
        }
    }
}

/**
 * Map filter tab index → mastery [Level] (null = « Tous »).
 * 0 = tous, 1 = LOW, 2 = MEDIUM, 3 = HIGH, 4 = MASTER.
 */
fun tabToLevel(tab: Int): Level? = when (tab) {
    1 -> Level.LOW
    2 -> Level.MEDIUM
    3 -> Level.HIGH
    4 -> Level.MASTER
    else -> null
}

/** Pilule de filtre par niveau : rond de couleur du niveau + compteur (sans texte). */
@Composable
private fun LevelFilterChip(
    dotColor: androidx.compose.ui.graphics.Color,
    count: Int,
    isSelected: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = if (isSelected) SurfaceAccent else SurfacePrimary
    val border = if (isSelected) AccentOrange else BorderDefault
    val textColor = if (isSelected) AccentOrange else TextMuted

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .clip(RoundedCornerShape(RadiusPill))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(RadiusPill))
            .clickable(onClick = onClick)
            .semantics { this.contentDescription = contentDescription }
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        Box(
            modifier = Modifier
                .size(11.dp)
                .clip(RoundedCornerShape(RadiusPill))
                .background(dotColor),
        )
        Text(
            text = "$count",
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = textColor,
        )
    }
}

/** Pill filter chip with an embedded count — replaces the Material tabs. */
@Composable
private fun MasteryFilterChip(
    label: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = if (isSelected) SurfaceAccent else SurfacePrimary
    val border = if (isSelected) AccentOrange else BorderDefault
    val textColor = if (isSelected) AccentOrange else TextMuted

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(RadiusPill))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(RadiusPill))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        Text(
            text = "$label · $count",
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = textColor,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0E17)
@Composable
private fun WordListScreenPreview() {
    YomikataTheme {
        WordListScreen(
            state = WordListUiState(
                title = "あ行%a-row",
                quizCount = 46,
                highCount = 15,
                masterCount = 5,
                mediumCount = 16,
                lowCount = 10,
                selectedTab = 0,
            ),
            onBack = {},
            onTabSelected = {},
            onSearchQueryChanged = {},
            onToggleGrid = {},
            onWordClick = {},
            onFavoriteClick = {},
            onAudioClick = {},
        )
    }
}
