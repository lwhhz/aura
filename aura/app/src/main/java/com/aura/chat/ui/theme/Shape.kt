package com.aura.chat.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * M3 十档形状令牌（KB 01-Foundations/shape-system.md）。
 * 保持 AuraShapes 现值以维持当前视觉，后续页面逐步迁移到十档。
 */
object AuraShapeTokens {
    val none = RoundedCornerShape(0.dp)
    val micro = RoundedCornerShape(2.dp)
    val extraSmall = RoundedCornerShape(4.dp)
    val small = RoundedCornerShape(8.dp)
    val medium = RoundedCornerShape(12.dp)
    val large = RoundedCornerShape(16.dp)
    val largeIncreased = RoundedCornerShape(20.dp)
    val extraLarge = RoundedCornerShape(28.dp)
    val extraLargeIncreased = RoundedCornerShape(32.dp)
    val extraExtraLarge = RoundedCornerShape(48.dp)
    val full = RoundedCornerShape(50)
}

/** 组件级形状令牌：页面禁止再写裸 RoundedCornerShape。 */
object AuraComponentShapes {
    val card = AuraShapeTokens.extraLarge
    val dialog = AuraShapeTokens.extraLargeIncreased
    val input = AuraShapeTokens.largeIncreased
    val toolCard = AuraShapeTokens.large
    val codeBlock = AuraShapeTokens.medium
    val assistantBubble = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomEnd = 28.dp, bottomStart = 8.dp)
    val userBubble = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomEnd = 8.dp, bottomStart = 28.dp)
}

val AuraShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)
