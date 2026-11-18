package com.example.myrunapp.feature.exercise

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myrunapp.ui.components.AppPageTopBar
import com.example.myrunapp.ui.components.appClickable
import com.example.myrunapp.ui.components.PageTopSpacing
import com.example.myrunapp.ui.theme.AppBackground
import com.example.myrunapp.ui.theme.AppSecondaryText
import java.util.Locale

private val StatsHeroTop = Color(0xFF1A2530)
private val StatsHeroBottom = Color(0xFF10161D)
private val StatsCardBackground = Color(0xFF141C24)
private val StatsCardBorder = Color.White.copy(alpha = 0.06f)
private val StatsDivider = Color(0x14FFFFFF)

@Composable
fun ExerciseStatsDetailRoute(
    viewModel: ExerciseViewModel,
    period: ExerciseStatsPeriod,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var periodOffset by rememberSaveable(period) { mutableIntStateOf(0) }
    // 只在真正换周期时重建 Flow：否则每次重组都会新建一个数据库订阅。
    val statsFlow = remember(period, periodOffset) {
        viewModel.observeStatsDetail(period, periodOffset)
    }
    val uiState by statsFlow.collectAsState(
        initial = viewModel.cachedStatsDetail(period, periodOffset)
            ?: ExerciseStatsDetailUiState(
                period = period,
                periodOffset = periodOffset,
                title = period.titleText
            )
    )

    ExerciseStatsDetailScreen(
        uiState = uiState,
        onBack = onBack,
        onPreviousPeriod = { periodOffset -= 1 },
        onNextPeriod = {
            if (periodOffset < 0) {
                periodOffset += 1
            }
        },
        modifier = modifier
    )
}

@Composable
fun ExerciseStatsDetailScreen(
    uiState: ExerciseStatsDetailUiState,
    onBack: () -> Unit,
    onPreviousPeriod: () -> Unit,
    onNextPeriod: () -> Unit,
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
            item {
                ExerciseStatsHero(
                    uiState = uiState,
                    onPreviousPeriod = onPreviousPeriod,
                    onNextPeriod = onNextPeriod
                )
            }
            item { ExerciseStatsMetricCard(uiState = uiState) }
        }
    }
}

@Composable
private fun ExerciseStatsHero(
    uiState: ExerciseStatsDetailUiState,
    onPreviousPeriod: () -> Unit,
    onNextPeriod: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(listOf(StatsHeroTop, StatsHeroBottom)))
            .border(1.dp, StatsCardBorder, RoundedCornerShape(22.dp))
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val glowCenter = Offset(size.width * 0.9f, size.height * 0.02f)
            val glowRadius = size.width * 0.75f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(ExerciseGreen.copy(alpha = 0.16f), Color.Transparent),
                    center = glowCenter,
                    radius = glowRadius
                ),
                radius = glowRadius,
                center = glowCenter
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ExerciseStatsPeriodPill(text = uiState.periodLabel)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ExerciseStatsArrow(
                        text = "‹",
                        enabled = uiState.canGoPrevious,
                        onClick = onPreviousPeriod
                    )
                    ExerciseStatsArrow(
                        text = "›",
                        enabled = uiState.canGoNext,
                        onClick = onNextPeriod
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row {
                    Text(
                        text = formatDistance(uiState.totalDistanceKm),
                        color = Color.White,
                        fontSize = 46.sp,
                        lineHeight = 50.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        style = ExerciseTabularNumbers,
                        modifier = Modifier.alignByBaseline()
                    )
                    Text(
                        text = " km",
                        color = AppSecondaryText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        modifier = Modifier.alignByBaseline()
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(ExerciseGreen)
                    )
                    Text(
                        text = when {
                            uiState.recordCount > 0 -> "累计里程 · ${uiState.rangeText}"
                            // rangeText 为空说明数据还没到，此时不显示空状态，避免进页面闪一下
                            uiState.rangeText.isNotEmpty() -> "该周期暂无运动记录"
                            else -> "累计里程"
                        },
                        color = AppSecondaryText,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun ExerciseStatsPeriodPill(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(ExerciseGreen.copy(alpha = 0.12f))
            .border(1.dp, ExerciseGreen.copy(alpha = 0.28f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = text,
            color = ExerciseGreen,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun ExerciseStatsArrow(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = if (enabled) 0.08f else 0.03f))
            .appClickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (enabled) ExerciseGreen else AppSecondaryText.copy(alpha = 0.3f),
            fontSize = 16.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ExerciseStatsMetricCard(uiState: ExerciseStatsDetailUiState) {
    val metrics = listOf(
        "总时长" to formatStatsDurationClock(uiState.totalDurationSeconds),
        "运动次数" to "${uiState.recordCount} 次",
        "打卡天数" to "${uiState.checkInDays} 天",
        "平均里程" to "${formatDistance(uiState.averageDistanceKm)} km",
        "平均配速" to uiState.averagePaceText,
        "最快速度" to "${formatDistance(uiState.fastestSpeedKmh)} km/h",
        "单次最长" to "${formatDistance(uiState.longestDistanceKm)} km",
        "最高消耗" to "${formatCalories(uiState.highestCaloriesKcal)} kcal"
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(StatsCardBackground)
            .border(1.dp, StatsCardBorder, RoundedCornerShape(20.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "数据概览",
            color = Color.White.copy(alpha = 0.86f),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
        metrics.chunked(2).forEachIndexed { index, pair ->
            if (index > 0) {
                HorizontalDivider(color = StatsDivider)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ExerciseStatsMetricCell(
                    label = pair[0].first,
                    value = pair[0].second,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(30.dp)
                        .background(StatsDivider)
                )
                ExerciseStatsMetricCell(
                    label = pair[1].first,
                    value = pair[1].second,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun ExerciseStatsMetricCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(
            text = label,
            color = AppSecondaryText,
            fontSize = 12.sp,
            maxLines = 1
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 21.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            style = ExerciseTabularNumbers
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
