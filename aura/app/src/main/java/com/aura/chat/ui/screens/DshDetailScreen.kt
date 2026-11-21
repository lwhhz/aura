package com.aura.chat.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aura.chat.data.DshSessionDetail
import com.aura.chat.ui.components.AuraPill
import com.aura.chat.ui.components.Eyebrow
import com.aura.chat.ui.components.LavenderField
import com.aura.chat.ui.components.LocalMarkdownImageAuth
import com.aura.chat.ui.components.LightTopBar
import com.aura.chat.ui.components.MarkdownImageAuth
import com.aura.chat.ui.components.MarkdownText
import com.aura.chat.ui.components.PillStyle
import com.aura.chat.ui.components.WaveProgress
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
    peer: com.aura.chat.data.DshPeer? = null,
    maxTurns: Int = 10,
    maxItems: Int = 20,
    sending: Boolean = false,
    uploadProgress: String? = null,
    sendError: String? = null,
    onSend: (String) -> Unit = {},
    onUploadMany: (List<Pair<String, ByteArray>>, String) -> Unit = { _, _ -> },
) {
    val accent = LocalAuraAccent.current
    val listState = rememberLazyListState()
    var draft by remember { mutableStateOf("") }
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            val files = uris.mapNotNull { uri ->
                val name = queryName(context, uri) ?: "upload.bin"
                val bytes = runCatching { context.contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()
                if (bytes != null && bytes.size <= 20 * 1024 * 1024) name to bytes else null
            }
            if (files.isNotEmpty()) onUploadMany(files, draft)
        }
    }

    val all = detail?.messages ?: emptyList()
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
    }.takeLast(minOf(maxTurns * 2, maxItems))
    val last = all.lastOrNull()
    val processing = last != null && last.role !in setOf("user", "assistant")
    val turnStart = all.indexOfLast { it.role == "user" }
    val steps = if (turnStart >= 0) all.drop(turnStart).count { it.role == "tool" } else 0
    val stepSuffix = if (steps > 0) " · 第 " + steps + " 步" else ""
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

    CompositionLocalProvider(
        LocalMarkdownImageAuth provides peer?.let {
            MarkdownImageAuth("http://" + it.host + ":" + it.port, it.token, detail?.cwd ?: "")
        },
    ) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            LavenderField()
            Column(Modifier.fillMaxSize().statusBarsPadding().imePadding().navigationBarsPadding()) {
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
                if (processing) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = accent.deep.copy(alpha = 0.10f),
                    ) {
                        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            Text(
                                text = "● " + statusText + stepSuffix,
                                style = MaterialTheme.typography.labelMedium,
                                color = accent.muted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.height(6.dp))
                            WaveProgress(Modifier.fillMaxWidth())
                        }
                    }
                }
                if (error != null) {
                    Column(Modifier.padding(horizontal = 20.dp)) {
                        Text("拉取失败：" + error, style = MaterialTheme.typography.bodySmall, color = accent.muted)
                        Spacer(Modifier.height(8.dp))
                        AuraPill(text = "重试", onClick = onRefresh, style = PillStyle.Deep)
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
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(display.size) { index ->
                        val message = display[index]
                        val isUser = message.role == "user"
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            color = if (isUser) accent.chip else accent.card,
                            contentColor = if (isUser) accent.onChip else accent.text,
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(
                                    text = message.role,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isUser) accent.onChip.copy(alpha = 0.7f) else accent.muted,
                                )
                                Spacer(Modifier.height(4.dp))
                                MarkdownText(
                                    text = renderUploads(message.text),
                                    color = if (isUser) accent.onChip else accent.text,
                                )
                            }
                        }
                    }
                    item(key = "spacer") { Spacer(Modifier.height(24.dp)) }
                }
                if (sendError != null) {
                    Text(
                        "发送失败：" + sendError,
                        Modifier.padding(horizontal = 20.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = accent.muted,
                    )
                }
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { picker.launch("*/*") }, enabled = !sending) {
                        Icon(Icons.Rounded.AttachFile, contentDescription = "上传文件", tint = accent.muted)
                    }
                    TextField(
                        value = draft,
                        onValueChange = { draft = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text(uploadProgress ?: if (sending) "正在发送…" else "发消息给这台电脑", style = MaterialTheme.typography.bodyMedium) },
                        shape = RoundedCornerShape(20.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = accent.card,
                            unfocusedContainerColor = accent.card,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                        ),
                        maxLines = 4,
                    )
                    Spacer(Modifier.width(8.dp))
                    AuraPill(
                        text = if (sending) "…" else "发送",
                        onClick = {
                            if (draft.isNotBlank() && !sending) {
                                onSend(draft.trim())
                                draft = ""
                            }
                        },
                        style = PillStyle.Deep,
                    )
                }
            }
        }
    }
}

private fun renderUploads(text: String): String {
    val imageExt = listOf(".png", ".jpg", ".jpeg", ".webp", ".gif")
    return text.split("\n").joinToString("\n") { line ->
        val trimmed = line.trim()
        if (trimmed.startsWith("已上传文件：")) {
            val path = trimmed.removePrefix("已上传文件：").trim()
            if (imageExt.any { path.lowercase().endsWith(it) }) "![已上传图片](" + path + ")" else line
        } else line
    }
}

private fun queryName(context: android.content.Context, uri: android.net.Uri): String? {
    return runCatching {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
        }
    }.getOrNull()
}
