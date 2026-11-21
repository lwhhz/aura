package com.aura.chat.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * 图标统一改用 Material Symbols（Rounded，Apache 2.0，随 compose material-icons-extended 提供），
 * 不再手绘 Canvas 路径。保留函数名，调用方无需改动。
 */
@Composable
private fun GlyphIcon(icon: ImageVector, modifier: Modifier = Modifier, tint: Color) {
    Icon(imageVector = icon, contentDescription = null, modifier = modifier.size(22.dp), tint = tint)
}

@Composable
fun MicGlyph(modifier: Modifier = Modifier, tint: Color) = GlyphIcon(Icons.Rounded.Mic, modifier, tint)

@Composable
fun SparkGlyph(modifier: Modifier = Modifier, tint: Color) = GlyphIcon(Icons.Rounded.AutoAwesome, modifier, tint)

@Composable
fun PlusGlyph(modifier: Modifier = Modifier, tint: Color) = GlyphIcon(Icons.Rounded.Add, modifier, tint)

@Composable
fun ArrowUpGlyph(modifier: Modifier = Modifier, tint: Color) = GlyphIcon(Icons.Rounded.ArrowUpward, modifier, tint)

@Composable
fun StopGlyph(modifier: Modifier = Modifier, tint: Color) = GlyphIcon(Icons.Rounded.Stop, modifier, tint)

@Composable
fun GlobeGlyph(modifier: Modifier = Modifier, tint: Color) = GlyphIcon(Icons.Rounded.Public, modifier, tint)

@Composable
fun CopyGlyph(modifier: Modifier = Modifier, tint: Color) = GlyphIcon(Icons.Rounded.ContentCopy, modifier, tint)

@Composable
fun RefreshGlyph(modifier: Modifier = Modifier, tint: Color) = GlyphIcon(Icons.Rounded.Refresh, modifier, tint)

@Composable
fun CloseGlyph(modifier: Modifier = Modifier, tint: Color) = GlyphIcon(Icons.Rounded.Close, modifier, tint)

@Composable
fun ArrowLeftGlyph(modifier: Modifier = Modifier, tint: Color) = GlyphIcon(Icons.Rounded.ArrowBack, modifier, tint)

@Composable
fun MoreGlyph(modifier: Modifier = Modifier, tint: Color) = GlyphIcon(Icons.Rounded.MoreHoriz, modifier, tint)

@Composable
fun ChevronGlyph(
    modifier: Modifier = Modifier,
    tint: Color,
    pointsUp: Boolean = false,
) = GlyphIcon(if (pointsUp) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown, modifier, tint)

/** 语音波形保留自绘（图库里没有等价的波形图标）。 */
@Composable
fun WaveGlyph(modifier: Modifier = Modifier, tint: Color = Color.White) {
    GlyphIcon(Icons.Rounded.Mic, modifier, tint)
}
