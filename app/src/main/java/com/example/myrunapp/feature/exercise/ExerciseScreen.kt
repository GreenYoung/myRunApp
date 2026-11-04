package com.example.myrunapp.feature.exercise

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myrunapp.feature.run.formatRunClock
import com.example.myrunapp.ui.components.AppPageTopBar
import com.example.myrunapp.ui.components.AppPrimaryButton
import com.example.myrunapp.ui.components.AppSecondaryButton
import com.example.myrunapp.ui.components.PageTopSpacing
import com.example.myrunapp.ui.dialogs.AddExerciseRecordDialog
import com.example.myrunapp.ui.dialogs.DeleteExerciseRecordDialog
import com.example.myrunapp.ui.dialogs.ExerciseGoalDialog
import com.example.myrunapp.ui.dialogs.ExerciseRecordActionDialog
import com.example.myrunapp.ui.theme.AppBackground
import com.example.myrunapp.ui.theme.AppSecondaryText
import com.example.myrunapp.ui.theme.MyRunAppTheme

internal val ExerciseGreen = Color(0xFF22C55E)
internal val ExerciseTabularNumbers = TextStyle(fontFeatureSettings = "tnum")

private val ExerciseCardTop = Color(0xFF1A2530)
private val ExerciseCardBottom = Color(0xFF10161D)
private val ExerciseDivider = Color(0x14FFFFFF)
private val ExerciseSegmentedBackground = Color(0xB30C1218)

@Composable
fun ExerciseRoute(
    viewModel: ExerciseViewModel,
    onBack: () -> Unit,
    onRecordClick: (Long) -> Unit,
    onStatsPeriodClick: (ExerciseStatsPeriod) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    ExerciseDetailScreen(
        uiState = uiState,
        onBack = onBack,
        onAddClick = viewModel::showAddDialog,
        onDismissDialog = viewModel::hideAddDialog,
        onGoalClick = viewModel::showGoalDialog,
        onDismissGoalDialog = viewModel::hideGoalDialog,
        onWeeklyGoalChange = viewModel::onWeeklyGoalChange,
        onMonthlyGoalChange = viewModel::onMonthlyGoalChange,
        onSaveGoals = viewModel::saveExerciseGoals,
        onDateChange = viewModel::onDateChange,
        onDistanceChange = viewModel::onDistanceChange,
        onDurationSelected = viewModel::onDurationSelected,
        onInclineChange = viewModel::onInclineChange,
        onSave = viewModel::saveExercise,
        onDeleteRecord = viewModel::deleteExercise,
        onEditRecord = viewModel::showEditDialog,
        onRecordClick = onRecordClick,
        onStatsPeriodClick = onStatsPeriodClick,
        onStatsRangeChange = viewModel::onStatsRangeChange,
        modifier = modifier
    )
}

