package com.jehutyno.yomikata.ui.quiz

import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.jehutyno.hiraganaedittext.HiraganaEditText
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.model.Sentence
import com.jehutyno.yomikata.model.Word
import com.jehutyno.yomikata.ui.components.FABBar
import com.jehutyno.yomikata.ui.components.FABBarState
import com.jehutyno.yomikata.ui.theme.AccentOrange
import com.jehutyno.yomikata.ui.theme.BackgroundCorrect
import com.jehutyno.yomikata.ui.theme.BackgroundHero
import com.jehutyno.yomikata.ui.theme.BackgroundHeroWrong
import com.jehutyno.yomikata.ui.theme.BackgroundPrimary
import com.jehutyno.yomikata.ui.theme.BackgroundWrong
import com.jehutyno.yomikata.ui.theme.BorderCorrect
import com.jehutyno.yomikata.ui.theme.BorderDefault
import com.jehutyno.yomikata.ui.theme.BorderHeroCorrect
import com.jehutyno.yomikata.ui.theme.RadiusMd
import com.jehutyno.yomikata.ui.theme.RadiusXl
import com.jehutyno.yomikata.ui.theme.BorderHeroWrong
import com.jehutyno.yomikata.ui.theme.BorderSubtle
import com.jehutyno.yomikata.ui.theme.BorderWrong
import com.jehutyno.yomikata.ui.theme.Correct
import com.jehutyno.yomikata.ui.theme.SurfacePrimary
import com.jehutyno.yomikata.ui.theme.TextDim
import com.jehutyno.yomikata.ui.theme.TextGhost
import com.jehutyno.yomikata.ui.theme.TextMuted
import com.jehutyno.yomikata.ui.theme.TextPrimary
import com.jehutyno.yomikata.ui.theme.TextSecondary
import com.jehutyno.yomikata.ui.theme.Wrong
import com.jehutyno.yomikata.ui.theme.YomikataTheme
import com.jehutyno.yomikata.util.getWordPositionInFuriSentence
import kotlin.math.roundToInt
import com.jehutyno.yomikata.util.language.cleanForQCM
import com.jehutyno.yomikata.util.quiz.QuizType
import com.jehutyno.yomikata.util.quiz.Level
import com.jehutyno.yomikata.util.sentenceNoAnswerFuri
import com.jehutyno.yomikata.util.sentenceNoFuri
import com.jehutyno.yomikata.view.furigana.FuriganaView

// ─── Data ─────────────────────────────────────────────────────────────────────

data class QuizUiState(
    val title: String = "",
    val words: List<Pair<Word, QuizType>> = emptyList(),
    val currentIndex: Int = 0,
    val sentence: Sentence = Sentence(),
    val answerMode: AnswerMode = AnswerMode.None,
    val qcmOptions: List<QcmOption> = List(4) { QcmOption("") },
    val qcmShowFuri: Boolean = false,
    val hintText: String? = null,
    val tapToRevealVisible: Boolean = false,
    val editText: String = "",
    val editTextColorInt: Int = Color.White.toArgb(),
    val editShowDisplayAnswer: Boolean = false,
    val editIsEnableConversion: Boolean = false,
    val isRevealed: Boolean = false,
    val segments: List<SegmentState> = emptyList(),
    val showFurigana: Boolean = true,
    val showTranslation: Boolean = true,
    // Orange for unanswered, green after correct, stays orange after wrong (AccentOrange if 0)
    val wordHighlightColor: Int = 0,
    val infiniteCount: Int? = null,
)

data class QcmOption(
    val label: String,
    val furiStart: Int = 0,
    val furiEnd: Int = 0,
    val buttonState: AnswerButtonState = AnswerButtonState.Default,
    val isFuri: Boolean = false,
)

enum class AnswerMode { None, QCM, Edit }

