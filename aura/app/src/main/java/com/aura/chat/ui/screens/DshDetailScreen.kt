package com.aura.chat.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aura.chat.data.DshSessionDetail
import com.aura.chat.ui.components.AuraPill
import com.aura.chat.ui.components.Eyebrow
import com.aura.chat.ui.components.LavenderField
import com.aura.chat.ui.components.LightTopBar
import com.aura.chat.ui.components.PillStyle
import com.aura.chat.ui.theme.LocalAuraAccent
import com.aura.chat.ui.theme.editorialFamily

@Composable
fun DshDetailScreen(
    sessionId: String,
    detail: DshSessionDetail?,
    loading: Boolean,
    error: String?,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
) {
    val accent = LocalAuraAccent.current
    val listState = rememberLazyListState()
    val all = detail?.messages ?: emptyList()
    // 只保留：用户输入 + 每轮最终的助手输出
    val display = buildList {
        var pending: com.aura.chat.data.DshDetailMessage? = null
        for (item in all) {
            val injected = item.role == "user" && (
                item.text.startsWith("[Plugin context") ||
                    item.text.startsWith("Time sampled") ||
                    item.text.startsWith("<system-reminder") ||
                    item.text.startsWith("<path>") ||
                    item.text.startsWith("<content>")
                )
            if (injected) continue
            when (item.role) {
                "user" -> {
                    pending?.let { add(it) }
                    pending = null
                    add(item)
                }
                "assistant" -> pending = item
                else -> Unit
            }
        }
        pending?.let { add(it) }
    }.takeLast(20)
    val last = all.lastOrNull()
    val processing = last != null && last.role !in setOf("user", "assistant")
    val statusText = when (last?.role) {
        "tool" -> "正在调用 " + last.name.ifBlank { "工具" } + "…"
        "tool_result" -> "工具已返回，继续处理…"
        "reasoning" -> "思考中…"
        "user" -> "已发送，等待响应…"
        else -> "处理中…"
    }
    val count = display.size
    LaunchedEffect(count) {
        if (count > 0) listState.scrollToItem(count - 1)
    }
    LaunchedEffect(sessionId) {
        while (true) {
            kotlinx.coroutines.delay(3000)
            onRefresh()
        }
    }
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LavenderField()
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            LightTopBar(title = "会话详情", onBack = onBack)
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                Eyebrow("实时输出 · 3 秒刷新", color = accent.onChip)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = detail?.title?.ifBlank { detail.cwd.substringAfterLast("\\") } ?: "读取中…",
                    style = MaterialTheme.typography.headlineSmall,
                    fontFamily = editorialFamily("会话详情"),
                    color = accent.text,
                )
                Text(
                    text = sessionId,
                    style = MaterialTheme.typography.labelSmall,
                    color = accent.muted,
                )
                Spacer(Modifier.height(10.dp))
            }
            if (error != null) {
                Column(Modifier.padding(horizontal = 20.dp)) {
                    Text("拉取失败：" + error, style = MaterialTheme.typography.bodySmall, color = accent.muted)
                    Spacer(Modifier.height(8.dp))
                    AuraPill(text = "重试", onClick = onRefresh, style = PillStyle.Deep)
                }
            }
            if (processing) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = accent.deep.copy(alpha = 0.10f),
                ) {
                    Text(
                        text = "● " + statusText,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = accent.muted,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                }
            }
            if (detail != null && display.isEmpty() && !loading) {
                Text(
                    "这个会话还没有消息。",
                    Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = accent.muted,
                )
            }
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(display.size) { index ->
                    val message = display[index]
                    val isUser = message.role == "user"
                    val isReasoning = message.role == "reasoning"
                    val isTool = message.role == "tool" || message.role == "tool_result"
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(if (isTool) 16.dp else 24.dp),
                        color = when {
                            isUser -> accent.chip
                            isReasoning -> accent.card.copy(alpha = 0.7f)
                            isTool -> accent.deep.copy(alpha = 0.10f)
                            else -> accent.card
                        },
                        contentColor = if (isUser) accent.onChip else accent.text,
                    ) {
                        Column(Modifier.padding(if (isTool) 12.dp else 16.dp)) {
                            Text(
                                text = when {
                                    isReasoning -> "思考"
                                    isTool -> (if (message.role == "tool_result") "结果 · " else "调用 · ") + message.name.ifBlank { "tool" }
                                    else -> message.role
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isUser) accent.onChip.copy(alpha = 0.7f) else accent.muted,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = message.text,
                                style = if (isTool) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                                maxLines = if (isTool || isReasoning) 2 else Int.MAX_VALUE,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                item(key = "spacer") { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}
