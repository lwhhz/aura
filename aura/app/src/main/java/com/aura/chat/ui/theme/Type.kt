package com.aura.chat.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import com.aura.chat.R

// 展示字：Playfair Display（SIL OFL 1.1，静态实例 400/600/700），用于 Latin 标题/字标
val DisplaySerif = FontFamily(
    Font(R.font.playfair_400, FontWeight.Normal),
    Font(R.font.playfair_600, FontWeight.SemiBold),
    Font(R.font.playfair_700, FontWeight.Bold),
)

// 中文衬线：Noto Serif CJK SC 子集（GB2312 一级字 + 本工程用字，SIL OFL 1.1）
val SerifCJK = FontFamily(
    Font(R.font.noto_serif_sc, FontWeight.Normal),
    Font(R.font.noto_serif_sc, FontWeight.Medium),
    Font(R.font.noto_serif_sc, FontWeight.SemiBold),
)

/** 含 CJK 字符的标题走中文衬线，纯 Latin 标题走 Playfair —— 两种字形都不掉到系统无衬线。 */
fun editorialFamily(text: String): FontFamily {
    val hasCjk = text.any { ch ->
        ch.code in 0x3400..0x9FFF || ch.code in 0xF900..0xFAFF || ch.code in 0x3000..0x303F
    }
    return if (hasCjk) SerifCJK else DisplaySerif
}

private val tight = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
)

val AuraTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = DisplaySerif,
        fontWeight = FontWeight.Normal,
        fontSize = 68.sp,
        lineHeight = 70.sp,
        letterSpacing = (-1.4).sp,
        lineHeightStyle = tight,
    ),
    displayMedium = TextStyle(
        fontFamily = DisplaySerif,
        fontWeight = FontWeight.Normal,
        fontSize = 50.sp,
        lineHeight = 52.sp,
        letterSpacing = (-0.9).sp,
        lineHeightStyle = tight,
    ),
    displaySmall = TextStyle(
        fontFamily = SerifCJK,
        fontWeight = FontWeight.SemiBold,
        fontSize = 38.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.2).sp,
        lineHeightStyle = tight,
    ),
    headlineLarge = TextStyle(
        fontFamily = SerifCJK,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = SerifCJK,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = SerifCJK,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.05.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.5.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.05.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.5.sp,
        lineHeight = 18.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.2.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.5.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 10.5.sp,
        lineHeight = 14.sp,
        letterSpacing = 1.1.sp,
    ),
)
