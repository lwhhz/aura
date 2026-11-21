package com.aura.chat.ui.components

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.aura.chat.ui.theme.AuraAccent

/** 对话页的两套皮肤：深色墨黑 / 浅色纸面，都由主题与 Brand 派生。 */
@Immutable
data class ChatColors(
    val background: Color,
    val surface: Color,
    val surfaceAlt: Color,
    val text: Color,
    val muted: Color,
    val userBubble: Color,
    val onUserBubble: Color,
    val highlight: Color,
    val onSurfaceControl: Color,
)

val LocalChatColors = staticCompositionLocalOf {
    ChatColors(
        background = Color(0xFF0D0B14),
        surface = Color(0xFF171320),
        surfaceAlt = Color(0xFF241E36),
        text = Color(0xFFF2EFFF),
        muted = Color(0xFFB4ACD0),
        userBubble = Color(0xFF7A74FF),
        onUserBubble = Color.White,
        highlight = Color(0xFFE9FF7A),
        onSurfaceControl = Color(0xFFC9C4FF),
    )
}

fun chatColors(dark: Boolean, accent: AuraAccent, scheme: ColorScheme): ChatColors =
    if (dark) {
        ChatColors(
            background = accent.ink,
            surface = accent.inkCard,
            surfaceAlt = accent.inkSoft,
            text = accent.onInk,
            muted = accent.onInkMuted,
            userBubble = accent.userBubble,
            onUserBubble = Color.White,
            highlight = accent.lemon,
            onSurfaceControl = accent.onChip,
        )
    } else {
        ChatColors(
            background = scheme.background,
            surface = scheme.surfaceContainerLowest,
            surfaceAlt = accent.chip,
            text = scheme.onSurface,
            muted = scheme.onSurfaceVariant,
            userBubble = accent.deep,
            onUserBubble = accent.onDeep,
            highlight = accent.deep,
            onSurfaceControl = accent.onChip,
        )
    }
