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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myrunapp.feature.run.AMapTrackPreview
import com.example.myrunapp.feature.run.RunMapDisplayType
import com.example.myrunapp.feature.run.RunMapTypeToggle
import com.example.myrunapp.feature.run.RunTrackPointUiModel
import com.example.myrunapp.feature.run.formatRunClock
import com.example.myrunapp.ui.components.AppBackButton
import com.example.myrunapp.ui.components.AppPageTopBar
import com.example.myrunapp.ui.components.PageHorizontalPadding
import com.example.myrunapp.ui.components.PageTopSpacing
import com.example.myrunapp.ui.theme.AppBackground
import com.example.myrunapp.ui.theme.AppSecondaryText
import com.example.myrunapp.ui.theme.MyRunAppTheme

private val DetailCardDark = Color(0xFF111820)
private val OutdoorRunHudBackground = Color(0xF5101820)
private val OutdoorRunHudGreen = Color(0xFF0BDA51)
private val OutdoorRunHudUnit = Color(0xFFB6BDC5)
private val OutdoorRunHudLabel = Color(0xFF7F8893)

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
    var mapDisplayType by remember { mutableStateOf(RunMapDisplayType.Normal) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DetailCardDark)
    ) {
        AMapTrackPreview(
            points = uiState.trackPoints,
            mapDisplayType = mapDisplayType,
            modifier = Modifier.fillMaxSize()
        )
        AppBackButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = PageHorizontalPadding, top = PageTopSpacing)
        )
        RunMapTypeToggle(
            mapDisplayType = mapDisplayType,
            onClick = { mapDisplayType = mapDisplayType.next() },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(end = PageHorizontalPadding, top = PageTopSpacing + 4.dp)
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
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(26.dp))
            .background(OutdoorRunHudBackground)
            .padding(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutdoorRunInfoHeader(
            title = "户外跑步",
            dateText = formatExerciseInputDate(record.startTime)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            OutdoorRunMetricItem(
                value = formatDistance(record.distanceKm),
                unit = "km",
                label = "距离",
                valueFontSize = 31,
                unitFontSize = 15,
                modifier = Modifier.weight(1f)
            )
            OutdoorRunMetricItem(
                value = formatRunClock(record.durationSeconds),
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
            val paceValue = record.paceText.toOutdoorRunPaceParts()
            OutdoorRunMetricItem(
                value = paceValue.first,
                unit = paceValue.second,
                label = "平均配速",
                valueFontSize = 27,
                unitFontSize = 13,
                modifier = Modifier.weight(1f)
            )
            OutdoorRunMetricItem(
                value = record.caloriesText.toCaloriesValueText(),
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
private fun OutdoorRunInfoHeader(
    title: String,
    dateText: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 21.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "●",
                color = OutdoorRunHudGreen,
                fontSize = 13.sp,
                lineHeight = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = dateText,
                color = OutdoorRunHudUnit,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun OutdoorRunMetricItem(
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
                    color = OutdoorRunHudUnit,
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
            color = OutdoorRunHudLabel,
            fontSize = 13.sp,
            lineHeight = 15.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
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
        Text(
            text = value,
            color = Color.White,
            fontSize = 19.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false
        )
        Text(label, color = AppSecondaryText, fontWeight = FontWeight.Medium, fontSize = 13.sp)
    }
}

private fun String.toOutdoorRunPaceParts(): Pair<String, String> {
    return if (endsWith("/km")) {
        removeSuffix("/km") to "/km"
    } else {
        this to ""
    }
}

private fun String.toCaloriesValueText(): String {
    return substringBefore(" ").ifBlank { filter { it.isDigit() }.ifBlank { "0" } }
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
