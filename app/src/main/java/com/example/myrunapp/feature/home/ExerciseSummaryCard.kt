package com.example.myrunapp.feature.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myrunapp.ui.theme.AppBackground
import com.example.myrunapp.ui.theme.AppGrid
import com.example.myrunapp.ui.theme.AppSecondaryText
import com.example.myrunapp.ui.theme.MyRunAppTheme
import com.example.myrunapp.feature.exercise.ExerciseSummaryUiState
import com.example.myrunapp.feature.exercise.formatCalories
import com.example.myrunapp.feature.exercise.formatDistance

private val ExerciseGreen = Color(0xFF22C55E)
private val ExerciseHudGreen = Color(0xFF0BDA51)
private val ExerciseFire = Color(0xFFFF654F)
private val ExerciseCardBackground = Color(0xFF101820)
private val ExerciseHudUnit = Color(0xFFB6BDC5)
private val ExerciseHudLabel = Color(0xFF7F8893)
private val MissingRecordAction = Color(0xFF86EFAC)

@Composable
fun ExerciseSummaryCard(
    uiState: ExerciseSummaryUiState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = ExerciseCardBackground)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(ExerciseCardBackground)
        ) {
            ExerciseSummaryCardBackground(modifier = Modifier.fillMaxSize())
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                ExerciseCardHeader(checkedInToday = uiState.checkedInToday)
                ExerciseHomeHudMetrics(uiState = uiState)
            }
        }
    }
}

@Composable
private fun ExerciseSummaryCardBackground(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val green = ExerciseGreen
        drawCircle(
            color = green.copy(alpha = 0.07f),
            radius = size.width * 0.42f,
            center = Offset(size.width * 0.12f, size.height * 0.05f)
        )
        drawCircle(
            color = Color(0xFF2AEEFF).copy(alpha = 0.045f),
            radius = size.width * 0.46f,
            center = Offset(size.width * 0.88f, size.height * 0.48f)
        )
    }
}

