package com.aura.chat.ui.components

import androidx.compose.material3.Icon
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.Icons
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aura.chat.data.Conversation
import com.aura.chat.data.Models
import com.aura.chat.ui.theme.editorialFamily
import com.aura.chat.ui.theme.LocalAuraAccent

/** 置顶的深色编辑卡：整块深紫实心，唯一焦点，无描边。 */
@Composable
fun FeaturedConversationCard(
    conversation: Conversation,
    onOpen: () -> Unit,
    onTogglePin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = LocalAuraAccent.current
    Surface(
        onClick = onOpen,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(36.dp),
        color = accent.featured,
        contentColor = accent.onFeatured,
    ) {
        Box(Modifier.fillMaxWidth()) {
            // 单条柠檬细弧线：纯装饰，不参与布局
            Canvas(Modifier.matchParentSize()) {
                val stroke = 2.dp.toPx()
                drawArc(
                    color = accent.lemon.copy(alpha = 0.92f),
                    startAngle = 96f,
                    sweepAngle = 78f,
                    useCenter = false,
                    topLeft = Offset(size.width * 0.52f, size.height * -0.72f),
                    size = Size(size.width * 0.86f, size.height * 1.02f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round),
                )
            }
            Column(Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Eyebrow("继续上次", color = accent.lemon)
                    Spacer(Modifier.weight(1f))
                    // 独立可点：不能是纯装饰标签，否则点击会被外层卡片吞掉
                    Surface(
                        onClick = onTogglePin,
                        shape = CircleShape,
                        color = if (conversation.pinnedAt != null) accent.lemon else Color.White.copy(alpha = 0.16f),
                        contentColor = if (conversation.pinnedAt != null) accent.onLemon else accent.onFeatured,
                    ) {
                        Text(
                            text = "PINNED",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                            letterSpacing = 0.6.sp,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }
                Spacer(Modifier.height(48.dp))
                Text(
                    text = conversation.title,
                    style = MaterialTheme.typography.headlineLarge,
                    fontFamily = editorialFamily(conversation.title),
                    color = accent.onFeatured,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = summaryOf(conversation.lastMessage?.text.orEmpty()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = accent.onFeatured.copy(alpha = 0.78f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(22.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(30.dp).background(accent.chip, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (Models.of(conversation.model).id == Models.DEEPSEEK_FLASH.id) Icons.Rounded.Bolt
                            else Icons.Rounded.ChatBubble,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = accent.onChip,
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = Models.of(conversation.model).label,
                        style = MaterialTheme.typography.labelLarge,
                        color = accent.onFeatured.copy(alpha = 0.9f),
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "打开 →",
                        style = MaterialTheme.typography.labelLarge,
                        color = accent.lemon,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
        }
    }
}

/** 普通会话卡：纯白实心色块，左侧文字右侧封面。 */
@Composable
fun ConversationCard(
    conversation: Conversation,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    selected: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val accent = LocalAuraAccent.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.98f else 1f, label = "cardScale")
    Surface(
        onClick = onOpen,
        modifier = modifier.fillMaxWidth().scale(scale),
        shape = RoundedCornerShape(30.dp),
        color = if (selected) accent.chip else accent.card,
        contentColor = accent.text,
        interactionSource = interaction,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = conversation.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = editorialFamily(conversation.title),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = summaryOf(conversation.lastMessage?.text.orEmpty()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = accent.muted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AuraTag(text = Models.of(conversation.model).label)
                    Text(
                        text = relativeTime(conversation.updatedAt),
                        style = MaterialTheme.typography.labelMedium,
                        color = accent.muted,
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Surface(
                onClick = onDelete,
                modifier = Modifier
                    .size(width = 64.dp, height = 64.dp)
                    .semantics { contentDescription = "删除对话" },
                shape = RoundedCornerShape(20.dp),
                color = accent.chip,
                contentColor = accent.onChip,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(26.dp),
                    )
                }
            }
        }
    }
}

/** 卡片摘要：取第一段，去掉轻标记。 */
fun summaryOf(text: String): String {
    val cleaned = text.replace("**", "").split("\n\n").firstOrNull().orEmpty()
        .replace("\n", " · ").trim()
    return cleaned.ifBlank { "还没有消息 · 点开开始" }
}

/** 去掉轻标记，用于卡片摘要。 */
fun stripMarkdown(text: String): String = text
    .replace("**", "")
    .replace("\n\n", " · ")
    .replace("\n· ", " · ")
    .replace("\n", " ")
    .trim()

fun relativeTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val minutes = diff / 60_000
    return when {
        minutes < 1 -> "刚刚"
        minutes < 60 -> minutes.toString() + " 分钟前"
        minutes < 60 * 24 -> (minutes / 60).toString() + " 小时前"
        minutes < 60 * 24 * 7 -> (minutes / (60 * 24)).toString() + " 天前"
        else -> (minutes / (60 * 24 * 7)).toString() + " 周前"
    }
}

/** 供需要显式字族的地方复用（如「Aura」字标固定 Playfair）。 */
@Composable
fun rememberSerifFor(text: String): FontFamily = editorialFamily(text)