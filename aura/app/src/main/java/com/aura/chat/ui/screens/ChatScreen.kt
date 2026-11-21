package com.aura.chat.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aura.chat.data.AppSettings
import com.aura.chat.data.ChatMessage
import com.aura.chat.data.Conversation
import com.aura.chat.data.MessageStatus
import com.aura.chat.data.Models
import com.aura.chat.data.Role
import com.aura.chat.ui.components.ArrowUpGlyph
import com.aura.chat.ui.components.AssistantBlock
import com.aura.chat.ui.components.ChatComposer
import com.aura.chat.ui.components.DarkTopBar
import com.aura.chat.ui.components.Eyebrow
import com.aura.chat.ui.components.InkField
import com.aura.chat.ui.components.LavenderField
import com.aura.chat.ui.components.LocalChatColors
import com.aura.chat.ui.components.UserBubble
import com.aura.chat.ui.components.chatColors
import com.aura.chat.ui.theme.LocalAuraAccent

private val quickPrompts = listOf(
    "帮我写一条音乐之夜邀请，像独立杂志那样",
    "把这周的播客剪成三句话，最后给我一个问题",
    "Make me a fit that's athleisure but elevated.",
)

@Composable
fun ChatScreen(
    conversation: Conversation,
    dark: Boolean,
    settings: AppSettings,
    streaming: Boolean,
    onSend: (String) -> Unit,
    onStop: () -> Unit,
    onBack: () -> Unit,
    onPickModel: () -> Unit,
    onCopy: (String) -> Unit,
    onRegenerate: () -> Unit,
    onToggleWebSearch: () -> Unit,
    onConsentTools: () -> Unit,
    onToggleDeepThinking: () -> Unit,
    attachmentName: String?,
    uploading: Boolean = false,
    onAttach: () -> Unit,
    onClearAttachment: () -> Unit,
    twoPane: Boolean = false,
    modifier: Modifier = Modifier,
) {
    var showToolsConsent by remember { mutableStateOf(false) }
    val accent = LocalAuraAccent.current
    val colors = chatColors(dark, accent, MaterialTheme.colorScheme)
    var draft by remember(conversation.id) { mutableStateOf("") }
    val listState = rememberLazyListState()
    val messages = conversation.messages
    val lastMessageId = messages.lastOrNull()?.id

    LaunchedEffect(lastMessageId, messages.lastOrNull()?.text?.length) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    CompositionLocalProvider(LocalChatColors provides colors) {
        Box(modifier.fillMaxSize().background(colors.background)) {
            if (dark) InkField() else LavenderField()
            Column(Modifier.fillMaxSize().statusBarsPadding()) {
                DarkTopBar(
                    showBack = !twoPane,
                    title = conversation.title,
                    subtitle = Models.of(conversation.model).label +
                        if (settings.demoMode || !settings.hasKey) " · 演示" else " · 已连接",
                    onBack = onBack,
                )
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxHeight().widthIn(max = 640.dp),
                        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        if (messages.isEmpty()) {
                            item(key = "welcome") {
                                ChatWelcome(onPrompt = { prompt -> onSend(prompt) })
                            }
                        } else {
                            items(messages, key = { it.id }) { message ->
                                MessageRow(
                                    message = message,
                                    streaming = streaming && message.id == lastMessageId,
                                    onCopy = onCopy,
                                    onRegenerate = onRegenerate,
                                )
                            }
                        }
                    }
                }
                Box(Modifier.fillMaxWidth().background(colors.background)) {
                    Column(
                        Modifier
                            .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 12.dp)
                            .imePadding()
                            .navigationBarsPadding()
                    ) {
                        ChatComposer(
                            value = draft,
                            onValueChange = { draft = it },
                            onSend = {
                                val text = draft
                                draft = ""
                                onSend(text)
                            },
                            onStop = onStop,
                            onPickModel = onPickModel,
                            streaming = streaming,
                            modelLabel = Models.of(conversation.model).shortLabel,
                            webSearchOn = settings.webSearchOn,
                            onToggleWebSearch = {
                            if (settings.webSearchOn) {
                                onToggleWebSearch()
                            } else if (!settings.toolsConsent) {
                                showToolsConsent = true
                            } else {
                                onToggleWebSearch()
                            }
                        },
                        deepThinkingOn = settings.deepThinkingOn,
                        onToggleDeepThinking = onToggleDeepThinking,
                            attachmentName = attachmentName,
                        uploading = uploading,
                            onAttach = onAttach,
                            onClearAttachment = onClearAttachment,
                            onUnavailable = { feature -> onCopy(feature) },
                            dark = dark,
                        )
                    }
                }
            }

            if (showToolsConsent) {
                AlertDialog(
                    onDismissRequest = { showToolsConsent = false },
                    title = { Text("开启官方联网工具？", style = MaterialTheme.typography.headlineSmall) },
                    text = {
                        Text(
                            "联网检索由 DeepSeek 服务端完成，模型会切换到 deepseek-flash；" +
                                "搜索、阅读链接与文件解析按官方价格额外计费。",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            showToolsConsent = false
                            onConsentTools()
                            onToggleWebSearch()
                        }) { Text("同意并开启") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showToolsConsent = false }) { Text("取消") }
                    },
                    shape = RoundedCornerShape(30.dp),
                    containerColor = MaterialTheme.colorScheme.surface,
                )
            }
        }
    }
}

@Composable
private fun MessageRow(
    message: ChatMessage,
    streaming: Boolean,
    onCopy: (String) -> Unit,
    onRegenerate: () -> Unit,
) {
    when (message.role) {
        Role.USER -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            UserBubble(text = message.text)
        }
        Role.ASSISTANT -> AssistantBlock(
            message = message,
            streaming = streaming && message.status == MessageStatus.STREAMING,
            onCopy = { onCopy(message.text) },
            onRegenerate = if (streaming) null else onRegenerate,
        )
    }
}

@Composable
private fun ChatWelcome(onPrompt: (String) -> Unit) {
    val colors = LocalChatColors.current
    Column(Modifier.fillMaxWidth().padding(top = 28.dp)) {
        Eyebrow("Aura · 新的对话", color = colors.highlight)
        Spacer(Modifier.height(12.dp))
        Text(
            text = "现在想聊什么？",
            style = MaterialTheme.typography.displaySmall,
            color = colors.text,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Aura 会先给结论，再给理由。需要思考的模型会先把思路摊开给你看。",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.muted,
            modifier = Modifier.width(300.dp),
        )
        Spacer(Modifier.height(24.dp))
        quickPrompts.forEach { prompt ->
            Surface(
                onClick = { onPrompt(prompt) },
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                shape = RoundedCornerShape(20.dp),
                color = colors.surface,
                contentColor = colors.text,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = prompt,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(10.dp))
                    ArrowUpGlyph(Modifier.size(18.dp), colors.highlight)
                }
            }
        }
    }
}