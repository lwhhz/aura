package com.aura.chat.ui.components

import com.aura.chat.ui.theme.AuraShapeTokens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.aura.chat.data.ChatMessage
import com.aura.chat.data.Citation
import com.aura.chat.data.Models

/** 用户气泡：用主题派生色，浅色下是 Brand 紫，深色下是长春花。 */
@Composable
fun UserBubble(text: String, modifier: Modifier = Modifier) {
    val colors = LocalChatColors.current
    Surface(
        modifier = modifier.fillMaxWidth(0.88f),
        shape = com.aura.chat.ui.theme.AuraComponentShapes.userBubble,
        color = colors.userBubble,
        contentColor = colors.onUserBubble,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

/** 思考过程折叠块：浅色纸面 / 深色紫黑两套。 */
@Composable
fun ReasoningBlock(
    reasoning: String,
    seconds: Int?,
    streaming: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = LocalChatColors.current
    var expanded by remember { mutableStateOf(false) }
    Surface(
        onClick = { expanded = !expanded },
        modifier = modifier.fillMaxWidth(),
        shape = AuraShapeTokens.largeIncreased,
        color = colors.surfaceAlt,
        contentColor = colors.muted,
    ) {
        Column(
            Modifier
                .animateContentSize()
                .padding(horizontal = 16.dp, vertical = 13.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SparkGlyph(Modifier.size(13.dp), colors.highlight)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (streaming) "正在思考…" else "已思考" + (seconds?.let { " " + it + " 秒" } ?: ""),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (streaming) colors.highlight else colors.muted,
                )
                Spacer(Modifier.weight(1f))
                ChevronGlyph(Modifier.size(14.dp), colors.muted, pointsUp = expanded)
            }
            AnimatedVisibility(
                visible = expanded && reasoning.isNotBlank(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Row(Modifier.padding(top = 10.dp).height(IntrinsicSize.Min)) {
                    Box(
                        Modifier
                            .width(2.dp)
                            .fillMaxHeight()
                            .background(colors.highlight.copy(alpha = 0.6f))
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = reasoning,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.muted,
                    )
                }
            }
        }
    }
}

/** 简版 markdown：加粗 + 换行。流式时在末尾追加闪烁光标。 */
@Composable
fun RichText(
    text: String,
    modifier: Modifier = Modifier,
    streaming: Boolean = false,
    color: Color = LocalChatColors.current.text,
) {
    val colors = LocalChatColors.current
    val transition = rememberInfiniteTransition(label = "caret")
    val caretAlpha by transition.animateFloat(
        initialValue = 0.15f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(620), RepeatMode.Reverse),
        label = "caretAlpha",
    )
    Column(modifier) {
        Text(
            text = markdown(text),
            style = MaterialTheme.typography.bodyLarge,
            color = color,
        )
        if (streaming) {
            Box(
                Modifier
                    .padding(top = 6.dp)
                    .size(width = 9.dp, height = 16.dp)
                    .alpha(caretAlpha)
                    .background(colors.highlight, AuraShapeTokens.micro)
            )
        }
    }
}

private fun markdown(text: String): AnnotatedString = buildAnnotatedString {
    val bold = "**"
    var index = 0
    while (index < text.length) {
        val start = text.indexOf(bold, index)
        if (start < 0) {
            append(text.substring(index))
            break
        }
        val end = text.indexOf(bold, start + bold.length)
        if (end < 0) {
            append(text.substring(index))
            break
        }
        append(text.substring(index, start))
        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
            append(text.substring(start + bold.length, end))
        }
        index = end + bold.length
    }
}

/** 助手回答主体：实心卡 + 波浪进度 + 引用 + 操作行。 */
@Composable
fun AssistantBlock(
    message: ChatMessage,
    streaming: Boolean,
    onCopy: () -> Unit,
    onRegenerate: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val colors = LocalChatColors.current
    Column(modifier.fillMaxWidth()) {
        if (!message.reasoning.isNullOrBlank()) {
            ReasoningBlock(
                reasoning = message.reasoning,
                seconds = message.reasoningSeconds,
                streaming = streaming && message.text.isBlank(),
            )
            Spacer(Modifier.height(10.dp))
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = com.aura.chat.ui.theme.AuraComponentShapes.assistantBubble,
            color = colors.surface,
            contentColor = colors.text,
        ) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
                if (streaming) {
                    RichText(
                        text = message.text.ifBlank { "" },
                        streaming = true,
                        color = colors.text,
                    )
                } else {
                    com.aura.chat.ui.components.MarkdownText(
                        text = message.text.ifBlank { "（空回答）" },
                        color = colors.text,
                    )
                }
                if (streaming) {
                    Spacer(Modifier.height(12.dp))
                    LinearWavyProgressIndicator(
                        modifier = Modifier.fillMaxWidth().height(12.dp),
                        color = colors.highlight,
                        trackColor = colors.muted.copy(alpha = 0.18f),
                    )
                }
            }
        }
        if (message.citations.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            CitationRow(message.citations)
        }
        if (!streaming) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AuraIconButton(
                    onClick = onCopy,
                    content = colors.muted,
                    size = 40.dp,
                    contentDescription = "复制回答",
                ) { m, c -> CopyGlyph(m, c) }
                if (onRegenerate != null) {
                    AuraIconButton(
                        onClick = onRegenerate,
                        content = colors.muted,
                        size = 40.dp,
                        contentDescription = "重新生成",
                    ) { m, c -> RefreshGlyph(m, c) }
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = Models.of(message.model).label + (message.elapsedSeconds?.let { " · " + it + "s" } ?: ""),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.muted.copy(alpha = 0.8f),
                )
            }
        }
    }
}

/** 引用来源横向卡片：最多 5 条，展开入口放在标题行右端。 */
@Composable
fun CitationRow(citations: List<Citation>, modifier: Modifier = Modifier) {
    val colors = LocalChatColors.current
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    var expanded by remember { mutableStateOf(false) }
    val shown = if (expanded) citations else citations.take(5)
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlobeGlyph(Modifier.size(13.dp), colors.highlight)
            Spacer(Modifier.width(6.dp))
            Text(
                text = "搜索到 " + citations.size + " 个网页",
                style = MaterialTheme.typography.labelMedium,
                color = colors.muted,
            )
            Spacer(Modifier.weight(1f))
            if (citations.size > 5) {
                Text(
                    text = if (expanded) "收起" else "展开全部",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.highlight,
                    modifier = Modifier.clickable { expanded = !expanded },
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(shown) { citation ->
                Surface(
                    onClick = { if (citation.url.isNotBlank()) runCatching { uriHandler.openUri(citation.url) } },
                    shape = AuraShapeTokens.largeIncreased,
                    color = colors.surfaceAlt,
                    contentColor = colors.muted,
                ) {
                    Column(Modifier.width(176.dp).padding(14.dp)) {
                        Text(
                            text = citation.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.text,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = citation.source,
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.highlight,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}