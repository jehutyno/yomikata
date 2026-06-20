package com.jehutyno.yomikata.ui.answers

import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.model.Answer
import com.jehutyno.yomikata.model.Quiz
import com.jehutyno.yomikata.model.Sentence
import com.jehutyno.yomikata.model.Word
import com.jehutyno.yomikata.model.getCategoryIcon
import com.jehutyno.yomikata.ui.components.SectionHeader
import com.jehutyno.yomikata.ui.theme.AccentOrange
import com.jehutyno.yomikata.ui.theme.BackgroundCorrect
import com.jehutyno.yomikata.ui.theme.BackgroundPrimary
import com.jehutyno.yomikata.ui.theme.BackgroundWrong
import com.jehutyno.yomikata.ui.theme.BorderCorrect
import com.jehutyno.yomikata.ui.theme.BorderDefault
import com.jehutyno.yomikata.ui.theme.BorderWrong
import com.jehutyno.yomikata.ui.theme.Correct
import com.jehutyno.yomikata.ui.theme.RadiusLg
import com.jehutyno.yomikata.ui.theme.RadiusXs
import com.jehutyno.yomikata.ui.theme.SurfacePrimary
import com.jehutyno.yomikata.ui.theme.TextDim
import com.jehutyno.yomikata.ui.theme.TextMuted
import com.jehutyno.yomikata.ui.theme.TextPrimary
import com.jehutyno.yomikata.ui.theme.TextSecondary
import com.jehutyno.yomikata.ui.theme.TypeWordTranslation
import com.jehutyno.yomikata.ui.theme.Wrong
import com.jehutyno.yomikata.ui.theme.YomikataTheme
import com.jehutyno.yomikata.util.getWordPositionInFuriSentence
import com.jehutyno.yomikata.util.quiz.Level
import com.jehutyno.yomikata.util.quiz.QuizType
import com.jehutyno.yomikata.view.furigana.FuriganaView

// ─── State ──────────────────────────────────────────────────────────────────

/**
 * One selectable quiz/selection row in the selection bottom sheet, with whether
 * the current word already belongs to it.
 */
data class SelectionEntry(val quiz: Quiz, val isChecked: Boolean)

/** State of the selection picker bottom sheet (null when closed). */
data class SelectionSheetState(
    val wordIndex: Int,
    val entries: List<SelectionEntry> = emptyList(),
)

data class AnswerReviewUiState(
    val items: List<Triple<Answer, Word, Sentence>> = emptyList(),
    val selectionSheet: SelectionSheetState? = null,
)

// ─── Screen ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnswerReviewScreen(
    state: AnswerReviewUiState,
    title: String,
    onBack: () -> Unit,
    onSelectionClick: (index: Int) -> Unit,
    onReportClick: (index: Int) -> Unit,
    onWordTtsClick: (index: Int) -> Unit,
    onSentenceTtsClick: (index: Int) -> Unit,
    onSelectionToggle: (quizId: Long, checked: Boolean) -> Unit,
    onCreateSelection: () -> Unit,
    onDismissSelectionSheet: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back_orange_24dp),
                            contentDescription = "Back",
                            tint = AccentOrange,
                        )
                    }
                },
                title = {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.W600,
                        color = TextPrimary,
                        maxLines = 1,
                    )
                },
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
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp),
        ) {
            // ── Summary header ───────────────────────────────────────────
            item {
                ResultSummary(
                    correct = state.items.count { it.first.result == 1 },
                    wrong = state.items.count { it.first.result != 1 },
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                )
            }

            // ── Answer cards ─────────────────────────────────────────────
            itemsIndexed(state.items) { index, item ->
                AnswerReviewCard(
                    answer = item.first,
                    word = item.second,
                    sentence = item.third,
                    onSelectionClick = { onSelectionClick(index) },
                    onReportClick = { onReportClick(index) },
                    onWordTtsClick = { onWordTtsClick(index) },
                    onSentenceTtsClick = { onSentenceTtsClick(index) },
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                )
            }
        }
    }

    // ── Selection picker ─────────────────────────────────────────────────
    val sheet = state.selectionSheet
    if (sheet != null) {
        ModalBottomSheet(
            onDismissRequest = onDismissSelectionSheet,
            sheetState = rememberModalBottomSheetState(),
            containerColor = SurfacePrimary,
        ) {
            SelectionSheetContent(
                entries = sheet.entries,
                onToggle = onSelectionToggle,
                onCreateSelection = onCreateSelection,
            )
        }
    }
}

