package com.example.myrunapp.feature.exercise

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myrunapp.ui.components.AppPageTopBar
import com.example.myrunapp.ui.components.PageTopSpacing
import com.example.myrunapp.ui.theme.AppBackground
import com.example.myrunapp.ui.theme.AppSecondaryText
import java.util.Locale

private val StatsCardDark = Color(0xFF111820)
private val StatsCardSurface = Color(0xFF151E26)
private val StatsAccentGreen = Color(0xFF22C55E)

@Composable
fun ExerciseStatsDetailRoute(
    viewModel: ExerciseViewModel,
    period: ExerciseStatsPeriod,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.observeStatsDetail(period).collectAsState(
        initial = ExerciseStatsDetailUiState(period = period, title = period.titleText)
    )

    ExerciseStatsDetailScreen(
        uiState = uiState,
        onBack = onBack,
        modifier = modifier
    )
}

@Composable
fun ExerciseStatsDetailScreen(
    uiState: ExerciseStatsDetailUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(AppBackground)
            .statusBarsPadding(),
        containerColor = AppBackground,
        contentWindowInsets = WindowInsets(0.dp)
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBackground)
                .padding(paddingValues),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = PageTopSpacing,
                end = 16.dp,
                bottom = 28.dp
            ),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                AppPageTopBar(
                    title = uiState.title,
                    showBackButton = true,
                    onBackClick = onBack
                )
            }
            item { ExerciseStatsHero(uiState = uiState) }
            item { ExerciseStatsMetricCard(uiState = uiState) }
        }
    }
}

@Composable
private fun ExerciseStatsHero(uiState: ExerciseStatsDetailUiState) {
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
                    Brush.linearGradient(
                        colors = listOf(StatsCardSurface, StatsCardDark),
                        start = Offset.Zero,
                        end = Offset.Infinite
                    )
                )
                .padding(22.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = uiState.rangeText,
                    color = AppSecondaryText,
                    fontSize = 13.sp,
                    maxLines = 1
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = formatDistance(uiState.totalDistanceKm),
                        color = Color.White,
                        fontSize = 48.sp,
                        lineHeight = 52.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1
                    )
                    Text(
                        text = " km",
                        color = AppSecondaryText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
                Text(
                    text = "累计里程",
                    color = StatsAccentGreen,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun ExerciseStatsMetricCard(uiState: ExerciseStatsDetailUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = StatsCardSurface.copy(alpha = 0.78f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            ExerciseStatsMetricRow {
                ExerciseStatsMetricItem("总时长", formatStatsDurationClock(uiState.totalDurationSeconds), Modifier.weight(1f))
                ExerciseStatsMetricItem("运动次数", "${uiState.recordCount} 次", Modifier.weight(1f))
            }
            ExerciseStatsMetricRow {
                ExerciseStatsMetricItem("打卡天数", "${uiState.checkInDays} 天", Modifier.weight(1f))
                ExerciseStatsMetricItem("平均里程", "${formatDistance(uiState.averageDistanceKm)} km", Modifier.weight(1f))
            }
            ExerciseStatsMetricRow {
                ExerciseStatsMetricItem("平均配速", uiState.averagePaceText, Modifier.weight(1f))
                ExerciseStatsMetricItem("最快速度", "${formatDistance(uiState.fastestSpeedKmh)} km/h", Modifier.weight(1f))
            }
            ExerciseStatsMetricRow {
                ExerciseStatsMetricItem("单次最长", "${formatDistance(uiState.longestDistanceKm)} km", Modifier.weight(1f))
                ExerciseStatsMetricItem("最高消耗", "${formatCalories(uiState.highestCaloriesKcal)} kcal", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ExerciseStatsMetricRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        content = content
    )
}

@Composable
private fun ExerciseStatsMetricItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            color = AppSecondaryText,
            fontSize = 13.sp,
            maxLines = 1
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 20.sp,
            lineHeight = 23.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Start
        )
    }
}

private val ExerciseStatsPeriod.titleText: String
    get() = when (this) {
        ExerciseStatsPeriod.WEEK -> "周统计"
        ExerciseStatsPeriod.MONTH -> "月统计"
        ExerciseStatsPeriod.YEAR -> "年统计"
    }

private fun formatStatsDurationClock(totalSeconds: Long): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
}
