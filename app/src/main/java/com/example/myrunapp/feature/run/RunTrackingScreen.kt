package com.example.myrunapp.feature.run

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myrunapp.core.log.AppLogger
import com.example.myrunapp.core.log.LogTags
import com.example.myrunapp.feature.exercise.formatCalories
import com.example.myrunapp.feature.exercise.formatDistance
import com.example.myrunapp.feature.exercise.formatDuration
import com.example.myrunapp.ui.components.AppDangerButton
import com.example.myrunapp.ui.components.AppDialogButtonRow
import com.example.myrunapp.ui.components.AppBackButton
import com.example.myrunapp.ui.components.AppPrimaryButton
import com.example.myrunapp.ui.components.PageHorizontalPadding
import com.example.myrunapp.ui.components.PageTopSpacing
import com.example.myrunapp.ui.theme.AppSecondaryText
import com.example.myrunapp.ui.theme.AppSurface
import com.example.myrunapp.ui.theme.MyRunAppTheme

private val RunGreen = Color(0xFF22C55E)
private val RunBottomPadding = 24.dp
private val RunBottomItemSpacing = 12.dp
private val RunActionButtonHeight = 52.dp

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
    onFinish: () -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDiscardDialog by remember { mutableStateOf(false) }
    val hasRunInfo = uiState.isTracking || uiState.distanceKm > 0.0 || uiState.durationSeconds > 0L

    LaunchedEffect(uiState.isTracking, uiState.trackPoints.size, uiState.distanceKm) {
        AppLogger.d(
            LogTags.RUN,
            "RUN_UI state isTracking=${uiState.isTracking} points=${uiState.trackPoints.size} distance=${"%.4f".format(uiState.distanceKm)}"
        )
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
            modifier = Modifier.fillMaxSize()
        )

        AppBackButton(
            onClick = {
                if (uiState.isTracking) {
                    showDiscardDialog = true
                } else {
                    onBack()
                }
            },
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
                RunInfoOverlayCard(
                    title = "户外跑步",
                    dateText = "实时记录",
                    firstRow = RunInfoMetricUiModel("${formatDistance(uiState.distanceKm)} km", "距离") to
                        RunInfoMetricUiModel(formatDuration(uiState.durationSeconds), "时长"),
                    secondRow = RunInfoMetricUiModel(uiState.averagePaceText, "平均配速") to
                        RunInfoMetricUiModel("${formatCalories(uiState.caloriesKcal)} kcal", "消耗"),
                    modifier = Modifier.fillMaxWidth(),
                    metricValueColor = Color.White
                )
            }
            RunTrackingOverlayCard(
                uiState = uiState,
                onRequestPermission = onRequestPermission,
                onStart = onStart,
                onFinish = onFinish
            )
        }
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            containerColor = AppSurface,
            title = { Text("放弃本次跑步？", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("返回后将停止 GPS 记录，本次数据不会保存。", color = AppSecondaryText) },
            confirmButton = {
                AppDialogButtonRow(
                    onCancel = { showDiscardDialog = false },
                    onConfirm = {
                        showDiscardDialog = false
                        onDiscard()
                        onBack()
                    },
                    confirmText = "放弃"
                )
            }
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
            onFinish = onFinish
        )
    }
}

@Composable
private fun RunGpsStatus(
    uiState: RunTrackingUiState,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .widthIn(max = 260.dp)
            .background(Color(0x4D111820), androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
            .padding(horizontal = 11.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = when {
                !uiState.hasLocationPermission -> "需要精确位置权限才能准确记录运动轨迹"
                !uiState.hasNotificationPermission -> "需要通知权限才能在息屏时保持跑步记录"
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

@Composable
private fun RunTrackingActionButton(
    uiState: RunTrackingUiState,
    onRequestPermission: () -> Unit,
    onStart: () -> Unit,
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
            AppDangerButton(
                text = if (uiState.isSaving) "保存中..." else "结束跑步",
                onClick = onFinish,
                modifier = Modifier.fillMaxWidth(),
                height = RunActionButtonHeight
            )
        }
        else -> {
            AppPrimaryButton(text = "开始跑步", onClick = onStart, modifier = Modifier.fillMaxWidth(), height = RunActionButtonHeight)
        }
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
            onFinish = {},
            onDiscard = {}
        )
    }
}