// ─── Result summary ───────────────────────────────────────────────────────

@Composable
private fun ResultSummary(
    correct: Int,
    wrong: Int,
    modifier: Modifier = Modifier,
) {
    val total = (correct + wrong).coerceAtLeast(1)
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(text = "結果 · Results")
        Spacer(modifier = Modifier.height(8.dp))

        // Proportional green/red bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(RadiusXs)),
        ) {
            if (correct > 0) {
                Box(
                    modifier = Modifier
                        .weight(correct.toFloat())
                        .fillMaxSize()
                        .background(Correct),
                )
            }
            if (wrong > 0) {
                Box(
                    modifier = Modifier
                        .weight(wrong.toFloat())
                        .fillMaxSize()
                        .background(Wrong),
                )
            }
            if (correct == 0 && wrong == 0) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .background(BorderDefault),
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = "$correct ✓", fontSize = 12.sp, color = Correct, fontWeight = FontWeight.W600)
            Text(text = "$wrong ✗", fontSize = 12.sp, color = Wrong, fontWeight = FontWeight.W600)
            Spacer(modifier = Modifier.weight(1f))
            Text(text = "${correct + wrong}", fontSize = 12.sp, color = TextDim)
        }
    }
}

// ─── Answer card ──────────────────────────────────────────────────────────

@Composable
private fun AnswerReviewCard(
    answer: Answer,
    word: Word,
    sentence: Sentence,
    onSelectionClick: () -> Unit,
    onReportClick: () -> Unit,
    onWordTtsClick: () -> Unit,
    onSentenceTtsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val correct = answer.result == 1
    val resultColor = if (correct) Correct else Wrong
    val bgColor = if (correct) BackgroundCorrect else BackgroundWrong
    val borderColor = if (correct) BorderCorrect else BorderWrong

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RadiusLg))
            .background(bgColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(RadiusLg))
            .padding(14.dp),
    ) {
        // ── Header: category icon + kanji + result badge ──────────────
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(50))
                    .background(SurfacePrimary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(getCategoryIcon(word.baseCategory)),
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(modifier = Modifier.size(10.dp))
            AnswerFuriganaView(
                text = word.japanese,
                markStart = 0,
                markEnd = word.japanese.length,
                highlightColor = resultColor.toArgb(),
                textColor = resultColor.toArgb(),
                textSizeSp = 22f,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = if (correct) "✓" else "✗",
                color = resultColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.W700,
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // ── Word translation ─────────────────────────────────────────
        Text(
            text = word.getTrad(),
            style = TypeWordTranslation,
            color = TextSecondary,
        )

        Spacer(modifier = Modifier.height(8.dp))

        // ── Given answer (HTML, possibly multi-try) ───────────────────
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.ic_tooltip_edit),
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.size(8.dp))
            HtmlText(
                html = answer.answer,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // ── Example sentence (target word highlighted by result) ──────
        val wordPos = getWordPositionInFuriSentence(sentence.jap, word)
        Row(verticalAlignment = Alignment.CenterVertically) {
            AnswerFuriganaView(
                text = sentence.jap,
                markStart = wordPos,
                markEnd = wordPos + word.japanese.length,
                highlightColor = resultColor.toArgb(),
                textColor = TextSecondary.toArgb(),
                textSizeSp = 17f,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onSentenceTtsClick, modifier = Modifier.size(32.dp)) {
                Icon(
                    painter = painterResource(R.drawable.ic_volume_up_black_24dp),
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // ── Sentence translation ──────────────────────────────────────
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.ic_translate),
                contentDescription = null,
                tint = TextDim,
                modifier = Modifier.size(14.dp),
            )
            Spacer(modifier = Modifier.size(6.dp))
            Text(
                text = sentence.getTrad(),
                fontSize = 13.sp,
                color = TextMuted,
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // ── Footer actions ────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FooterIcon(R.drawable.ic_star_black_24dp, onSelectionClick)
            FooterIcon(R.drawable.ic_report_black_24dp, onReportClick)
            FooterIcon(R.drawable.ic_volume_up_black_24dp, onWordTtsClick)
        }
    }
}

