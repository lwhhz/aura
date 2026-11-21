package com.aura.chat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.chat.ui.theme.LocalAuraAccent

/** 小号全大写眉标（编辑风排版里的 kicker）。 */
@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier, color: Color = LocalAuraAccent.current.muted) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.8.sp,
        ),
        color = color,
    )
}

/** 区块标题：衬线 + 右侧可选动作。 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontFamily = com.aura.chat.ui.theme.editorialFamily(title),
            color = MaterialTheme.colorScheme.onBackground,
        )
        trailing?.invoke()
    }
}

/** 细分割线：低对比，替代卡片堆叠。 */
@Composable
fun Hairline(modifier: Modifier = Modifier, color: Color = LocalAuraAccent.current.muted.copy(alpha = 0.22f)) {
    androidx.compose.foundation.layout.Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(color)
    )
}

/** 页头：眉标 + 大号衬线标题 + 一句说明。 */
@Composable
fun EditorialHeader(
    eyebrow: String,
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    titleColor: Color = MaterialTheme.colorScheme.onBackground,
) {
    Column(modifier.fillMaxWidth()) {
        Eyebrow(eyebrow)
        Spacer(Modifier.height(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.displayMedium,
            fontFamily = com.aura.chat.ui.theme.editorialFamily(title),
            color = titleColor,
        )
        if (subtitle != null) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = LocalAuraAccent.current.muted,
                modifier = Modifier.width(300.dp),
            )
        }
    }
}