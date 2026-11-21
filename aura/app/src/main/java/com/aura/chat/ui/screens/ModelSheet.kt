package com.aura.chat.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aura.chat.data.ModelInfo
import com.aura.chat.data.Models
import com.aura.chat.ui.components.Eyebrow
import com.aura.chat.ui.theme.LocalAuraAccent
import com.aura.chat.ui.theme.editorialFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelSheet(
    models: List<ModelInfo>,
    current: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val accent = LocalAuraAccent.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    // 选完先播收起动画，再真正移除弹层（知识库 bottom-sheet：允许「点选菜单项」作为关闭方式）
    fun hideThenDismiss() {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) onDismiss()
        }
    }
    ModalBottomSheet(
        onDismissRequest = { hideThenDismiss() },
        sheetState = sheetState,
        containerColor = accent.ink,
        contentColor = accent.onInk,
        scrimColor = Color.Black.copy(alpha = 0.5f),
        shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp),
        dragHandle = null,
    ) {
        Column(Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, top = 26.dp, bottom = 30.dp)) {
            Eyebrow("选择模型", color = accent.lemon)
            Spacer(Modifier.height(10.dp))
            Text(
                text = "谁来回答？",
                style = MaterialTheme.typography.headlineLarge,
                fontFamily = editorialFamily("谁来回答？"),
                color = accent.onInk,
            )
            Spacer(Modifier.height(20.dp))
            models.forEach { model ->
                val selected = model.id == current
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            when {
                                selected -> accent.lemon
                                else -> Color.White.copy(alpha = 0.07f)
                            },
                            RoundedCornerShape(26.dp),
                        )
                        .clickable {
                            onSelect(model.id)
                            hideThenDismiss()
                        }
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(44.dp)
                            .background(
                                if (selected) accent.onLemon else accent.deep,
                                RoundedCornerShape(16.dp),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = when {
                                model.id == Models.DEEPSEEK_FLASH.id -> Icons.Rounded.Bolt
                                model.reasoning -> Icons.Rounded.Psychology
                                else -> Icons.Rounded.ChatBubble
                            },
                            contentDescription = null,
                            tint = if (selected) accent.lemon else accent.onDeep,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = model.label,
                            style = MaterialTheme.typography.titleLarge,
                            fontFamily = editorialFamily(model.label),
                            color = if (selected) accent.onLemon else accent.onInk,
                        )
                        Text(
                            text = model.tagline,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (selected) accent.onLemon.copy(alpha = 0.7f) else accent.onInkMuted,
                        )
                    }
                    Box(
                        Modifier
                            .size(22.dp)
                            .background(
                                if (selected) accent.onLemon else Color.White.copy(alpha = 0.2f),
                                CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (selected) {
                            Box(Modifier.size(9.dp).background(accent.lemon, CircleShape))
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "大肥鱼牛比",
                style = MaterialTheme.typography.bodySmall,
                color = accent.onInkMuted,
            )
        }
    }
}