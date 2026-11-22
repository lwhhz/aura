package com.aura.chat.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 8dp 基线间距令牌（知识库 01-Foundations/spacing.md）。
 * space100 = 8dp；space50 = 4dp 仅用于紧凑微调。
 */
@Immutable
data class AuraSpacing(
    val space0: Dp = 0.dp,
    val space25: Dp = 2.dp,
    val space50: Dp = 4.dp,
    val space75: Dp = 6.dp,
    val space100: Dp = 8.dp,
    val space125: Dp = 10.dp,
    val space150: Dp = 12.dp,
    val space175: Dp = 14.dp,
    val space200: Dp = 16.dp,
    val space250: Dp = 20.dp,
    val space300: Dp = 24.dp,
    val space400: Dp = 32.dp,
    val space500: Dp = 40.dp,
    val space600: Dp = 48.dp,
    val space800: Dp = 64.dp,
    val space1200: Dp = 96.dp,
) {
    // 旧命名兼容（渐进替换）
    val xxs: Dp get() = space25
    val xs: Dp get() = space50
    val sm: Dp get() = space100
    val smd: Dp get() = space150
    val md: Dp get() = space200
    val lg: Dp get() = space300
    val xl: Dp get() = space400
    val xxl: Dp get() = 44.dp
}

val LocalAuraSpacing = staticCompositionLocalOf { AuraSpacing() }

/** 页面边距：compact 16dp，其余断点 24dp（KB 01-Foundations/layout.md）。 */
object AuraPageMargin {
    val compact = 16.dp
    val regular = 24.dp
}