@Composable
private fun FooterIcon(iconRes: Int, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(36.dp)) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = TextMuted,
            modifier = Modifier.size(18.dp),
        )
    }
}

// ─── Selection bottom sheet ─────────────────────────────────────────────────

@Composable
private fun SelectionSheetContent(
    entries: List<SelectionEntry>,
    onToggle: (quizId: Long, checked: Boolean) -> Unit,
    onCreateSelection: () -> Unit,
) {
    Column(modifier = Modifier.padding(bottom = 24.dp)) {
        Text(
            text = stringResource(R.string.add_to_selections),
            fontSize = 16.sp,
            fontWeight = FontWeight.W600,
            color = TextPrimary,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )
        entries.forEach { entry ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle(entry.quiz.id, !entry.isChecked) }
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = entry.quiz.getName(),
                    fontSize = 14.sp,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f),
                )
                if (entry.isChecked) {
                    Icon(
                        painter = painterResource(R.drawable.ic_star_black_24dp),
                        contentDescription = null,
                        tint = AccentOrange,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCreateSelection() }
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_add),
                contentDescription = null,
                tint = AccentOrange,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.size(10.dp))
            Text(
                text = stringResource(R.string.new_selection),
                fontSize = 14.sp,
                color = AccentOrange,
            )
        }
    }
}

// ─── Furigana + HTML interop ────────────────────────────────────────────────

@Composable
private fun AnswerFuriganaView(
    text: String,
    markStart: Int,
    markEnd: Int,
    highlightColor: Int,
    textColor: Int = TextPrimary.toArgb(),
    textSizeSp: Float = 18f,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        factory = { ctx ->
            FuriganaView(ctx).apply {
                setTextColor(textColor)
                textSize = textSizeSp
            }
        },
        update = { view ->
            view.setTextColor(textColor)
            view.textSize = textSizeSp
            view.text_set(text, markStart, markEnd, highlightColor)
        },
        modifier = modifier,
    )
}

@Composable
private fun HtmlText(
    html: String,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        factory = { ctx ->
            TextView(ctx).apply {
                textSize = 14f
                setTextColor(TextPrimary.toArgb())
            }
        },
        update = { view ->
            view.text = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_LEGACY)
        },
        modifier = modifier,
    )
}

// ─── Previews ─────────────────────────────────────────────────────────────

@Preview(name = "AnswerReviewScreen — dark", showBackground = true, backgroundColor = 0xFF0A0E17)
@Composable
private fun AnswerReviewScreenPreview() {
    val wordOk = Word(
        id = 1, japanese = "下", english = "below", french = "en dessous",
        reading = "した", level = Level.LOW, countTry = 3, countSuccess = 2,
        countFail = 1, isKana = 0, repetition = 0, points = 8,
        baseCategory = 0, isSelected = 0, sentenceId = null, pos = "n",
    )
    val wordKo = Word(
        id = 2, japanese = "男", english = "man", french = "homme",
        reading = "おとこ", level = Level.LOW, countTry = 3, countSuccess = 1,
        countFail = 2, isKana = 0, repetition = 0, points = 2,
        baseCategory = 0, isSelected = 0, sentenceId = null, pos = "n",
    )
    val sentence = Sentence(
        id = 1, jap = "{下;した}を{見;み}ろ。",
        en = "See below.", fr = "Voir ci-dessous.",
    )
    val state = AnswerReviewUiState(
        items = listOf(
            Triple(
                Answer(1, "<font color='#4ADE80'>した</font>", 1, 1, QuizType.TYPE_PRONUNCIATION),
                wordOk, sentence,
            ),
            Triple(
                Answer(0, "<font color='#F87171'>ふん</font>", 2, 1, QuizType.TYPE_PRONUNCIATION),
                wordKo, sentence,
            ),
        ),
    )
    YomikataTheme {
        AnswerReviewScreen(
            state = state,
            title = "Review of answers",
            onBack = {}, onSelectionClick = {}, onReportClick = {},
            onWordTtsClick = {}, onSentenceTtsClick = {},
            onSelectionToggle = { _, _ -> }, onCreateSelection = {},
            onDismissSelectionSheet = {},
        )
    }
}
