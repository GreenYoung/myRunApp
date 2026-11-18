package com.example.myrunapp.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ripple
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role

/**
 * 深色卡片上点击反馈的统一颜色。
 *
 * `Modifier.clickable` 默认的 ripple 取 `LocalContentColor`，而裸 Box 不是 Surface，
 * 取到的是默认黑色 —— 在深色背景上几乎看不见。所有可点击区域请用 [appClickable]，
 * 需要长按时用这个颜色显式构造 `ripple(...)`。
 */
internal val AppPressRippleColor = Color.White.copy(alpha = 0.16f)

/**
 * 带统一按压反馈的 clickable。
 *
 * 用法：`Modifier.appClickable(onClick = ...)`，替换裸 `Modifier.clickable`。
 */
fun Modifier.appClickable(
    enabled: Boolean = true,
    role: Role? = Role.Button,
    onClickLabel: String? = null,
    onClick: () -> Unit
): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    clickable(
        interactionSource = interaction,
        indication = ripple(color = AppPressRippleColor),
        enabled = enabled,
        role = role,
        onClickLabel = onClickLabel,
        onClick = onClick
    )
}
