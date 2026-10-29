package com.example.myrunapp.feature.run

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myrunapp.ui.theme.AppSecondaryText

private val RunInfoOverlayShape = RoundedCornerShape(24.dp)
private val RunInfoOverlayTop = Color(0xF0111922)
private val RunInfoOverlayBottom = Color(0xF7090E13)
private val RunInfoOverlayDivider = Color(0x12FFFFFF)
private val RunInfoNumbers = TextStyle(fontFeatureSettings = "tnum")

data class RunInfoMetricUiModel(
    val value: String,
    val unit: String = "",
    val label: String
)

@Composable
fun RunInfoOverlayCard(
    title: String,
    dateText: String,
    firstRow: Pair<RunInfoMetricUiModel, RunInfoMetricUiModel>,
    secondRow: Pair<RunInfoMetricUiModel, RunInfoMetricUiModel>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RunInfoOverlayShape)
            .background(
                Brush.verticalGradient(colors = listOf(RunInfoOverlayTop, RunInfoOverlayBottom))
            )
            .border(1.dp, Color.White.copy(alpha = 0.07f), RunInfoOverlayShape)
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        RunInfoOverlayHeader(title = title, dateText = dateText)
        RunInfoMetricRow(left = firstRow.first, right = firstRow.second)
        HorizontalDivider(color = RunInfoOverlayDivider)
        RunInfoMetricRow(left = secondRow.first, right = secondRow.second)
    }
}

@Composable
private fun RunInfoOverlayHeader(
    title: String,
    dateText: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = Color.White.copy(alpha = 0.82f),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.06f))
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Text(
                text = dateText,
                color = AppSecondaryText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun RunInfoMetricRow(
    left: RunInfoMetricUiModel,
    right: RunInfoMetricUiModel,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxWidth()) {
        RunInfoMetric(left, Modifier.weight(1f))
        RunInfoMetric(right, Modifier.weight(1f))
    }
}

@Composable
private fun RunInfoMetric(
    metric: RunInfoMetricUiModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row {
            Text(
                text = metric.value,
                color = Color.White,
                fontSize = 28.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
                style = RunInfoNumbers,
                modifier = Modifier.alignByBaseline()
            )
            if (metric.unit.isNotEmpty()) {
                Text(
                    text = " ${metric.unit}",
                    color = AppSecondaryText,
                    fontSize = 12.sp,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.alignByBaseline()
                )
            }
        }
        Text(
            text = metric.label,
            color = AppSecondaryText,
            fontSize = 12.sp,
            maxLines = 1
        )
    }
}
