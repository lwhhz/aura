package com.aura.chat.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

/**
 * 编辑风强调色（纯色块取向，不用描边）。
 * 组件只从这里取色，不写裸 Hex（知识库 android-ui-rules C1）。
 */
@Immutable
data class AuraAccent(
    val deep: Color,
    val onDeep: Color,
    val featured: Color,
    val onFeatured: Color,
    val featuredGlow: Color,
    val card: Color,
    val chip: Color,
    val onChip: Color,
    val ink: Color,
    val inkCard: Color,
    val inkSoft: Color,
    val lemon: Color,
    val onLemon: Color,
    val userBubble: Color,
    val cyan: Color,
    val text: Color,
    val muted: Color,
    val onInk: Color,
    val onInkMuted: Color,
    val statusRunning: Color,
    val statusRunningContainer: Color,
    val statusIdle: Color,
    val statusOffline: Color,
    val statusError: Color,
    val mutedStrong: Color,
)

private val LightAccent = AuraAccent(
    deep = Brand,
    onDeep = Color.White,
    featured = Brand,
    onFeatured = Color.White,
    featuredGlow = Lemon,
    card = Color.White,
    chip = Lavender200,
    onChip = Violet700,
    ink = Ink950,
    inkCard = Ink900,
    inkSoft = Color(0xFF2B2440),
    lemon = Lemon,
    onLemon = Ink950,
    userBubble = Periwinkle,
    cyan = Color(0xFF3DDCFF),
    text = Ink950,
    muted = MutedStrong,
    onInk = Color(0xFFF6F2FF),
    onInkMuted = MutedDark,
    statusRunning = StatusGreenLight,
    statusRunningContainer = Color(0xFFD7F5E4),
    statusIdle = StatusAmberLight,
    statusOffline = StatusGrayLight,
    statusError = Color(0xFFB3261E),
    mutedStrong = MutedStrong,
)

private val DarkAccent = AuraAccent(
    deep = Color(0xFF8B87FF),
    onDeep = Color(0xFF14121F),
    // 深色主题与 Brand 同源：紫黑卡面 + 柠檬弧线
    featured = Color(0xFF262154),
    onFeatured = Color(0xFFF2EFFF),
    featuredGlow = Lemon,
    card = Color(0xFF1A1726),
    chip = Color(0xFF2A2550),
    onChip = Color(0xFFC9C4FF),
    ink = Color(0xFF0D0B14),
    inkCard = Color(0xFF1E1930),
    inkSoft = Color(0xFF2A2440),
    lemon = Lemon,
    onLemon = Ink950,
    userBubble = Color(0xFF7A74FF),
    cyan = Color(0xFF59E1FF),
    text = Color(0xFFF2EFFF),
    muted = Color(0xFFB4ACD0),
    onInk = Color(0xFFF2EFFF),
    onInkMuted = Color(0xFFB4ACD0),
    statusRunning = StatusGreenDark,
    statusRunningContainer = Color(0xFF123B27),
    statusIdle = StatusAmberDark,
    statusOffline = StatusGrayDark,
    statusError = Color(0xFFFFB4AB),
    mutedStrong = Color(0xFFC9C4E4),
)

val LocalAuraAccent = staticCompositionLocalOf { LightAccent }

