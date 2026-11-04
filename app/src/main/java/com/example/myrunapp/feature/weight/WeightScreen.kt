package com.example.myrunapp.feature.weight

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myrunapp.ui.components.AppPageTopBar
import com.example.myrunapp.ui.components.AppPrimaryButton
import com.example.myrunapp.ui.components.AppSecondaryButton
import com.example.myrunapp.ui.components.PageTopSpacing
import com.example.myrunapp.ui.dialogs.TargetWeightDialog
import com.example.myrunapp.ui.dialogs.WeightInputDialog
import com.example.myrunapp.ui.theme.AppBackground
import com.example.myrunapp.ui.theme.AppSecondaryText
import com.example.myrunapp.ui.theme.MyRunAppTheme
import java.util.Locale
import kotlin.math.abs

internal val DetailGreen = Color(0xFF22C55E)
internal val DetailRed = Color(0xFFF87171)

private val WeightHeroTop = Color(0xFF17251D)
private val WeightHeroBottom = Color(0xFF0C1319)
private val WeightTrendTop = Color(0xFF151E26)
private val WeightTrendBottom = Color(0xFF0D141A)
private val WeightHairline = Color(0x14FFFFFF)
private val WeightMint = Color(0xFF34D399)

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
                .padding(start = 20.dp, end = 20.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                top = PageTopSpacing,
                bottom = 28.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { WeightDetailTopBar(onBack = onBack) }
            item { WeightHeroCard(uiState.weightDetail) }
            item { WeightTrendCard(uiState.weightDetail, onClick = onTrendClick) }
            item {
                RecentWeightRecords(
                    records = uiState.weightDetail.recentRecords,
                    onEditRecord = onEditRecord,
                    onDeleteRecord = onDeleteRecord
                )
            }
            item {
                WeightActionSection(
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
    AppPageTopBar(title = "体重", showBackButton = true, onBackClick = onBack)
}

/**
 * 主卡片：最新体重 + 本次变化 + BMI + 目标进度。
 * 原先分散在「最新体重」「体重概览」「目标进度环」三处的信息合成一张卡。
 */
@Composable
private fun WeightHeroCard(detail: WeightDetailUiState) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(Brush.linearGradient(listOf(WeightHeroTop, WeightHeroBottom)))
            .border(1.dp, WeightHairline, RoundedCornerShape(26.dp))
    ) {
        HeroGlow(modifier = Modifier.matchParentSize())
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("最新体重", color = AppSecondaryText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Text(
                    text = detail.latestDate?.takeLast(5) ?: "暂无记录",
                    color = AppSecondaryText,
                    fontSize = 12.sp
                )
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = detail.latestWeight?.let(::formatWeight) ?: "--.--",
                    color = Color.White,
                    fontSize = 54.sp,
                    lineHeight = 56.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = " kg",
                    color = AppSecondaryText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 9.dp)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WeightChip(
                    text = detail.previousChange.toArrowChangeText(unit = true),
                    color = detail.previousChange.toChangeColor()
                )
                WeightChip(
                    text = detail.bmi.bmi?.let {
                        "BMI ${String.format(Locale.US, "%.1f", it)} · ${detail.bmi.category}"
                    } ?: "BMI --",
                    color = AppSecondaryText
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(WeightHairline)
            )
            WeightTargetProgress(detail)
        }
    }
}

@Composable
private fun HeroGlow(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val center = Offset(size.width * 0.92f, size.height * 0.04f)
        val radius = size.width * 0.72f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(DetailGreen.copy(alpha = 0.16f), Color.Transparent),
                center = center,
                radius = radius
            ),
            radius = radius,
            center = center
        )
    }
}

