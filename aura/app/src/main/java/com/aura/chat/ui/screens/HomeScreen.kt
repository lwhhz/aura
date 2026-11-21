package com.aura.chat.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.aura.chat.data.Conversation
import com.aura.chat.ui.components.DshDock
import com.aura.chat.ui.components.AuraIconButton
import com.aura.chat.ui.components.AuraPill
import com.aura.chat.ui.components.AuraTag
import com.aura.chat.ui.components.ConversationCard
import com.aura.chat.ui.components.EditorialHeader
import com.aura.chat.ui.components.Eyebrow
import com.aura.chat.ui.components.FeaturedConversationCard
import com.aura.chat.ui.components.LavenderField
import com.aura.chat.ui.components.NewChatDock
import com.aura.chat.ui.components.PillStyle
import com.aura.chat.ui.components.SectionHeader
import com.aura.chat.ui.theme.DisplaySerif
import com.aura.chat.ui.theme.editorialFamily
import com.aura.chat.ui.theme.LocalAuraAccent

@Composable
fun HomeScreen(
    featured: Conversation?,
    recent: List<Conversation>,
    demoMode: Boolean,
    onOpen: (String) -> Unit,
    onNewChat: () -> Unit,
    onQuickStart: (String) -> Unit,
    onSettings: () -> Unit,
    onTogglePin: (String) -> Unit,
    onOpenDsh: () -> Unit,
    onDelete: (String) -> Unit,
    selectedConversationId: String? = null,
    contentMaxWidth: androidx.compose.ui.unit.Dp = 560.dp,
    onOpenSuggestion: (String) -> Unit = onOpen,
    modifier: Modifier = Modifier,
) {
    val accent = LocalAuraAccent.current
    var confirmDelete by remember { mutableStateOf<Conversation?>(null) }

    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LavenderField()
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxHeight()
                .widthIn(max = contentMaxWidth),
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 148.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item(key = "header") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Eyebrow("Aura · DeepSeek 对话", color = accent.onChip)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Aura",
                                style = MaterialTheme.typography.displayLarge,
                                fontFamily = DisplaySerif,
                                color = accent.text,
                            )
                        }
                        AuraIconButton(
                            onClick = onSettings,
                            container = accent.chip,
                            content = accent.onChip,
                            size = 50.dp,
                            contentDescription = "设置",
                        ) { m, _ ->
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = null,
                                modifier = m.size(20.dp),
                                tint = accent.onChip,
                            )
                        }
                    }
                }

                item(key = "manifesto") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "慢一点，想清楚再回答。",
                            style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic),
                            color = accent.muted,
                            maxLines = 1,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.size(10.dp))
                        AuraTag(
                            text = if (demoMode) "演示模式" else "已连接",
                            container = accent.lemon,
                            content = accent.onLemon,
                        )
                    }
                }

                if (featured != null) {
                    item(key = "featured") {
                        AnimatedContent(
                            targetState = featured,
                            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
                            label = "featured",
                        ) { conversation ->
                            FeaturedConversationCard(
                                conversation = conversation,
                                onOpen = { onOpen(conversation.id) },
                                onTogglePin = { onTogglePin(conversation.id) },
                            )
                        }
                    }
                }

                item(key = "section") {
                    Spacer(Modifier.height(4.dp))
                    SectionHeader(title = "最近的对话") {
                        Text(
                            text = recent.size.toString() + " 段",
                            style = MaterialTheme.typography.labelMedium,
                            color = accent.muted,
                        )
                    }
                }

                if (recent.isEmpty()) {
                    item(key = "empty") { EmptyHomeCard(onNewChat = onNewChat) }
                } else {
                    items(recent, key = { it.id }) { conversation ->
                        ConversationCard(
                            conversation = conversation,
                            onOpen = { onOpen(conversation.id) },
                            onDelete = { confirmDelete = conversation },
                            selected = conversation.id == selectedConversationId,
                        )
                    }
                }

                item(key = "footer") {
                    Text(
                        text = "内容由 DeepSeek 模型生成 · 演示数据保存在本机",
                        style = MaterialTheme.typography.labelSmall,
                        color = accent.muted,
                        modifier = Modifier.padding(start = 4.dp, top = 6.dp),
                    )
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, MaterialTheme.colorScheme.background)
                        )
                    )
            )

            if (confirmDelete != null) {
                AlertDialog(
                    onDismissRequest = { confirmDelete = null },
                    title = {
                        Text(
                            "删除这段对话？",
                            style = MaterialTheme.typography.headlineSmall,
                            fontFamily = editorialFamily("删除这段对话？"),
                        )
                    },
                    text = { Text("删除后不可恢复。", style = MaterialTheme.typography.bodyMedium) },
                    confirmButton = {
                        TextButton(onClick = {
                            confirmDelete?.let { onDelete(it.id) }
                            confirmDelete = null
                        }) { Text("删除", color = MaterialTheme.colorScheme.error) }
                    },
                    dismissButton = {
                        TextButton(onClick = { confirmDelete = null }) { Text("取消", color = accent.muted) }
                    },
                    shape = RoundedCornerShape(30.dp),
                    containerColor = accent.card,
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 20.dp, bottom = 20.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DshDock(onOpen = onOpenDsh)
                NewChatDock(onNewChat = onNewChat)
            }
        }
    }
}

@Composable
private fun EmptyHomeCard(onNewChat: () -> Unit) {
    val accent = LocalAuraAccent.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        color = accent.card,
    ) {
        Column(Modifier.padding(22.dp)) {
            EditorialHeader(
                eyebrow = "From scratch",
                title = "先从一句话开始",
                subtitle = "写下一个你最近在想的问题，Aura 会先给结论，再给理由。",
            )
            Spacer(Modifier.height(18.dp))
            AuraPill(text = "开始新对话", onClick = onNewChat, style = PillStyle.Deep)
        }
    }
}