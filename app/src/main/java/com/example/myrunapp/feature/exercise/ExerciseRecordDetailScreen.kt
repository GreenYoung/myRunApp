package com.example.myrunapp.feature.exercise

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myrunapp.core.util.AppDateFormats
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
import java.util.Locale

private val DetailCardDark = Color(0xFF111820)
private val OutdoorRunHudBackground = Color(0xF5101820)
private val OutdoorRunHudGreen = Color(0xFF0BDA51)
private val OutdoorRunHudUnit = Color(0xFFB6BDC5)
private val OutdoorRunHudLabel = Color(0xFF7F8893)
private val RecordCardTop = Color(0xFF17251D)
private val RecordCardBottom = Color(0xFF0D1418)
private val RecordHairline = Color(0x14FFFFFF)

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
    var mapDisplayType by rememberSaveable { mutableStateOf(RunMapDisplayType.Normal) }

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
        RecordDetailHeader(
            title = record.detailTypeLabel(),
            dateText = record.detailDateText()
        )
        RecordDetailMetricGrid(record = record)
    }
}

/** 距离 / 时长 / 平均配速 / 消耗 —— 户外详情和室内详情共用同一套排版。 */
@Composable
private fun RecordDetailMetricGrid(
    record: ExerciseRecordUiModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            RecordDetailMetricItem(
                value = formatDistance(record.distanceKm),
                unit = "km",
                label = "距离",
                valueFontSize = 31,
                unitFontSize = 15,
                modifier = Modifier.weight(1f)
            )
            RecordDetailMetricItem(
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
            RecordDetailMetricItem(
                value = paceValue.first,
                unit = paceValue.second,
                label = "平均配速",
                valueFontSize = 27,
                unitFontSize = 13,
                modifier = Modifier.weight(1f)
            )
            RecordDetailMetricItem(
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
private fun RecordDetailHeader(
    title: String,
    dateText: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
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
private fun RecordDetailMetricItem(
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
            .verticalScroll(rememberScrollState())
            .padding(
                start = PageHorizontalPadding,
                top = PageTopSpacing,
                end = PageHorizontalPadding,
                bottom = 24.dp
            ),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        AppPageTopBar(title = "", showBackButton = true, onBackClick = onBack)
        RecordDetailCard(record = record)
        Text(
            text = record.detailFootnote(),
            color = AppSecondaryText,
            fontSize = 12.sp,
            lineHeight = 16.sp
        )
    }
}

/**
 * 室内（跑步机）记录的详情卡片：和户外详情共用一套指标排版，
 * 但背景换成带渐变和绿色光晕的卡片，避免在纯色页面上显得"平"。
 */
@Composable
private fun RecordDetailCard(
    record: ExerciseRecordUiModel,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(Brush.linearGradient(listOf(RecordCardTop, RecordCardBottom)))
            .border(1.dp, RecordHairline, RoundedCornerShape(26.dp))
    ) {
        RecordCardGlow(modifier = Modifier.matchParentSize())
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            RecordDetailHeader(
                title = record.detailTypeLabel(),
                dateText = record.detailDateText()
            )
            RecordDetailMetricGrid(record = record)
        }
    }
}

@Composable
private fun RecordCardGlow(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val center = Offset(size.width * 0.92f, size.height * 0.04f)
        val radius = size.width * 0.62f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(OutdoorRunHudGreen.copy(alpha = 0.16f), Color.Transparent),
                center = center,
                radius = radius
            ),
            radius = radius,
            center = center
        )
    }
}

/** 详情页标题按运动类型走，和列表里的类型叫法保持一致（户外跑 / 室内跑）。 */
private fun ExerciseRecordUiModel.detailTypeLabel(): String {
    return when (type) {
        ExerciseType.TREADMILL -> "室内跑"
        ExerciseType.OUTDOOR_RUNNING -> "户外跑"
    }
}

private fun ExerciseRecordUiModel.detailDateText(): String {
    return AppDateFormats.format("M月d日 EEEE", startTime, Locale.CHINA)
}

private fun ExerciseRecordUiModel.detailFootnote(): String {
    return when (type) {
        ExerciseType.TREADMILL -> "室内跑不记录轨迹，以上数据来自本次运动记录。"
        ExerciseType.OUTDOOR_RUNNING -> "本次户外跑步没有可用的轨迹数据。"
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
