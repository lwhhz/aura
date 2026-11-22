package com.aura.chat.ui.components

import com.aura.chat.ui.theme.AuraShapeTokens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.chat.ui.theme.LocalAuraAccent

/** 纯色块风格：没有描边，全部靠填充色和对比度分层。 */
enum class PillStyle { Lemon, Deep, Soft, OnDark }

/**
 * 胶囊主操作。一屏只允许一个 [PillStyle.Lemon]（知识库 B2：同级强调只能有一个）。
 */
@Composable
fun AuraPill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: PillStyle = PillStyle.Lemon,
    enabled: Boolean = true,
    leading: (@Composable () -> Unit)? = null,
) {
    val accent = LocalAuraAccent.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, label = "pillScale")

    val (bg, fg) = when (style) {
        PillStyle.Lemon -> accent.lemon to accent.onLemon
        PillStyle.Deep -> accent.deep to accent.onDeep
        PillStyle.Soft -> accent.chip to accent.onChip
        PillStyle.OnDark -> Color.White.copy(alpha = 0.12f) to accent.onInk
    }

    Surface(
        onClick = onClick,
        modifier = modifier.scale(scale).alpha(if (enabled) 1f else 0.45f),
        enabled = enabled,
        shape = CircleShape,
        color = bg,
        contentColor = fg,
        border = null,
        interactionSource = interaction,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.height(54.dp).padding(horizontal = 26.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (leading != null) {
                leading()
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                letterSpacing = 0.3.sp,
            )
        }
    }
}

/** 圆形图标按钮：视觉 44dp，触控区 48dp（知识库：最小触控目标 48dp）。 */
@Composable
fun AuraIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    container: Color = Color.Transparent,
    content: Color = LocalContentColor.current,
    size: Dp = 44.dp,
    enabled: Boolean = true,
    contentDescription: String? = null,
    glyph: @Composable (Modifier, Color) -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .size(size)
            // 知识库 ui-review-checklist：可交互元素触控目标 ≥ 48dp；视觉 44dp 时用最小触控尺寸补足
            .minimumInteractiveComponentSize()
            .semantics {
                role = Role.Button
                contentDescription?.let { this.contentDescription = it }
            },
        enabled = enabled,
        shape = CircleShape,
        color = container,
        contentColor = content,
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(10.dp)) {
            glyph(Modifier.size(size - 18.dp), content)
        }
    }
}

/** 小号纯色标签（模型名、来源、状态）。 */
@Composable
fun AuraTag(
    text: String,
    modifier: Modifier = Modifier,
    container: Color = LocalAuraAccent.current.chip,
    content: Color = LocalAuraAccent.current.onChip,
    dot: Color? = null,
) {
    Surface(
        modifier = modifier,
        shape = AuraShapeTokens.full,
        color = container,
        contentColor = content,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (dot != null) {
                Box(Modifier.size(6.dp).background(dot, CircleShape))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                letterSpacing = 0.2.sp,
                maxLines = 1,
                softWrap = false,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
        }
    }
}



/**
 * 三段连接式单选：用 Expressive ToggleButton + connected shapes。
 * 选中时按组件自带的 checked shape 变形为胶囊（不再用自定义 Surface，避免丢失形变）。
 */
@Composable
fun ConnectedChoiceRow(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    unselectedContainer: Color? = null,
    unselectedContent: Color? = null,
) {
    val accent = LocalAuraAccent.current
    val colors = ToggleButtonDefaults.colors(
        containerColor = unselectedContainer ?: accent.chip,
        contentColor = unselectedContent ?: accent.onChip,
        checkedContainerColor = accent.lemon,
        checkedContentColor = accent.onLemon,
    )
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        options.forEachIndexed { index, label ->
            val shapes = when {
                options.size == 1 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                index == 0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                index == options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
            }
            ToggleButton(
                checked = index == selectedIndex,
                onCheckedChange = { checked -> if (checked) onSelect(index) },
                shapes = shapes,
                colors = colors,
                modifier = Modifier.weight(1f).height(52.dp),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                )
            }
        }
    }
}

/** 空状态 / 说明用的内边距常量。 */
val ScreenHorizontal = PaddingValues(horizontal = 20.dp)
/** 独立的模型选择胶囊：不依赖工具底托的尺寸，内容自适应宽度。 */
@Composable
fun ModelPill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = LocalAuraAccent.current
    Surface(
        onClick = onClick,
        // 高度与工具底托一致：44dp 图标 + 上下各 6dp
        modifier = modifier.height(56.dp),
        shape = CircleShape,
        color = accent.chip,
        contentColor = accent.onChip,
    ) {
        Row(
            modifier = Modifier.height(56.dp).padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(8.dp).background(accent.deep, CircleShape))
            Spacer(Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                fontFamily = com.aura.chat.ui.theme.editorialFamily(text),
                maxLines = 1,
                softWrap = false,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
        }
    }
}