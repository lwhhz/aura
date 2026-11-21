package com.aura.chat.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.aura.chat.ui.theme.LocalAuraAccent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/** 图片鉴权：手机端通过 sidecar 读取电脑本地图片。 */
data class MarkdownImageAuth(val baseUrl: String, val token: String, val cwd: String = "")

val LocalMarkdownImageAuth = androidx.compose.runtime.staticCompositionLocalOf<MarkdownImageAuth?> { null }

/** 轻量 Markdown：标题、代码块、表格、图片、行内代码/粗体/斜体/删除线/可点击链接、列表。 */
@Composable
fun MarkdownText(text: String, modifier: Modifier = Modifier, color: Color = LocalAuraAccent.current.text) {
    val accent = LocalAuraAccent.current
    val blocks = remember(text) { parseMarkdownBlocks(text) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (block in blocks) {
            when (block) {
                is MdBlock.Code -> Surface(shape = RoundedCornerShape(12.dp), color = accent.chip) {
                    Text(
                        text = block.text,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = accent.onChip,
                    )
                }
                is MdBlock.Heading -> Text(
                    text = inlineMarkdown(block.text, color, accent.deep),
                    style = when (block.level) {
                        1 -> MaterialTheme.typography.titleLarge
                        2 -> MaterialTheme.typography.titleMedium
                        else -> MaterialTheme.typography.titleSmall
                    }.copy(fontWeight = FontWeight.Bold),
                    color = color,
                )
                is MdBlock.Bullet -> Row {
                    Text("·  ", style = MaterialTheme.typography.bodyMedium, color = accent.muted)
                    Text(inlineMarkdown(block.text, color, accent.deep), style = MaterialTheme.typography.bodyMedium)
                }
                is MdBlock.Ordered -> Row {
                    Text(block.marker + "  ", style = MaterialTheme.typography.bodyMedium, color = accent.muted)
                    Text(inlineMarkdown(block.text, color, accent.deep), style = MaterialTheme.typography.bodyMedium)
                }
                is MdBlock.Image -> MarkdownImage(block.alt, block.url)
                is MdBlock.Table -> MarkdownTable(block.header, block.rows, color)
                is MdBlock.Paragraph -> Text(
                    text = inlineMarkdown(block.text, color, accent.deep),
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
    data class Image(val alt: String, val url: String) : MdBlock
    data class Table(val header: List<String>, val rows: List<List<String>>) : MdBlock
}

private fun isTableSeparator(line: String): Boolean {
    val cells = line.trim().trim('|').split("|")
    if (cells.isEmpty()) return false
    return cells.all { cell ->
        val core = cell.trim().trim(':')
        core.length >= 3 && core.all { it == '-' }
    }
}

private fun parseImage(line: String): Pair<String, String>? {
    if (!line.startsWith("![")) return null
    val close = line.indexOf("](", 2)
    if (close < 0 || !line.endsWith(")")) return null
    return line.substring(2, close) to line.substring(close + 2, line.length - 1)
}

private fun headingLevel(line: String): Pair<Int, String>? {
    val hashes = line.takeWhile { it == '#' }.length
    if (hashes == 0 || hashes > 6) return null
    if (line.getOrNull(hashes) != ' ') return null
    return hashes to line.substring(hashes + 1).trim()
}

private fun bulletText(line: String): String? {
    if (line.startsWith("- ") || line.startsWith("* ")) return line.substring(2).trim()
    return null
}

private fun orderedText(line: String): Pair<String, String>? {
    var digits = 0
    while (digits < line.length && line[digits].isDigit()) digits++
    if (digits == 0) return null
    val marker = line.substring(0, digits)
    val next = line.getOrNull(digits) ?: return null
    if ((next != '.' && next != ')') || line.getOrNull(digits + 1) != ' ') return null
    return marker + next to line.substring(digits + 2).trim()
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

    var index = 0
    while (index < lines.size) {
        val raw = lines[index]
        val trimmed = raw.trim()
        if (trimmed.startsWith("```")) {
            if (inCode) { blocks.add(MdBlock.Code(code.toString().trimEnd())); code.clear() } else flushParagraph()
            inCode = !inCode
            index++
            continue
        }
        if (inCode) { code.appendLine(raw); index++; continue }

        if (trimmed.startsWith("|") && index + 1 < lines.size && isTableSeparator(lines[index + 1])) {
            flushParagraph()
            val header = splitRow(trimmed)
            val rows = mutableListOf<List<String>>()
            var cursor = index + 2
            while (cursor < lines.size && lines[cursor].trim().startsWith("|")) {
                rows.add(splitRow(lines[cursor].trim()))
                cursor++
            }
            blocks.add(MdBlock.Table(header, rows))
            index = cursor
            continue
        }

        val image = parseImage(trimmed)
        if (image != null) {
            flushParagraph()
            blocks.add(MdBlock.Image(image.first, image.second))
            index++
            continue
        }

        val heading = headingLevel(trimmed)
        val bullet = bulletText(trimmed)
        val ordered = orderedText(trimmed)
        if (trimmed.isEmpty()) {
            flushParagraph()
        } else if (heading != null) {
            flushParagraph()
            blocks.add(MdBlock.Heading(heading.first, heading.second))
        } else if (bullet != null) {
            flushParagraph()
            blocks.add(MdBlock.Bullet(bullet))
        } else if (ordered != null) {
            flushParagraph()
            blocks.add(MdBlock.Ordered(ordered.first, ordered.second))
        } else {
            if (paragraph.isNotEmpty()) paragraph.append(" ")
            paragraph.append(trimmed)
        }
        index++
    }
    if (inCode) blocks.add(MdBlock.Code(code.toString().trimEnd()))
    flushParagraph()
    return blocks
}

private fun splitRow(row: String): List<String> =
    row.trim().trim('|').split("|").map { it.trim() }

private fun inlineMarkdown(text: String, color: Color, linkColor: Color): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < text.length) {
        when {
            text.startsWith("![", i) -> {
                val close = text.indexOf(']', i + 2)
                val open = if (close > 0) text.indexOf('(', close) else -1
                val end = if (open == close + 1) text.indexOf(')', open) else -1
                if (close > 0 && open == close + 1 && end > 0) {
                    withStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)) {
                        append("[图片] " + text.substring(i + 2, close))
                    }
                    i = end + 1
                } else { append(text[i]); i++ }
            }
            text.startsWith("**", i) -> {
                val end = text.indexOf("**", i + 2)
                if (end > i) { withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(text.substring(i + 2, end)) }; i = end + 2 }
                else { append(text[i]); i++ }
            }
            text[i] == '`' -> {
                val end = text.indexOf('`', i + 1)
                if (end > i) {
                    withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = linkColor.copy(alpha = 0.10f))) {
                        append(text.substring(i + 1, end))
                    }
                    i = end + 1
                } else { append(text[i]); i++ }
            }
            text.startsWith("~~", i) -> {
                val end = text.indexOf("~~", i + 2)
                if (end > i) { withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) { append(text.substring(i + 2, end)) }; i = end + 2 }
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
                val end = if (open == close + 1) text.indexOf(')', open) else -1
                if (close > 0 && open == close + 1 && end > 0) {
                    val url = text.substring(open + 1, end)
                    withLink(
                        LinkAnnotation.Url(
                            url,
                            TextLinkStyles(style = SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)),
                        ),
                    ) { append(text.substring(i + 1, close)) }
                    i = end + 1
                } else { append(text[i]); i++ }
            }
            else -> { append(text[i]); i++ }
        }
    }
}

