package com.jehutyno.yomikata.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Named text styles matching DESIGN.md section 2
val TypeDisplay = TextStyle(
    fontSize = 52.sp,
    fontWeight = FontWeight.W300,
)
val TypeQuizWord = TextStyle(
    fontSize = 46.sp,
    fontWeight = FontWeight.W300,
)
val TypeHeroTitle = TextStyle(
    fontSize = 22.sp,
    fontWeight = FontWeight.W600,
)
val TypeScreenTitle = TextStyle(
    fontSize = 16.sp,
    fontWeight = FontWeight.W600,
)
val TypeSentence = TextStyle(
    fontSize = 17.sp,
    fontWeight = FontWeight.W400,
)
val TypeWordTranslation = TextStyle(
    fontSize = 18.sp,
    fontWeight = FontWeight.W400,
)
val TypeListTitle = TextStyle(
    fontSize = 14.sp,
    fontWeight = FontWeight.W500,
)
val TypeBody = TextStyle(
    fontSize = 13.sp,
    fontWeight = FontWeight.W400,
)
val TypeAnswer = TextStyle(
    fontSize = 13.sp,
    fontWeight = FontWeight.W400,
)
val TypeCaption = TextStyle(
    fontSize = 11.sp,
    fontWeight = FontWeight.W400,
)
val TypeLabel = TextStyle(
    fontSize = 10.sp,
    fontWeight = FontWeight.W600,
    letterSpacing = 0.14.sp,
)
val TypeMicro = TextStyle(
    fontSize = 9.sp,
    fontWeight = FontWeight.W400,
)
val TypeStatusBar = TextStyle(
    fontSize = 10.sp,
    fontWeight = FontWeight.W500,
)

val YomikataTypography = Typography(
    displayLarge = TypeDisplay,
    displayMedium = TypeQuizWord,
    headlineMedium = TypeHeroTitle,
    titleMedium = TypeScreenTitle,
    titleSmall = TypeListTitle,
    bodyLarge = TypeSentence,
    bodyMedium = TypeBody,
    bodySmall = TypeCaption,
    labelSmall = TypeLabel,
)