private val LightScheme = lightColorScheme(
    primary = Brand,
    onPrimary = Color.White,
    primaryContainer = Lavender200,
    onPrimaryContainer = BrandDeep,
    secondary = Periwinkle,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCD7FF),
    onSecondaryContainer = Violet700,
    tertiary = Lemon,
    onTertiary = Ink950,
    tertiaryContainer = Color(0xFFF1FFB8),
    onTertiaryContainer = Ink950,
    background = LavenderPage,
    onBackground = Ink950,
    surface = LavenderPage,
    onSurface = Ink950,
    surfaceVariant = Lavender200,
    onSurfaceVariant = BrandDeep,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFBF8FF),
    surfaceContainer = Color.White,
    surfaceContainerHigh = Lavender100,
    surfaceContainerHighest = Lavender200,
    outline = Lavender200,
    outlineVariant = Lavender200,
    error = Color(0xFFC5221F),
    onError = Color.White,
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFFA9A6FF),
    onPrimary = Color(0xFF16122B),
    primaryContainer = Color(0xFF2A2550),
    onPrimaryContainer = Color(0xFFDDD9FF),
    secondary = Color(0xFF9A93FF),
    onSecondary = Color(0xFF171340),
    secondaryContainer = Color(0xFF2F2A55),
    onSecondaryContainer = Color(0xFFD9D5FF),
    tertiary = Lemon,
    onTertiary = Ink950,
    tertiaryContainer = Color(0xFF44491A),
    onTertiaryContainer = Lemon,
    background = Color(0xFF0F0D17),
    onBackground = Color(0xFFF2EFFF),
    surface = Color(0xFF0F0D17),
    onSurface = Color(0xFFF2EFFF),
    surfaceVariant = Color(0xFF1A1726),
    onSurfaceVariant = Color(0xFFB4ACD0),
    surfaceContainerLowest = Color(0xFF0B0912),
    surfaceContainerLow = Color(0xFF141120),
    surfaceContainer = Color(0xFF1A1726),
    surfaceContainerHigh = Color(0xFF241F36),
    surfaceContainerHighest = Color(0xFF2E2748),
    outline = Color(0xFF3B3358),
    outlineVariant = Color(0xFF3B3358),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

@Composable
fun AuraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }
    CompositionLocalProvider(
        LocalAuraAccent provides if (darkTheme) DarkAccent else LightAccent,
        LocalAuraSpacing provides AuraSpacing(),
    ) {
        // Expressive 主题入口：颜色槽位不变，默认形状更圆、组件动效走弹簧（KB 01-Foundations/motion.md）
        MaterialExpressiveTheme(
            colorScheme = animateScheme(if (darkTheme) DarkScheme else LightScheme),
            motionScheme = MotionScheme.expressive(),
            typography = AuraTypography,
            shapes = AuraShapes,
            content = content,
        )
    }
}

/**
 * 主题切换的颜色动画：只插值用户直接看到的角色，避免每帧重建全部槽位。
 * 依据：知识库 01-Foundations/motion.md 的 expressive spatial 弹簧（damping 1.0 / stiffness 380）。
 */
@Composable
private fun animateScheme(target: ColorScheme): ColorScheme {
    return target.copy(
        primary = animatedColor(target.primary, "primary"),
        onPrimary = animatedColor(target.onPrimary, "onPrimary"),
        primaryContainer = animatedColor(target.primaryContainer, "primaryContainer"),
        onPrimaryContainer = animatedColor(target.onPrimaryContainer, "onPrimaryContainer"),
        secondary = animatedColor(target.secondary, "secondary"),
        secondaryContainer = animatedColor(target.secondaryContainer, "secondaryContainer"),
        tertiary = animatedColor(target.tertiary, "tertiary"),
        background = animatedColor(target.background, "background"),
        onBackground = animatedColor(target.onBackground, "onBackground"),
        surface = animatedColor(target.surface, "surface"),
        onSurface = animatedColor(target.onSurface, "onSurface"),
        surfaceContainer = animatedColor(target.surfaceContainer, "surfaceContainer"),
        surfaceContainerHigh = animatedColor(target.surfaceContainerHigh, "surfaceContainerHigh"),
        onSurfaceVariant = animatedColor(target.onSurfaceVariant, "onSurfaceVariant"),
    )
}

@Composable
private fun animatedColor(value: Color, label: String): Color =
    animateColorAsState(
        targetValue = value,
        animationSpec = spring(dampingRatio = 1f, stiffness = 380f),
        label = label,
    ).value

/** 供预览用的浅色方案。 */
val AuraLightScheme: ColorScheme = LightScheme