@Composable
fun ExerciseDetailScreen(
    uiState: ExerciseUiState,
    onBack: () -> Unit,
    onAddClick: () -> Unit,
    onDismissDialog: () -> Unit,
    onGoalClick: () -> Unit,
    onDismissGoalDialog: () -> Unit,
    onWeeklyGoalChange: (String) -> Unit,
    onMonthlyGoalChange: (String) -> Unit,
    onSaveGoals: () -> Unit,
    onDateChange: (String) -> Unit,
    onDistanceChange: (String) -> Unit,
    onDurationSelected: (Int) -> Unit,
    onInclineChange: (String) -> Unit,
    onSave: () -> Unit,
    onDeleteRecord: (Long) -> Unit,
    onEditRecord: (ExerciseRecordUiModel) -> Unit,
    onRecordClick: (Long) -> Unit,
    onStatsPeriodClick: (ExerciseStatsPeriod) -> Unit,
    onStatsRangeChange: (ExerciseStatsRange) -> Unit,
    modifier: Modifier = Modifier
) {
    var actionRecord by remember { mutableStateOf<ExerciseRecordUiModel?>(null) }
    var pendingDeleteRecord by remember { mutableStateOf<ExerciseRecordUiModel?>(null) }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(AppBackground)
            .statusBarsPadding(),
        containerColor = AppBackground,
        contentWindowInsets = WindowInsets(0.dp),
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppBackground)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AppSecondaryButton(
                    text = "目标",
                    onClick = onGoalClick,
                    modifier = Modifier.weight(0.35f),
                    height = 56.dp
                )
                AppPrimaryButton(
                    text = "+ 增加运动记录",
                    onClick = onAddClick,
                    modifier = Modifier.weight(0.65f),
                    height = 56.dp
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBackground)
                .padding(paddingValues),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp,
                top = PageTopSpacing,
                end = 16.dp,
                bottom = 104.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { ExerciseDetailTopBar(onBack = onBack) }
            item {
                ExerciseSummaryCard(
                    detail = uiState.detail,
                    onGoalClick = onGoalClick,
                    onStatsPeriodClick = onStatsPeriodClick
                )
            }
            if (uiState.detail.records.isEmpty()) {
                item { EmptyExerciseRecords() }
            } else {
                val monthGroups = uiState.detail.records.toExerciseMonthGroups()
                monthGroups.forEach { group ->
                    item(key = "month-${group.year}-${group.month}") {
                        ExerciseMonthCard(
                            group = group,
                            monthlyGoalKm = uiState.goal.monthlyGoalKm,
                            onRecordClick = onRecordClick,
                            onLongClick = { actionRecord = it }
                        )
                    }
                }
            }
        }
    }

    if (uiState.isAddDialogVisible) {
        AddExerciseRecordDialog(
            uiState = uiState,
            onDismiss = onDismissDialog,
            onDateChange = onDateChange,
            onDistanceChange = onDistanceChange,
            onDurationSelected = onDurationSelected,
            onInclineChange = onInclineChange,
            onSave = onSave
        )
    }

    if (uiState.isGoalDialogVisible) {
        ExerciseGoalDialog(
            weeklyGoal = uiState.inputWeeklyGoalKm,
            monthlyGoal = uiState.inputMonthlyGoalKm,
            error = uiState.goalInputError,
            onWeeklyGoalChange = onWeeklyGoalChange,
            onMonthlyGoalChange = onMonthlyGoalChange,
            onDismiss = onDismissGoalDialog,
            onSave = onSaveGoals
        )
    }

    actionRecord?.let { record ->
        ExerciseRecordActionDialog(
            record = record,
            onDismiss = { actionRecord = null },
            onEdit = {
                actionRecord = null
                onEditRecord(record)
            },
            onDelete = {
                actionRecord = null
                pendingDeleteRecord = record
            }
        )
    }

    pendingDeleteRecord?.let { record ->
        DeleteExerciseRecordDialog(
            record = record,
            onDismiss = { pendingDeleteRecord = null },
            onDelete = {
                onDeleteRecord(record.id)
                pendingDeleteRecord = null
            }
        )
    }
}

@Composable
private fun ExerciseDetailTopBar(onBack: () -> Unit) {
    AppPageTopBar(title = "运动记录", showBackButton = true, onBackClick = onBack)
}

@Composable
private fun ExerciseSummaryCard(
    detail: ExerciseDetailUiState,
    onGoalClick: () -> Unit,
    onStatsPeriodClick: (ExerciseStatsPeriod) -> Unit
) {
    val records = detail.records
    val totalDurationSeconds = records.sumOf { it.durationSeconds }
    val averageDistanceKm = if (records.isEmpty()) 0.0 else detail.totalDistanceKm / records.size
    val pace = splitPaceUnit(formatPace(totalDurationSeconds, detail.totalDistanceKm))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(ExerciseCardTop, ExerciseCardBottom),
                    start = Offset.Zero,
                    end = Offset.Infinite
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(22.dp))
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val glowCenter = Offset(size.width * 0.9f, size.height * 0.02f)
            val glowRadius = size.width * 0.75f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(ExerciseGreen.copy(alpha = 0.16f), Color.Transparent),
                    center = glowCenter,
                    radius = glowRadius
                ),
                radius = glowRadius,
                center = glowCenter
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text("累计里程", color = AppSecondaryText, fontSize = 13.sp)
                    Row {
                        Text(
                            text = formatDistance(detail.totalDistanceKm),
                            color = Color.White,
                            fontSize = 34.sp,
                            lineHeight = 38.sp,
                            fontWeight = FontWeight.Black,
                            style = ExerciseTabularNumbers,
                            modifier = Modifier.alignByBaseline()
                        )
                        Text(
                            text = " km",
                            color = AppSecondaryText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.alignByBaseline()
                        )
                    }
                }
                ExerciseGoalChip(onClick = onGoalClick)
            }
            HorizontalDivider(color = ExerciseDivider)
            Row(modifier = Modifier.fillMaxWidth()) {
                ExerciseHeroStat("累计运动", "${records.size}", "次", Modifier.weight(1f))
                ExerciseHeroStat("总时长", formatRunClock(totalDurationSeconds), "", Modifier.weight(1f))
                ExerciseHeroStat("总消耗", formatCaloriesCompact(detail.totalCaloriesKcal), "kcal", Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                ExerciseHeroStat("平均里程", formatDistance(averageDistanceKm), "km", Modifier.weight(1f))
                ExerciseHeroStat("平均配速", pace.first, pace.second, Modifier.weight(1f))
                ExerciseHeroStat("最长连续", "${detail.consistency.streakDays}", "天", Modifier.weight(1f))
            }
            ExerciseStatsSegmented(onStatsPeriodClick = onStatsPeriodClick)
        }
    }
}

