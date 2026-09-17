package com.example.myrunapp.feature.run

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myrunapp.core.log.AppLogger
import com.example.myrunapp.core.log.LogTags
import com.example.myrunapp.ui.components.AppBackButton
import com.example.myrunapp.ui.components.AppPrimaryButton
import com.example.myrunapp.ui.components.PageHorizontalPadding
import com.example.myrunapp.ui.components.PageTopSpacing
import com.example.myrunapp.ui.theme.AppSecondaryText
import com.example.myrunapp.ui.theme.MyRunAppTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

private val RunGreen = Color(0xFF22C55E)
private val RunHudGreen = Color(0xFF0BDA51)
private val RunHudBackground = Color(0xF5101820)
private val RunHudUnit = Color(0xFFB6BDC5)
private val RunHudLabel = Color(0xFF7F8893)
private val RunBottomPadding = 24.dp
private val RunBottomItemSpacing = 12.dp
private val RunActionButtonHeight = 58.dp
private const val AutoFollowIdleTimeoutMs = 3 * 60 * 1000L

@Composable
fun RunTrackingRoute(
    viewModel: RunTrackingViewModel,
    onBack: () -> Unit,
    onSaved: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.refreshPermission()
    }

    LaunchedEffect(Unit) {
        viewModel.refreshPermission()
        if (!uiState.hasLocationPermission || !uiState.hasNotificationPermission) {
            permissionLauncher.launch(runTrackingPermissions())
        }
    }

    RunTrackingScreen(
        uiState = uiState,
        onBack = onBack,
        onRequestPermission = {
            permissionLauncher.launch(runTrackingPermissions())
        },
        onStart = viewModel::startTracking,
        onPause = viewModel::pauseTracking,
        onResume = viewModel::resumeTracking,
        onFinish = { viewModel.finishTracking(onSaved) },
        onDiscard = viewModel::stopWithoutSaving,
        modifier = modifier
    )
}

@Composable
fun RunTrackingScreen(
    uiState: RunTrackingUiState,
    onBack: () -> Unit,
    onRequestPermission: () -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onFinish: () -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier
) {
    var mapDisplayType by remember { mutableStateOf(RunMapDisplayType.Normal) }
    var autoFollow by remember { mutableStateOf(true) }
    var lastMapTouchAt by remember { mutableStateOf(0L) }
    val hasRunInfo = uiState.isTracking || uiState.distanceKm > 0.0 || uiState.durationSeconds > 0L

    LaunchedEffect(uiState.isTracking, uiState.trackPoints.size, uiState.distanceKm) {
        AppLogger.d(
            LogTags.RUN,
            "RUN_UI state isTracking=${uiState.isTracking} points=${uiState.trackPoints.size} distance=${"%.4f".format(uiState.distanceKm)}"
        )
    }

    LaunchedEffect(autoFollow, lastMapTouchAt) {
        if (!autoFollow && lastMapTouchAt > 0L) {
            delay(AutoFollowIdleTimeoutMs)
            if (!autoFollow && lastMapTouchAt > 0L) {
                AppLogger.d(LogTags.RUN, "auto follow restored after idle timeout")
                autoFollow = true
                lastMapTouchAt = 0L
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF111820))
    ) {
        AMapRunTrackingMap(
            points = uiState.trackPoints,
            isTracking = uiState.isTracking,
            hasLocationPermission = uiState.hasLocationPermission,
            mapDisplayType = mapDisplayType,
            autoFollow = autoFollow,
            onMapTouched = {
                autoFollow = false
                lastMapTouchAt = System.currentTimeMillis()
            },
            modifier = Modifier.fillMaxSize()
        )

        AppBackButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = PageHorizontalPadding, top = PageTopSpacing)
        )

        RunGpsStatus(
            uiState = uiState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = PageTopSpacing + 7.dp)
        )

        RunMapTypeToggle(
            mapDisplayType = mapDisplayType,
            onClick = {
                mapDisplayType = mapDisplayType.next()
                autoFollow = true
                lastMapTouchAt = 0L
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(end = PageHorizontalPadding, top = PageTopSpacing + 4.dp)
        )

        RunMapFollowToggle(
            autoFollow = autoFollow,
            onClick = {
                autoFollow = true
                lastMapTouchAt = 0L
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(end = PageHorizontalPadding, top = PageTopSpacing + 44.dp)
        )

        uiState.errorMessage?.let {
            RunTrackingErrorMessage(
                text = it,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = PageHorizontalPadding)
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(
                    start = PageHorizontalPadding,
                    end = PageHorizontalPadding,
                    bottom = RunBottomPadding
                ),
            verticalArrangement = Arrangement.spacedBy(RunBottomItemSpacing)
        ) {
            if (hasRunInfo) {
                RunningStatsPanel(uiState = uiState, modifier = Modifier.fillMaxWidth())
            }
            RunTrackingOverlayCard(
                uiState = uiState,
                onRequestPermission = onRequestPermission,
                onStart = onStart,
                onPause = onPause,
                onResume = onResume,
                onFinish = onFinish
            )
        }
    }
}

