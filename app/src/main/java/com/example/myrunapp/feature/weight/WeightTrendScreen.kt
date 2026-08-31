package com.example.myrunapp.feature.weight

import android.content.res.Configuration
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                        start = PageHorizontalPadding,
                        top = PageTopSpacing,
                        end = PageHorizontalPadding,
                        bottom = if (isLandscape) 10.dp else 18.dp
                    ),
                verticalArrangement = Arrangement.spacedBy(if (isLandscape) 8.dp else 14.dp)
            ) {
                AppPageTopBar(title = "体重趋势", showBackButton = true, onBackClick = onBack)
                if (!isLandscape) {
                    WeightTrendRangeSelector(selected = uiState.selectedRange, onRangeChange = onRangeChange)
                }
                WeightTrendChartCard(
                    uiState = uiState,
                    selectedIndex = selectedIndex,
                    onPointSelected = { selectedIndex = it },
                    isLandscape = isLandscape,
                    isFullscreen = false,
                    onChartDoubleTap = { isFullscreenChart = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
                if (isLandscape) {
                    WeightTrendRangeSelector(selected = uiState.selectedRange, onRangeChange = onRangeChange)
                }
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
        modifier = modifier
            .fillMaxWidth()
            .background(TrendCardDark, RoundedCornerShape(18.dp))
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        WeightRange.entries.forEach { range ->
            val isSelected = selected == range
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        color = if (isSelected) TrendGreen.copy(alpha = 0.20f) else Color.Transparent,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable { onRangeChange(range) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = range.label,
                    color = if (isSelected) TrendGreen else AppSecondaryText,
                    fontSize = 14.sp,
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
    onChartDoubleTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(if (isFullscreen || isLandscape) 16.dp else 20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = TrendCardDark)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(TrendCardLight.copy(alpha = 0.20f))
                .padding(if (isLandscape) 10.dp else 14.dp)
        ) {
            if (uiState.records.isEmpty()) {
                WeightTrendEmptyState(modifier = Modifier.fillMaxSize())
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(if (isLandscape || isFullscreen) 6.dp else 12.dp)
                ) {
                    if (!isLandscape && !isFullscreen) {
                        Text("体重变化", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    WeightTrendChart(
                        records = uiState.records,
                        targetWeight = uiState.targetWeight,
                        selectedIndex = selectedIndex.coerceIn(0, uiState.records.lastIndex),
                        onPointSelected = onPointSelected,
                        onDoubleTap = onChartDoubleTap,
                        showBottomDateLabels = !isFullscreen,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                    if (!isFullscreen) {
                        WeightTrendSelectedInfo(
                            point = uiState.records.getOrNull(selectedIndex.coerceIn(0, uiState.records.lastIndex)),
                            isLandscape = isLandscape
                        )
                    }
                }
            }
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

@Composable
private fun WeightTrendSelectedInfo(
    point: WeightTrendPointUiState?,
    isLandscape: Boolean,
    modifier: Modifier = Modifier
) {
    if (point == null) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(if (isLandscape) 48.dp else 62.dp)
            .background(Color(0x990C1218), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TrendInfoItem(title = "日期", value = point.date.substring(5), modifier = Modifier.weight(1f))
        TrendInfoItem(title = "体重", value = "${formatWeight(point.weightKg)} kg", modifier = Modifier.weight(1f))
        TrendInfoItem(
            title = "较上次",
            value = point.previousChange.toSignedTrendText(),
            color = point.previousChange.toTrendChangeColor(),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TrendInfoItem(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    color: Color = Color.White
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(title, color = AppSecondaryText, style = MaterialTheme.typography.bodySmall, maxLines = 1)
        Text(value, color = color, fontWeight = FontWeight.Bold, maxLines = 1, textAlign = TextAlign.Center)
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
    modifier: Modifier = Modifier
) {
    val weightPoints = remember(records) { records.map { WeightPoint(it.date, it.weightKg) } }
    val axisRecords = remember(weightPoints, targetWeight) {
        if (targetWeight == null) {
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
