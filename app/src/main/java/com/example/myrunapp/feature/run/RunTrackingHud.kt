package com.example.myrunapp.feature.run

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myrunapp.ui.components.appClickable
import com.example.myrunapp.ui.components.AppPrimaryButton
import com.example.myrunapp.ui.components.AppSecondaryButton
import com.example.myrunapp.ui.theme.AppSecondaryText
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

private val RunHudCardTop = Color(0xF0111922)
private val RunHudCardBottom = Color(0xF7090E13)
private val RunHudDivider = Color(0x12FFFFFF)
private val RunHudPaused = Color(0xFFF59E0B)
private val RunActionGradientStart = Color(0xFF2FCB6D)
private val RunActionGradientEnd = Color(0xFF0E8A3E)
private val RunHudNumbers = TextStyle(fontFeatureSettings = "tnum")

@Composable
private fun RunDiscardButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        color = if (enabled) Color(0xFFF87171) else Color(0x66F87171),
        fontSize = 13.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Medium,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .appClickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun RecoveredRunActions(
    isSaving: Boolean,
    canSave: Boolean,
    onContinue: () -> Unit,
    onSave: () -> Unit,
    onDiscard: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "发现未完成的跑步记录",
            color = Color.White,
            fontSize = 15.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AppSecondaryButton(
                text = when {
                    isSaving -> "保存中..."
                    canSave -> "保存数据"
                    else -> "无法保存"
                },
                onClick = {
                    if (!isSaving && canSave) onSave()
                },
                modifier = Modifier.weight(1f),
                height = RunActionButtonHeight
            )
            AppPrimaryButton(
                text = "继续跑步",
                onClick = {
                    if (!isSaving) onContinue()
                },
                modifier = Modifier.weight(1f),
                height = RunActionButtonHeight
            )
        }
        RunDiscardButton(
            text = "放弃记录",
            enabled = !isSaving,
            onClick = onDiscard
        )
    }
}