@Composable
private fun WeightChip(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun WeightTargetProgress(detail: WeightDetailUiState) {
    val target = detail.targetWeight
    val remaining = detail.remainingWeight
    val progress = (detail.targetProgress ?: 0f).coerceIn(0f, 1f)

    if (target == null) {
        Text("还没有设置目标体重", color = AppSecondaryText, fontSize = 12.sp)
        return
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "目标 ${formatWeight(target)} kg",
            color = AppSecondaryText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = if (remaining != null && remaining > 0.0) {
                "还差 ${formatWeight(remaining)} kg"
            } else {
                "已达成"
            },
            color = if (remaining != null && remaining > 0.0) WeightMint else DetailGreen,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(Color.White.copy(alpha = 0.08f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress)
                .fillMaxHeight()
                .clip(RoundedCornerShape(3.dp))
                .background(Brush.horizontalGradient(listOf(DetailGreen, WeightMint)))
        )
    }
}

/**
 * 趋势卡：迷你折线 + 本周变化 + 近 7 天跑量，点整张卡进趋势页。
 * 原来「运动关联」和「体重趋势入口」两张卡合并成这一张。
 */
@Composable
private fun WeightTrendCard(detail: WeightDetailUiState, onClick: () -> Unit) {
    val points = detail.chartRecords.ifEmpty {
        detail.recentRecords.asReversed().takeLast(30).map { WeightPoint(it.date, it.weightKg) }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(listOf(WeightTrendTop, WeightTrendBottom)))
            .border(1.dp, WeightHairline, RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("体重趋势", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(
                text = "${detail.selectedRange.labelText()} ›",
                color = DetailGreen,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        WeightSparkline(
            points = points,
            modifier = Modifier
                .fillMaxWidth()
                .height(88.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "本周 ${detail.weeklyChange.toSignedPlainText()}",
                color = detail.weeklyChange.toChangeColor(),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "近7天跑步 ${formatWeight(detail.exerciseCorrelation.sevenDayDistanceKm)} km",
                color = AppSecondaryText,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun WeightSparkline(points: List<WeightPoint>, modifier: Modifier = Modifier) {
    if (points.size < 2) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("再记录一次体重就能看到趋势", color = AppSecondaryText, fontSize = 12.sp)
        }
        return
    }

    val minWeight = points.minOf { it.weightKg }
    val maxWeight = points.maxOf { it.weightKg }
    val span = (maxWeight - minWeight).coerceAtLeast(1.0)

    Canvas(modifier = modifier) {
        val verticalPadding = 8.dp.toPx()
        val usableHeight = (size.height - verticalPadding * 2).coerceAtLeast(1f)
        val stepX = size.width / (points.size - 1)

        val offsets = points.mapIndexed { index, point ->
            Offset(
                x = stepX * index,
                y = verticalPadding + (1f - ((point.weightKg - minWeight) / span).toFloat()) * usableHeight
            )
        }

        val fillPath = Path().apply {
            moveTo(offsets.first().x, size.height)
            offsets.forEach { lineTo(it.x, it.y) }
            lineTo(offsets.last().x, size.height)
            close()
        }
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(DetailGreen.copy(alpha = 0.28f), Color.Transparent),
                startY = offsets.minOf { it.y },
                endY = size.height
            )
        )

        val linePath = Path().apply {
            moveTo(offsets.first().x, offsets.first().y)
            offsets.drop(1).forEach { lineTo(it.x, it.y) }
        }
        drawPath(
            path = linePath,
            color = DetailGreen,
            style = Stroke(
                width = 2.5.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        drawCircle(color = DetailGreen.copy(alpha = 0.35f), radius = 5.5.dp.toPx(), center = offsets.last())
        drawCircle(color = Color.White, radius = 2.4.dp.toPx(), center = offsets.last())
    }
}

@Composable
private fun WeightActionSection(
    onTargetClick: () -> Unit,
    onRecordClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AppSecondaryButton(
            text = "设置目标",
            onClick = onTargetClick,
            modifier = Modifier.weight(1f),
            height = 56.dp
        )
        AppPrimaryButton(text = "+ 记录体重", onClick = onRecordClick, modifier = Modifier.weight(1f), height = 56.dp)
    }
}

private fun WeightRange.labelText(): String {
    return when (this) {
        WeightRange.DAYS_7 -> "近7天"
        WeightRange.DAYS_30 -> "近30天"
        WeightRange.DAYS_90 -> "近90天"
        WeightRange.ALL -> "全部"
    }
}

internal fun Double?.toArrowChangeText(unit: Boolean): String {    if (this == null) return "--"
    val suffix = if (unit) " kg" else ""
    val value = formatWeight(abs(this))
    return when {
        this < 0.0 -> "↓ $value$suffix"
        this > 0.0 -> "↑ $value$suffix"
        else -> "0.00$suffix"
    }
}

internal fun Double?.toSignedPlainText(): String {
    if (this == null) return "--"
    return String.format(Locale.US, "%+.2f kg", this)
}

internal fun Double?.toChangeColor(): Color {
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
                        WeightPoint("2026-08-14", 78.4),
                        WeightPoint("2026-08-15", 78.1),
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
