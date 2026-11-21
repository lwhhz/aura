package com.aura.chat.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.aura.chat.data.AppSettings
import com.aura.chat.data.Models
import com.aura.chat.data.ThemeMode
import com.aura.chat.ui.components.ConnectedChoiceRow
import com.aura.chat.ui.components.Eyebrow
import com.aura.chat.ui.components.LavenderField
import com.aura.chat.ui.components.LightTopBar
import com.aura.chat.ui.theme.LocalAuraAccent
import com.aura.chat.ui.theme.editorialFamily

/** 设置页配色：浅色模式整页用 Brand 紫卡；深色模式用紫黑卡。 */
@Immutable
private data class SettingsPalette(
    val card: Color,
    val onCard: Color,
    val eyebrow: Color,
    val field: Color,
    val onField: Color,
    val muted: Color,
    val danger: Color,
)

private val LocalSettingsPalette = staticCompositionLocalOf {
    SettingsPalette(
        card = Color.White,
        onCard = Color(0xFF15131A),
        eyebrow = Color(0xFF4D348F),
        field = Color(0xFFD4BFFF),
        onField = Color(0xFF4D348F),
        muted = Color(0xFF827B91),
        danger = Color(0xFFC5221F),
    )
}

@Composable
fun SettingsScreen(
    settings: AppSettings,
    dark: Boolean,
    onBack: () -> Unit,
    onChange: ((AppSettings) -> AppSettings) -> Unit,
    onClear: () -> Unit,
    onRestoreDemo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = LocalAuraAccent.current
    val palette = if (dark) {
        SettingsPalette(
            card = accent.card,
            onCard = accent.text,
            eyebrow = accent.onChip,
            field = accent.chip,
            onField = accent.text,
            muted = accent.muted,
            danger = MaterialTheme.colorScheme.error,
        )
    } else {
        SettingsPalette(
            card = accent.deep,
            onCard = accent.onDeep,
            eyebrow = accent.lemon,
            field = Color.White.copy(alpha = 0.16f),
            onField = accent.onDeep,
            muted = accent.onDeep.copy(alpha = 0.72f),
            danger = Color(0xFFFFD9E4),
        )
    }
    var keyVisible by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    var keyDraft by remember { mutableStateOf(settings.apiKey) }
    var urlDraft by remember { mutableStateOf(settings.baseUrl) }

    CompositionLocalProvider(LocalSettingsPalette provides palette) {
        Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            LavenderField()
            Column(
                Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxHeight()
                    .widthIn(max = 600.dp)
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                LightTopBar(title = "设置", onBack = onBack)
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(32.dp),
                            color = accent.deep,
                            contentColor = accent.onDeep,
                        ) {
                            Column(Modifier.padding(22.dp)) {
                                Eyebrow("设置", color = accent.lemon)
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    text = "把这台手机接到\n你自己的模型上",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontFamily = editorialFamily("把这台手机接到你自己的模型上"),
                                    color = accent.onDeep,
                                )
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    text = "浅色是同一支紫的纸面与卡片，深色是它的夜色；对话页随主题切换。",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = accent.onDeep.copy(alpha = 0.78f),
                                )
                            }
                        }
                    }

                    item {
                        SettingsCard(eyebrow = "连接", title = "DeepSeek API") {
                            TextField(
                                value = keyDraft,
                                onValueChange = { value ->
                                    keyDraft = value
                                    onChange { it.copy(apiKey = value.trim()) }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("API Key") },
                                placeholder = { Text("sk-…") },
                                singleLine = true,
                                shape = RoundedCornerShape(20.dp),
                                visualTransformation = if (keyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                trailingIcon = {
                                    TextButton(onClick = { keyVisible = !keyVisible }) {
                                        Text(if (keyVisible) "隐藏" else "显示", color = palette.eyebrow)
                                    }
                                },
                                colors = solidFieldColors(),
                            )
                            Spacer(Modifier.height(12.dp))
                            TextField(
                                value = urlDraft,
                                onValueChange = { value ->
                                    urlDraft = value
                                    onChange { it.copy(baseUrl = value.trim()) }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Base URL") },
                                singleLine = true,
                                shape = RoundedCornerShape(20.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                                colors = solidFieldColors(),
                            )

                            Spacer(Modifier.height(16.dp))
                            ToggleRow(
                                title = "显示操作提示",
                                subtitle = "关闭后不再弹出底部提示条（调试用）。",
                                checked = settings.showNotices,
                                onCheckedChange = { value -> onChange { it.copy(showNotices = value) } },
                            )
                            ToggleRow(
                                title = "演示模式",
                                subtitle = "不联网，使用本机内置回答与引用，方便先看界面。",
                                checked = settings.demoMode,
                                onCheckedChange = { value -> onChange { it.copy(demoMode = value) } },
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = if (settings.hasKey) {
                                    "联网检索与会话页的开关共用；工具调用按官方价格额外计费。"
                                } else "还没有 Key：当前只使用演示数据，不会发出网络请求。",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.muted,
                            )
                        }
                    }

                    item {
                        SettingsCard(eyebrow = "模型", title = "谁来回答") {
                            Models.all.forEach { model ->
                                ModelRow(
                                    label = model.label,
                                    tagline = model.tagline,
                                    modelId = model.id,
                                    reasoning = model.reasoning,
                                    selected = settings.model == model.id,
                                    onClick = { onChange { it.copy(model = model.id) } },
                                )
                            }
                        }
                    }

                    item {
                        SettingsCard(eyebrow = "外观", title = "主题") {
                            ConnectedChoiceRow(
                                options = listOf("浅色", "深色", "跟随系统"),
                                selectedIndex = ThemeMode.entries.indexOf(settings.themeMode),
                                onSelect = { index -> onChange { it.copy(themeMode = ThemeMode.entries[index]) } },
                                unselectedContainer = if (dark) null else Color.White.copy(alpha = 0.16f),
                                unselectedContent = if (dark) null else accent.onDeep,
                            )
                            Spacer(Modifier.height(14.dp))
                            Text(
                                text = "浅色是同一支紫的纸面，深色是墨黑紫；对话页也跟随主题。",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.muted,
                            )
                        }
                    }

                    item {
                        SettingsCard(eyebrow = "数据", title = "本机内容") {
                            DataRow(title = "恢复演示对话", subtitle = "重新写入 4 段示例会话", onClick = onRestoreDemo)
                            DataRow(
                                title = "清空全部对话",
                                subtitle = "不可撤销，仅清本机存储",
                                danger = true,
                                onClick = { confirmClear = true },
                            )
                        }
                    }

                    item {
                        SettingsCard(eyebrow = "关于", title = "Aura 1.0.8") {
                            Text(
                                text = "Aura 是 DeepSeek 官方开源 chat-demo（MIT）的 Android 复刻：界面按 Material 3 Expressive 与薰衣草编辑风重新设计，对话能力接入官方 chat/completions。" +
                                    "联网检索、深度思考与文件解析均为官方服务端工具，调用会按官方价格额外计费。" +
                                    "图标来自 Material Symbols（Apache 2.0），字体 Playfair Display 与 Noto Serif CJK SC 子集（SIL OFL 1.1）。" +
                                    "本应用与 DeepSeek 官方无隶属关系。",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.muted,
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, MaterialTheme.colorScheme.background)
                        )
                    )
            )

            if (confirmClear) {
                AlertDialog(
                    onDismissRequest = { confirmClear = false },
                    title = { Text("清空全部对话？", style = MaterialTheme.typography.headlineSmall, fontFamily = editorialFamily("清空全部对话？")) },
                    text = { Text("只清空本机存储，不可撤销。", style = MaterialTheme.typography.bodyMedium) },
                    confirmButton = {
                        TextButton(onClick = {
                            confirmClear = false
                            onClear()
                        }) { Text("清空", color = MaterialTheme.colorScheme.error) }
                    },
                    dismissButton = {
                        TextButton(onClick = { confirmClear = false }) { Text("取消", color = Color(0xFF827B91)) }
                    },
                    shape = RoundedCornerShape(30.dp),
                    containerColor = Color.White,
                )
            }
        }
    }
}

