package com.example.myrunapp.feature.weight

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myrunapp.ui.components.AppPageTopBar
import com.example.myrunapp.ui.components.AppDialogButtonRow
import com.example.myrunapp.ui.components.AppDangerButton
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
import java.util.Locale
import kotlin.math.abs

private val DetailGreen = Color(0xFF22C55E)
private val DetailRed = Color(0xFFF87171)
private val CardDark = Color(0xFF111820)
private val CardLight = Color(0xFF151E26)
private val RecordRowMinHeight = 38.dp

@Composable
fun WeightRoute(
    viewModel: WeightViewModel,
    onBack: () -> Unit,
    onTrendClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    WeightDetailScreen(
        uiState = uiState,
        onBack = onBack,
        onRecordClick = viewModel::showRecordDialog,
        onEditRecord = viewModel::showEditRecordDialog,
        onDeleteRecord = viewModel::deleteWeight,
        onDismissRecordDialog = viewModel::hideRecordDialog,
        onDateChange = viewModel::onInputDateChange,
        onWeightChange = viewModel::onInputWeightChange,
        onNoteChange = viewModel::onInputNoteChange,
        onSaveWeight = viewModel::saveWeight,
        onRangeChange = viewModel::onRangeChange,
        onTrendClick = onTrendClick,
        onTargetClick = viewModel::showTargetDialog,
        onDismissTargetDialog = viewModel::hideTargetDialog,
        onTargetWeightChange = viewModel::onTargetWeightChange,
        onHeightCmChange = viewModel::onHeightCmChange,
        onSaveTargetWeight = viewModel::saveTargetWeight,
        modifier = modifier
    )
}

@Composable
fun WeightDetailScreen(
    uiState: WeightUiState,
    onBack: () -> Unit,
    onRecordClick: () -> Unit,
    onEditRecord: (WeightRecordItem) -> Unit,
    onDeleteRecord: (WeightRecordItem) -> Unit,
    onDismissRecordDialog: () -> Unit,
    onDateChange: (String) -> Unit,
    onWeightChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onSaveWeight: () -> Unit,
    onRangeChange: (WeightRange) -> Unit,
    onTrendClick: () -> Unit,
    onTargetClick: () -> Unit,
    onDismissTargetDialog: () -> Unit,
    onTargetWeightChange: (String) -> Unit,
    onHeightCmChange: (String) -> Unit,
    onSaveTargetWeight: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 23.dp, end = 23.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                top = PageTopSpacing,
                bottom = 28.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { WeightDetailTopBar(onBack = onBack) }
            item { LatestWeightCard(uiState.weightDetail) }
            item { WeightOverviewCard(uiState.weightDetail) }
            item { WeightExerciseCorrelationCard(uiState.weightDetail.exerciseCorrelation) }
            item { WeightTrendEntryCard(uiState = uiState.weightDetail, onClick = onTrendClick) }
            item {
                RecentWeightRecords(
                    records = uiState.weightDetail.recentRecords,
                    onEditRecord = onEditRecord,
                    onDeleteRecord = onDeleteRecord
                )
            }
            item {
                WeightActionSection(
                    targetWeight = uiState.weightDetail.targetWeight,
                    onTargetClick = onTargetClick,
                    onRecordClick = onRecordClick
                )
            }
        }

        if (uiState.isRecordDialogVisible) {
            WeightInputDialog(
                uiState = uiState,
                onDismiss = onDismissRecordDialog,
                onDateChange = onDateChange,
                onWeightChange = onWeightChange,
                onNoteChange = onNoteChange,
                onSave = onSaveWeight
            )
        }

        if (uiState.isTargetDialogVisible) {
            TargetWeightDialog(
                value = uiState.inputTargetWeight,
                heightCm = uiState.inputHeightCm,
                error = uiState.targetInputError,
                onValueChange = onTargetWeightChange,
                onHeightCmChange = onHeightCmChange,
                onDismiss = onDismissTargetDialog,
                onSave = onSaveTargetWeight
            )
        }
    }
}

@Composable
private fun WeightDetailTopBar(onBack: () -> Unit) {
    AppPageTopBar(title = "体重详情", showBackButton = true, onBackClick = onBack)
}

