package com.example.myrunapp.feature.weight

import android.content.res.Configuration
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myrunapp.ui.components.appClickable
import com.example.myrunapp.ui.components.AppBackButton
import com.example.myrunapp.ui.components.AppPageTopBar
import com.example.myrunapp.ui.components.PageHorizontalPadding
import com.example.myrunapp.ui.components.PageTopSpacing
import com.example.myrunapp.ui.theme.AppBackground
import com.example.myrunapp.ui.theme.AppGrid
import com.example.myrunapp.ui.theme.AppSecondaryText
import com.example.myrunapp.ui.theme.MyRunAppTheme
import kotlin.math.abs

private val TrendGreen = Color(0xFF22C55E)
private val TrendRed = Color(0xFFF87171)
private val TrendCardDark = Color(0xFF111820)
private val TrendCardLight = Color(0xFF151E26)
private val WeightTrendHeroTop = Color(0xFF17251D)
private val WeightTrendHeroBottom = Color(0xFF0C1319)

@Composable
fun WeightTrendRoute(
    viewModel: WeightViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    WeightTrendScreen(
        uiState = uiState.weightTrend,
        onBack = onBack,
        onRangeChange = viewModel::onRangeChange,
        modifier = modifier
    )
}

@Composable
fun WeightTrendScreen(
    uiState: WeightTrendUiState,
    onBack: () -> Unit,
    onRangeChange: (WeightRange) -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    var selectedIndex by remember { mutableIntStateOf(-1) }
    var isFullscreenChart by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.records) {
        selectedIndex = uiState.records.lastIndex
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        if (isFullscreenChart) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = PageHorizontalPadding,
                        top = PageTopSpacing,
                        end = PageHorizontalPadding,
                        bottom = 12.dp
                    )
            ) {
                AppBackButton(
                    onClick = { isFullscreenChart = false },
                    modifier = Modifier.align(Alignment.TopStart)
                )
                WeightTrendChartCard(
                    uiState = uiState,
                    selectedIndex = selectedIndex,
                    onPointSelected = { selectedIndex = it },
                    isLandscape = true,
                    isFullscreen = true,
                    onRangeChange = onRangeChange,
                    onChartDoubleTap = {},
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 54.dp)
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = 20.dp,
                        top = PageTopSpacing,
                        end = 20.dp,
                        bottom = if (isLandscape) 10.dp else 18.dp
                    ),
                verticalArrangement = Arrangement.spacedBy(if (isLandscape) 8.dp else 14.dp)
            ) {
                AppPageTopBar(title = "", showBackButton = true, onBackClick = onBack)
                if (!isLandscape) {
                    WeightTrendSummaryCard(uiState = uiState)
                }
                WeightTrendChartCard(
                    uiState = uiState,
                    selectedIndex = selectedIndex,
                    onPointSelected = { selectedIndex = it },
                    isLandscape = isLandscape,
                    isFullscreen = false,
                    onRangeChange = onRangeChange,
                    onChartDoubleTap = { isFullscreenChart = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            }
        }
    }
}

