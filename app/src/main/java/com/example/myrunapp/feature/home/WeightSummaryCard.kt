package com.example.myrunapp.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myrunapp.feature.weight.WeightCardUiState
import com.example.myrunapp.feature.weight.formatWeight
import com.example.myrunapp.ui.theme.Accent
import com.example.myrunapp.ui.theme.AppBackground
import com.example.myrunapp.ui.theme.AppGrid
import com.example.myrunapp.ui.theme.AppSecondaryText
import com.example.myrunapp.ui.theme.MyRunAppTheme
import kotlin.math.abs

private val MissingRecordAction = Color(0xFF86EFAC)

@Composable
fun WeightSummaryCard(
    uiState: WeightCardUiState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(236.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF151E26), Color(0xFF111820)),
                        start = Offset.Zero,
                        end = Offset.Infinite
                    )
                )
        ) {
            GradientSportCardBackground(modifier = Modifier.fillMaxSize())
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                WeightCardHeader(hasTodayWeight = uiState.hasTodayWeight)
                WeightMainValue(
                    todayWeight = uiState.todayWeight,
                    latestWeight = uiState.latestWeight,
                    latestDate = uiState.latestDate
                )
                WeightChangeSection(
                    previousChange = uiState.previousChange,
                    totalChange = uiState.totalChange,
                    hasData = uiState.latestWeight != null
                )
            }
        }
    }
}

@Composable
private fun WeightCardHeader(
    hasTodayWeight: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "今日体重",
            color = Color(0xFFF7F8FA),
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CalendarMark()
            Text(
                text = if (hasTodayWeight) "今日已记录" else "今天还未记录",
                color = if (hasTodayWeight) Accent else AppSecondaryText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun CalendarMark(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.width(14.dp).height(14.dp)) {
        val strokeWidth = 1.3.dp.toPx()
        val corner = 2.dp.toPx()
        val body = Path().apply {
            moveTo(corner, 2.dp.toPx())
            lineTo(size.width - corner, 2.dp.toPx())
            quadraticTo(size.width, 2.dp.toPx(), size.width, 2.dp.toPx() + corner)
            lineTo(size.width, size.height - corner)
            quadraticTo(size.width, size.height, size.width - corner, size.height)
            lineTo(corner, size.height)
            quadraticTo(0f, size.height, 0f, size.height - corner)
            lineTo(0f, 2.dp.toPx() + corner)
            quadraticTo(0f, 2.dp.toPx(), corner, 2.dp.toPx())
        }
        drawPath(
            path = body,
            color = AppSecondaryText,
            style = Stroke(width = strokeWidth)
        )
        drawLine(
            color = AppSecondaryText,
            start = Offset(0f, 5.dp.toPx()),
            end = Offset(size.width, 5.dp.toPx()),
            strokeWidth = strokeWidth
        )
    }
}

@Composable
private fun WeightMainValue(
    todayWeight: Double?,
    latestWeight: Double?,
    latestDate: String?,
    modifier: Modifier = Modifier
) {
    if (todayWeight == null) {
        Column(
            modifier = modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "去记录",
                    color = MissingRecordAction,
                    fontSize = 42.sp,
                    lineHeight = 64.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = latestWeight?.let { "最新 ${formatWeight(it)} kg · ${latestDate?.toMonthDayLabel() ?: "-- --"}" } ?: "记录第一条体重",
                color = AppSecondaryText,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        return
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = formatWeight(todayWeight),
            color = Color.White,
            fontSize = 60.sp,
            lineHeight = 64.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "kg",
            color = AppSecondaryText,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 9.dp)
        )
    }
}

@Composable
private fun WeightChangeSection(
    previousChange: Double?,
    totalChange: Double?,
    hasData: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(70.dp)
            .background(Color(0xB30C1218), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        if (!hasData) {
            Text(
                text = "记录你的第一条体重",
                color = AppSecondaryText,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center)
            )
            return@Box
        }

        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            WeightChangeItem(
                change = previousChange,
                label = "较上次",
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(42.dp)
                    .background(AppGrid.copy(alpha = 0.65f))
            )
            WeightChangeItem(
                change = totalChange,
                label = "累计变化",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun WeightChangeItem(
    change: Double?,
    label: String,
    modifier: Modifier = Modifier
) {
    val display = change.toChangeDisplay()
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = display.text,
            color = display.color,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        Text(
            text = label,
            color = AppSecondaryText,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

private data class ChangeDisplay(
    val text: String,
    val color: Color
)

private fun Double?.toChangeDisplay(): ChangeDisplay {
    if (this == null) {
        return ChangeDisplay("--.-- kg", AppSecondaryText)
    }
    val value = abs(this)
    return when {
        this < 0.0 -> ChangeDisplay("↓ ${formatWeight(value)} kg", Color(0xFF22C55E))
        this > 0.0 -> ChangeDisplay("↑ ${formatWeight(value)} kg", Color(0xFFF87171))
        else -> ChangeDisplay("${formatWeight(0.0)} kg", AppSecondaryText)
    }
}

private fun String.toMonthDayLabel(): String {
    return if (length >= 10) substring(5, 10) else this
}

@Preview(showBackground = true)
@Composable
private fun WeightSummaryCardPreview() {
    MyRunAppTheme(darkTheme = true) {
        Box(
            modifier = Modifier
                .background(AppBackground)
                .padding(16.dp)
        ) {
            WeightSummaryCard(
                uiState = WeightCardUiState(
                    todayWeight = 77.20,
                    hasTodayWeight = true,
                    latestWeight = 77.20,
                    latestDate = "2026-08-20",
                    previousChange = -0.30,
                    totalChange = -1.80
                ),
                onClick = {}
            )
        }
    }
}
