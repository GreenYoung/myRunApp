package com.example.myrunapp.feature.exercise

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myrunapp.feature.home.GradientSportCardBackground
import com.example.myrunapp.feature.run.RunTrackPointUiModel
import com.example.myrunapp.ui.components.AppPageTopBar
import com.example.myrunapp.ui.components.AppDangerButton
import com.example.myrunapp.ui.components.AppDialogButtonRow
import com.example.myrunapp.ui.components.AppInputField
import com.example.myrunapp.ui.components.AppPrimaryButton
import com.example.myrunapp.ui.components.AppSecondaryButton
import com.example.myrunapp.ui.components.ExerciseDatePicker
import com.example.myrunapp.ui.components.PageTopSpacing
import com.example.myrunapp.ui.theme.Accent
import com.example.myrunapp.ui.theme.AppBackground
import com.example.myrunapp.ui.theme.AppGrid
import com.example.myrunapp.ui.theme.AppSecondaryText
import com.example.myrunapp.ui.theme.AppSurface
import com.example.myrunapp.ui.theme.MyRunAppTheme
import kotlin.math.abs

private val ExerciseGreen = Color(0xFF22C55E)
private val CardDark = Color(0xFF111820)
private val RecordRowMinHeight = 64.dp

@Composable
fun ExerciseRoute(
    viewModel: ExerciseViewModel,
    onBack: () -> Unit,
    onRecordClick: (Long) -> Unit,
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
        onTypeChange = viewModel::onTypeChange,
        onDateChange = viewModel::onDateChange,
        onDistanceChange = viewModel::onDistanceChange,
        onDurationSelected = viewModel::onDurationSelected,
        onInclineChange = viewModel::onInclineChange,
        onSave = viewModel::saveExercise,
        onDeleteRecord = viewModel::deleteExercise,
        onEditRecord = viewModel::showEditDialog,
        onRecordClick = onRecordClick,
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
    onTypeChange: (ExerciseType) -> Unit,
    onDateChange: (String) -> Unit,
    onDistanceChange: (String) -> Unit,
    onDurationSelected: (Int) -> Unit,
    onInclineChange: (String) -> Unit,
    onSave: () -> Unit,
    onDeleteRecord: (Long) -> Unit,
    onEditRecord: (ExerciseRecordUiModel) -> Unit,
    onRecordClick: (Long) -> Unit,
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppBackground)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ExerciseGoalButton(
                        onClick = onGoalClick,
                        modifier = Modifier.weight(0.4f)
                    )
                    AppPrimaryButton(
                        text = "+ 增加运动记录",
                        onClick = onAddClick,
                        modifier = Modifier.weight(0.6f),
                        height = 56.dp
                    )
                }
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
                bottom = 18.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { ExerciseDetailTopBar(onBack = onBack) }
            item {
                ExerciseOverviewCard(
                    detail = uiState.detail,
                    onStatsRangeChange = onStatsRangeChange
                )
            }
            if (uiState.detail.records.isEmpty()) {
                item { EmptyExerciseRecords() }
            } else {
                items(uiState.detail.records, key = { it.id }) { record ->
                    ExerciseRecordItem(
                        record = record,
                        onClick = { onRecordClick(record.id) },
                        onLongClick = { actionRecord = record }
                    )
                }
            }
        }
    }

    if (uiState.isAddDialogVisible) {
        AddExerciseRecordDialog(
            uiState = uiState,
            onDismiss = onDismissDialog,
            onTypeChange = onTypeChange,
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
    AppPageTopBar(title = "运动详情", showBackButton = true, onBackClick = onBack)
}

@Composable
private fun ExerciseOverviewCard(
    detail: ExerciseDetailUiState,
    onStatsRangeChange: (ExerciseStatsRange) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF151E26), CardDark),
                        start = Offset.Zero,
                        end = Offset.Infinite
                    )
                )
        ) {
            //GradientSportCardBackground(modifier = Modifier.fillMaxSize())
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                ExerciseStatsRangeSelector(
                    selectedRange = detail.selectedStatsRange,
                    onRangeChange = onStatsRangeChange
                )
                OverviewRow {
                    OverviewMetric("${formatDistance(detail.rangeStats.totalDistanceKm)} km", "累计里程", Modifier.weight(1f))
                    OverviewMetric("${formatCalories(detail.rangeStats.totalCaloriesKcal)} kcal", "累计消耗", Modifier.weight(1f))
                }
                OverviewRow {
                    OverviewMetric("${detail.rangeStats.checkInDays} 天", "打卡天数", Modifier.weight(1f))
                    OverviewMetric(detail.rangeStats.averagePaceText, "平均配速", Modifier.weight(1f))
                }
                OverviewRow {
                    OverviewMetric("${formatDistance(detail.rangeStats.longestDistanceKm)} km", "单次最长距离", Modifier.weight(1f))
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ExerciseStatsRangeSelector(
    selectedRange: ExerciseStatsRange,
    onRangeChange: (ExerciseStatsRange) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xB30C1218), RoundedCornerShape(12.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ExerciseStatsRange.values().forEach { range ->
            val selected = range == selectedRange
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .background(
                        color = if (selected) ExerciseGreen else Color.Transparent,
                        shape = RoundedCornerShape(9.dp)
                    )
                    .clickable { onRangeChange(range) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = range.label,
                    color = if (selected) Color(0xFF06130E) else AppSecondaryText,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 14.sp,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun OverviewRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

@Composable
private fun OverviewMetric(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(value, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(label, color = AppSecondaryText, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ExerciseGoalButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = AppBackground),
        border = androidx.compose.foundation.BorderStroke(1.dp, ExerciseGreen)
    ) {
        Text("运动目标", color = ExerciseGreen, fontWeight = FontWeight.Bold)
    }
}

private val ExerciseStatsRange.label: String
    get() = when (this) {
        ExerciseStatsRange.WEEK -> "周"
        ExerciseStatsRange.MONTH -> "月"
        ExerciseStatsRange.YEAR -> "年"
        ExerciseStatsRange.ALL -> "全部"
    }

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ExerciseRecordItem(
    record: ExerciseRecordUiModel,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    if (record.isOutdoorRun && record.hasTrack) {
        OutdoorExerciseRecordItem(record = record, onClick = onClick, onLongClick = onLongClick)
    } else {
        BasicExerciseRecordItem(record = record, onClick = onClick, onLongClick = onLongClick)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BasicExerciseRecordItem(
    record: ExerciseRecordUiModel,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .defaultMinSize(minHeight = RecordRowMinHeight)
            .padding(horizontal = 2.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(2.55f),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🏃", color = ExerciseGreen)
                Text(record.typeText, color = Color.White, fontWeight = FontWeight.SemiBold)
            }
            Text(
                text = record.dateTimeText,
                modifier = Modifier.weight(1.55f),
                color = AppSecondaryText,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.End,
                maxLines = 1
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RecordValue(record.distanceText, Modifier.weight(1.05f))
            RecordValue(record.durationText, Modifier.weight(0.9f))
            RecordValue(record.paceText, Modifier.weight(1.18f))
            RecordValue(record.caloriesText, Modifier.weight(1.05f))
        }
        HorizontalDivider(color = Color.White.copy(alpha = 0.10f), thickness = 1.dp)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun OutdoorExerciseRecordItem(
    record: ExerciseRecordUiModel,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .defaultMinSize(minHeight = 82.dp)
            .padding(horizontal = 2.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(2.55f),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🏃", color = ExerciseGreen)
                Text(record.typeText, color = Color.White, fontWeight = FontWeight.SemiBold)
            }
            Text(
                text = record.dateTimeText,
                modifier = Modifier.weight(1.55f),
                color = AppSecondaryText,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.End,
                maxLines = 1
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            TrackThumbnail(
                points = record.trackPoints,
                modifier = Modifier.size(60.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RecordValue(record.distanceText, Modifier.weight(1f))
                    RecordValue(record.durationText, Modifier.weight(1f))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RecordValue(record.paceText, Modifier.weight(1f))
                    RecordValue(record.caloriesText, Modifier.weight(1f))
                }
            }
        }
        HorizontalDivider(color = Color.White.copy(alpha = 0.10f), thickness = 1.dp)
    }
}

@Composable
private fun TrackThumbnail(
    points: List<RunTrackPointUiModel>,
    modifier: Modifier = Modifier
) {
    val validPoints = points.filter {
        it.latitude in -90.0..90.0 &&
            it.longitude in -180.0..180.0 &&
            !(it.latitude == 0.0 && it.longitude == 0.0)
    }
    Canvas(
        modifier = modifier
            .background(Color(0xB30C1218), RoundedCornerShape(12.dp))
            .padding(8.dp)
    ) {
        if (validPoints.size < 2) return@Canvas

        val minLat = validPoints.minOf { it.latitude }
        val maxLat = validPoints.maxOf { it.latitude }
        val minLng = validPoints.minOf { it.longitude }
        val maxLng = validPoints.maxOf { it.longitude }
        val latSpan = (maxLat - minLat).takeIf { it > 0.0 } ?: 0.0001
        val lngSpan = (maxLng - minLng).takeIf { it > 0.0 } ?: 0.0001

        fun offsetOf(point: RunTrackPointUiModel): Offset {
            val x = ((point.longitude - minLng) / lngSpan).toFloat() * size.width
            val y = size.height - ((point.latitude - minLat) / latSpan).toFloat() * size.height
            return Offset(x, y)
        }

        val offsets = validPoints.map(::offsetOf)
        val path = Path().apply {
            moveTo(offsets.first().x, offsets.first().y)
            offsets.drop(1).forEach { lineTo(it.x, it.y) }
        }
        drawPath(
            path = path,
            color = ExerciseGreen,
            style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round)
        )
        drawCircle(color = Color(0xFF38BDF8), radius = 3.dp.toPx(), center = offsets.first())
        drawCircle(color = ExerciseGreen, radius = 3.dp.toPx(), center = offsets.last())
    }
}

@Composable
private fun ExerciseGoalDialog(
    weeklyGoal: String,
    monthlyGoal: String,
    error: String?,
    onWeeklyGoalChange: (String) -> Unit,
    onMonthlyGoalChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppSurface,
        title = {
            Text(
                text = "运动目标",
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                ExerciseGoalInputField(
                    label = "周运动里程",
                    value = weeklyGoal,
                    onValueChange = onWeeklyGoalChange
                )
                ExerciseGoalInputField(
                    label = "月运动里程",
                    value = monthlyGoal,
                    onValueChange = onMonthlyGoalChange
                )
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            AppDialogButtonRow(
                onCancel = onDismiss,
                onConfirm = onSave,
                confirmText = "保存"
            )
        }
    )
}

@Composable
private fun ExerciseGoalInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppInputField(
            label = label,
            value = value,
            onValueChange = onValueChange,
            keyboardType = KeyboardType.Decimal,
            modifier = Modifier.weight(1f)
        )
        Text("km", color = AppSecondaryText, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ExerciseRecordActionDialog(
    record: ExerciseRecordUiModel,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppSurface,
        title = {
            Text("记录操作", color = Color.White, fontWeight = FontWeight.Bold)
        },
        text = {
            Text(
                text = "${record.dateTimeText} · ${record.typeText} · ${record.distanceText}",
                color = AppSecondaryText,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AppSecondaryButton(
                    text = "取消",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    height = 52.dp
                )
                AppPrimaryButton(
                    text = "编辑",
                    onClick = onEdit,
                    modifier = Modifier.weight(1f),
                    height = 52.dp
                )
                AppDangerButton(text = "删除", onClick = onDelete, modifier = Modifier.weight(1f))
            }
        }
    )
}

@Composable
private fun DeleteExerciseRecordDialog(
    record: ExerciseRecordUiModel,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppSurface,
        title = {
            Text("删除运动记录", color = Color.White, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("确认删除这条运动记录？", color = Color.White)
                Text(
                    text = "${record.dateTimeText} · ${record.typeText} · ${record.distanceText}",
                    color = AppSecondaryText,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AppSecondaryButton(
                    text = "取消",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    height = 52.dp
                )
                AppDangerButton(text = "删除", onClick = onDelete, modifier = Modifier.weight(1f))
            }
        }
    )
}

@Composable
private fun RecordValue(text: String, modifier: Modifier = Modifier) {
    Text(text = text, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, modifier = modifier)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddExerciseRecordDialog(
    uiState: ExerciseUiState,
    onDismiss: () -> Unit,
    onTypeChange: (ExerciseType) -> Unit,
    onDateChange: (String) -> Unit,
    onDistanceChange: (String) -> Unit,
    onDurationSelected: (Int) -> Unit,
    onInclineChange: (String) -> Unit,
    onSave: () -> Unit
) {
    var isDatePickerVisible by remember { mutableStateOf(false) }
    var isDurationPickerVisible by remember { mutableStateOf(false) }

    AlertDialog(
        modifier = Modifier.imePadding(),
        onDismissRequest = onDismiss,
        containerColor = AppSurface,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 430.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ExerciseDateField(
                    value = uiState.inputDate.ifBlank { todayExerciseDate() },
                    onClick = { isDatePickerVisible = true }
                )
                ExerciseTypeSelector(selected = uiState.inputType, onTypeChange = onTypeChange)
                InputField("里程 km", uiState.inputDistanceKm, onDistanceChange, KeyboardType.Decimal)
                ExerciseDurationField(
                    selectedMinutes = uiState.selectedDurationMinutes,
                    onClick = { isDurationPickerVisible = true }
                )
                if (uiState.inputType == ExerciseType.TREADMILL) {
                    InputField("坡度 %", uiState.inputInclinePercent, onInclineChange, KeyboardType.Decimal)
                }
                uiState.inputError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = { AppDialogButtonRow(onCancel = onDismiss, onConfirm = onSave, confirmText = "保存") }
    )

    if (isDatePickerVisible) {
        ExerciseDatePicker(
            selectedDateText = uiState.inputDate,
            onDateSelected = {
                onDateChange(it)
                isDatePickerVisible = false
            },
            onDismiss = { isDatePickerVisible = false }
        )
    }

    if (isDurationPickerVisible) {
        ExerciseDurationPicker(
            selectedMinutes = uiState.selectedDurationMinutes,
            onDurationSelected = {
                onDurationSelected(it)
                isDurationPickerVisible = false
            },
            onDismiss = { isDurationPickerVisible = false }
        )
    }
}

@Composable
private fun ExerciseDateField(value: String, onClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text("运动日期", color = AppSecondaryText, style = MaterialTheme.typography.bodySmall)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(AppBackground, RoundedCornerShape(8.dp))
                .border(1.dp, AppGrid, RoundedCornerShape(8.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(value, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Text("📅", color = Accent, fontSize = 18.sp)
        }
    }
}

@Composable
private fun ExerciseDurationField(selectedMinutes: Int?, onClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text("运动时长", color = AppSecondaryText, style = MaterialTheme.typography.bodySmall)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(AppBackground, RoundedCornerShape(8.dp))
                .border(1.dp, AppGrid, RoundedCornerShape(8.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = selectedMinutes?.let { "$it 分钟" } ?: "请选择",
                color = if (selectedMinutes == null) AppSecondaryText else Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text("›", color = Accent, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ExerciseDurationPicker(
    selectedMinutes: Int?,
    onDurationSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF151E26), CardDark),
                        start = Offset.Zero,
                        end = Offset.Infinite
                    ),
                    shape = RoundedCornerShape(18.dp)
                )
                .padding(horizontal = 16.dp, vertical = 22.dp),
            contentAlignment = Alignment.Center
        ) {
            DurationWheel(
                initialMinutes = selectedMinutes ?: 30,
                onDurationSelected = onDurationSelected
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DurationWheel(
    initialMinutes: Int,
    onDurationSelected: (Int) -> Unit
) {
    val safeInitial = initialMinutes.coerceIn(1, 300)
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = safeInitial - 1
    )
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val centeredMinutes = centeredDurationMinutes(listState) ?: safeInitial

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        contentAlignment = Alignment.Center
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            state = listState,
            flingBehavior = flingBehavior,
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 88.dp)
        ) {
            items(300) { index ->
                val minutes = index + 1
                DurationWheelItem(
                    minutes = minutes,
                    isCentered = minutes == centeredMinutes,
                    distanceFromCenter = abs(minutes - centeredMinutes),
                    onClick = {
                        if (minutes == centeredMinutes) {
                            onDurationSelected(minutes)
                        }
                    }
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            HorizontalDivider(color = ExerciseGreen.copy(alpha = 0.35f), thickness = 1.dp)
            HorizontalDivider(color = ExerciseGreen.copy(alpha = 0.35f), thickness = 1.dp)
        }
    }
}

@Composable
private fun DurationWheelItem(
    minutes: Int,
    isCentered: Boolean,
    distanceFromCenter: Int,
    onClick: () -> Unit
) {
    val alpha = when {
        isCentered -> 1f
        distanceFromCenter == 1 -> 0.78f
        distanceFromCenter == 2 -> 0.48f
        else -> 0.25f
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (isCentered) "$minutes 分钟" else minutes.toString(),
            color = if (isCentered) ExerciseGreen else AppSecondaryText.copy(alpha = alpha),
            fontSize = if (isCentered) 22.sp else 17.sp,
            fontWeight = if (isCentered) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}

private fun centeredDurationMinutes(listState: LazyListState): Int? {
    val layoutInfo = listState.layoutInfo
    val visibleItems = layoutInfo.visibleItemsInfo
    if (visibleItems.isEmpty()) return null

    val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
    val centeredItem = visibleItems.minByOrNull { item ->
        abs((item.offset + item.size / 2) - viewportCenter)
    } ?: return null

    return (centeredItem.index + 1).coerceIn(1, 300)
}

@Composable
private fun ExerciseTypeSelector(selected: ExerciseType, onTypeChange: (ExerciseType) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf(ExerciseType.OUTDOOR_RUNNING, ExerciseType.TREADMILL).forEach { type ->
            val active = selected == type
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(if (active) Accent else AppBackground, RoundedCornerShape(8.dp))
                    .border(1.dp, if (active) Accent else AppGrid, RoundedCornerShape(8.dp))
                    .clickable { onTypeChange(type) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(exerciseTypeText(type), color = if (active) Color(0xFF06130E) else Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun InputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType,
    modifier: Modifier = Modifier
) {
    AppInputField(
        label = label,
        value = value,
        onValueChange = onValueChange,
        keyboardType = keyboardType,
        modifier = modifier
    )
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
                    records = listOf(
                        ExerciseRecordUiModel(1, ExerciseType.TREADMILL, 1_777_046_400_000, 1_980, 5.20, "跑步机", "2026-08-21", "5.20 km", "33min", "6'23\"/km", "356 kcal"),
                        ExerciseRecordUiModel(2, ExerciseType.OUTDOOR_RUNNING, 1_776_960_000_000, 5_460, 4.80, "户外", "2026-08-20", "4.80 km", "1h31min", "6'35\"/km", "328 kcal")
                    )
                )
            ),
            onBack = {},
            onAddClick = {},
            onDismissDialog = {},
            onGoalClick = {},
            onDismissGoalDialog = {},
            onWeeklyGoalChange = {},
            onMonthlyGoalChange = {},
            onSaveGoals = {},
            onTypeChange = {},
            onDateChange = {},
            onDistanceChange = {},
            onDurationSelected = {},
            onInclineChange = {},
            onSave = {},
            onDeleteRecord = {},
            onEditRecord = {},
            onRecordClick = {},
            onStatsRangeChange = {}
        )
    }
}
