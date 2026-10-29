package com.example.myrunapp.feature.weight

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myrunapp.ui.theme.AppGrid
import com.example.myrunapp.ui.theme.AppSecondaryText
import java.util.Locale

private val CardDark = Color(0xFF111820)
private val CardLight = Color(0xFF151E26)

@Composable
internal fun LatestWeightCard(detail: WeightDetailUiState) {
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
internal fun WeightOverviewCard(detail: WeightDetailUiState) {
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
internal fun WeightExerciseCorrelationCard(correlation: WeightExerciseCorrelationUiState) {
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
internal fun WeightTrendEntryCard(
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
internal fun DetailCard(
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
