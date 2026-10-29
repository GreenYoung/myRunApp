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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.myrunapp.core.log.AppLogger
import com.example.myrunapp.core.log.LogTags
import com.example.myrunapp.ui.components.AppBackButton
import com.example.myrunapp.ui.components.PageHorizontalPadding
import com.example.myrunapp.ui.components.PageTopSpacing
import com.example.myrunapp.ui.theme.MyRunAppTheme
import kotlinx.coroutines.delay

internal val RunGreen = Color(0xFF22C55E)
internal val RunHudGreen = Color(0xFF0BDA51)
internal val RunHudUnit = Color(0xFFB6BDC5)
internal val RunHudLabel = Color(0xFF7F8893)
internal val RunActionButtonHeight = 58.dp
private val RunBottomPadding = 24.dp
private val RunBottomItemSpacing = 12.dp
private const val AutoFollowIdleTimeoutMs = 3 * 60 * 1000L

@Composable
fun RunTrackingRoute(
    viewModel: RunTrackingViewModel,
    onBack: () -> Unit,
    onSaved: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var isBatteryOptimizationDialogVisible by remember { mutableStateOf(false) }
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
        onStart = {
            if (shouldPromptBatteryOptimization(context)) {
                isBatteryOptimizationDialogVisible = true
            } else {
                viewModel.startTracking()
            }
        },
        onPause = viewModel::pauseTracking,
        onResume = viewModel::resumeTracking,
        onFinish = { viewModel.finishTracking(onSaved, onDiscarded = onBack) },
        onContinueRecoveredDraft = viewModel::continueRecoveredDraft,
        onSaveRecoveredDraft = { viewModel.saveRecoveredDraft(onSaved) },
        onDiscardRecoveredDraft = viewModel::discardRecoveredDraft,
        modifier = modifier
    )

    if (isBatteryOptimizationDialogVisible) {
        BatteryOptimizationDialog(
            onConfirm = {
                isBatteryOptimizationDialogVisible = false
                openBatteryOptimizationSettings(context)
            },
            onDecline = {
                isBatteryOptimizationDialogVisible = false
                markBatteryOptimizationPrompted(context)
                viewModel.startTracking()
            },
            onCancel = {
                isBatteryOptimizationDialogVisible = false
                markBatteryOptimizationPrompted(context)
            }
        )
    }
}

@Composable
private fun BatteryOptimizationDialog(
    onConfirm: () -> Unit,
    onDecline: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(text = "开启后台持续定位") },
        text = {
            Text(text = "锁屏后系统可能冻结定位和计时，导致轨迹中断。建议将 MyRun 加入电池优化白名单，保证跑步全程持续记录。")
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = "去设置")
            }
        },
        dismissButton = {
            TextButton(onClick = onDecline) {
                Text(text = "暂不")
            }
        }
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
    onContinueRecoveredDraft: () -> Unit,
    onSaveRecoveredDraft: () -> Unit,
    onDiscardRecoveredDraft: () -> Unit,
    modifier: Modifier = Modifier
) {
    var mapDisplayType by remember { mutableStateOf(RunMapDisplayType.Normal) }
    var autoFollow by remember { mutableStateOf(true) }
    var lastMapTouchAt by remember { mutableStateOf(0L) }
    var isDiscardRecoveredDialogVisible by remember { mutableStateOf(false) }
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
            sharedMapLocation = uiState.mapLocation,
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
                onFinish = onFinish,
                onContinueRecoveredDraft = onContinueRecoveredDraft,
                onSaveRecoveredDraft = onSaveRecoveredDraft,
                onDiscardRecoveredDraft = { isDiscardRecoveredDialogVisible = true }
            )
        }

        if (isDiscardRecoveredDialogVisible) {
            AlertDialog(
                onDismissRequest = { isDiscardRecoveredDialogVisible = false },
                title = { Text(text = "放弃未完成的跑步记录？") },
                text = { Text(text = "未完成的跑步记录将被删除，且无法恢复。") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            isDiscardRecoveredDialogVisible = false
                            onDiscardRecoveredDraft()
                        }
                    ) {
                        Text(text = "放弃", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { isDiscardRecoveredDialogVisible = false }) {
                        Text(text = "保留记录")
                    }
                }
            )
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
            onPause = {},
            onResume = {},
            onFinish = {},
            onContinueRecoveredDraft = {},
            onSaveRecoveredDraft = {},
            onDiscardRecoveredDraft = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RunTrackingRecoveredPreview() {
    MyRunAppTheme(darkTheme = true) {
        RunTrackingScreen(
            uiState = RunTrackingUiState(
                hasLocationPermission = true,
                hasRecoveredDraft = true,
                gpsStatusText = "发现未完成的跑步记录",
                distanceKm = 3.26,
                durationSeconds = 1_296,
                averagePaceText = "6'37\"/km",
                caloriesKcal = 208,
                canSave = true,
                isTracking = false
            ),
            onBack = {},
            onRequestPermission = {},
            onStart = {},
            onPause = {},
            onResume = {},
            onFinish = {},
            onContinueRecoveredDraft = {},
            onSaveRecoveredDraft = {},
            onDiscardRecoveredDraft = {}
        )
    }
}
