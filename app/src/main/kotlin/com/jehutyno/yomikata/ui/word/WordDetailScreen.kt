package com.jehutyno.yomikata.ui.word

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.model.KanjiSolo
import com.jehutyno.yomikata.model.KanjiSoloRadical
import com.jehutyno.yomikata.model.Sentence
import com.jehutyno.yomikata.model.Word
import com.jehutyno.yomikata.model.getWordColor
import com.jehutyno.yomikata.ui.components.MasteryDots
import com.jehutyno.yomikata.ui.components.SectionHeader
import com.jehutyno.yomikata.ui.theme.AccentOrange
import com.jehutyno.yomikata.ui.theme.BackgroundHero
import com.jehutyno.yomikata.ui.theme.BackgroundPrimary
import com.jehutyno.yomikata.ui.theme.BorderDefault
import com.jehutyno.yomikata.ui.theme.RadiusMd
import com.jehutyno.yomikata.ui.theme.SurfacePrimary
import com.jehutyno.yomikata.ui.theme.TextDim
import com.jehutyno.yomikata.ui.theme.TextMuted
import com.jehutyno.yomikata.ui.theme.TextPrimary
import com.jehutyno.yomikata.ui.theme.TextSecondary
import com.jehutyno.yomikata.ui.theme.YomikataTheme
import com.jehutyno.yomikata.util.getWordPositionInFuriSentence
import com.jehutyno.yomikata.util.quiz.Level
import com.jehutyno.yomikata.view.furigana.FuriganaView

// ─── State ────────────────────────────────────────────────────────────────────

data class WordDetailUiState(
    val words: List<Triple<Word, List<KanjiSoloRadical?>, Sentence>> = emptyList(),
    val currentIndex: Int = 0,
    val isAudioPlaying: Boolean = false,
    val updateCounter: Int = 0,
    /** True when the current word belongs to at least one user selection (orange star). */
    val isFavorite: Boolean = false,
)

// ─── Screen ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WordDetailScreen(
    state: WordDetailUiState,
    title: String,
    onBack: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onWordTtsClick: () -> Unit,
    onSentenceTtsClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onCopyClick: () -> Unit,
    onReportClick: () -> Unit,
    onLevelUp: () -> Unit,
    onLevelDown: () -> Unit,
) {
    val entry = state.words.getOrNull(state.currentIndex) ?: return
    val word = entry.first
    val kanjiComponents = entry.second.filterNotNull()
    val sentence = entry.third
    val total = state.words.size
    val context = LocalContext.current
    val wordColor = getWordColor(context, word.points)

    YomikataTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                painter = painterResource(R.drawable.ic_arrow_back_orange_24dp),
                                contentDescription = "Retour",
                                tint = AccentOrange,
                            )
                        }
                    },
                    title = {
                        Text(
                            text = title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.W500,
                            color = TextSecondary,
                            maxLines = 1,
                        )
                    },
                    actions = {
                        if (total > 1) {
                            IconButton(
                                onClick = onPrev,
                                enabled = state.currentIndex > 0,
                            ) {
                                Text(
                                    text = "❮",
                                    color = if (state.currentIndex > 0) AccentOrange else BorderDefault,
                                    fontSize = 16.sp,
                                )
                            }
                            Text(
                                text = "${state.currentIndex + 1} / $total",
                                color = TextMuted,
                                fontSize = 13.sp,
                                modifier = Modifier.align(Alignment.CenterVertically),
                            )
                            IconButton(
                                onClick = onNext,
                                enabled = state.currentIndex < total - 1,
                            ) {
                                Text(
                                    text = "❯",
                                    color = if (state.currentIndex < total - 1) AccentOrange else BorderDefault,
                                    fontSize = 16.sp,
                                )
                            }
                        }
                    },
                    // TopBar plus foncée que la zone hero
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = BackgroundPrimary,
                    ),
                )
            },
            containerColor = BackgroundPrimary,
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                // ── Hero zone ─────────────────────────────────────────────
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(BackgroundHero)
                            .padding(horizontal = 16.dp, vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        MasteryDots(score = word.points)

                        Spacer(modifier = Modifier.height(16.dp))

                        // Kanji — couleur mastery, furigana orange, centré
                        val wordText = if (word.isKana == 2) word.japanese
                        else "{${word.japanese};${word.reading}}"
                        WordDetailFuriganaView(
                            text = wordText,
                            markStart = 0,
                            markEnd = 0,
                            highlightColor = wordColor,
                            textColor = wordColor,
                            furiganaColor = AccentOrange.toArgb(),
                            textSizeSp = 52f,
                            centered = true,
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // POS chips
                        val tokens = word.getPosTokens()
                        if (tokens.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(bottom = 8.dp),
                            ) {
                                tokens.take(3).forEach { token ->
                                    PosChip(token = token)
                                }
                            }
                        }

                        // Translation
                        Text(
                            text = word.getTrad(),
                            fontSize = 16.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(bottom = 16.dp),
                        )

                        // Level up / down buttons
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(bottom = 12.dp),
                        ) {
                            TextButton(onClick = onLevelDown) {
                                Text("▼ Reculer", color = TextMuted, fontSize = 12.sp)
                            }
                            TextButton(onClick = onLevelUp) {
                                Text("▲ Avancer", color = AccentOrange, fontSize = 12.sp)
                            }
                        }

                        // Action bar
                        WordActionBar(
                            isFavorite = state.isFavorite,
                            isAudioPlaying = state.isAudioPlaying,
                            onFavoriteClick = onFavoriteClick,
                            onAudioClick = onWordTtsClick,
                            onCopyClick = onCopyClick,
                            onReportClick = onReportClick,
                        )
                    }
                }

                // ── Composition section ───────────────────────────────────
                if (kanjiComponents.isNotEmpty()) {
                    item {
                        SectionHeader(
                            text = "構成 · Composition",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        )
                    }
                    items(kanjiComponents.size) { i ->
                        KanjiComponentCard(
                            kanji = kanjiComponents[i],
                            meaning = kanjiComponents[i].getTrad(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                    }
                    item { Spacer(modifier = Modifier.height(4.dp)) }
                }

                // ── Example section ───────────────────────────────────────
                item {
                    SectionHeader(
                        text = "例文 · Exemple",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }
                item {
                    val wordPos = getWordPositionInFuriSentence(sentence.jap, word)
                    ExampleCard(
                        sentenceJap = sentence.jap,
                        sentenceTrad = sentence.getTrad(),
                        markStart = wordPos,
                        markEnd = wordPos + word.japanese.length,
                        onAudioClick = onSentenceTtsClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

// ─── Example card ─────────────────────────────────────────────────────────────

@Composable
private fun ExampleCard(
    sentenceJap: String,
    sentenceTrad: String,
    markStart: Int,
    markEnd: Int,
    onAudioClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(RadiusMd))
            .background(SurfacePrimary)
            .border(1.dp, BorderDefault, RoundedCornerShape(RadiusMd))
            .padding(12.dp),
    ) {
        // Japanese sentence + audio button on same row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            WordDetailFuriganaView(
                text = sentenceJap,
                markStart = markStart,
                markEnd = markEnd,
                highlightColor = AccentOrange.toArgb(),
                textColor = TextSecondary.toArgb(),
                textSizeSp = 16f,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = onAudioClick,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_volume_up_black_24dp),
                    contentDescription = "Lire la phrase",
                    tint = TextMuted,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        // Separator
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(BorderDefault)
                .padding(vertical = 8.dp),
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Translation with icon
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_translate),
                contentDescription = null,
                tint = TextDim,
                modifier = Modifier
                    .size(14.dp)
                    .padding(end = 0.dp),
            )
            Spacer(modifier = Modifier.size(4.dp))
            Text(
                text = sentenceTrad,
                fontSize = 13.sp,
                color = TextMuted,
            )
        }
    }
}