@Composable
private fun LatestWeightCard(detail: WeightDetailUiState) {
    DetailCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("最新体重", color = AppSecondaryText, style = MaterialTheme.typography.titleMedium)
                WeightValueText(weight = detail.latestWeight, numberSize = 54)
                WeightBmiSummary(detail.bmi)
                Text(
                    text = detail.latestDate?.let { "记录日期：$it" } ?: "暂无记录",
                    color = AppSecondaryText,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            TargetProgressBlock(detail)
        }
    }
}

@Composable
private fun WeightBmiSummary(bmi: WeightBmiUiState) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "BMI",
            color = AppSecondaryText,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = bmi.bmi?.let { String.format(Locale.US, "%.1f", it) } ?: "--",
            color = if (bmi.bmi == null) AppSecondaryText else DetailGreen,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = bmi.category,
            color = AppSecondaryText,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
private fun TargetProgressBlock(detail: WeightDetailUiState) {
    Column(
        modifier = Modifier.width(96.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (detail.targetWeight == null || detail.targetProgress == null) {
            Text("目标体重", color = AppSecondaryText, style = MaterialTheme.typography.bodySmall)
            Text("未设置", color = Color.White, fontWeight = FontWeight.Bold)
            return
        }

        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(74.dp)) {
                val stroke = 7.dp.toPx()
                drawCircle(color = AppGrid, radius = (size.minDimension - stroke) / 2f, style = Stroke(width = stroke))
                drawArc(
                    color = DetailGreen,
                    startAngle = -90f,
                    sweepAngle = 360f * detail.targetProgress,
                    useCenter = false,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
            Text("${(detail.targetProgress * 100).toInt()}%", color = Color.White, fontWeight = FontWeight.Bold)
        }
        Text("目标进度", color = AppSecondaryText, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun WeightOverviewCard(detail: WeightDetailUiState) {
    DetailCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(82.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OverviewItem(
                title = "累计变化",
                value = detail.totalChange.toSignedPlainText(),
                subtitle = detail.firstWeight?.let { "初始 ${formatWeight(it)} kg" } ?: "初始 --",
                color = detail.totalChange.toChangeColor(),
                modifier = Modifier.weight(1f)
            )
            OverviewDivider()
            OverviewItem(
                title = "目标体重",
                value = detail.targetWeight?.let { "${formatWeight(it)} kg" } ?: "未设置",
                subtitle = detail.remainingWeight?.let { if (it <= 0.0) "已达成" else "剩余 ${formatWeight(it)} kg" } ?: "点击设置",
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            OverviewDivider()
            OverviewItem(
                title = "本周变化",
                value = detail.weeklyChange.toSignedPlainText(),
                subtitle = "最近7天",
                color = detail.weeklyChange.toChangeColor(),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun OverviewItem(
    title: String,
    value: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(title, color = AppSecondaryText, style = MaterialTheme.typography.bodySmall)
        Text(value, color = color, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(subtitle, color = AppSecondaryText, style = MaterialTheme.typography.bodySmall, maxLines = 1)
    }
}

@Composable
private fun OverviewDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(54.dp)
            .background(AppGrid.copy(alpha = 0.7f))
    )
}

@Composable
private fun WeightHealthInsightCard(detail: WeightDetailUiState) {
    DetailCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("健康分析", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(76.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HealthInsightItem(
                    title = "BMI",
                    value = detail.bmi.bmi?.let { String.format(Locale.US, "%.1f", it) } ?: "--",
                    subtitle = detail.bmi.category,
                    color = if (detail.bmi.bmi == null) AppSecondaryText else DetailGreen,
                    modifier = Modifier.weight(1f)
                )
                OverviewDivider()
                HealthInsightItem(
                    title = detail.changeSpeed.label,
                    value = detail.changeSpeed.description,
                    subtitle = "体重变化速度",
                    color = detail.changeSpeed.weeklyChangeKg.toChangeColor(),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun HealthInsightItem(
    title: String,
    value: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(title, color = AppSecondaryText, style = MaterialTheme.typography.bodySmall)
        Text(value, color = color, fontWeight = FontWeight.Bold, maxLines = 1, textAlign = TextAlign.Center)
        Text(subtitle, color = AppSecondaryText, style = MaterialTheme.typography.bodySmall, maxLines = 1, textAlign = TextAlign.Center)
    }
}

@Composable
private fun WeightExerciseCorrelationCard(correlation: WeightExerciseCorrelationUiState) {
    DetailCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("运动关联", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(correlation.insight, color = DetailGreen, style = MaterialTheme.typography.bodySmall, maxLines = 1)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(78.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CorrelationMetric(
                    title = "近7天运动",
                    value = "${formatWeight(correlation.sevenDayDistanceKm)} km",
                    subtitle = "消耗 ${correlation.sevenDayCaloriesKcal} kcal",
                    modifier = Modifier.weight(1f)
                )
                OverviewDivider()
                CorrelationMetric(
                    title = "近7天体重",
                    value = correlation.sevenDayWeightChangeKg.toSignedPlainText(),
                    subtitle = "较区间首条",
                    color = correlation.sevenDayWeightChangeKg.toChangeColor(),
                    modifier = Modifier.weight(1f)
                )
                OverviewDivider()
                CorrelationMetric(
                    title = "近30天",
                    value = "${formatWeight(correlation.thirtyDayDistanceKm)} km",
                    subtitle = correlation.thirtyDayWeightChangeKg.toSignedPlainText(),
                    color = correlation.thirtyDayWeightChangeKg.toChangeColor(),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun CorrelationMetric(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    color: Color = Color.White
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(title, color = AppSecondaryText, style = MaterialTheme.typography.bodySmall, maxLines = 1)
        Text(value, color = color, fontWeight = FontWeight.Bold, maxLines = 1, textAlign = TextAlign.Center)
        Text(subtitle, color = AppSecondaryText, style = MaterialTheme.typography.bodySmall, maxLines = 1, textAlign = TextAlign.Center)
    }
}

@Composable
private fun WeightTrendEntryCard(
    uiState: WeightDetailUiState,
    onClick: () -> Unit
) {
    DetailCard(
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("体重趋势", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = if (uiState.recentRecords.isEmpty()) "记录体重后查看完整趋势" else "7天 / 30天 / 90天 / 全部",
                    color = AppSecondaryText,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Text("查看", color = DetailGreen, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun RecentWeightRecords(
    records: List<WeightRecordItem>,
    onEditRecord: (WeightRecordItem) -> Unit,
    onDeleteRecord: (WeightRecordItem) -> Unit
) {
    var actionRecord by remember { mutableStateOf<WeightRecordItem?>(null) }
    var pendingDeleteRecord by remember { mutableStateOf<WeightRecordItem?>(null) }
    var expandedDate by remember { mutableStateOf<String?>(null) }

    DetailCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "体重记录",
                color = Color.White,
                fontSize = 16.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.Bold
            )
            if (records.isEmpty()) {
                Text("暂无记录", color = AppSecondaryText, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                return@Column
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 240.dp)
            ) {
                items(records, key = { it.date }) { record ->
                    WeightRecordRow(
                        record = record,
                        expanded = expandedDate == record.date,
                        onClick = {
                            expandedDate = if (expandedDate == record.date) null else record.date
                        },
                        onLongClick = { actionRecord = record },
                        onEdit = { onEditRecord(record) },
                        onDelete = { pendingDeleteRecord = record }
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.10f), thickness = 1.dp)
                }
            }
        }
    }

    actionRecord?.let { record ->
        WeightRecordActionDialog(
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
        DeleteWeightRecordDialog(
            record = record,
            onDismiss = { pendingDeleteRecord = null },
            onDelete = {
                onDeleteRecord(record)
                pendingDeleteRecord = null
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WeightRecordRow(
    record: WeightRecordItem,
    expanded: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .defaultMinSize(minHeight = RecordRowMinHeight)
            .padding(vertical = 0.dp),
        verticalArrangement = if (expanded) Arrangement.spacedBy(6.dp) else Arrangement.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = record.date,
                modifier = Modifier.weight(1.45f),
                color = AppSecondaryText,
                fontSize = 13.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            Text(
                text = "${formatWeight(record.weightKg)} kg",
                modifier = Modifier.weight(1f),
                color = Color.White,
                fontSize = 14.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = record.previousChange.toArrowChangeText(unit = false),
                modifier = Modifier.weight(0.85f),
                color = record.previousChange.toChangeColor(),
                fontSize = 13.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
        if (expanded) {
            WeightRecordExpandedContent(onEdit = onEdit, onDelete = onDelete)
        }
    }
}

@Composable
private fun WeightRecordExpandedContent(
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AppSecondaryButton(
                text = "编辑",
                onClick = onEdit,
                modifier = Modifier.weight(1f),
                height = 42.dp
            )
            AppDangerButton(
                text = "删除",
                onClick = onDelete,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun WeightRecordActionDialog(
    record: WeightRecordItem,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppSurface,
        title = { Text("记录操作", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Text(
                text = "${record.date} · ${formatWeight(record.weightKg)} kg",
                color = AppSecondaryText,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AppSecondaryButton(text = "取消", onClick = onDismiss, modifier = Modifier.weight(1f), height = 52.dp)
                AppPrimaryButton(text = "编辑", onClick = onEdit, modifier = Modifier.weight(1f), height = 52.dp)
                AppDangerButton(text = "删除", onClick = onDelete, modifier = Modifier.weight(1f))
            }
        }
    )
}

@Composable
private fun DeleteWeightRecordDialog(
    record: WeightRecordItem,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppSurface,
        title = { Text("删除体重记录", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Text(
                text = "${record.date} · ${formatWeight(record.weightKg)} kg",
                color = AppSecondaryText,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AppSecondaryButton(text = "取消", onClick = onDismiss, modifier = Modifier.weight(1f), height = 52.dp)
                AppDangerButton(text = "删除", onClick = onDelete, modifier = Modifier.weight(1f))
            }
        }
    )
}

@Composable
private fun WeightActionSection(
    targetWeight: Double?,
    onTargetClick: () -> Unit,
    onRecordClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AppSecondaryButton(
            text = "目标体重\n${targetWeight?.let { "${formatWeight(it)} kg" } ?: "未设置"}",
            onClick = onTargetClick,
            modifier = Modifier.weight(1f),
            height = 58.dp
        )
        AppPrimaryButton(text = "+ 记录体重", onClick = onRecordClick, modifier = Modifier.weight(1f), height = 58.dp)
    }
}

@Composable
private fun WeightValueText(weight: Double?, numberSize: Int) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        Text(
            text = weight?.let(::formatWeight) ?: "--.--",
            color = Color.White,
            fontSize = numberSize.sp,
            lineHeight = (numberSize + 4).sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(7.dp))
        Text("kg", color = AppSecondaryText, fontSize = 20.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 7.dp))
    }
}

@Composable
private fun DetailCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark)
    ) {
        Column(
            modifier = Modifier
                .background(CardLight.copy(alpha = 0.18f))
                .padding(18.dp),
            content = content
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeightInputDialog(
    uiState: WeightUiState,
    onDismiss: () -> Unit,
    onDateChange: (String) -> Unit,
    onWeightChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onSave: () -> Unit
) {
    var isCalendarVisible by remember { mutableStateOf(false) }

    AlertDialog(
        modifier = Modifier.imePadding(),
        onDismissRequest = onDismiss,
        containerColor = AppSurface,
        title = { Text("记录体重", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AppInputField(
                    label = "体重 kg",
                    value = uiState.inputWeight,
                    onValueChange = onWeightChange,
                    keyboardType = KeyboardType.Decimal
                )
                DatePickerField(selectedDate = uiState.inputDate, onClick = { isCalendarVisible = true })
                WeightNoteInputField(
                    value = uiState.inputNote,
                    onValueChange = onNoteChange
                )
                uiState.inputError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = { AppDialogButtonRow(onCancel = onDismiss, onConfirm = onSave, confirmText = "保存") }
    )

    if (isCalendarVisible) {
        ExerciseDatePicker(
            selectedDateText = uiState.inputDate,
            onDateSelected = {
                onDateChange(it)
                isCalendarVisible = false
            },
            onDismiss = { isCalendarVisible = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TargetWeightDialog(
    value: String,
    heightCm: String,
    error: String?,
    onValueChange: (String) -> Unit,
    onHeightCmChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        modifier = Modifier.imePadding(),
        onDismissRequest = onDismiss,
        containerColor = AppSurface,
        title = { Text("设置目标体重", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AppInputField(
                    label = "目标体重 kg",
                    value = value,
                    onValueChange = onValueChange,
                    keyboardType = KeyboardType.Decimal
                )
                AppInputField(
                    label = "身高 cm（用于 BMI）",
                    value = heightCm,
                    onValueChange = onHeightCmChange,
                    keyboardType = KeyboardType.Decimal
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = { AppDialogButtonRow(onCancel = onDismiss, onConfirm = onSave, confirmText = "保存") }
    )
}

@Composable
private fun WeightNoteInputField(
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.take(100)) },
        modifier = Modifier
            .fillMaxWidth()
            .height(82.dp),
        label = { Text("备注（选填）") },
        singleLine = false,
        maxLines = 3,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
    )
}

@Composable
private fun DatePickerField(
    selectedDate: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppBackground, RoundedCornerShape(8.dp))
            .border(1.dp, AppGrid, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("日期", color = AppSecondaryText, style = MaterialTheme.typography.bodySmall)
            Text(selectedDate, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        Text("选择", color = Accent, fontWeight = FontWeight.Bold)
    }
}

private val WeightRange.label: String
    get() = when (this) {
        WeightRange.DAYS_7 -> "7天"
        WeightRange.DAYS_30 -> "30天"
        WeightRange.DAYS_90 -> "90天"
        WeightRange.ALL -> "全部"
    }

private fun Double?.toArrowChangeText(unit: Boolean): String {
    if (this == null) return "--"
    val suffix = if (unit) " kg" else ""
    val value = formatWeight(abs(this))
    return when {
        this < 0.0 -> "↓ $value$suffix"
        this > 0.0 -> "↑ $value$suffix"
        else -> "0.00$suffix"
    }
}

private fun Double?.toSignedPlainText(): String {
    if (this == null) return "--"
    return String.format(Locale.US, "%+.2f kg", this)
}

private fun Double?.toChangeColor(): Color {
    return when {
        this == null -> AppSecondaryText
        this < 0.0 -> DetailGreen
        this > 0.0 -> DetailRed
        else -> AppSecondaryText
    }
}

@Preview(showBackground = true)
@Composable
private fun WeightDetailScreenPreview() {
    MyRunAppTheme(darkTheme = true) {
        WeightDetailScreen(
            uiState = WeightUiState(
                weightDetail = WeightDetailUiState(
                    latestWeight = 77.20,
                    previousChange = -0.30,
                    totalChange = -1.80,
                    weeklyChange = -0.60,
                    firstWeight = 79.00,
                    targetWeight = 72.00,
                    remainingWeight = 5.20,
                    targetProgress = 0.35f,
                    latestDate = "2026-08-20",
                    bmi = WeightBmiUiState(heightCm = 178.0, bmi = 24.4, category = "偏高"),
                    changeSpeed = WeightChangeSpeedUiState(weeklyChangeKg = -0.42, description = "↓ 0.42 kg/周"),
                    exerciseCorrelation = WeightExerciseCorrelationUiState(
                        sevenDayDistanceKm = 12.3,
                        sevenDayCaloriesKcal = 760,
                        sevenDayWeightChangeKg = -0.4,
                        thirtyDayDistanceKm = 48.2,
                        thirtyDayWeightChangeKg = -1.2,
                        insight = "最近运动后体重呈下降趋势"
                    ),
                    chartRecords = listOf(
                        WeightPoint("2026-08-16", 78.0),
                        WeightPoint("2026-08-17", 77.8),
                        WeightPoint("2026-08-18", 77.6),
                        WeightPoint("2026-08-19", 77.5),
                        WeightPoint("2026-08-20", 77.2)
                    ),
                    recentRecords = listOf(
                        WeightRecordItem("2026-08-20", 77.2, -0.3, "跑后称重"),
                        WeightRecordItem("2026-08-19", 77.5, -0.1)
                    )
                )
            ),
            onBack = {},
            onRecordClick = {},
            onEditRecord = {},
            onDeleteRecord = {},
            onDismissRecordDialog = {},
            onDateChange = {},
            onWeightChange = {},
            onNoteChange = {},
            onSaveWeight = {},
            onRangeChange = {},
            onTrendClick = {},
            onTargetClick = {},
            onDismissTargetDialog = {},
            onTargetWeightChange = {},
            onHeightCmChange = {},
            onSaveTargetWeight = {}
        )
    }
}
