package com.example.myrunapp.feature.run

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.example.myrunapp.feature.exercise.formatDistance
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

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(
                    start = PageHorizontalPadding,
                    end = PageHorizontalPadding,
                    bottom = 24.dp
                ),
        ) {
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
private fun RunTrackingOverlayCard(
    uiState: RunTrackingUiState,
    onRequestPermission: () -> Unit,
    onStart: () -> Unit,
    onFinish: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 15.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (uiState.isTracking || uiState.distanceKm > 0.0 || uiState.durationSeconds > 0L) {
            RunDistanceBlock(distanceKm = uiState.distanceKm)
            RunMetricsRow(uiState = uiState)
        }
        uiState.errorMessage?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
        RunTrackingActionButton(
            uiState = uiState,
            onRequestPermission = onRequestPermission,
            onStart = onStart,
            onFinish = onFinish
        )
    }
}

@Composable
private fun RunDistanceBlock(distanceKm: Double) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "${formatDistance(distanceKm)} km",
            color = RunGreen,
            fontSize = 46.sp,
            lineHeight = 50.sp,
            fontWeight = FontWeight.Bold
        )
        Text("当前距离", color = AppSecondaryText, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun RunMetricsRow(uiState: RunTrackingUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        RunMetricItem(title = "运动时长", value = formatRunClock(uiState.durationSeconds), modifier = Modifier.weight(1f))
        RunMetricItem(title = "平均配速", value = uiState.averagePaceText, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun RunMetricItem(title: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .padding(vertical = 11.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(value, color = RunGreen, fontSize = 19.sp, lineHeight = 22.sp, fontWeight = FontWeight.Bold)
        Text(title, color = AppSecondaryText, fontSize = 11.sp, fontWeight = FontWeight.Medium)
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
            AppPrimaryButton(text = "开启定位权限", onClick = onRequestPermission, modifier = Modifier.fillMaxWidth(), height = 52.dp)
        }
        !uiState.hasNotificationPermission -> {
            AppPrimaryButton(text = "开启通知权限", onClick = onRequestPermission, modifier = Modifier.fillMaxWidth(), height = 52.dp)
        }
        uiState.isTracking -> {
            AppDangerButton(
                text = if (uiState.isSaving) "保存中..." else "结束跑步",
                onClick = onFinish,
                modifier = Modifier.fillMaxWidth(),
                height = 52.dp
            )
        }
        else -> {
            AppPrimaryButton(text = "开始跑步", onClick = onStart, modifier = Modifier.fillMaxWidth(), height = 52.dp)
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
