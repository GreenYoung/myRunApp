package com.example.myrunapp.feature.exercise

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myrunapp.feature.run.AMapTrackPreview
import com.example.myrunapp.feature.run.RunInfoMetricUiModel
import com.example.myrunapp.feature.run.RunInfoOverlayCard
import com.example.myrunapp.feature.run.RunTrackPointUiModel
import com.example.myrunapp.ui.components.AppBackButton
import com.example.myrunapp.ui.components.AppPageTopBar
import com.example.myrunapp.ui.components.PageHorizontalPadding
import com.example.myrunapp.ui.components.PageTopSpacing
import com.example.myrunapp.ui.theme.AppBackground
import com.example.myrunapp.ui.theme.AppSecondaryText
import com.example.myrunapp.ui.theme.MyRunAppTheme

private val DetailCardDark = Color(0xFF111820)

@Composable
fun ExerciseRecordDetailRoute(
    viewModel: ExerciseViewModel,
    recordId: Long,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.observeRecordDetail(recordId).collectAsState(
        initial = ExerciseRecordDetailUiState()
    )

    ExerciseRecordDetailScreen(
        uiState = uiState,
        onBack = onBack,
        modifier = modifier
    )
}

@Composable
fun ExerciseRecordDetailScreen(
    uiState: ExerciseRecordDetailUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        uiState.isMissing || uiState.record == null -> MissingExerciseRecordScreen(onBack = onBack, modifier = modifier)
        uiState.isOutdoorRun && uiState.hasTrack -> OutdoorRunRecordDetailScreen(uiState = uiState, onBack = onBack, modifier = modifier)
        else -> TreadmillRecordDetailScreen(record = uiState.record, onBack = onBack, modifier = modifier)
    }
}

@Composable
private fun OutdoorRunRecordDetailScreen(
    uiState: ExerciseRecordDetailUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val record = uiState.record ?: return
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DetailCardDark)
    ) {
        AMapTrackPreview(points = uiState.trackPoints, modifier = Modifier.fillMaxSize())
        AppBackButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = PageHorizontalPadding, top = PageTopSpacing)
        )
        OutdoorRunInfoOverlayCard(
            record = record,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = PageHorizontalPadding, vertical = 24.dp)
        )
    }
}

@Composable
private fun OutdoorRunInfoOverlayCard(
    record: ExerciseRecordUiModel,
    modifier: Modifier = Modifier
) {
    RunInfoOverlayCard(
        title = record.typeText,
        dateText = formatExerciseInputDate(record.startTime),
        firstRow = RunInfoMetricUiModel(record.distanceText, "距离") to
            RunInfoMetricUiModel(record.durationText, "时长"),
        secondRow = RunInfoMetricUiModel(record.paceText, "平均配速") to
            RunInfoMetricUiModel(record.caloriesText, "消耗"),
        modifier = modifier,
        metricValueColor = Color.White
    )
}

@Composable
private fun TreadmillRecordDetailScreen(
    record: ExerciseRecordUiModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(
                start = PageHorizontalPadding,
                top = PageTopSpacing,
                end = PageHorizontalPadding,
                bottom = 24.dp
            ),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        AppPageTopBar(title = "${record.typeText}详情", showBackButton = true, onBackClick = onBack)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = DetailCardDark,
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                DetailMetricRow(
                    leftValue = record.distanceText,
                    leftLabel = "距离",
                    rightValue = record.durationText,
                    rightLabel = "时长"
                )
                DetailMetricRow(
                    leftValue = record.paceText,
                    leftLabel = "平均配速",
                    rightValue = record.caloriesText,
                    rightLabel = "消耗"
                )
                Text(
                    text = "日期：${formatExerciseInputDate(record.startTime)}",
                    color = AppSecondaryText,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun MissingExerciseRecordScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = PageHorizontalPadding, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        AppPageTopBar(title = "运动记录", showBackButton = true, onBackClick = onBack)
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("未找到运动记录", color = AppSecondaryText, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun DetailMetricRow(
    leftValue: String,
    leftLabel: String,
    rightValue: String,
    rightLabel: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        DetailMetric(leftValue, leftLabel, Modifier.weight(1f))
        DetailMetric(rightValue, rightLabel, Modifier.weight(1f))
    }
}

@Composable
private fun DetailMetric(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(value, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(label, color = AppSecondaryText, fontWeight = FontWeight.Medium, fontSize = 13.sp)
    }
}

@Preview(showBackground = true)
@Composable
private fun ExerciseRecordDetailScreenPreview() {
    MyRunAppTheme(darkTheme = true) {
        ExerciseRecordDetailScreen(
            uiState = ExerciseRecordDetailUiState(
                record = ExerciseRecordUiModel(
                    id = 1,
                    type = ExerciseType.OUTDOOR_RUNNING,
                    startTime = 1_777_046_400_000,
                    durationSeconds = 1_980,
                    distanceKm = 5.20,
                    typeText = "户外",
                    dateTimeText = "08-21",
                    distanceText = "5.20 km",
                    durationText = "33min",
                    paceText = "6'23\"/km",
                    caloriesText = "356 kcal",
                    runSessionId = 1,
                    trackPoints = listOf(
                        RunTrackPointUiModel(30.1, 120.1, null, null, null, 1),
                        RunTrackPointUiModel(30.12, 120.11, null, null, null, 2),
                        RunTrackPointUiModel(30.13, 120.15, null, null, null, 3)
                    )
                ),
                runSessionId = 1,
                trackPoints = listOf(
                    RunTrackPointUiModel(30.1, 120.1, null, null, null, 1),
                    RunTrackPointUiModel(30.12, 120.11, null, null, null, 2),
                    RunTrackPointUiModel(30.13, 120.15, null, null, null, 3)
                )
            ),
            onBack = {}
        )
    }
}
