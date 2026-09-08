package com.example.myrunapp.feature.run

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myrunapp.feature.exercise.formatExerciseInputDate
import com.example.myrunapp.ui.components.AppBackButton
import com.example.myrunapp.ui.components.PageHorizontalPadding
import com.example.myrunapp.ui.components.PageTopSpacing
import com.example.myrunapp.ui.theme.AppBackground
import com.example.myrunapp.ui.theme.AppSecondaryText
import com.example.myrunapp.ui.theme.MyRunAppTheme

@Composable
fun RunTrackDetailRoute(
    viewModel: RunTrackingViewModel,
    sessionId: Long,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.observeTrackDetail(sessionId).collectAsState(
        initial = RunTrackDetailUiState(sessionId = sessionId)
    )

    RunTrackDetailScreen(
        uiState = uiState,
        onBack = onBack,
        modifier = modifier
    )
}

@Composable
fun RunTrackDetailScreen(
    uiState: RunTrackDetailUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var mapDisplayType by remember { mutableStateOf(RunMapDisplayType.Normal) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        val hasValidTrackPoint = uiState.points.any { it.hasValidMapCoordinate() }

        if (!uiState.isMissing && hasValidTrackPoint) {
            AMapTrackPreview(
                points = uiState.points,
                mapDisplayType = mapDisplayType,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            TrackDetailEmptyState(
                title = if (uiState.isMissing) "未找到轨迹记录" else "暂无有效轨迹",
                subtitle = if (uiState.isMissing) "这条运动记录可能已被删除" else "本次记录没有可展示的 GPS 点",
                modifier = Modifier.fillMaxSize()
            )
        }

        AppBackButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(
                    start = PageHorizontalPadding,
                    top = PageTopSpacing,
                    end = PageHorizontalPadding
                )
        )

        if (!uiState.isMissing && hasValidTrackPoint) {
            RunMapTypeToggle(
                mapDisplayType = mapDisplayType,
                onClick = { mapDisplayType = mapDisplayType.next() },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(end = PageHorizontalPadding, top = PageTopSpacing + 4.dp)
            )
        }

        if (!uiState.isMissing) {
            TrackSummaryOverlay(
                uiState = uiState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(
                        start = PageHorizontalPadding,
                        end = PageHorizontalPadding,
                        bottom = 24.dp
                    )
            )
        }
    }
}

@Composable
private fun TrackDetailEmptyState(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold)
            Text(subtitle, color = AppSecondaryText, fontSize = 12.sp)
        }
    }
}

@Composable
private fun TrackSummaryOverlay(uiState: RunTrackDetailUiState, modifier: Modifier = Modifier) {
    RunInfoOverlayCard(
        title = "户外跑步",
        dateText = uiState.startTime?.let { formatExerciseInputDate(it) } ?: "--",
        firstRow = RunInfoMetricUiModel(formatRunInfoDistance(uiState.distanceKm), "距离") to
            RunInfoMetricUiModel(formatRunInfoDuration(uiState.durationSeconds), "时长"),
        secondRow = RunInfoMetricUiModel(formatRunInfoPace(uiState.paceText), "平均配速") to
            RunInfoMetricUiModel(formatRunInfoCalories(uiState.caloriesKcal), "消耗"),
        modifier = modifier,
        metricValueColor = Color.White
    )
}

private fun RunTrackPointUiModel.hasValidMapCoordinate(): Boolean {
    return latitude in -90.0..90.0 &&
        longitude in -180.0..180.0 &&
        !(latitude == 0.0 && longitude == 0.0)
}

@Preview(showBackground = true)
@Composable
private fun RunTrackDetailScreenPreview() {
    MyRunAppTheme(darkTheme = true) {
        RunTrackDetailScreen(
            uiState = RunTrackDetailUiState(
                sessionId = 1,
                startTime = 1_777_046_400_000,
                distanceKm = 5.2,
                durationSeconds = 1_980,
                paceText = "6'23\"/km",
                caloriesKcal = 356,
                points = listOf(
                    RunTrackPointUiModel(30.1, 120.1, null, null, null, 1),
                    RunTrackPointUiModel(30.12, 120.11, null, null, null, 2),
                    RunTrackPointUiModel(30.13, 120.15, null, null, null, 3),
                    RunTrackPointUiModel(30.16, 120.18, null, null, null, 4)
                )
            ),
            onBack = {}
        )
    }
}
