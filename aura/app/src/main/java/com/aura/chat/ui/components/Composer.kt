package com.aura.chat.ui.components

import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.window.Popup
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aura.chat.ui.theme.LocalAuraAccent
import com.aura.chat.ui.theme.editorialFamily

/**
 * 输入区：实心输入胶囊 + HorizontalFloatingToolbar（Expressive 组件）。
 * 对应截图里的 pill 输入框（Make me a fit …）与悬浮按钮组。
 */
@Composable
fun ChatComposer(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    onPickModel: () -> Unit,
    streaming: Boolean,
    modelLabel: String,
    webSearchOn: Boolean,
    onToggleWebSearch: () -> Unit,
    reasoningLevel: Int,
    onChangeReasoning: (Int) -> Unit,
    onReasoningPanelToggle: (Boolean) -> Unit = {},
    onBoltBounds: (androidx.compose.ui.geometry.Rect) -> Unit = {},
    attachmentName: String?,
    uploading: Boolean = false,
    onAttach: () -> Unit,
    onClearAttachment: () -> Unit,
    onUnavailable: (String) -> Unit,
    dark: Boolean,
    modifier: Modifier = Modifier,
) {
    val accent = LocalAuraAccent.current
    val chat = LocalChatColors.current
    var trayWidth by remember { mutableStateOf(160.dp) }
    var barHeight by remember { mutableStateOf(56.dp) }
    val density = LocalDensity.current
    Column(modifier.fillMaxWidth()) {
        androidx.compose.animation.AnimatedVisibility(
            visible = uploading,
            enter = androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(180)) +
                androidx.compose.animation.expandVertically(androidx.compose.animation.core.tween(220)),
            exit = androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(140)) +
                androidx.compose.animation.shrinkVertically(androidx.compose.animation.core.tween(180)),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "正在上传并解析…",
                    style = MaterialTheme.typography.labelMedium,
                    color = chat.muted,
                )
                Spacer(Modifier.width(10.dp))
                LinearProgressIndicator(
                    modifier = Modifier.weight(1f).height(3.dp),
                    color = accent.deep,
                    trackColor = chat.surfaceAlt,
                )
            }
        }
        androidx.compose.animation.AnimatedVisibility(
            visible = attachmentName != null,
            enter = androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(180)) +
                androidx.compose.animation.expandVertically(androidx.compose.animation.core.tween(220)),
            exit = androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(140)) +
                androidx.compose.animation.shrinkVertically(androidx.compose.animation.core.tween(180)),
        ) {
            Row(
                modifier = Modifier.padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(shape = CircleShape, color = accent.chip, contentColor = chat.onSurfaceControl) {
                    Row(
                        modifier = Modifier.padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = attachmentName.orEmpty(),
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.width(6.dp))
                        AuraIconButton(
                            onClick = onClearAttachment,
                            container = chat.onSurfaceControl.copy(alpha = 0.12f),
                            content = chat.onSurfaceControl,
                            size = 30.dp,
                            contentDescription = "移除附件",
                        ) { m, c -> CloseGlyph(m, c) }
                    }
                }
            }
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(30.dp),
            color = chat.surface,
            contentColor = chat.text,
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .height(barHeight)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 24.dp, max = 132.dp),
                    textStyle = TextStyle(
                        color = chat.text,
                        fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                        lineHeight = MaterialTheme.typography.bodyLarge.lineHeight,
                    ),
                    cursorBrush = SolidColor(chat.highlight),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                    // 占位与真实文本共用同一装饰盒，保证同一基线/同一左边距
                    decorationBox = { innerTextField ->
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                            if (value.isEmpty()) {
                                Text(
                                    text = "问点什么，或试试「把播客剪成三句话」",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = chat.muted.copy(alpha = 0.75f),
                                )
                            }
                            innerTextField()
                        }
                    },
                )
            }
        }
        Spacer(Modifier.padding(top = 12.dp))
        // 输入胶囊与发送钮分离：胶囊承载模型与工具，发送钮独立，两者垂直居中
        BoxWithConstraints(Modifier.fillMaxWidth()) {
        val maxPillWidth = (maxWidth - trayWidth - 52.dp - 20.dp).coerceAtLeast(56.dp)
        Row(
            modifier = Modifier.fillMaxWidth().onSizeChanged { barHeight = with(density) { it.height.toDp() } },
            verticalAlignment = Alignment.CenterVertically,
        ) {
        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(50),
            color = if (dark) Color.White.copy(alpha = 0.08f) else Color.White,
            contentColor = chat.text,
        ) {
            Row(
                modifier = Modifier.padding(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 独立组件：内容自适应；最多占一半空间，离底托至少 6dp
                ModelPill(
                    text = modelLabel,
                    onClick = onPickModel,
                    modifier = Modifier.widthIn(max = maxPillWidth),
                )
                Spacer(Modifier.weight(1f))
                // 三个动作共用一块浅色底托，视觉上成组
                Surface(
                    modifier = Modifier.onSizeChanged { trayWidth = with(density) { it.width.toDp() } },
                    shape = RoundedCornerShape(50),
                    color = if (dark) Color.White.copy(alpha = 0.06f) else accent.chip.copy(alpha = 0.55f),
                ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                AuraIconButton(
                    onClick = onAttach,
                    content = chat.onSurfaceControl,
                    size = 44.dp,
                    contentDescription = "添加附件",
                ) { m, c -> PlusGlyph(m, c) }
                ReasoningLevelControl(
                    level = reasoningLevel,
                    onOpenChange = onReasoningPanelToggle,
                    onChange = onChangeReasoning,
                    onBounds = onBoltBounds,
                )
                AuraIconButton(
                    onClick = onToggleWebSearch,
                    container = when {
                        !webSearchOn -> accent.chip
                        dark -> accent.lemon
                        else -> accent.deep
                    },
                    content = when {
                        !webSearchOn -> chat.onSurfaceControl
                        dark -> accent.onLemon
                        else -> accent.lemon
                    },
                    size = 44.dp,
                    contentDescription = if (webSearchOn) "关闭联网检索" else "打开联网检索",
                ) { m, c -> GlobeGlyph(m, c) }
                }
                }
            }
        }
        Spacer(Modifier.width(10.dp))
        SendButton(
            streaming = streaming,
            enabled = value.isNotBlank() || streaming,
            onClick = if (streaming) onStop else onSend,
            dark = dark,
        )
        }
        }
    }
}

