package com.jehutyno.yomikata.ui.wordlist

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.model.Word
import com.jehutyno.yomikata.ui.theme.AccentOrange
import com.jehutyno.yomikata.ui.theme.BackgroundPrimary
import com.jehutyno.yomikata.ui.theme.BorderDefault
import com.jehutyno.yomikata.ui.theme.RadiusSm
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
    val tabs = listOf("Tous", "À revoir", "Maîtrisés")

    val filteredWords = remember(state.words, state.selectedTab, state.searchQuery) {
        val byTab: List<Word> = when (state.selectedTab) {
            1 -> state.words.filter { it.level == Level.LOW || it.level == Level.MEDIUM }
            2 -> state.words.filter { it.level == Level.HIGH || it.level == Level.MASTER }
            else -> state.words
        }
        if (state.searchQuery.isBlank()) byTab
        else byTab.filter { w ->
            w.japanese.contains(state.searchQuery, ignoreCase = true) ||
            w.getTrad().contains(state.searchQuery, ignoreCase = true) ||
            w.reading.contains(state.searchQuery, ignoreCase = true)
        }
    }

    // Title: strip the "%EN" part of "JP%EN" quiz names
    val displayTitle = state.title.substringBefore("%")

    val reviewCount = state.lowCount + state.mediumCount
    val masteredCount = state.highCount + state.masterCount
    val subtitle = when (state.selectedTab) {
        1 -> "${state.quizCount} mots · $reviewCount à revoir"
        2 -> "${state.quizCount} mots · $masteredCount maîtrisés"
        else -> "${state.quizCount} mots"
    }

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
                            contentDescription = "Retour",
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
                            contentDescription = if (state.isGrid) "Mode liste" else "Mode grille",
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
            // Tab row
            TabRow(
                selectedTabIndex = state.selectedTab,
                containerColor = BackgroundPrimary,
                contentColor = AccentOrange,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[state.selectedTab]),
                        color = AccentOrange,
                    )
                },
            ) {
                tabs.forEachIndexed { index, label ->
                    Tab(
                        selected = state.selectedTab == index,
                        onClick = { onTabSelected(index) },
                        text = {
                            Text(
                                text = label,
                                color = if (state.selectedTab == index) AccentOrange else TextMuted,
                                fontSize = 13.sp,
                            )
                        },
                    )
                }
            }

            // Search field
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = onSearchQueryChanged,
                placeholder = {
                    Text("Rechercher…", color = TextGhost, fontSize = 13.sp)
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
                            isFavorite = false,
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
                            isFavorite = false,
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