val QuizUiState.currentWord: Word? get() = words.getOrNull(currentIndex)?.first
val QuizUiState.currentQuizType: QuizType? get() = words.getOrNull(currentIndex)?.second
val QuizUiState.counterText: String
    get() = if (infiniteCount != null) "$infiniteCount" else "${currentIndex + 1} / ${words.size}"

// ─── Screen ───────────────────────────────────────────────────────────────────

@Composable
fun QuizScreen(
    uiState: QuizUiState,
    onClose: () -> Unit,
    onTtsSettings: () -> Unit,
    onDisplayAnswers: () -> Unit,
    onOptionClick: (Int) -> Unit,
    onNextWord: () -> Unit,
    onFuriToggle: (Boolean) -> Unit,
    onTradToggle: () -> Unit,
    onItemClick: () -> Unit,
    onSelectionClick: () -> Unit,
    onReportClick: () -> Unit,
    onSentenceTts: () -> Unit,
    onSoundClick: () -> Unit,
    onEditTextChange: (String) -> Unit,
    onEditBeforeTextChange: () -> Unit,
    onEditSubmit: (String) -> Unit,
    onEditAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val word = uiState.currentWord
    val quizType = uiState.currentQuizType

    val answered = uiState.isRevealed
    val isCorrect = answered && uiState.wordHighlightColor == Correct.toArgb()
    val isWrong = answered && !isCorrect

    val heroBg by animateColorAsState(
        targetValue = when {
            isCorrect -> BackgroundCorrect
            isWrong -> BackgroundHeroWrong
            else -> BackgroundHero
        },
        label = "heroBg",
    )
    val heroBorder by animateColorAsState(
        targetValue = when {
            isCorrect -> BorderHeroCorrect
            isWrong -> BorderHeroWrong
            else -> Color.Transparent
        },
        label = "heroBorder",
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundPrimary)
            // Consomme l'inset du bas = max(barre de navigation, clavier).
            // À l'ouverture du clavier, la Column se réduit pour rester au-dessus de
            // l'IME (au lieu de faire défiler tout l'écran) → la card s'adapte à la
            // hauteur restante et le champ d'édition reste visible.
            .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime)),
    ) {
        // TopAppBar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    painter = painterResource(R.drawable.ic_clear_orange_24dp),
                    contentDescription = null,
                    tint = AccentOrange,
                    modifier = Modifier.size(22.dp),
                )
            }
            Text(
                text = uiState.title,
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.W600,
                modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
            )
            IconButton(onClick = onTtsSettings) {
                Icon(
                    painter = painterResource(R.drawable.ic_tts_settings),
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(22.dp),
                )
            }
            IconButton(onClick = onDisplayAnswers) {
                Icon(
                    painter = painterResource(R.drawable.ic_tooltip_edit),
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(22.dp),
                )
            }
        }

        // Counter + progress bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (uiState.segments.isNotEmpty()) {
                // La barre affiche déjà le compteur "x / total" sous elle —
                // pas de compteur dupliqué à droite.
                ProgressSegmentBar(
                    total = uiState.segments.size,
                    correctCount = uiState.segments.count { it == SegmentState.Correct },
                    wrongCount = uiState.segments.count { it == SegmentState.Wrong },
                    current = uiState.currentIndex,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                // Mode infini : pas de segments, on affiche juste le compteur.
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = uiState.counterText,
                    color = TextDim,
                    fontSize = 13.sp,
                )
            }
        }

        // Question zone — card avec bords arrondis et marges
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(RadiusXl))
                .border(1.5.dp, heroBorder, RoundedCornerShape(RadiusXl))
                .background(heroBg),
        ) {
            QuestionZone(
                word = word,
                quizType = quizType,
                sentence = uiState.sentence,
                showFurigana = uiState.showFurigana,
                showTranslation = uiState.showTranslation,
                wordHighlightColor = uiState.wordHighlightColor,
                isRevealed = uiState.isRevealed,
                isCorrect = isCorrect,
                isWrong = isWrong,
                onItemClick = onItemClick,
                onSelectionClick = onSelectionClick,
                onReportClick = onReportClick,
                onSentenceTts = onSentenceTts,
                onSoundClick = onSoundClick,
                onFuriToggle = onFuriToggle,
                onTradToggle = onTradToggle,
                modifier = Modifier.fillMaxSize(),
            )
        }

        // Espace souple au-dessus de la zone de réponse. En QCM, un espace identique
        // est ajouté en dessous (voir plus bas) pour centrer verticalement le bloc de
        // réponses entre la card et le bouton, sans réduire la taille de la card.
        val isQcm = uiState.answerMode == AnswerMode.QCM
        Spacer(modifier = Modifier.weight(if (isQcm) 0.2f else 0.4f))

        // Instruction label for QCM
        if (!uiState.hintText.isNullOrEmpty() && uiState.answerMode == AnswerMode.QCM) {
            Text(
                text = uiState.hintText.uppercase(),
                color = AccentOrange.copy(alpha = 0.75f),
                fontSize = 11.sp,
                fontWeight = FontWeight.W600,
                letterSpacing = 1.2.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }

        // Answer zone
        when (uiState.answerMode) {
            AnswerMode.QCM -> QcmAnswers(
                options = uiState.qcmOptions,
                isRevealed = uiState.isRevealed,
                onOptionClick = onOptionClick,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            )
            AnswerMode.Edit -> EditAnswerMode(
                text = uiState.editText,
                textColorInt = uiState.editTextColorInt,
                isEnableConversion = uiState.editIsEnableConversion,
                showDisplayAnswer = uiState.editShowDisplayAnswer,
                isRevealed = uiState.isRevealed,
                onTextChange = onEditTextChange,
                onBeforeTextChange = onEditBeforeTextChange,
                onSubmit = onEditSubmit,
                onAction = onEditAction,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            AnswerMode.None -> Spacer(modifier = Modifier.height(8.dp))
        }

        // En QCM, espace souple sous les réponses (symétrique de celui du dessus)
        // pour centrer le bloc de réponses entre la card et le bouton.
        if (isQcm) {
            Spacer(modifier = Modifier.weight(0.2f))
        }

        // FABBar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 12.dp),
        ) {
            if (uiState.isRevealed) {
                FABBar(state = FABBarState.Next, onClick = onNextWord)
            } else {
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}

// ─── Question zone ────────────────────────────────────────────────────────────

@Composable
private fun QuestionZone(
    word: Word?,
    quizType: QuizType?,
    sentence: Sentence,
    showFurigana: Boolean,
    showTranslation: Boolean,
    wordHighlightColor: Int,
    isRevealed: Boolean,
    isCorrect: Boolean,
    isWrong: Boolean,
    onItemClick: () -> Unit,
    onSelectionClick: () -> Unit,
    onReportClick: () -> Unit,
    onSentenceTts: () -> Unit,
    onSoundClick: () -> Unit,
    onFuriToggle: (Boolean) -> Unit,
    onTradToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // P4: orange si non répondu, green après bonne réponse (défault si 0 → orange)
    val sentenceHighlight = if (wordHighlightColor != 0) wordHighlightColor else AccentOrange.toArgb()

    Column(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.Top,
    ) {
        when {
            // TYPE_AUDIO : juste l'icône speaker centrée
            quizType == QuizType.TYPE_AUDIO -> {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    IconButton(
                        onClick = onSoundClick,
                        modifier = Modifier.size(96.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_volume_up_black_48dp),
                            contentDescription = null,
                            tint = AccentOrange,
                            modifier = Modifier.size(72.dp),
                        )
                    }
                }
            }

            // TYPE_EN_JAP : traduction anglaise comme question (grande, centrée)
            quizType == QuizType.TYPE_EN_JAP && word != null -> {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clickable(onClick = onItemClick),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = word.getTrad().cleanForQCM(false),
                        color = AccentOrange,
                        fontSize = 22.sp,
                        lineHeight = 30.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            // Cas japonais : kanji en vedette au-dessus de la phrase, bloc centré verticalement
            word != null -> {
                val kanjiColor by animateColorAsState(
                    targetValue = when {
                        isCorrect -> Correct
                        isWrong -> Wrong
                        else -> TextPrimary
                    },
                    label = "kanjiColor",
                )
                val scale by animateFloatAsState(
                    targetValue = if (isCorrect) 1.13f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium,
                    ),
                    label = "kanjiScale",
                )
                val shakeOffset = remember { Animatable(0f) }
                LaunchedEffect(isWrong) {
                    if (isWrong) {
                        shakeOffset.animateTo(
                            targetValue = 0f,
                            animationSpec = keyframes {
                                durationMillis = 450
                                0f at 0
                                14f at 60
                                -14f at 120
                                10f at 180
                                -10f at 240
                                6f at 300
                                -6f at 360
                                0f at 450
                            },
                        )
                    } else {
                        shakeOffset.snapTo(0f)
                    }
                }

                val colorEntireWord = word.isKana == 2 && quizType == QuizType.TYPE_JAP_EN
                val wordPos = if (colorEntireWord) 0 else getWordPositionInFuriSentence(sentence.jap, word)
                val furiText = if (showFurigana) {
                    if (colorEntireWord) sentence.jap else sentenceNoAnswerFuri(sentence, word)
                } else {
                    val noFuri = sentenceNoFuri(sentence)
                    if (colorEntireWord) noFuri else noFuri.replace("%", word.japanese)
                }
                val markEnd = wordPos + if (colorEntireWord) {
                    (if (showFurigana) sentence.jap else sentenceNoFuri(sentence)).length
                } else {
                    word.japanese.length
                }

                // P3 : furigana masqué pour les types "donner la lecture" avant révélation
                val hideReadingFuri = !isRevealed &&
                    (quizType == QuizType.TYPE_PRONUNCIATION || quizType == QuizType.TYPE_PRONUNCIATION_QCM)
                val showWordFuri = showFurigana && word.isKana == 0 && !hideReadingFuri
                val largeWordText = if (showWordFuri) {
                    " {${word.japanese};${word.reading.split("/")[0].trim()}} "
                } else {
                    word.japanese
                }

                // Bloc centré verticalement dans l'espace disponible
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        // Mot en vedette — centré, avec spring bounce + shake
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer { scaleX = scale; scaleY = scale }
                                .offset { IntOffset(shakeOffset.value.roundToInt(), 0) },
                            contentAlignment = Alignment.Center,
                        ) {
                            FuriganaAndroidView(
                                text = largeWordText,
                                markStart = 0,
                                markEnd = 0,
                                highlightColor = kanjiColor.toArgb(),
                                textColor = kanjiColor.toArgb(),
                                textSizeSp = 46f,
                                modifier = Modifier.wrapContentWidth(),
                            )
                        }

                        // Phrase d'exemple + traduction — centrées horizontalement
                        // La traduction utilise alpha() pour réserver l'espace sans décaler les autres éléments
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            FuriganaAndroidView(
                                text = furiText,
                                markStart = wordPos,
                                markEnd = markEnd,
                                highlightColor = sentenceHighlight,
                                textSizeSp = 18f,
                                modifier = Modifier
                                    .wrapContentWidth()
                                    .clickable(onClick = onItemClick),
                            )
                            Text(
                                text = sentence.getTrad(),
                                color = TextSecondary,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.alpha(
                                    if (showTranslation && quizType != QuizType.TYPE_JAP_EN) 1f else 0f
                                ),
                            )
                        }
                    }
                }
            }

            else -> Spacer(modifier = Modifier.weight(1f))
        }

        // GROUPE 3 (ou bas pour audio/EN-JAP) : boutons d'action
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (quizType != QuizType.TYPE_EN_JAP && quizType != QuizType.TYPE_AUDIO) {
                IconButton(onClick = { onFuriToggle(!showFurigana) }) {
                    Text(text = "あ", color = if (showFurigana) AccentOrange else TextGhost, fontSize = 18.sp)
                }
            }
            if (quizType != QuizType.TYPE_JAP_EN && quizType != QuizType.TYPE_AUDIO && quizType != QuizType.TYPE_EN_JAP) {
                IconButton(onClick = onTradToggle) {
                    Icon(
                        painter = painterResource(if (showTranslation) R.drawable.ic_trad_check else R.drawable.ic_trad_uncheck),
                        contentDescription = null,
                        tint = if (showTranslation) AccentOrange else TextGhost,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onSelectionClick) {
                Icon(
                    painter = painterResource(R.drawable.ic_star_black_24dp),
                    contentDescription = null,
                    tint = TextGhost,
                    modifier = Modifier.size(20.dp),
                )
            }
            IconButton(onClick = onReportClick) {
                Icon(
                    painter = painterResource(R.drawable.ic_report_black_24dp),
                    contentDescription = null,
                    tint = TextGhost,
                    modifier = Modifier.size(20.dp),
                )
            }
            if (quizType != QuizType.TYPE_AUDIO) {
                IconButton(onClick = onSentenceTts) {
                    Icon(
                        painter = painterResource(R.drawable.ic_volume_up_black_24dp),
                        contentDescription = null,
                        tint = TextGhost,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

// ─── QCM answers (grille 2×2) ─────────────────────────────────────────────────

@Composable
private fun QcmAnswers(
    options: List<QcmOption>,
    isRevealed: Boolean,
    onOptionClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val letters = listOf('A', 'B', 'C', 'D')
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for (row in 0..1) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                for (col in 0..1) {
                    val i = row * 2 + col
                    if (i < options.size) {
                        val option = options[i]
                        val letter = letters.getOrElse(i) { 'A' }
                        if (option.isFuri) {
                            FuriAnswerButton(
                                option = option,
                                indexLetter = letter,
                                isRevealed = isRevealed,
                                onClick = { onOptionClick(i) },
                                modifier = Modifier.weight(1f),
                            )
                        } else {
                            AnswerButton(
                                label = option.label,
                                indexLetter = letter,
                                state = option.buttonState,
                                isRevealed = isRevealed,
                                isSelected = option.buttonState != AnswerButtonState.Default,
                                onClick = { onOptionClick(i) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

// ─── FuriAnswerButton ─────────────────────────────────────────────────────────

@Composable
private fun FuriAnswerButton(
    option: QcmOption,
    indexLetter: Char,
    isRevealed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimmed = isRevealed && option.buttonState == AnswerButtonState.Default
    val bgColor = when (option.buttonState) {
        AnswerButtonState.Default -> SurfacePrimary
        AnswerButtonState.Correct -> BackgroundCorrect
        AnswerButtonState.Wrong -> BackgroundWrong
    }
    val borderColor = when (option.buttonState) {
        AnswerButtonState.Default -> BorderDefault
        AnswerButtonState.Correct -> BorderCorrect
        AnswerButtonState.Wrong -> BorderWrong
    }
    val textHighlight = when (option.buttonState) {
        AnswerButtonState.Default -> TextPrimary.toArgb()
        AnswerButtonState.Correct -> Correct.toArgb()
        AnswerButtonState.Wrong -> Wrong.toArgb()
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .alpha(if (dimmed) 0.4f else 1f)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .background(bgColor, RoundedCornerShape(16.dp))
            .clickable(enabled = !isRevealed, onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(Color(0xFF1E2B3A), RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = indexLetter.toString(), fontSize = 10.sp, color = TextDim)
        }
        FuriganaAndroidView(
            text = option.label,
            markStart = option.furiStart,
            markEnd = option.furiEnd,
            highlightColor = textHighlight,
            textSizeSp = 16f,
            modifier = Modifier.weight(1f),
        )
    }
}

// ─── Edit mode ────────────────────────────────────────────────────────────────

@Composable
private fun EditAnswerMode(
    text: String,
    textColorInt: Int,
    isEnableConversion: Boolean,
    showDisplayAnswer: Boolean,
    isRevealed: Boolean,
    onTextChange: (String) -> Unit,
    onBeforeTextChange: () -> Unit,
    onSubmit: (String) -> Unit,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Champ d'édition mis en avant : liseré accent + surface, hauteur confortable.
        // Le soulignement orange natif de l'EditText est retiré (background = null).
        Box(
            modifier = Modifier
                .weight(1f)
                .height(56.dp)
                .clip(RoundedCornerShape(RadiusMd))
                .background(SurfacePrimary)
                .border(1.5.dp, AccentOrange, RoundedCornerShape(RadiusMd))
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            AndroidView(
                factory = { ctx ->
                    HiraganaEditText(ctx).apply {
                        hint = ctx.getString(R.string.quiz_input_hint)
                        inputType = InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS or
                                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                        setTextColor(textColorInt)
                        setHintTextColor(ContextCompat.getColor(ctx, R.color.gray))
                        textSize = 18f
                        background = null
                        setPadding(0, 0, 0, 0)
                        addTextChangedListener(object : TextWatcher {
                            override fun afterTextChanged(s: Editable?) {}
                            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                                onBeforeTextChange()
                            }
                            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                                onTextChange(s?.toString() ?: "")
                            }
                        })
                        setOnEditorActionListener { _, actionId, event ->
                            if (actionId == EditorInfo.IME_ACTION_DONE ||
                                event?.keyCode == KeyEvent.KEYCODE_ENTER
                            ) {
                                onSubmit(this.text.toString())
                                true
                            } else false
                        }
                    }
                },
                update = { view ->
                    view.isEnableConversion = isEnableConversion
                    view.setTextColor(textColorInt)
                    if (view.text.toString() != text) {
                        view.setText(text)
                        if (text.isNotEmpty()) view.setSelection(text.length)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        IconButton(onClick = onAction) {
            Icon(
                painter = painterResource(
                    if (showDisplayAnswer) R.drawable.ic_visibility_black_24dp
                    else R.drawable.ic_cancel_black_24dp,
                ),
                contentDescription = null,
                tint = if (showDisplayAnswer) Correct else Color(0xFFBBBBBB),
            )
        }
    }
}

// ─── FuriganaAndroidView ──────────────────────────────────────────────────────

@Composable
internal fun FuriganaAndroidView(
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

// ─── Previews ──────────────────────────────────────────────────────────────────

private val previewWord = Word(
    id = 1L, japanese = "食べる", english = "to eat", french = "manger",
    reading = "たべる", level = Level.LOW, countTry = 5, countSuccess = 3,
    countFail = 2, isKana = 0, repetition = 0, points = 0,
    baseCategory = 0, isSelected = 0, sentenceId = null,
)
private val previewSentence = Sentence(
    jap = "毎日%食べる%のが好きです", en = "I like eating every day", fr = "J'aime manger tous les jours",
)

@Preview(showBackground = true, backgroundColor = 0xFF0A0E17, name = "QuizScreen — avant réponse")
@Composable
private fun PreviewQuizBeforeAnswer() {
    YomikataTheme {
        QuizScreen(
            uiState = QuizUiState(
                segments = listOf(
                    SegmentState.Correct,
                    SegmentState.Wrong,
                    SegmentState.Current,
                    SegmentState.Pending,
                    SegmentState.Pending,
                ),
                answerMode = AnswerMode.QCM,
                qcmOptions = listOf(
                    QcmOption("たべる", buttonState = AnswerButtonState.Default),
                    QcmOption("のむ", buttonState = AnswerButtonState.Default),
                    QcmOption("かく", buttonState = AnswerButtonState.Default),
                    QcmOption("みる", buttonState = AnswerButtonState.Default),
                ),
                hintText = "Give hiragana reading",
                isRevealed = false,
                currentIndex = 0,
                words = listOf(Pair(previewWord, QuizType.TYPE_JAP_EN)),
                sentence = previewSentence,
                wordHighlightColor = AccentOrange.toArgb(),
            ),
            onClose = {},
            onTtsSettings = {},
            onDisplayAnswers = {},
            onOptionClick = {},
            onNextWord = {},
            onFuriToggle = {},
            onTradToggle = {},
            onItemClick = {},
            onSelectionClick = {},
            onReportClick = {},
            onSentenceTts = {},
            onSoundClick = {},
            onEditTextChange = {},
            onEditBeforeTextChange = {},
            onEditSubmit = {},
            onEditAction = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0E17, name = "QuizScreen — bonne réponse")
@Composable
private fun PreviewQuizCorrect() {
    YomikataTheme {
        QuizScreen(
            uiState = QuizUiState(
                segments = listOf(
                    SegmentState.Correct,
                    SegmentState.Wrong,
                    SegmentState.Correct,
                    SegmentState.Pending,
                    SegmentState.Pending,
                ),
                answerMode = AnswerMode.QCM,
                qcmOptions = listOf(
                    QcmOption("たべる", buttonState = AnswerButtonState.Correct),
                    QcmOption("のむ", buttonState = AnswerButtonState.Default),
                    QcmOption("かく", buttonState = AnswerButtonState.Default),
                    QcmOption("みる", buttonState = AnswerButtonState.Default),
                ),
                hintText = "Give hiragana reading",
                isRevealed = true,
                currentIndex = 0,
                words = listOf(Pair(previewWord, QuizType.TYPE_JAP_EN)),
                sentence = previewSentence,
                wordHighlightColor = Correct.toArgb(),
            ),
            onClose = {},
            onTtsSettings = {},
            onDisplayAnswers = {},
            onOptionClick = {},
            onNextWord = {},
            onFuriToggle = {},
            onTradToggle = {},
            onItemClick = {},
            onSelectionClick = {},
            onReportClick = {},
            onSentenceTts = {},
            onSoundClick = {},
            onEditTextChange = {},
            onEditBeforeTextChange = {},
            onEditSubmit = {},
            onEditAction = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0E17, name = "QuizScreen — mauvaise réponse")
@Composable
private fun PreviewQuizWrong() {
    YomikataTheme {
        QuizScreen(
            uiState = QuizUiState(
                segments = listOf(
                    SegmentState.Correct,
                    SegmentState.Wrong,
                    SegmentState.Wrong,
                    SegmentState.Pending,
                    SegmentState.Pending,
                ),
                answerMode = AnswerMode.QCM,
                qcmOptions = listOf(
                    QcmOption("たべる", buttonState = AnswerButtonState.Correct),
                    QcmOption("のむ", buttonState = AnswerButtonState.Default),
                    QcmOption("からだ", buttonState = AnswerButtonState.Wrong),
                    QcmOption("みる", buttonState = AnswerButtonState.Default),
                ),
                hintText = "Give hiragana reading",
                isRevealed = true,
                currentIndex = 0,
                words = listOf(Pair(previewWord, QuizType.TYPE_JAP_EN)),
                sentence = previewSentence,
                wordHighlightColor = AccentOrange.toArgb(),
            ),
            onClose = {},
            onTtsSettings = {},
            onDisplayAnswers = {},
            onOptionClick = {},
            onNextWord = {},
            onFuriToggle = {},
            onTradToggle = {},
            onItemClick = {},
            onSelectionClick = {},
            onReportClick = {},
            onSentenceTts = {},
            onSoundClick = {},
            onEditTextChange = {},
            onEditBeforeTextChange = {},
            onEditSubmit = {},
            onEditAction = {},
        )
    }
}