// ─── POS chip ─────────────────────────────────────────────────────────────────

@Composable
private fun PosChip(token: String) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(posChipColor(token))
            .padding(horizontal = 7.dp, vertical = 3.dp),
    ) {
        Text(
            text = posChipLabel(token),
            fontSize = 10.sp,
            fontWeight = FontWeight.W600,
            color = Color.White,
        )
    }
}

// ─── FuriganaAndroidView (local to this screen) ───────────────────────────────

@Composable
private fun WordDetailFuriganaView(
    text: String,
    markStart: Int,
    markEnd: Int,
    highlightColor: Int,
    textColor: Int = TextPrimary.toArgb(),
    furiganaColor: Int = -1,
    textSizeSp: Float = 18f,
    centered: Boolean = false,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        factory = { ctx ->
            FuriganaView(ctx).apply {
                setTextColor(textColor)
                textSize = textSizeSp
                setFuriganaColor(furiganaColor)
                setCenter(centered)
            }
        },
        update = { view ->
            view.setTextColor(textColor)
            view.textSize = textSizeSp
            view.setFuriganaColor(furiganaColor)
            view.setCenter(centered)
            view.text_set(text, markStart, markEnd, highlightColor)
        },
        modifier = modifier,
    )
}

// ─── Previews ─────────────────────────────────────────────────────────────────

@Preview(name = "WordDetailScreen — dark", showBackground = true, backgroundColor = 0xFF0A0E17)
@Composable
private fun WordDetailScreenPreview() {
    val word = Word(
        id = 1, japanese = "食べる", english = "to eat", french = "manger",
        reading = "たべる", level = Level.LOW, countTry = 10, countSuccess = 6,
        countFail = 4, isKana = 0, repetition = 0, points = 5,
        baseCategory = 0, isSelected = 0, sentenceId = null,
        pos = "v1,vt",
    )
    val kanji = KanjiSolo(
        kanji = "食", strokes = 9, en = "eat, food", fr = "manger",
        kunyomi = "た.べる", onyomi = "ショク",
        radical = "食",
    )
    val sentence = Sentence(
        id = 1, jap = "{毎日;まいにち}、{食;た}べます。",
        en = "I eat every day.", fr = "Je mange tous les jours.",
    )
    val state = WordDetailUiState(
        words = listOf(Triple(word, listOf(kanji as KanjiSoloRadical?), sentence)),
        currentIndex = 0,
    )
    WordDetailScreen(
        state = state,
        title = "食べ物 · N5",
        onBack = {}, onPrev = {}, onNext = {},
        onWordTtsClick = {}, onSentenceTtsClick = {},
        onFavoriteClick = {}, onCopyClick = {}, onReportClick = {},
        onLevelUp = {}, onLevelDown = {},
    )
}
