package com.aura.chat.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.aura.chat.ui.theme.LocalAuraAccent

/**
 * 轻量 Markdown 渲染：标题、代码块、行内代码、粗体、斜体、删除线、有序/无序列表。
 * 不引入第三方依赖，覆盖 DSH 输出的常见格式。
 */
@Composable
fun MarkdownText(text: String, modifier: Modifier = Modifier, color: Color = LocalAuraAccent.current.text) {
    val accent = LocalAuraAccent.current
    val blocks = remember(text) { parseMarkdownBlocks(text) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (block in blocks) {
            when (block) {
                is MdBlock.Code -> Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = accent.chip,
                    contentColor = accent.onChip,
                ) {
                    Text(
                        text = block.text,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = accent.onChip,
                    )
                }
                is MdBlock.Heading -> Text(
                    text = inlineMarkdown(block.text, color, accent.onChip, accent.deep),
                    style = when (block.level) {
                        1 -> MaterialTheme.typography.titleLarge
                        2 -> MaterialTheme.typography.titleMedium
                        else -> MaterialTheme.typography.titleSmall
                    }.copy(fontWeight = FontWeight.Bold),
                    color = color,
                )
                is MdBlock.Bullet -> Row {
                    Text("·  ", style = MaterialTheme.typography.bodyMedium, color = accent.muted)
                    Text(inlineMarkdown(block.text, color, accent.onChip, accent.deep), style = MaterialTheme.typography.bodyMedium)
                }
                is MdBlock.Ordered -> Row {
                    Text(block.marker + "  ", style = MaterialTheme.typography.bodyMedium, color = accent.muted)
                    Text(inlineMarkdown(block.text, color, accent.onChip, accent.deep), style = MaterialTheme.typography.bodyMedium)
                }
                is MdBlock.Paragraph -> Text(
                    text = inlineMarkdown(block.text, color, accent.onChip, accent.deep),
                    style = MaterialTheme.typography.bodyMedium,
                    color = color,
                )
            }
        }
    }
}

private sealed interface MdBlock {
    data class Paragraph(val text: String) : MdBlock
    data class Heading(val level: Int, val text: String) : MdBlock
    data class Code(val text: String) : MdBlock
    data class Bullet(val text: String) : MdBlock
    data class Ordered(val marker: String, val text: String) : MdBlock
}

private fun parseMarkdownBlocks(source: String): List<MdBlock> {
    val blocks = mutableListOf<MdBlock>()
    val lines = source.replace("\r\n", "\n").split("\n")
    var inCode = false
    val code = StringBuilder()
    val paragraph = StringBuilder()
    fun flushParagraph() {
        if (paragraph.isNotBlank()) blocks.add(MdBlock.Paragraph(paragraph.toString().trim()))
        paragraph.clear()
    }
    for (line in lines) {
        val trimmed = line.trim()
        if (trimmed.startsWith("```")) {
            if (inCode) {
                blocks.add(MdBlock.Code(code.toString().trimEnd()))
                code.clear()
            } else {
                flushParagraph()
            }
            inCode = !inCode
            continue
        }
        if (inCode) { code.appendLine(line); continue }
        val heading = Regex("^(#{1,6})\\s+(.*)$").find(trimmed)
        val bullet = Regex("^[-*]\\s+(.*)$").find(trimmed)
        val ordered = Regex("^(\\d+[.)])\\s+(.*)$").find(trimmed)
        if (trimmed.isEmpty()) {
            flushParagraph()
        } else if (heading != null) {
            flushParagraph()
            blocks.add(MdBlock.Heading(heading.groupValues[1].length, heading.groupValues[2]))
        } else if (bullet != null) {
            flushParagraph()
            blocks.add(MdBlock.Bullet(bullet.groupValues[1]))
        } else if (ordered != null) {
            flushParagraph()
            blocks.add(MdBlock.Ordered(ordered.groupValues[1], ordered.groupValues[2]))
        } else {
            if (paragraph.isNotEmpty()) paragraph.append(" ")
            paragraph.append(trimmed)
        }
    }
    if (inCode) blocks.add(MdBlock.Code(code.toString().trimEnd()))
    flushParagraph()
    return blocks
}

private fun inlineMarkdown(text: String, color: Color, codeBg: Color, linkColor: Color): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < text.length) {
        when {
            text.startsWith("**", i) -> {
                val end = text.indexOf("**", i + 2)
                if (end > i) { withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(text.substring(i + 2, end)) }; i = end + 2 }
                else { append(text[i]); i++ }
            }
            text[i] == '`' -> {
                val end = text.indexOf('`', i + 1)
                if (end > i) { withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = codeBg, color = color)) { append(text.substring(i + 1, end)) }; i = end + 1 }
                else { append(text[i]); i++ }
            }
            text.startsWith("~~", i) -> {
                val end = text.indexOf("~~", i + 2)
                if (end > i) { withStyle(SpanStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough)) { append(text.substring(i + 2, end)) }; i = end + 2 }
                else { append(text[i]); i++ }
            }
            text[i] == '*' -> {
                val end = text.indexOf('*', i + 1)
                if (end > i) { withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(text.substring(i + 1, end)) }; i = end + 1 }
                else { append(text[i]); i++ }
            }
            text[i] == '[' -> {
                val close = text.indexOf(']', i)
                val open = if (close > 0) text.indexOf('(', close) else -1
                val end = if (open > 0) text.indexOf(')', open) else -1
                if (close > 0 && open == close + 1 && end > 0) {
                    withStyle(SpanStyle(color = linkColor, textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline)) { append(text.substring(i + 1, close)) }
                    i = end + 1
                } else { append(text[i]); i++ }
            }
            else -> { append(text[i]); i++ }
        }
    }
}

@Composable
fun WaveProgress(modifier: Modifier = Modifier) {
    val accent = LocalAuraAccent.current
    val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "wave")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(1400, easing = androidx.compose.animation.core.LinearEasing),
        ),
        label = "phase",
    )
    androidx.compose.foundation.Canvas(modifier.fillMaxWidth().height(12.dp)) {
        drawRoundRect(color = accent.chip, cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2))
        for (layer in 0..1) {
            val path = androidx.compose.ui.graphics.Path()
            val amplitude = size.height * (0.28f - layer * 0.08f)
            val mid = size.height / 2f
            var x = 0f
            path.moveTo(0f, mid)
            while (x <= size.width) {
                val y = mid + kotlin.math.sin((x / size.width * 6.2832f * 2f) + phase * 6.2832f).toFloat() * amplitude
                path.lineTo(x, y)
                x += 6f
            }
            drawPath(path, color = if (layer == 0) accent.deep.copy(alpha = 0.55f) else accent.lemon.copy(alpha = 0.85f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f))
        }
    }
}
