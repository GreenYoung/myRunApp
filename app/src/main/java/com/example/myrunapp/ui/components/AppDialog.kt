package com.example.myrunapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.myrunapp.ui.theme.Accent
import com.example.myrunapp.ui.theme.AppSecondaryText

private val DialogTop = Color(0xFF18222A)
private val DialogBottom = Color(0xFF0D1318)
private val DialogHairline = Color(0x14FFFFFF)
private val DialogFieldBackground = Color(0x0DFFFFFF)
private val DialogErrorColor = Color(0xFFF87171)

/**
 * 全 App 统一的输入类弹窗骨架：渐变卡面 + 标题 + 内容 + 底部两个按钮。
 *
 * 用它替换 Material3 的 AlertDialog，避免默认的紫色描边、28dp 圆角和按钮内边距：
 * 弹窗也应该和首页/详情页的卡片是一套视觉语言。
 *
 * 内部拆成 [AppDialogScaffold]（Dialog 窗口）+ [AppDialogCard]（纯卡片，可在 Android Studio 预览）。
 */
@Composable
fun AppDialogScaffold(
    title: String,
    onDismiss: () -> Unit,
    confirmText: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    cancelText: String = "取消",
    actions: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 26.dp)
                .imePadding()
        ) {
            AppDialogCard(
                title = title,
                onDismiss = onDismiss,
                confirmText = confirmText,
                onConfirm = onConfirm,
                cancelText = cancelText,
                actions = actions,
                content = content
            )
        }
    }
}

/**
 * 弹窗卡面本体，不依赖 Dialog 窗口，因此可以直接放进 @Preview 里看效果。
 *
 * [actions] 为空时渲染默认的「取消 / 确认」两按钮；需要三个按钮时传自定义 Row 内容。
 */
@Composable
fun AppDialogCard(
    title: String,
    onDismiss: () -> Unit,
    confirmText: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    cancelText: String = "取消",
    actions: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(Brush.linearGradient(listOf(DialogTop, DialogBottom)))
            .border(1.dp, DialogHairline, RoundedCornerShape(26.dp))
    ) {
        DialogGlow(modifier = Modifier.matchParentSize())
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 18.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Bold
            )
            content()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (actions != null) {
                    actions()
                } else {
                    AppSecondaryButton(
                        text = cancelText,
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        height = 48.dp,
                        cornerRadius = 14.dp
                    )
                    AppPrimaryButton(
                        text = confirmText,
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        height = 48.dp,
                        cornerRadius = 14.dp
                    )
                }
            }
        }
    }
}

@Composable
private fun DialogGlow(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val center = Offset(size.width * 0.9f, 0f)
        val radius = size.width * 0.75f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Accent.copy(alpha = 0.14f), Color.Transparent),
                center = center,
                radius = radius
            ),
            radius = radius,
            center = center
        )
    }
}

/** 弹窗里的输入项：标题在上、输入框在下，单位固定在右侧。 */
@Composable
fun AppDialogField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    unit: String? = null,
    placeholder: String = "请输入",
    keyboardType: KeyboardType = KeyboardType.Decimal
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            color = AppSecondaryText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(DialogFieldBackground)
                .border(1.dp, DialogHairline, RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFeatureSettings = "tnum"
                ),
                cursorBrush = SolidColor(Accent),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                modifier = Modifier.weight(1f),
                decorationBox = { innerTextField ->
                    Box {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                color = AppSecondaryText.copy(alpha = 0.7f),
                                fontSize = 16.sp
                            )
                        }
                        innerTextField()
                    }
                }
            )
            unit?.let {
                Text(
                    text = it,
                    color = AppSecondaryText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/** 弹窗里的"点开选择"行（日期、时长等），样式与输入框一致。 */
@Composable
fun AppDialogPickerField(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "请选择",
    actionText: String = "选择 ›"
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            color = AppSecondaryText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(DialogFieldBackground)
                .border(1.dp, DialogHairline, RoundedCornerShape(14.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value.ifBlank { placeholder },
                color = if (value.isBlank()) AppSecondaryText else Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Text(
                text = actionText,
                color = Accent,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/** 日期选择行：日期类字段统一放在其它输入项之前。 */
@Composable
fun AppDialogDateField(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AppDialogPickerField(
        label = label,
        value = value,
        onClick = onClick,
        modifier = modifier,
        placeholder = "选择日期"
    )
}

@Composable
fun AppDialogError(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.fillMaxWidth(),
        color = DialogErrorColor,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )
}

/** 弹窗里的正文说明（确认类弹窗用）。 */
@Composable
fun AppDialogMessage(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.fillMaxWidth(),
        color = AppSecondaryText,
        fontSize = 13.sp,
        lineHeight = 19.sp
    )
}