@Composable
private fun SettingsCard(
    eyebrow: String,
    title: String,
    content: @Composable () -> Unit,
) {
    val palette = LocalSettingsPalette.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        color = palette.card,
        contentColor = palette.onCard,
    ) {
        Column(Modifier.padding(20.dp)) {
            Eyebrow(eyebrow, color = palette.eyebrow)
            Spacer(Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontFamily = editorialFamily(title),
                color = palette.onCard,
            )
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
private fun solidFieldColors() = run {
    val palette = LocalSettingsPalette.current
    TextFieldDefaults.colors(
        focusedContainerColor = palette.field,
        unfocusedContainerColor = palette.field,
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
        disabledIndicatorColor = Color.Transparent,
        cursorColor = palette.eyebrow,
        focusedLabelColor = palette.onField.copy(alpha = 0.85f),
        unfocusedLabelColor = palette.onField.copy(alpha = 0.7f),
        focusedTextColor = palette.onField,
        unfocusedTextColor = palette.onField,
        focusedPlaceholderColor = palette.onField.copy(alpha = 0.55f),
        unfocusedPlaceholderColor = palette.onField.copy(alpha = 0.55f),
    )
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val accent = LocalAuraAccent.current
    val palette = LocalSettingsPalette.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = palette.onCard)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = palette.muted)
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = accent.onLemon,
                checkedTrackColor = accent.lemon,
                checkedBorderColor = Color.Transparent,
                uncheckedThumbColor = palette.onCard.copy(alpha = 0.8f),
                uncheckedTrackColor = palette.field,
                uncheckedBorderColor = Color.Transparent,
            ),
        )
    }
}

@Composable
private fun ModelRow(
    label: String,
    tagline: String,
    modelId: String,
    reasoning: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val accent = LocalAuraAccent.current
    val palette = LocalSettingsPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .background(accent.lemon, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = when {
                    modelId == Models.DEEPSEEK_FLASH.id -> Icons.Rounded.Bolt
                    reasoning -> Icons.Rounded.Psychology
                    else -> Icons.Rounded.ChatBubble
                },
                contentDescription = null,
                tint = accent.onLemon,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleMedium, color = palette.onCard)
            Text(tagline, style = MaterialTheme.typography.bodySmall, color = palette.muted)
        }
        Box(
            Modifier
                .size(24.dp)
                .background(if (selected) accent.lemon else palette.field, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Box(Modifier.size(9.dp).background(palette.card, CircleShape))
            }
        }
    }
}

@Composable
private fun DataRow(
    title: String,
    subtitle: String,
    danger: Boolean = false,
    onClick: () -> Unit,
) {
    val palette = LocalSettingsPalette.current
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = if (danger) palette.danger else palette.onCard,
        )
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = palette.muted)
    }
}