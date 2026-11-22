package com.aura.chat.ui.components

import com.aura.chat.ui.theme.AuraShapeTokens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.chat.ui.theme.Blush
import com.aura.chat.ui.theme.DisplaySerif
import com.aura.chat.ui.theme.Ink950
import com.aura.chat.ui.theme.Lavender100
import com.aura.chat.ui.theme.Lavender200
import com.aura.chat.ui.theme.Lavender300
import com.aura.chat.ui.theme.Lemon
import com.aura.chat.ui.theme.Periwinkle
import com.aura.chat.ui.theme.Sky
import com.aura.chat.ui.theme.Violet500
import com.aura.chat.ui.theme.Violet700

/** 程序化「封面艺术」：按主题索引取渐变，不引用任何外部图片，避免版权与体积问题。 */
data class ArtPalette(val from: Color, val to: Color, val glow: Color, val glyph: String)

private val palettes = listOf(
    ArtPalette(from = Violet700, to = Periwinkle, glow = Lavender300, glyph = "S"),
    ArtPalette(from = Lavender200, to = Blush, glow = com.aura.chat.ui.theme.ArtGlowWarm, glyph = "A"),
    ArtPalette(from = com.aura.chat.ui.theme.ArtNavy, to = Periwinkle, glow = Sky, glyph = "7"),
    ArtPalette(from = Ink950, to = Violet500, glow = Lemon, glyph = "W"),
    ArtPalette(from = com.aura.chat.ui.theme.ArtPlum, to = Lavender300, glow = com.aura.chat.ui.theme.ArtGlowGold, glyph = "N"),
)

fun paletteOf(accent: Int): ArtPalette = palettes[accent % palettes.size]

@Composable
fun AuraArt(
    accent: Int,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = AuraShapeTokens.extraLarge,
    glyph: String? = null,
    glyphSize: Int = 40,
) {
    val p = paletteOf(accent)
    Box(
        modifier = modifier.clip(shape).background(Brush.linearGradient(listOf(p.from, p.to))),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawAura(p, 0.62f)
        }
        if (glyph != null) {
            Text(
                text = glyph,
                style = TextStyle(
                    fontFamily = DisplaySerif,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = glyphSize.sp, // ui-lint: allow
                    color = Color.White.copy(alpha = 0.92f),
                ),
            )
        }
    }
}

private fun DrawScope.drawAura(p: ArtPalette, glowAlpha: Float) {
    val w = size.width
    val h = size.height
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(p.glow.copy(alpha = glowAlpha), Color.Transparent),
            center = Offset(w * 0.78f, h * 0.18f),
            radius = w * 0.72f,
        ),
        radius = w * 0.72f,
        center = Offset(w * 0.78f, h * 0.18f),
    )
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(Color.White.copy(alpha = 0.20f), Color.Transparent),
            center = Offset(w * 0.18f, h * 0.86f),
            radius = w * 0.55f,
        ),
        topLeft = Offset(w * -0.10f, h * 0.55f),
        size = Size(w * 0.9f, h * 0.8f),
    )
}

/** 头像：渐变底 + 首字母，编辑风格统一。 */
@Composable
fun AuraAvatar(
    accent: Int,
    initials: String,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
) {
    val p = paletteOf(accent)
    Box(
        modifier = modifier
            .size(size)
            .clip(AuraShapeTokens.full)
            .background(Brush.linearGradient(listOf(p.from, p.to))),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials,
            fontSize = (size.value * 0.36f).sp, // ui-lint: allow
            fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(alpha = 0.95f),
        )
    }
}

/** 页面底纹：薰衣草纸面 + 两团低饱和光晕（对应截图里柔和的紫色底）。 */
@Composable
fun LavenderField(modifier: Modifier = Modifier, dark: Boolean = false) {
    val glowA = if (dark) Lavender300.copy(alpha = 0.20f) else Lavender300.copy(alpha = 0.38f)
    val glowB = if (dark) Periwinkle.copy(alpha = 0.16f) else Blush.copy(alpha = 0.30f)
    Canvas(modifier.fillMaxSize()) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(glowA, Color.Transparent),
                center = Offset(size.width * 0.92f, size.height * 0.04f),
                radius = size.width * 0.85f,
            ),
            radius = size.width * 0.85f,
            center = Offset(size.width * 0.92f, size.height * 0.04f),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(glowB, Color.Transparent),
                center = Offset(size.width * 0.02f, size.height * 0.62f),
                radius = size.width * 0.75f,
            ),
            radius = size.width * 0.75f,
            center = Offset(size.width * 0.02f, size.height * 0.62f),
        )
    }
}

/** 深色页底：墨黑 + 右上角紫色光晕。 */
@Composable
fun InkField(modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxSize()) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Periwinkle.copy(alpha = 0.26f), Color.Transparent),
                center = Offset(size.width * 0.88f, size.height * 0.02f),
                radius = size.width * 0.8f,
            ),
            radius = size.width * 0.8f,
            center = Offset(size.width * 0.88f, size.height * 0.02f),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Lemon.copy(alpha = 0.07f), Color.Transparent),
                center = Offset(size.width * 0.05f, size.height * 0.98f),
                radius = size.width * 0.7f,
            ),
            radius = size.width * 0.7f,
            center = Offset(size.width * 0.05f, size.height * 0.98f),
        )
    }
}