@Composable
private fun SendButton(streaming: Boolean, enabled: Boolean, onClick: () -> Unit, dark: Boolean) {
    val accent = LocalAuraAccent.current
    val chat = LocalChatColors.current
    Surface(
        onClick = onClick,
        modifier = Modifier
            .size(52.dp)
            .semantics { contentDescription = if (streaming) "停止生成" else "发送" },
        enabled = enabled,
        shape = CircleShape,
        color = when {
            streaming -> chat.surfaceAlt
            dark -> accent.lemon
            else -> accent.deep
        },
        contentColor = when {
            streaming -> accent.lemon
            dark -> accent.onLemon
            else -> accent.lemon
        },
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (streaming) {
                StopGlyph(Modifier.size(16.dp), accent.lemon)
            } else {
                ArrowUpGlyph(Modifier.size(20.dp), if (dark) accent.onLemon else accent.lemon)
            }
        }
    }
}

/** 首页底部常驻的新对话入口：整颗胶囊都可点。 */
@Composable
fun NewChatDock(onNewChat: () -> Unit, modifier: Modifier = Modifier) {
    val accent = LocalAuraAccent.current
    Surface(
        onClick = onNewChat,
        modifier = modifier,
        shape = CircleShape,
        color = accent.deep,
        contentColor = accent.onDeep,
        shadowElevation = 16.dp,
    ) {
        Row(
            modifier = Modifier.padding(start = 22.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "新的对话",
                style = MaterialTheme.typography.labelLarge,
                color = accent.onDeep,
                maxLines = 1,
                softWrap = false,
            )
            Spacer(Modifier.width(14.dp))
            Surface(
                modifier = Modifier.size(46.dp),
                shape = CircleShape,
                color = accent.lemon,
                contentColor = accent.onLemon,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    PlusGlyph(Modifier.size(18.dp), accent.onLemon)
                }
            }
        }
    }
}