@Composable
private fun ExerciseGoalChip(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(ExerciseGreen.copy(alpha = 0.12f))
            .border(1.dp, ExerciseGreen.copy(alpha = 0.3f), RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("⚑", color = ExerciseGreen, fontSize = 11.sp)
        Text("目标", color = ExerciseGreen, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ExerciseHeroStat(
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        // 数值和单位都显式用同一个 style（不继承主题的 lineHeight），
        // 这样"有单位/没单位"的三列数值行高度一致，标签才会落在同一水平线上。
        Row {
            Text(
                text = value,
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
                style = ExerciseTabularNumbers,
                modifier = Modifier.alignByBaseline()
            )
            if (unit.isNotEmpty()) {
                Text(
                    text = " $unit",
                    color = AppSecondaryText,
                    fontSize = 10.sp,
                    maxLines = 1,
                    softWrap = false,
                    style = ExerciseTabularNumbers,
                    modifier = Modifier.alignByBaseline()
                )
            }
        }
        Text(label, color = AppSecondaryText, fontSize = 12.sp, maxLines = 1)
    }
}

@Composable
private fun ExerciseStatsSegmented(
    onStatsPeriodClick: (ExerciseStatsPeriod) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ExerciseSegmentedBackground, RoundedCornerShape(12.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        listOf(
            ExerciseStatsPeriod.WEEK to "周统计",
            ExerciseStatsPeriod.MONTH to "月统计",
            ExerciseStatsPeriod.YEAR to "年统计"
        ).forEach { (period, label) ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(34.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .clickable { onStatsPeriodClick(period) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = ExerciseGreen,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
        }
    }
}

private fun splitPaceUnit(paceText: String): Pair<String, String> {
    return if (paceText.endsWith("/km")) {
        paceText.removeSuffix("/km") to "/km"
    } else {
        paceText to ""
    }
}

@Composable
private fun EmptyExerciseRecords() {
    Box(modifier = Modifier
        .fillMaxWidth()
        .height(190.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("暂无运动记录", color = Color.White, fontWeight = FontWeight.Bold)
            Text("添加第一条运动记录，\n开始记录你的运动数据", color = AppSecondaryText, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ExerciseDetailScreenPreview() {
    MyRunAppTheme(darkTheme = true) {
        ExerciseDetailScreen(
            uiState = ExerciseUiState(
                detail = ExerciseDetailUiState(
                    totalDistanceKm = 532.80,
                    checkInDays = 48,
                    weeklyDistanceKm = 21.30,
                    monthlyDistanceKm = 65.80,
                    totalCaloriesKcal = 18542,
                    monthlyCaloriesKcal = 6842,
                    selectedStatsRange = ExerciseStatsRange.MONTH,
                    rangeStats = ExerciseRangeStatsUiState(
                        range = ExerciseStatsRange.MONTH,
                        totalDistanceKm = 65.80,
                        totalCaloriesKcal = 6842,
                        checkInDays = 12,
                        averagePaceText = "6'18\"/km",
                        longestDistanceKm = 8.60
                    ),
                    consistency = ExerciseConsistencyUiState(streakDays = 48),
                    records = listOf(
                        ExerciseRecordUiModel(1, ExerciseType.TREADMILL, 1_777_046_400_000, 1_980, 5.20, "跑步机", "2026-08-21", "5.20 km", "33min", "6'23\"/km", "356 kcal"),
                        ExerciseRecordUiModel(2, ExerciseType.OUTDOOR_RUNNING, 1_776_960_000_000, 5_460, 4.80, "户外", "2026-08-20", "4.80 km", "1h31min", "6'35\"/km", "328 kcal")
                    )
                ),
                goal = ExerciseGoalUiState(weeklyGoalKm = 20.0, monthlyGoalKm = 80.0)
            ),
            onBack = {},
            onAddClick = {},
            onDismissDialog = {},
            onGoalClick = {},
            onDismissGoalDialog = {},
            onWeeklyGoalChange = {},
            onMonthlyGoalChange = {},
            onSaveGoals = {},
            onDateChange = {},
            onDistanceChange = {},
            onDurationSelected = {},
            onInclineChange = {},
            onSave = {},
            onDeleteRecord = {},
            onEditRecord = {},
            onRecordClick = {},
            onStatsPeriodClick = {},
            onStatsRangeChange = {}
        )
    }
}