@Composable
private fun ExerciseCardHeader(
    checkedInToday: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            //RunnerMark()
            Text(
                text = "运动打卡",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("●", color = ExerciseHudGreen, fontSize = 13.sp, lineHeight = 13.sp, fontWeight = FontWeight.Bold)
            Text(
                text = if (checkedInToday) "今日已打卡" else "今日未打卡",
                color = if (checkedInToday) ExerciseHudUnit else AppSecondaryText,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun ExerciseHomeHudMetrics(
    uiState: ExerciseSummaryUiState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            ExerciseHomeHudMetric(
                value = if (uiState.checkedInToday) formatDistance(uiState.todayDistanceKm) else "去打卡",
                unit = if (uiState.checkedInToday) "km" else "",
                label = "今日里程",
                valueColor = if (uiState.checkedInToday) Color.White else MissingRecordAction,
                valueFontSize = if (uiState.checkedInToday) 35 else 31,
                unitFontSize = 17,
                modifier = Modifier.weight(1f)
            )
            ExerciseHomeHudMetric(
                value = formatDistance(uiState.weeklyDistanceKm),
                unit = "km",
                label = "本周累计",
                valueFontSize = 32,
                unitFontSize = 16,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            ExerciseHomeHudMetric(
                value = formatDistance(uiState.monthlyDistanceKm),
                unit = "km",
                label = "本月累计",
                valueFontSize = 31,
                unitFontSize = 15,
                modifier = Modifier.weight(1f)
            )
            ExerciseHomeHudMetric(
                value = formatCalories(uiState.todayCaloriesKcal),
                unit = "kcal",
                label = "今日消耗",
                valueFontSize = 31,
                unitFontSize = 15,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ExerciseHomeHudMetric(
    value: String,
    unit: String,
    label: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Color.White,
    valueFontSize: Int,
    unitFontSize: Int
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                color = valueColor,
                fontSize = valueFontSize.sp,
                lineHeight = (valueFontSize + 3).sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                style = TextStyle(fontFeatureSettings = "tnum")
            )
            if (unit.isNotEmpty()) {
                Text(
                    text = " $unit",
                    color = ExerciseHudUnit,
                    fontSize = unitFontSize.sp,
                    lineHeight = (unitFontSize + 2).sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }
        Text(
            text = label,
            color = ExerciseHudLabel,
            fontSize = 13.sp,
            lineHeight = 15.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
private fun ExerciseMainStats(
    uiState: ExerciseSummaryUiState,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TodayDistanceSection(
            todayDistanceKm = uiState.todayDistanceKm,
            dailyGoalKm = uiState.dailyGoalKm,
            dailyProgress = uiState.dailyProgress,
            checkedInToday = uiState.checkedInToday,
            modifier = Modifier.weight(1.25f)
        )
        CircularDistanceSection(
            title = "本周累计",
            distanceKm = uiState.weeklyDistanceKm,
            goalKm = uiState.weeklyGoalKm,
            progress = uiState.weeklyProgress,
            modifier = Modifier.weight(1f)
        )
        CircularDistanceSection(
            title = "本月累计",
            distanceKm = uiState.monthlyDistanceKm,
            goalKm = uiState.monthlyGoalKm,
            progress = uiState.monthlyProgress,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ExerciseGoalReminder(
    uiState: ExerciseSummaryUiState,
    modifier: Modifier = Modifier
) {
    val text = when {
        uiState.weeklyGoalCompleted && uiState.monthlyGoalCompleted -> "周/月目标已完成"
        uiState.weeklyGoalCompleted -> "本周目标已完成"
        uiState.monthlyGoalCompleted -> "本月目标已完成"
        else -> "本周还差 ${formatDistance(uiState.weeklyRemainingKm)} km"
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(ExerciseGreen.copy(alpha = 0.10f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp)
    ) {
        Text(
            text = text,
            color = ExerciseGreen,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun TodayDistanceSection(
    todayDistanceKm: Double,
    dailyGoalKm: Double,
    dailyProgress: Float,
    checkedInToday: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text("今日里程", color = AppSecondaryText, style = MaterialTheme.typography.bodyMedium)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = if (checkedInToday) formatDistance(todayDistanceKm) else "去打卡",
                color = if (checkedInToday) Color.White else MissingRecordAction,
                fontSize = if (checkedInToday) 38.sp else 34.sp,
                lineHeight = 48.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            if (checkedInToday) {
                Text(
                    text = "km",
                    color = AppSecondaryText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier
                        .widthIn(min = 16.dp)
                        .padding(bottom = 6.dp)
                )
            }
        }
//        Text(
//            text = "目标 ${formatDistance(dailyGoalKm)} km",
//            color = AppSecondaryText,
//            style = MaterialTheme.typography.bodySmall,
//            maxLines = 1
//        )
//        Text(
//            text = "完成度 ${formatPercent(dailyProgress)}",
//            color = ExerciseGreen,
//            style = MaterialTheme.typography.bodySmall,
//            fontWeight = FontWeight.Bold,
//            maxLines = 1
//        )

        Text(
            text = if (checkedInToday) "" else "去记录一次运动",
            color = if (checkedInToday) ExerciseGreen else AppSecondaryText,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun CircularDistanceSection(
    title: String,
    distanceKm: Double,
    goalKm: Double,
    progress: Float,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = title,
            color = AppSecondaryText,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1
        )
        Box(contentAlignment = Alignment.Center) {
            CircularGoalProgress(progress = progress, modifier = Modifier.size(72.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = formatDistance(distanceKm),
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text("km", color = AppSecondaryText, style = MaterialTheme.typography.bodySmall)
            }
        }
        Text(
            text = "目标 ${formatDistance(goalKm)}",
            color = AppSecondaryText,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
private fun CircularGoalProgress(
    progress: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val stroke = 6.dp.toPx()
        drawCircle(
            color = AppGrid.copy(alpha = 0.72f),
            radius = (size.minDimension - stroke) / 2f,
            style = Stroke(width = stroke)
        )
        drawArc(
            color = ExerciseGreen,
            startAngle = -90f,
            sweepAngle = 360f * progress.coerceIn(0f, 1f),
            useCenter = false,
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun ExerciseMetricsGrid(
    uiState: ExerciseSummaryUiState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xB30C1218), RoundedCornerShape(16.dp))
            .padding(horizontal = 10.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ExerciseMetricItem(
                icon = MetricIcon.Route,
                label = "总里程",
                value = formatDistance(uiState.totalDistanceKm),
                unit = "km",
                iconColor = ExerciseGreen,
                modifier = Modifier.weight(1f)
            )
            MetricsDivider()
            ExerciseMetricItem(
                icon = MetricIcon.Calendar,
                label = "打卡天数",
                value = uiState.checkInDays.toString(),
                unit = "天",
                iconColor = ExerciseGreen,
                modifier = Modifier.weight(1f)
            )
            MetricsDivider()
            ExerciseMetricItem(
                icon = MetricIcon.Fire,
                label = "今日消耗",
                value = formatCalories(uiState.todayCaloriesKcal),
                unit = "kcal",
                iconColor = ExerciseFire,
                modifier = Modifier.weight(1f)
            )
        }

//        Row(
//            modifier = Modifier.fillMaxWidth(),
//            horizontalArrangement = Arrangement.spacedBy(8.dp),
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            ExerciseMetricItem(
//                icon = MetricIcon.Fire,
//                label = "本周消耗",
//                value = formatCalories(uiState.weeklyCaloriesKcal),
//                unit = "kcal",
//                iconColor = ExerciseFire,
//                modifier = Modifier.weight(1f)
//            )
//            MetricsDivider()
//            ExerciseMetricItem(
//                icon = MetricIcon.Fire,
//                label = "本月消耗",
//                value = formatCalories(uiState.monthlyCaloriesKcal),
//                unit = "kcal",
//                iconColor = ExerciseFire,
//                modifier = Modifier.weight(1f)
//            )
//        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ExerciseMetricItem(
                icon = MetricIcon.Route,
                label = "本周目标",
                value = if (uiState.weeklyGoalCompleted) "已完成" else formatDistance(uiState.weeklyRemainingKm),
                unit = if (uiState.weeklyGoalCompleted) "" else "km",
                iconColor = ExerciseGreen,
                modifier = Modifier.weight(1f)
            )
            MetricsDivider()
            ExerciseMetricItem(
                icon = MetricIcon.Route,
                label = "月完成",
                value = uiState.monthlyProgressPercent.toString(),
                unit = "%",
                iconColor = ExerciseGreen,
                modifier = Modifier.weight(1f)
            )
            MetricsDivider()
            ExerciseMetricItem(
                icon = MetricIcon.Calendar,
                label = "连续打卡",
                value = uiState.streakDays.toString(),
                unit = "天",
                iconColor = ExerciseGreen,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ExerciseMetricItem(
    icon: MetricIcon,
    label: String,
    value: String,
    unit: String,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(label, color = AppSecondaryText, style = MaterialTheme.typography.bodySmall, maxLines = 1)
        Text(value, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(unit, color = AppSecondaryText, style = MaterialTheme.typography.bodySmall, maxLines = 1)
    }
}

@Composable
private fun MetricsDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(56.dp)
            .background(AppGrid.copy(alpha = 0.55f))
    )
}

@Composable
private fun RunnerMark(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(20.dp)) {
        val stroke = 2.2.dp.toPx()
        drawCircle(color = ExerciseGreen, radius = 2.7.dp.toPx(), center = Offset(size.width * 0.62f, size.height * 0.2f))
        drawLine(color = ExerciseGreen, start = Offset(size.width * 0.52f, size.height * 0.38f), end = Offset(size.width * 0.34f, size.height * 0.58f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color = ExerciseGreen, start = Offset(size.width * 0.52f, size.height * 0.38f), end = Offset(size.width * 0.78f, size.height * 0.48f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color = ExerciseGreen, start = Offset(size.width * 0.44f, size.height * 0.55f), end = Offset(size.width * 0.28f, size.height * 0.82f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color = ExerciseGreen, start = Offset(size.width * 0.47f, size.height * 0.56f), end = Offset(size.width * 0.76f, size.height * 0.80f), strokeWidth = stroke, cap = StrokeCap.Round)
    }
}

@Composable
private fun MetricMark(
    icon: MetricIcon,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(18.dp)) {
        val stroke = 1.8.dp.toPx()
        when (icon) {
            MetricIcon.Route -> {
                drawLine(color = color, start = Offset(size.width * 0.18f, size.height * 0.75f), end = Offset(size.width * 0.82f, size.height * 0.25f), strokeWidth = stroke, cap = StrokeCap.Round)
                drawCircle(color = color, radius = 2.2.dp.toPx(), center = Offset(size.width * 0.18f, size.height * 0.75f))
                drawCircle(color = color, radius = 2.2.dp.toPx(), center = Offset(size.width * 0.82f, size.height * 0.25f))
            }
            MetricIcon.Calendar -> {
                drawRoundRect(color = color, topLeft = Offset(size.width * 0.14f, size.height * 0.20f), size = androidx.compose.ui.geometry.Size(size.width * 0.72f, size.height * 0.66f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx(), 3.dp.toPx()), style = Stroke(width = stroke))
                drawLine(color = color, start = Offset(size.width * 0.14f, size.height * 0.40f), end = Offset(size.width * 0.86f, size.height * 0.40f), strokeWidth = stroke)
            }
            MetricIcon.Fire -> {
                drawLine(color = color, start = Offset(size.width * 0.50f, size.height * 0.16f), end = Offset(size.width * 0.34f, size.height * 0.56f), strokeWidth = stroke, cap = StrokeCap.Round)
                drawLine(color = color, start = Offset(size.width * 0.50f, size.height * 0.16f), end = Offset(size.width * 0.72f, size.height * 0.56f), strokeWidth = stroke, cap = StrokeCap.Round)
                drawCircle(color = color.copy(alpha = 0.82f), radius = 4.dp.toPx(), center = Offset(size.width * 0.52f, size.height * 0.68f))
            }
        }
    }
}

private enum class MetricIcon {
    Route,
    Calendar,
    Fire
}

private fun formatPercent(progress: Float): String {
    return "${(progress.coerceIn(0f, 1f) * 100).toInt()}%"
}

@Preview(showBackground = true)
@Composable
private fun ExerciseSummaryCardPreview() {
    MyRunAppTheme(darkTheme = true) {
        Box(
            modifier = Modifier
                .background(AppBackground)
                .padding(16.dp)
        ) {
            ExerciseSummaryCard(
                uiState = ExerciseSummaryUiState(
                    todayDistanceKm = 5.20,
                    dailyGoalKm = 8.00,
                    dailyProgress = 0.65f,
                    weeklyDistanceKm = 21.30,
                    weeklyGoalKm = 40.00,
                    weeklyProgress = 0.5325f,
                    weeklyRemainingKm = 18.70,
                    weeklyGoalCompleted = false,
                    monthlyDistanceKm = 65.80,
                    monthlyGoalKm = 100.00,
                    monthlyProgress = 0.658f,
                    monthlyProgressPercent = 66,
                    monthlyGoalCompleted = false,
                    totalDistanceKm = 532.80,
                    checkInDays = 48,
                    streakDays = 5,
                    todayCaloriesKcal = 356,
                    weeklyCaloriesKcal = 1587,
                    monthlyCaloriesKcal = 6842,
                    checkedInToday = true
                ),
                onClick = {}
            )
        }
    }
}