@Composable
private fun WeightTrendRangeSelector(
    selected: WeightRange,
    onRangeChange: (WeightRange) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        WeightRange.entries.forEach { range ->
            val isSelected = selected == range
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (isSelected) TrendGreen.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.05f)
                    )
                    .border(
                        width = 1.dp,
                        color = if (isSelected) TrendGreen.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(50)
                    )
                    .appClickable { onRangeChange(range) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = range.label,
                    color = if (isSelected) TrendGreen else AppSecondaryText,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun WeightTrendChartCard(
    uiState: WeightTrendUiState,
    selectedIndex: Int,
    onPointSelected: (Int) -> Unit,
    isLandscape: Boolean,
    isFullscreen: Boolean,
    onRangeChange: (WeightRange) -> Unit,
    onChartDoubleTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(if (isFullscreen) 16.dp else 22.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(Brush.linearGradient(listOf(TrendCardLight, TrendCardDark)))
            .border(1.dp, Color.White.copy(alpha = 0.08f), shape)
            .padding(if (isLandscape) 10.dp else 14.dp),
        verticalArrangement = Arrangement.spacedBy(if (isLandscape) 8.dp else 10.dp)
    ) {
        if (!isFullscreen) {
            WeightTrendRangeSelector(selected = uiState.selectedRange, onRangeChange = onRangeChange)
        }
        if (uiState.records.isEmpty()) {
            WeightTrendEmptyState(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
            return@Column
        }
        WeightTrendChart(
            records = uiState.records,
            targetWeight = uiState.targetWeight,
            selectedIndex = selectedIndex.coerceIn(0, uiState.records.lastIndex),
            onPointSelected = onPointSelected,
            onDoubleTap = onChartDoubleTap,
            showBottomDateLabels = !isFullscreen,
            isFullscreen = isFullscreen,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )
        if (!isFullscreen) {
            WeightTrendSelectedLine(
                point = uiState.records.getOrNull(selectedIndex.coerceIn(0, uiState.records.lastIndex))
            )
        }
    }
}

@Composable
private fun WeightTrendEmptyState(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("暂无体重数据", color = Color.White, fontWeight = FontWeight.Bold)
            Text("记录体重后即可查看变化趋势", color = AppSecondaryText, style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** 顶部概要卡：当前体重 + 区间变化 + 距目标，替代原来那条四列小字。 */
@Composable
private fun WeightTrendSummaryCard(uiState: WeightTrendUiState) {
    val firstWeight = uiState.records.firstOrNull()?.weightKg
    val latestWeight = uiState.latestWeight ?: uiState.records.lastOrNull()?.weightKg
    val rangeChange = if (firstWeight != null && latestWeight != null) latestWeight - firstWeight else null

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(WeightTrendHeroTop, WeightTrendHeroBottom)))
            .border(1.dp, Color(0x14FFFFFF), RoundedCornerShape(24.dp))
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val center = Offset(size.width * 0.92f, size.height * 0.05f)
            val radius = size.width * 0.7f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(TrendGreen.copy(alpha = 0.16f), Color.Transparent),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )
        }
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("当前体重", color = AppSecondaryText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Text(
                    text = uiState.records.lastOrNull()?.date?.takeLast(5) ?: "暂无记录",
                    color = AppSecondaryText,
                    fontSize = 12.sp
                )
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = latestWeight?.let(::formatWeight) ?: "--.--",
                    color = Color.White,
                    fontSize = 44.sp,
                    lineHeight = 46.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = " kg",
                    color = AppSecondaryText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TrendChip(
                    text = "区间 ${rangeChange.toSignedTrendText()}",
                    color = rangeChange.toTrendChangeColor()
                )
                TrendChip(
                    text = uiState.remainingWeight?.let { "距目标 ${formatWeight(it)} kg" } ?: "未设置目标",
                    color = TrendGreen
                )
            }
        }
    }
}