@Composable
private fun RunningStatsPanel(
    uiState: RunTrackingUiState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(26.dp))
            .background(RunHudBackground)
            .padding(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        RunningPanelHeader(isPaused = uiState.isPaused)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            RunningMetricItem(
                value = String.format(java.util.Locale.US, "%.2f", uiState.distanceKm),
                unit = "km",
                label = "距离",
                valueFontSize = 31,
                unitFontSize = 15,
                modifier = Modifier.weight(1f)
            )
            RunningMetricItem(
                value = formatRunClock(uiState.durationSeconds),
                unit = "",
                label = "时长",
                valueFontSize = 27,
                unitFontSize = 0,
                useTabularNumbers = true,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            val paceValue = formatHudPaceValue(uiState.averagePaceText)
            RunningMetricItem(
                value = paceValue.first,
                unit = paceValue.second,
                label = "平均配速",
                valueFontSize = 27,
                unitFontSize = 13,
                modifier = Modifier.weight(1f)
            )
            RunningMetricItem(
                value = uiState.caloriesKcal.toString(),
                unit = "kcal",
                label = "消耗",
                valueFontSize = 28,
                unitFontSize = 14,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun RunningPanelHeader(isPaused: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "户外跑步",
            color = Color.White,
            fontSize = 21.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "●",
                color = RunHudGreen,
                fontSize = 13.sp,
                lineHeight = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (isPaused) "已暂停" else "记录中",
                color = RunHudUnit,
                fontSize = 13.sp,
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
    valueFontSize: Int,
    unitFontSize: Int,
    modifier: Modifier = Modifier,
    useTabularNumbers: Boolean = false
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = value,
                color = Color.White,
                fontSize = valueFontSize.sp,
                lineHeight = (valueFontSize + 3).sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
                style = TextStyle(fontFeatureSettings = if (useTabularNumbers) "tnum" else null)
            )
            if (unit.isNotEmpty()) {
                Text(
                    text = " $unit",
                    color = RunHudUnit,
                    fontSize = unitFontSize.sp,
                    lineHeight = (unitFontSize + 2).sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }
        Text(
            text = label,
            color = RunHudLabel,
            fontSize = 13.sp,
            lineHeight = 15.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
private fun RunTrackingErrorMessage(
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
private fun RunTrackingOverlayCard(
    uiState: RunTrackingUiState,
    onRequestPermission: () -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onFinish: () -> Unit
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
            onFinish = onFinish
        )
    }
}

@Composable
private fun RunGpsStatus(
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
                    uiState.isPaused -> "${uiState.gpsStatusText} · 息屏保持中"
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
private fun RunMapFollowToggle(
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
    onFinish: () -> Unit
) {
    when {
        !uiState.hasLocationPermission -> {
            AppPrimaryButton(text = "开启定位权限", onClick = onRequestPermission, modifier = Modifier.fillMaxWidth(), height = RunActionButtonHeight)
        }
        !uiState.hasNotificationPermission -> {
            AppPrimaryButton(text = "开启通知权限", onClick = onRequestPermission, modifier = Modifier.fillMaxWidth(), height = RunActionButtonHeight)
        }
        uiState.isTracking -> {
            RunPauseResumeHoldButton(
                text = when {
                    uiState.isSaving -> "保存中..."
                    uiState.isPaused -> "继续"
                    else -> "暂停"
                },
                onClick = {
                    if (uiState.isPaused) onResume() else onPause()
                },
                onHoldComplete = onFinish,
                enabled = !uiState.isSaving,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        else -> {
            AppPrimaryButton(text = "开始跑步", onClick = onStart, modifier = Modifier.fillMaxWidth(), height = RunActionButtonHeight)
        }
    }
}

@Composable
private fun RunPauseResumeHoldButton(
    text: String,
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
            .background(if (enabled) RunGreen else Color(0xFF26313A))
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
                .background(Color(0xFF06130E).copy(alpha = 0.18f))
        )
        Text(
            text = if (progress.value > 0f) {
                "长按结束 ${(progress.value * 100).toInt()}%"
            } else {
                runActionButtonText(text)
            },
            color = Color(0xFF07130C),
            fontSize = 18.sp,
            lineHeight = 21.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

private fun formatHudPaceValue(paceText: String): Pair<String, String> {
    val normalized = paceText
        .replace("''/公里", "\"/km")
        .replace("/公里", "/km")
    return if (normalized.endsWith("/km")) {
        normalized.removeSuffix("/km") to "/km"
    } else {
        normalized to ""
    }
}

private fun runActionButtonText(text: String): String {
    return when (text) {
        "暂停" -> "Ⅱ  暂停"
        "继续" -> "▶  继续"
        else -> text
    }
}

private fun runTrackingPermissions(): Array<String> {
    val permissions = mutableListOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        permissions += Manifest.permission.POST_NOTIFICATIONS
    }
    return permissions.toTypedArray()
}

@Preview(showBackground = true)
@Composable
private fun RunTrackingScreenPreview() {
    MyRunAppTheme(darkTheme = true) {
        RunTrackingScreen(
            uiState = RunTrackingUiState(
                hasLocationPermission = true,
                gpsStatusText = "GPS 信号良好",
                distanceKm = 3.26,
                durationSeconds = 1_296,
                averagePaceText = "6'37\"/km",
                isTracking = true
            ),
            onBack = {},
            onRequestPermission = {},
            onStart = {},
            onPause = {},
            onResume = {},
            onFinish = {},
            onDiscard = {}
        )
    }
}