/** 深色页顶栏：纯色块返回钮 + 衬线标题 + 模型副标。 */
@Composable
fun DarkTopBar(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    showBack: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
) {
    val chat = LocalChatColors.current
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showBack) {
            AuraIconButton(
                onClick = onBack,
                container = chat.surfaceAlt,
                content = chat.text,
                size = 46.dp,
                contentDescription = "返回",
            ) { m, c -> ArrowLeftGlyph(m, c) }
            Spacer(Modifier.width(14.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontFamily = editorialFamily(title),
                color = chat.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = chat.muted,
                maxLines = 1,
            )
        }
        trailing?.invoke()
    }
}

/** 浅色页顶栏：同样只用实心色块。 */
@Composable
fun LightTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = LocalAuraAccent.current
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AuraIconButton(
            onClick = onBack,
            container = accent.chip,
            content = accent.onChip,
            size = 46.dp,
            contentDescription = "返回",
        ) { m, c -> ArrowLeftGlyph(m, c) }
        Spacer(Modifier.width(14.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontFamily = editorialFamily(title),
            color = accent.text,
        )
    }
}
/** 推理等级按钮：Popup 真正锚定在按钮上方，外层整页模糊不影响 Popup 窗口。 */
@Composable
private fun ReasoningLevelControl(
    level: Int,
    onOpenChange: (Boolean) -> Unit,
    onChange: (Int) -> Unit,
    onBounds: (androidx.compose.ui.geometry.Rect) -> Unit,
) {
    val accent = LocalAuraAccent.current
    val chat = LocalChatColors.current
    var open by remember { mutableStateOf(false) }
    val label = reasoningLabels[level.coerceIn(0, 3)]
    val density = LocalDensity.current
    Box(Modifier.onGloballyPositioned { onBounds(it.boundsInWindow()) }) {
        AuraIconButton(
            onClick = { open = true; onOpenChange(true) },
            container = accent.chip,
            content = chat.onSurfaceControl,
            size = 44.dp,
            contentDescription = "推理等级：" + label,
        ) { m, cb ->
            Icon(
                imageVector = Icons.Rounded.Bolt,
                contentDescription = null,
                modifier = m.size(20.dp),
                tint = cb,
            )
        }
        if (open) {
            Popup(
                alignment = Alignment.TopCenter,
                offset = androidx.compose.ui.unit.IntOffset(0, -with(density) { 172.dp.roundToPx() }),
                onDismissRequest = { open = false; onOpenChange(false) },
                properties = androidx.compose.ui.window.PopupProperties(focusable = true),
            ) {
                ReasoningPanel(
                    level = level,
                    onLevelChange = { onChange(it) },
                )
            }
        }
    }
}

private val reasoningLabels = listOf("OFF", "LOW", "HIGH", "MAX")
/** 推理等级面板：宽圆角矩形 + 四级滑块，文本颜色随等级变化。 */
@Composable
fun ReasoningPanel(level: Int, onLevelChange: (Int) -> Unit) {
    val accent = LocalAuraAccent.current
    val chat = LocalChatColors.current
    val label = reasoningLabels[level.coerceIn(0, 3)]
    val labelColor = when (level) {
        0 -> chat.muted
        1 -> accent.deep.copy(alpha = 0.55f)
        2 -> accent.deep.copy(alpha = 0.8f)
        else -> accent.deep
    }
    Surface(
        modifier = Modifier.width(268.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        shadowElevation = 0.dp,
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.headlineSmall,
                fontFamily = editorialFamily(label),
                color = labelColor,
            )
            Spacer(Modifier.height(10.dp))
            androidx.compose.material3.Slider(
                value = level.toFloat(),
                onValueChange = { onLevelChange(it.coerceIn(0f, 3f).toInt()) },
                valueRange = 0f..3f,
                steps = 2,
                colors = androidx.compose.material3.SliderDefaults.colors(
                    thumbColor = accent.deep,
                    activeTrackColor = accent.deep,
                    inactiveTrackColor = accent.chip,
                ),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                reasoningLabels.forEach { item ->
                    Text(
                        text = item,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (item == label) labelColor else chat.muted.copy(alpha = 0.6f),
                    )
                }
            }
        }
    }
}