@Composable
private fun TrendChip(text: String, color: Color) {
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

/** 选中点的一行信息：日期 · 体重 · 较上次。 */
@Composable
private fun WeightTrendSelectedLine(point: WeightTrendPointUiState?, modifier: Modifier = Modifier) {
    if (point == null) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x660C1218))
            .padding(horizontal = 12.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = point.date.takeLast(5),
            color = AppSecondaryText,
            fontSize = 12.sp,
            maxLines = 1
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${formatWeight(point.weightKg)} kg",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = point.previousChange.toSignedTrendText(),
                color = point.previousChange.toTrendChangeColor(),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun WeightTrendChart(
    records: List<WeightTrendPointUiState>,
    targetWeight: Double?,
    selectedIndex: Int,
    onPointSelected: (Int) -> Unit,
    onDoubleTap: () -> Unit,
    showBottomDateLabels: Boolean,
    isFullscreen: Boolean,
    modifier: Modifier = Modifier
) {
    val weightPoints = remember(records) { records.map { WeightPoint(it.date, it.weightKg) } }
    val axisRecords = remember(weightPoints, targetWeight) {
        if (targetWeight == null || weightPoints.isEmpty()) {
            weightPoints
        } else {
            weightPoints + WeightPoint(records.last().date, targetWeight)
        }
    }
    val axis = calculateWeightAxisRange(axisRecords)

    Canvas(
        modifier = modifier
            .pointerInput(records, axis) {
                detectTapGestures(
                    onDoubleTap = { onDoubleTap() },
                    onTap = { offset ->
                        nearestTrendPointIndex(offset, Size(size.width.toFloat(), size.height.toFloat()), weightPoints, axis, this)
                            ?.let(onPointSelected)
                    }
                )
            }
            .pointerInput(records, axis) {
                detectDragGestures(
                    onDragStart = { offset ->
                        nearestTrendPointIndex(offset, Size(size.width.toFloat(), size.height.toFloat()), weightPoints, axis, this)
                            ?.let(onPointSelected)
                    },
                    onDrag = { change, _ ->
                        nearestTrendPointIndex(change.position, Size(size.width.toFloat(), size.height.toFloat()), weightPoints, axis, this)
                            ?.let(onPointSelected)
                    }
                )
            }
    ) {
        val metrics = chartMetrics(size)
        val gridLines = 4
        val axisSpan = (axis.max - axis.min).coerceAtLeast(1.0)
        val labelPaint = Paint().apply {
            color = android.graphics.Color.rgb(141, 147, 161)
            textSize = 11.dp.toPx()
            isAntiAlias = true
        }

        repeat(gridLines + 1) { index ->
            val y = metrics.top + metrics.height * index / gridLines
            val labelValue = axis.max - axisSpan * index / gridLines
            drawLine(
                color = AppGrid.copy(alpha = 0.65f),
                start = Offset(metrics.left, y),
                end = Offset(metrics.right, y),
                strokeWidth = 1.dp.toPx()
            )
            labelPaint.textAlign = Paint.Align.RIGHT
            drawContext.canvas.nativeCanvas.drawText(
                String.format("%.1f", labelValue),
                metrics.left - 8.dp.toPx(),
                y - 1.dp.toPx(),
                labelPaint
            )
        }

        if (targetWeight != null) {
            val y = metrics.bottom - metrics.height * ((targetWeight - axis.min) / axisSpan).toFloat()
            drawLine(
                color = TrendGreen.copy(alpha = 0.45f),
                start = Offset(metrics.left, y),
                end = Offset(metrics.right, y),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx()))
            )
            labelPaint.textAlign = Paint.Align.LEFT
            labelPaint.color = android.graphics.Color.rgb(34, 197, 94)
            drawContext.canvas.nativeCanvas.drawText("目标 ${formatWeight(targetWeight)} kg", metrics.left + 6.dp.toPx(), y - 4.dp.toPx(), labelPaint)
        }

        val offsets = weightPoints.mapIndexed { index, point -> pointOffset(index, point, weightPoints, axis, metrics) }
        if (offsets.size > 1) {
            drawPath(
                path = smoothTrendPath(offsets),
                color = TrendGreen,
                style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        offsets.firstOrNull()?.let { firstOffset ->
            drawEndpointWeightLabel(
                text = formatWeight(weightPoints.first().weightKg),
                offset = firstOffset,
                metrics = metrics,
                paint = labelPaint
            )
        }
        if (offsets.size > 1) {
            offsets.lastOrNull()?.let { lastOffset ->
                drawEndpointWeightLabel(
                    text = formatWeight(weightPoints.last().weightKg),
                    offset = lastOffset,
                    metrics = metrics,
                    paint = labelPaint
                )
            }
        }

        if (!isFullscreen && selectedIndex in offsets.indices) {
            val selectedOffset = offsets[selectedIndex]
            drawLine(
                color = TrendGreen.copy(alpha = 0.30f),
                start = Offset(selectedOffset.x, metrics.top),
                end = Offset(selectedOffset.x, metrics.bottom),
                strokeWidth = 1.dp.toPx()
            )
            drawCircle(
                color = TrendCardDark,
                radius = 5.dp.toPx(),
                center = selectedOffset
            )
            drawCircle(
                color = TrendGreen,
                radius = 3.5.dp.toPx(),
                center = selectedOffset
            )
        }

        if (showBottomDateLabels) {
            labelPaint.color = android.graphics.Color.rgb(141, 147, 161)
            labelPaint.textAlign = Paint.Align.LEFT
            drawContext.canvas.nativeCanvas.drawText(records.first().date.substring(5), metrics.left, size.height - 4.dp.toPx(), labelPaint)
            labelPaint.textAlign = Paint.Align.RIGHT
            drawContext.canvas.nativeCanvas.drawText(records.last().date.substring(5), metrics.right, size.height - 4.dp.toPx(), labelPaint)
        }
    }
}

private fun smoothTrendPath(offsets: List<Offset>): Path {
    return Path().apply {
        moveTo(offsets.first().x, offsets.first().y)
        offsets.zipWithNext().forEach { (from, to) ->
            val controlX = (from.x + to.x) / 2f
            cubicTo(controlX, from.y, controlX, to.y, to.x, to.y)
        }
    }
}

private fun DrawScope.drawEndpointWeightLabel(
    text: String,
    offset: Offset,
    metrics: ChartMetrics,
    paint: Paint
) {
    paint.color = android.graphics.Color.WHITE
    paint.textSize = 11.dp.toPx()
    paint.textAlign = Paint.Align.CENTER
    paint.isAntiAlias = true
    paint.isFakeBoldText = true
    val x = offset.x.coerceIn(metrics.left + 18.dp.toPx(), metrics.right - 18.dp.toPx())
    val y = (offset.y - 7.dp.toPx()).coerceAtLeast(metrics.top + 11.dp.toPx())
    drawContext.canvas.nativeCanvas.drawText(text, x, y, paint)
    paint.isFakeBoldText = false
}

private data class ChartMetrics(
    val left: Float,
    val right: Float,
    val top: Float,
    val bottom: Float
) {
    val width: Float = right - left
    val height: Float = bottom - top
}

private fun Density.chartMetrics(size: Size): ChartMetrics {
    val left = 42.dp.toPx()
    val right = size.width - 8.dp.toPx()
    val top = 14.dp.toPx()
    val bottom = size.height - 24.dp.toPx()
    return ChartMetrics(left = left, right = right, top = top, bottom = bottom)
}

private fun pointOffset(
    index: Int,
    point: WeightPoint,
    records: List<WeightPoint>,
    axis: WeightAxisRange,
    metrics: ChartMetrics
): Offset {
    val axisSpan = (axis.max - axis.min).coerceAtLeast(1.0)
    val x = if (records.size == 1) {
        metrics.left + metrics.width / 2f
    } else {
        metrics.left + metrics.width * index / records.lastIndex.toFloat()
    }
    val y = metrics.bottom - metrics.height * ((point.weightKg - axis.min) / axisSpan).toFloat()
    return Offset(x, y)
}

private fun nearestTrendPointIndex(
    offset: Offset,
    canvasSize: Size,
    records: List<WeightPoint>,
    axis: WeightAxisRange,
    density: Density
): Int? {
    if (records.isEmpty()) return null
    val metrics = with(density) { chartMetrics(canvasSize) }
    return records
        .mapIndexed { index, point -> index to pointOffset(index, point, records, axis, metrics) }
        .minByOrNull { (_, pointOffset) -> abs(pointOffset.x - offset.x) }
        ?.first
}

private val WeightRange.label: String
    get() = when (this) {
        WeightRange.DAYS_7 -> "7天"
        WeightRange.DAYS_30 -> "30天"
        WeightRange.DAYS_90 -> "90天"
        WeightRange.ALL -> "全部"
    }

private fun Double?.toSignedTrendText(): String {
    if (this == null) return "--"
    return String.format(java.util.Locale.US, "%+.2f kg", this)
}

private fun Double?.toTrendChangeColor(): Color {
    return when {
        this == null -> AppSecondaryText
        this < 0.0 -> TrendGreen
        this > 0.0 -> TrendRed
        else -> AppSecondaryText
    }
}

@Preview(showBackground = true)
@Composable
private fun WeightTrendScreenPreview() {
    MyRunAppTheme(darkTheme = true) {
        WeightTrendScreen(
            uiState = WeightTrendUiState(
                selectedRange = WeightRange.DAYS_30,
                targetWeight = 72.00,
                records = listOf(
                    WeightTrendPointUiState("2026-08-16", 78.00, null),
                    WeightTrendPointUiState("2026-08-18", 77.70, -0.30),
                    WeightTrendPointUiState("2026-08-20", 77.50, -0.20),
                    WeightTrendPointUiState("2026-08-22", 77.80, 0.30),
                    WeightTrendPointUiState("2026-08-25", 77.40, -0.40)
                )
            ),
            onBack = {},
            onRangeChange = {}
        )
    }
}
