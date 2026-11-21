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
    val count = detail?.messages?.size ?: 0
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
            if (detail != null && detail.messages.isEmpty() && !loading) {
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
                items(detail?.messages?.size ?: 0) { index ->
                    val message = detail!!.messages[index]
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
                            Text(text = message.text, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                item(key = "spacer") { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}