@Composable
private fun MarkdownTable(header: List<String>, rows: List<List<String>>, color: Color) {
    val accent = LocalAuraAccent.current
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(accent.chip.copy(alpha = 0.45f)),
    ) {
        Row(Modifier.background(accent.chip).padding(horizontal = 10.dp, vertical = 8.dp)) {
            header.forEach { cell ->
                Text(
                    text = inlineMarkdown(cell, color, accent.deep),
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = color,
                )
            }
        }
        rows.forEachIndexed { rowIndex, row ->
            Row(
                Modifier
                    .background(if (rowIndex % 2 == 1) Color.White.copy(alpha = 0.35f) else Color.Transparent)
                    .padding(horizontal = 10.dp, vertical = 7.dp),
            ) {
                row.forEach { cell ->
                    Text(
                        text = inlineMarkdown(cell, color, accent.deep),
                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = color,
                    )
                }
            }
        }
    }
}

@Composable
private fun MarkdownImage(alt: String, rawUrl: String) {
    val accent = LocalAuraAccent.current
    val auth = LocalMarkdownImageAuth.current
    val url = rawUrl.trim()
        .substringBefore(" \"").trim()
        .removePrefix("<").removeSuffix(">")
        .removePrefix("file://")
    val raw = url
    val path = if (raw.startsWith("/") || (raw.length > 2 && raw[1] == ':')) {
        raw
    } else {
        val base = auth?.cwd?.trimEnd('/', '\\') ?: ""
        if (base.isBlank()) raw else base + "/" + raw.trimStart('/', '\\')
    }
    val remote = when {
        url.startsWith("http://") || url.startsWith("https://") -> url
        auth != null -> auth.baseUrl + "/aura/file?path=" + java.net.URLEncoder.encode(path, "UTF-8") + "&token=" + auth.token
        else -> null
    }
    val localPath = if (remote == null) path else null
    val bitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(initialValue = null, remote, localPath) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                if (remote != null) {
                    val connection = (URL(remote).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 6000
                        readTimeout = 15000
                    }
                    connection.inputStream.use { BitmapFactory.decodeStream(it)?.asImageBitmap() }
                } else if (localPath != null) {
                    java.io.File(localPath).takeIf { it.isFile }?.inputStream()?.use { BitmapFactory.decodeStream(it)?.asImageBitmap() }
                } else null
            }.getOrNull()
        }
    }
    var preview by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val image = bitmap
    if (image != null && preview) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { preview = false }, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
            androidx.compose.foundation.layout.Box(
                Modifier.fillMaxWidth().heightIn(max = 2000.dp).background(Color.Black)
                    .clickable { preview = false },
                contentAlignment = androidx.compose.ui.Alignment.Center,
            ) {
                Image(
                    bitmap = image,
                    contentDescription = alt,
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.Fit,
                )
            }
        }
    }
    if (image != null) {
        Image(
            bitmap = image,
            contentDescription = alt.ifBlank { null },
            modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp).clip(RoundedCornerShape(12.dp))
                .clickable { preview = true },
            contentScale = ContentScale.Fit,
        )
    } else {
        Surface(shape = RoundedCornerShape(12.dp), color = accent.chip) {
            Text(
                text = "图片" + (if (alt.isBlank()) "" else "：" + alt) + "\n" + url,
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodySmall,
                color = accent.onChip,
            )
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
            drawPath(
                path,
                color = if (layer == 0) accent.deep.copy(alpha = 0.55f) else accent.lemon.copy(alpha = 0.85f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f),
            )
        }
    }
}
