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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aura.chat.ui.components.AuraPill
import com.aura.chat.ui.components.Eyebrow
import com.aura.chat.ui.components.LavenderField
import com.aura.chat.ui.components.LightTopBar
import com.aura.chat.ui.components.PillStyle
import com.aura.chat.ui.theme.LocalAuraAccent
import com.aura.chat.ui.theme.editorialFamily

/** DSH 远程会话的列表项（当前为本地占位数据，后续接 dsh-api-gateway）。 */
data class DshSession(
    val host: String,
    val title: String,
    val status: DshStatus,
    val model: String,
    val lastActive: String,
    val toolCalls: Int,
)

enum class DshStatus(val label: String) { RUNNING("运行中"), IDLE("空闲"), OFFLINE("离线") }

private val demoSessions = listOf(
    DshSession("DESKTOP-01", "重构 Aura 的会话存储层", DshStatus.RUNNING, "deepseek-flash", "刚刚", 12),
    DshSession("DESKTOP-01", "整理 Android-MD3 知识库大纲", DshStatus.IDLE, "deepseek-flash", "18 分钟前", 4),
    DshSession("NB-14", "跑一遍 ui-review-checklist", DshStatus.OFFLINE, "deepseek-flash", "昨天", 0),
)

@Composable
fun DshSessionsScreen(
    onBack: () -> Unit,
    peer: com.aura.chat.data.DshPeer? = null,
    onOpenSession: (DshSession) -> Unit,
    onPair: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = LocalAuraAccent.current
    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LavenderField()
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            LightTopBar(title = "DSH 实时对话", onBack = onBack)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (peer != null) {
                    item(key = "peer") {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            color = accent.lemon,
                            contentColor = accent.onLemon,
                        ) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("已配对", style = MaterialTheme.typography.labelMedium, color = accent.onLemon)
                                    Text(
                                        peer.host + ":" + peer.port,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = accent.onLemon,
                                    )
                                }
                                Text("token 已保存", style = MaterialTheme.typography.labelSmall, color = accent.onLemon.copy(alpha = 0.8f))
                            }
                        }
                    }
                }
                item(key = "header") {
                    Column(Modifier.fillMaxWidth()) {
                        Eyebrow("远程 · 本机 DSH", color = accent.onChip)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "电脑上正在跑什么",
                            style = MaterialTheme.typography.headlineMedium,
                            fontFamily = editorialFamily("电脑上正在跑什么"),
                            color = accent.text,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "这里显示已配对电脑上的 DSH 会话；点开可以看实时输出、发指令、上传文件。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = accent.muted,
                        )
                    }
                }
                items(demoSessions.size) { index ->
                    val session = demoSessions[index]
                    DshSessionCard(session = session, onClick = { onOpenSession(session) })
                }
                item(key = "pair") {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        color = Color.Transparent,
                    ) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = "还没有配对新电脑？",
                                style = MaterialTheme.typography.bodySmall,
                                color = accent.muted,
                            )
                            Spacer(Modifier.height(10.dp))
                            AuraPill(text = "配对新电脑", onClick = onPair, style = PillStyle.Deep)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DshSessionCard(session: DshSession, onClick: () -> Unit) {
    val accent = LocalAuraAccent.current
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = accent.card,
        contentColor = accent.text,
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(52.dp).background(accent.chip, RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Computer,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = accent.onChip,
                )
            }
            Spacer(Modifier.size(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .background(
                                when (session.status) {
                                    DshStatus.RUNNING -> Color(0xFF2FBF71)
                                    DshStatus.IDLE -> accent.lemon
                                    DshStatus.OFFLINE -> accent.muted.copy(alpha = 0.5f)
                                },
                                CircleShape,
                            )
                    )
                    Spacer(Modifier.size(6.dp))
                    Text(
                        text = session.status.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = accent.muted,
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        text = session.host,
                        style = MaterialTheme.typography.labelSmall,
                        color = accent.muted,
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = session.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = editorialFamily(session.title),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = accent.text,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = session.model + " · " + session.toolCalls + " 次工具调用 · " + session.lastActive,
                    style = MaterialTheme.typography.labelSmall,
                    color = accent.muted,
                )
            }
        }
    }
}