package com.example.myrunapp.feature.run

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myrunapp.ui.theme.AppSecondaryText

val RunInfoOverlayGreen = Color(0xFF22C55E)

private val RunInfoOverlayShape = RoundedCornerShape(22.dp)
private val RunInfoOverlayBase = Color(0x99111820)

data class RunInfoMetricUiModel(
    val value: String,
    val label: String
)

@Composable
fun RunInfoOverlayCard(
    title: String,
    dateText: String,
    firstRow: Pair<RunInfoMetricUiModel, RunInfoMetricUiModel>,
    secondRow: Pair<RunInfoMetricUiModel, RunInfoMetricUiModel>,
    modifier: Modifier = Modifier,
    metricValueColor: Color = Color.White
) {
    RunInfoOverlayContainer(modifier = modifier) {
        RunInfoOverlayHeader(title = title, dateText = dateText)
        RunInfoMetricRow(
            left = firstRow.first,
            right = firstRow.second,
            valueColor = metricValueColor
        )
        RunInfoMetricRow(
            left = secondRow.first,
            right = secondRow.second,
            valueColor = metricValueColor
        )
    }
}

@Composable
fun RunInfoOverlayContainer(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RunInfoOverlayShape,
        color = Color.Transparent,
        shadowElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .clip(RunInfoOverlayShape)
                .background(RunInfoOverlayBase)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
fun RunInfoOverlayHeader(
    title: String,
    dateText: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(dateText, color = AppSecondaryText, fontWeight = FontWeight.Medium, fontSize = 14.sp)
    }
}

@Composable
fun RunInfoMetricRow(
    left: RunInfoMetricUiModel,
    right: RunInfoMetricUiModel,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RunInfoMetric(left, valueColor, Modifier.weight(1f))
        RunInfoMetric(right, valueColor, Modifier.weight(1f))
    }
}

@Composable
fun RunInfoMetric(
    metric: RunInfoMetricUiModel,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(
            text = metric.value,
            color = valueColor,
            fontSize = 19.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false
        )
        Text(metric.label, color = AppSecondaryText, fontWeight = FontWeight.Medium, fontSize = 13.sp)
    }
}
