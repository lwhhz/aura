package com.aura.chat.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import com.aura.chat.ui.theme.AuraComponentShapes
import com.aura.chat.ui.theme.AuraShapeTokens
import com.aura.chat.ui.theme.LocalAuraAccent

/** DSH 详情页输入栏：附件暂存、逐项移除、文本 + 多文件一次发送。 */
@Composable
fun DshComposer(
    draft: String,
    onDraftChange: (String) -> Unit,
    pendingFiles: List<Pair<String, ByteArray>>,
    onRemoveFile: (Int) -> Unit,
    attachError: String?,
    sendError: String?,
    sending: Boolean,
    uploadProgress: String?,
    onPick: () -> Unit,
    onSend: (String, List<Pair<String, ByteArray>>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = LocalAuraAccent.current
    val keyboard = LocalSoftwareKeyboardController.current
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    LaunchedEffect(focused) { if (focused) keyboard?.show() }

    androidx.compose.foundation.layout.Column(modifier.fillMaxWidth()) {
        if (sendError != null) {
            Text(
                "发送失败：" + sendError,
                Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.labelSmall,
                color = accent.muted,
            )
        }
        if (attachError != null) {
            Text(
                attachError,
                Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.labelSmall,
                color = accent.muted,
            )
        }
        if (pendingFiles.isNotEmpty()) {
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    Text(
                        text = pendingFiles.size.toString() + " 个待发送",
                        style = MaterialTheme.typography.labelSmall,
                        color = accent.muted,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
                items(pendingFiles.size) { index ->
                    val (name, bytes) = pendingFiles[index]
                    Surface(shape = AuraShapeTokens.small, color = accent.chip) {
                        Row(
                            modifier = Modifier.padding(start = 8.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = name.take(14) + " · " + (bytes.size / 1024) + "KB",
                                style = MaterialTheme.typography.labelSmall,
                                color = accent.onChip,
                                maxLines = 1,
                            )
                            Text(
                                text = "  ×",
                                modifier = Modifier.clickable { onRemoveFile(index) },
                                style = MaterialTheme.typography.labelMedium,
                                color = accent.onChip,
                            )
                        }
                    }
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onPick, enabled = !sending) {
                Icon(Icons.Rounded.AttachFile, contentDescription = "上传文件", tint = accent.muted)
            }
            TextField(
                value = draft,
                onValueChange = onDraftChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text(uploadProgress ?: if (sending) "正在发送…" else "发消息给这台电脑", style = MaterialTheme.typography.bodyMedium) },
                shape = AuraComponentShapes.input,
                interactionSource = interaction,
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
                onClick = { if (!sending) onSend(draft, pendingFiles) },
                style = PillStyle.Deep,
            )
        }
    }
}