@Composable
internal fun RunningStatsPanel(
    uiState: RunTrackingUiState,
    modifier: Modifier = Modifier
) {
    val pace = formatHudPaceValue(uiState.averagePaceText)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(colors = listOf(RunHudCardTop, RunHudCardBottom))
            )
            .border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(24.dp))
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 18.dp)
    ) {
        RunningPanelHeader(uiState = uiState)
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            RunningMetricItem(
                value = String.format(java.util.Locale.US, "%.2f", uiState.distanceKm),
                unit = "km",
                label = "距离",
                modifier = Modifier.weight(1f)
            )
            RunningMetricItem(
                value = formatRunClock(uiState.durationSeconds),
                unit = "",
                label = "时长",
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        HorizontalDivider(color = RunHudDivider)
        Spacer(modifier = Modifier.height(14.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            RunningMetricItem(
                value = pace.first,
                unit = pace.second,
                label = "平均配速",
                modifier = Modifier.weight(1f)
            )
            RunningMetricItem(
                value = uiState.caloriesKcal.toString(),
                unit = "kcal",
                label = "消耗",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun RunningPanelHeader(uiState: RunTrackingUiState) {
    val statusText = when {
        uiState.hasRecoveredDraft -> "未完成"
        uiState.isPaused -> "已暂停"
        else -> "记录中"
    }
    val statusColor = when {
        uiState.hasRecoveredDraft -> RunHudLabel
        uiState.isPaused -> RunHudPaused
        else -> RunHudGreen
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "户外跑步",
            color = Color.White.copy(alpha = 0.82f),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(statusColor.copy(alpha = 0.12f))
                .border(1.dp, statusColor.copy(alpha = 0.28f), RoundedCornerShape(50))
                .padding(horizontal = 10.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )
            Text(
                text = statusText,
                color = statusColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun RunningMetricItem(
    value: String,
    unit: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Row {
            Text(
                text = value,
                color = Color.White,
                fontSize = 28.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
                style = RunHudNumbers,
                modifier = Modifier.alignByBaseline()
            )
            if (unit.isNotEmpty()) {
                Text(
                    text = " $unit",
                    color = RunHudUnit,
                    fontSize = 12.sp,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.alignByBaseline()
                )
            }
        }
        Text(
            text = label,
            color = RunHudLabel,
            fontSize = 12.sp,
            maxLines = 1
        )
    }
}

@Composable
internal fun RunTrackingErrorMessage(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        modifier = modifier
            .background(Color(0xB3111820), androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    )
}

@Composable
internal fun RunTrackingOverlayCard(
    uiState: RunTrackingUiState,
    onRequestPermission: () -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onFinish: () -> Unit,
    onContinueRecoveredDraft: () -> Unit,
    onSaveRecoveredDraft: () -> Unit,
    onDiscardRecoveredDraft: () -> Unit,
    onDiscardRequest: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        RunTrackingActionButton(
            uiState = uiState,
            onRequestPermission = onRequestPermission,
            onStart = onStart,
            onPause = onPause,
            onResume = onResume,
            onFinish = onFinish,
            onContinueRecoveredDraft = onContinueRecoveredDraft,
            onSaveRecoveredDraft = onSaveRecoveredDraft,
            onDiscardRecoveredDraft = onDiscardRecoveredDraft,
            onDiscardRequest = onDiscardRequest
        )
    }
}

@Composable
internal fun RunGpsStatus(
    uiState: RunTrackingUiState,
    modifier: Modifier = Modifier
) {
    val signalLevel = remember(uiState.gpsStatusText, uiState.trackPoints.size) {
        gpsSignalLevel(
            accuracyMeters = uiState.trackPoints.lastOrNull()?.accuracyMeters,
            statusText = uiState.gpsStatusText
        )
    }
    Box(
        modifier = modifier
            .widthIn(max = 260.dp)
            .background(Color(0x4D111820), androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
            .padding(horizontal = 11.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RunGpsSignalBars(level = signalLevel)
            Text(
                text = when {
                    !uiState.hasLocationPermission -> "需要精确位置权限才能准确记录运动轨迹"
                    !uiState.hasNotificationPermission -> "需要通知权限才能在息屏时保持跑步记录"
                    uiState.isPaused -> "${uiState.gpsStatusText} · 定位已关闭"
                    uiState.isServiceRunning -> "${uiState.gpsStatusText} · 息屏记录中"
                    else -> uiState.gpsStatusText
                },
                color = if (uiState.hasLocationPermission && uiState.hasNotificationPermission) RunGreen else AppSecondaryText,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun RunGpsSignalBars(level: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        listOf(5.dp, 8.dp, 11.dp).forEachIndexed { index, height ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(height)
                    .background(
                        color = if (index < level) RunGreen else Color(0x667F8893),
                        shape = RoundedCornerShape(2.dp)
                    )
            )
        }
    }
}

@Composable
internal fun RunMapFollowToggle(
    autoFollow: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = if (autoFollow) Color(0x9922C55E) else Color(0x99111820),
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        onClick = onClick
    ) {
        Text(
            text = if (autoFollow) "跟随中" else "跟随",
            color = if (autoFollow) Color.White else RunGreen,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp)
        )
    }
}

private fun gpsSignalLevel(
    accuracyMeters: Float?,
    statusText: String
): Int {
    accuracyMeters?.let {
        return when {
            it <= 10f -> 3
            it <= 25f -> 2
            it <= 50f -> 1
            else -> 0
        }
    }
    return when {
        statusText.contains("良好") -> 3
        statusText.contains("精度") -> 2
        statusText.contains("等待") || statusText.contains("校准") || statusText.contains("定位中") -> 1
        else -> 0
    }
}

@Composable
private fun RunTrackingActionButton(
    uiState: RunTrackingUiState,
    onRequestPermission: () -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onFinish: () -> Unit,
    onContinueRecoveredDraft: () -> Unit,
    onSaveRecoveredDraft: () -> Unit,
    onDiscardRecoveredDraft: () -> Unit,
    onDiscardRequest: () -> Unit
) {
    when {
        uiState.hasRecoveredDraft -> {
            RecoveredRunActions(
                isSaving = uiState.isSaving,
                canSave = uiState.canSave,
                onContinue = onContinueRecoveredDraft,
                onSave = onSaveRecoveredDraft,
                onDiscard = onDiscardRecoveredDraft
            )
        }
        !uiState.hasLocationPermission -> {
            AppPrimaryButton(text = "开启定位权限", onClick = onRequestPermission, modifier = Modifier.fillMaxWidth(), height = RunActionButtonHeight)
        }
        // 通知权限只影响"熄屏后能不能看到通知"，不再阻断开始跑步
        uiState.isTracking -> {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RunPauseResumeHoldButton(
                    text = when {
                        uiState.isSaving -> "保存中..."
                        uiState.isPaused -> "继续"
                        else -> "暂停"
                    },
                    isPaused = uiState.isPaused,
                    onClick = {
                        if (uiState.isPaused) onResume() else onPause()
                    },
                    onHoldComplete = onFinish,
                    enabled = !uiState.isSaving,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = "长按按钮结束本次跑步",
                    color = RunHudLabel.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    maxLines = 1
                )
                Text(
                    text = "放弃本次跑步",
                    color = RunHudLabel.copy(alpha = 0.55f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .appClickable(enabled = !uiState.isSaving, onClick = onDiscardRequest)
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }
        else -> {
            AppPrimaryButton(text = "开始跑步", onClick = onStart, modifier = Modifier.fillMaxWidth(), height = RunActionButtonHeight)
        }
    }
}

@Composable
private fun RunPauseResumeHoldButton(
    text: String,
    isPaused: Boolean,
    onClick: () -> Unit,
    onHoldComplete: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val progress = remember { Animatable(0f) }
    val shape = RoundedCornerShape(20.dp)
    BoxWithConstraints(
        modifier = modifier
            .height(RunActionButtonHeight)
            .clip(shape)
            .background(
                brush = if (enabled) {
                    Brush.linearGradient(colors = listOf(RunActionGradientStart, RunActionGradientEnd))
                } else {
                    Brush.linearGradient(colors = listOf(Color(0xFF26313A), Color(0xFF1B242C)))
                }
            )
            .border(1.dp, Color.White.copy(alpha = if (enabled) 0.20f else 0.08f), shape)
            .pointerInput(enabled, text) {
                if (!enabled) return@pointerInput
                detectTapGestures(
                    onPress = {
                        coroutineScope {
                            var holdCompleted = false
                            val holdJob = launch {
                                progress.snapTo(0f)
                                progress.animateTo(
                                    targetValue = 1f,
                                    animationSpec = tween(durationMillis = 1_000, easing = LinearEasing)
                                )
                                holdCompleted = true
                                onHoldComplete()
                                progress.snapTo(0f)
                            }
                            val released = tryAwaitRelease()
                            if (released && !holdCompleted) {
                                holdJob.cancel()
                                progress.animateTo(0f, tween(durationMillis = 120))
                                onClick()
                            } else if (!holdCompleted) {
                                holdJob.cancel()
                                progress.snapTo(0f)
                            }
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(maxWidth * progress.value)
                .height(RunActionButtonHeight)
                .background(Color.White.copy(alpha = 0.18f))
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isPaused) "▶" else "Ⅱ",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black
                )
            }
            Text(
                text = if (progress.value > 0f) {
                    "长按结束 ${(progress.value * 100).toInt()}%"
                } else {
                    text
                },
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

internal fun formatHudPaceValue(paceText: String): Pair<String, String> {
    val normalized = paceText
        .replace("''/公里", "\"/km")
        .replace("/公里", "/km")
    return if (normalized.endsWith("/km")) {
        normalized.removeSuffix("/km") to "/km"
    } else {
        normalized to ""
    }
}
