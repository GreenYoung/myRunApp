package com.example.myrunapp.feature.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myrunapp.feature.exercise.ExerciseSummaryUiState
import com.example.myrunapp.feature.exercise.ExerciseUiState
import com.example.myrunapp.feature.exercise.formatCalories
import com.example.myrunapp.core.util.AppDateFormats
import com.example.myrunapp.feature.exercise.formatDistance
import com.example.myrunapp.feature.weight.WeightCardUiState
import com.example.myrunapp.feature.weight.WeightUiState
import com.example.myrunapp.feature.weight.formatWeight
import com.example.myrunapp.ui.components.PageHorizontalPadding
import com.example.myrunapp.ui.components.appClickable
import com.example.myrunapp.ui.components.PageTopSpacing
import com.example.myrunapp.ui.theme.AppBackground
import com.example.myrunapp.ui.theme.AppSecondaryText
import com.example.myrunapp.ui.theme.NumericText
import com.example.myrunapp.ui.theme.MyRunAppTheme
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private val HomeGreen = Color(0xFF0BDA51)
private val HomeMint = Color(0xFF34D399)
private val HomeSoftGreen = Color(0xFF86EFAC)
private val HomeCyan = Color(0xFF22D3EE)
private val HeroTop = Color(0xFF18261F)
private val HeroBottom = Color(0xFF0D1418)
private val Hairline = Color(0x14FFFFFF)
private val TabularNumbers = NumericText

@Composable
fun HomeScreen(
    weightUiState: WeightUiState,
    exerciseUiState: ExerciseUiState,
    onExerciseClick: () -> Unit,
    onStartOutdoorRunClick: () -> Unit,
    onWeightClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(
                start = PageHorizontalPadding,
                top = PageTopSpacing,
                end = PageHorizontalPadding,
                bottom = 28.dp
            ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            HomeHeader(streakDays = exerciseUiState.summary.streakDays)
            TodayExerciseCard(
                uiState = exerciseUiState.summary,
                onStartClick = onStartOutdoorRunClick,
                onClick = onExerciseClick
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HomeWeightCard(
                uiState = weightUiState.weightCard,
                onClick = onWeightClick,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            HomeWeeklyGoalCard(
                uiState = exerciseUiState.summary,
                onClick = onExerciseClick,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }
    }
}

/**
 * 顶部信息行：左边日期，右边连续运动 pill。
 *
 * 占位 72dp = 原来的 58dp 头部 + 14dp 间距，内容改为贴底对齐、底部留 2dp，
 * 这样 pill 底边距今日运动卡片只有 2dp，而卡片及下方所有内容位置保持不变。
 */
@Composable
private fun HomeHeader(
    streakDays: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(HomeHeaderHeight),
        verticalArrangement = Arrangement.Bottom
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = HomeHeaderBottomGap),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = todayLabelText(),
                color = AppSecondaryText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            // 连续运动超过 1 天才展示连击标签；0 天或只有 1 天时整个标签隐藏
            if (streakDays > 1) {
                StreakPill(streakDays = streakDays)
            }
        }
    }
}

private val HomeHeaderHeight = 72.dp
private val HomeHeaderBottomGap = 2.dp

@Composable
private fun StreakPill(streakDays: Int) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF161C23))
            .border(1.dp, Hairline, RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(HomeMint, CircleShape)
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (streakDays > 0) streakDays.toString() else "--",
                color = Color.White,
                fontSize = 16.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Bold,
                style = TabularNumbers
            )
            Text(
                text = "连续天",
                color = AppSecondaryText,
                fontSize = 9.sp,
                lineHeight = 11.sp
            )
        }
    }
}

@Composable
private fun TodayExerciseCard(
    uiState: ExerciseSummaryUiState,
    onStartClick: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(HeroTop, HeroBottom),
                    start = Offset.Zero,
                    end = Offset.Infinite
                )
            )
            .border(1.dp, Hairline, RoundedCornerShape(24.dp))
            .appClickable(onClick = onClick)
            .padding(20.dp)
    ) {
        HeroGlow(modifier = Modifier.matchParentSize())

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "今日运动",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(
                                if (uiState.checkedInToday) HomeGreen else AppSecondaryText,
                                CircleShape
                            )
                    )
                    Text(
                        text = if (uiState.checkedInToday) "今日已打卡" else "今日未打卡",
                        color = if (uiState.checkedInToday) HomeSoftGreen else AppSecondaryText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = if (uiState.checkedInToday) {
                                formatDistance(uiState.todayDistanceKm)
                            } else {
                                "--.--"
                            },
                            color = Color.White,
                            fontSize = 50.sp,
                            lineHeight = 54.sp,
                            fontWeight = FontWeight.Bold,
                            style = TabularNumbers
                        )
                        Text(
                            text = " km",
                            color = AppSecondaryText,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 9.dp)
                        )
                    }
                    Text(
                        text = if (uiState.checkedInToday) {
                            "每日目标 ${formatDistance(uiState.dailyGoalKm)} km · 已完成 ${(uiState.dailyRatio * 100).roundToInt()}%"
                        } else {
                            "今天还没有运动记录"
                        },
                        color = AppSecondaryText,
                        fontSize = 12.sp
                    )
                }
                DailyGoalRing(ratio = uiState.dailyRatio)
            }

            StartRunButton(onClick = onStartClick)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0x1FFFFFFF))
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                HeroMetric(
                    value = formatDistance(uiState.weeklyDistanceKm),
                    unit = "km",
                    label = "本周里程",
                    modifier = Modifier.weight(1f)
                )
                HeroMetric(
                    value = formatDistance(uiState.monthlyDistanceKm),
                    unit = "km",
                    label = "本月里程",
                    modifier = Modifier.weight(1f)
                )
                HeroMetric(
                    value = formatCalories(uiState.todayCaloriesKcal),
                    unit = "kcal",
                    label = "今日消耗",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun HeroGlow(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(HomeGreen.copy(alpha = 0.18f), Color.Transparent),
                center = Offset(size.width * 0.92f, size.height * 0.06f),
                radius = size.width * 0.55f
            ),
            radius = size.width * 0.55f,
            center = Offset(size.width * 0.92f, size.height * 0.06f)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(HomeCyan.copy(alpha = 0.07f), Color.Transparent),
                center = Offset(size.width * 0.04f, size.height * 0.92f),
                radius = size.width * 0.5f
            ),
            radius = size.width * 0.5f,
            center = Offset(size.width * 0.04f, size.height * 0.92f)
        )
    }
}

/** 完成度 ≥100% 后圆环/进度条开始换色，超额越多颜色越深。 */
private fun progressColor(ratio: Float): Color {
    return when {
        ratio >= 3f -> Color(0xFF065F46)
        ratio >= 2f -> Color(0xFF047857)
        ratio >= 1f -> Color(0xFF059669)
        else -> HomeGreen
    }
}

/** 圆环上的「×2 / ×3」角标：完成度达到 2 倍才出现。 */
private fun overAchieveMultiplier(ratio: Float): Int? {
    return if (ratio >= 2f) ratio.toInt() else null
}

@Composable
private fun DailyGoalRing(
    ratio: Float,
    modifier: Modifier = Modifier
) {
    val safeRatio = ratio.coerceAtLeast(0f)
    val sweepProgress = safeRatio.coerceAtMost(1f)
    val ringColor = progressColor(safeRatio)
    val highlight = lerp(ringColor, Color.White, 0.22f)

    Box(
        modifier = modifier.size(86.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 7.dp.toPx()
            drawCircle(
                color = Color.White.copy(alpha = 0.08f),
                style = Stroke(width = stroke)
            )
            drawArc(
                brush = Brush.sweepGradient(listOf(ringColor, highlight, ringColor)),
                startAngle = -90f,
                sweepAngle = 360f * sweepProgress,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${(safeRatio * 100).roundToInt()}%",
                color = Color.White,
                fontSize = 20.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Bold,
                style = TabularNumbers
            )
            Text(
                text = "今日目标",
                color = AppSecondaryText,
                fontSize = 9.sp,
                lineHeight = 11.sp
            )
        }
        overAchieveMultiplier(safeRatio)?.let { multiplier ->
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 10.dp, y = (-4).dp)
                    .clip(RoundedCornerShape(50))
                    .background(ringColor.copy(alpha = 0.22f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "×$multiplier",
                    color = highlight,
                    fontSize = 9.sp,
                    lineHeight = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun StartRunButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(HomeGreen, Color(0xFF84CC16)),
                    start = Offset.Zero,
                    end = Offset.Infinite
                )
            )
            .appClickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "▶",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = " 开始运动",
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun HeroMetric(
    value: String,
    unit: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row {
            Text(
                text = value,
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                style = TabularNumbers,
                modifier = Modifier.alignByBaseline()
            )
            Text(
                text = " $unit",
                color = AppSecondaryText,
                fontSize = 10.sp,
                modifier = Modifier.alignByBaseline()
            )
        }
        Text(
            text = label,
            color = AppSecondaryText,
            fontSize = 11.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun HomeWeightCard(
    uiState: WeightCardUiState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .heightIn(min = 132.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF1A2226), Color(0xFF12181D)),
                    start = Offset.Zero,
                    end = Offset.Infinite
                )
            )
            .border(1.dp, Hairline, RoundedCornerShape(20.dp))
            .appClickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "今日体重",
                color = AppSecondaryText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(
                        if (uiState.hasTodayWeight) HomeMint else AppSecondaryText,
                        CircleShape
                    )
            )
        }

        if (uiState.todayWeight != null) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = formatWeight(uiState.todayWeight),
                    color = Color.White,
                    fontSize = 30.sp,
                    lineHeight = 32.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    style = TabularNumbers
                )
                Text(
                    text = " kg",
                    color = AppSecondaryText,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        } else {
            Text(
                text = "去记录",
                color = HomeSoftGreen,
                fontSize = 26.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            text = weightCaption(
                hasTodayWeight = uiState.hasTodayWeight,
                previousChange = uiState.previousChange,
                latestWeight = uiState.latestWeight,
                latestDate = uiState.latestDate
            ),
            color = if (uiState.hasTodayWeight && (uiState.previousChange ?: 0.0) < 0.0) {
                HomeMint
            } else {
                AppSecondaryText
            },
            fontSize = 11.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun HomeWeeklyGoalCard(
    uiState: ExerciseSummaryUiState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ratio = uiState.weeklyRatio
    val fill = ratio.coerceIn(0f, 1f)
    val barColor = progressColor(ratio)
    val barHighlight = lerp(barColor, Color.White, 0.22f)
    val overKm = (uiState.weeklyDistanceKm - uiState.weeklyGoalKm).coerceAtLeast(0.0)
    Column(
        modifier = modifier
            .heightIn(min = 132.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF17251D), Color(0xFF111A17)),
                    start = Offset.Zero,
                    end = Offset.Infinite
                )
            )
            .border(1.dp, Hairline, RoundedCornerShape(20.dp))
            .appClickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "本周目标",
                color = AppSecondaryText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${(ratio * 100).roundToInt()}%",
                color = barColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                style = TabularNumbers
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = formatDistance(uiState.weeklyDistanceKm),
                color = Color.White,
                fontSize = 24.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
                style = TabularNumbers
            )
            Text(
                text = " / ${formatDistance(uiState.weeklyGoalKm)} km",
                color = AppSecondaryText,
                fontSize = 10.sp,
                lineHeight = 14.sp,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.padding(bottom = 3.dp)
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.White.copy(alpha = 0.08f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fill)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            Brush.horizontalGradient(listOf(barColor, barHighlight))
                        )
                )
                if (ratio > 1f) {
                    // 超过 100%：整条进度条叠斜纹，表示"已经跑满并超额"
                    Canvas(modifier = Modifier.matchParentSize()) {
                        val step = 8.dp.toPx()
                        val stripeWidth = 3.dp.toPx()
                        var x = -size.height
                        while (x < size.width) {
                            drawLine(
                                color = Color.White.copy(alpha = 0.22f),
                                start = Offset(x, size.height),
                                end = Offset(x + size.height, 0f),
                                strokeWidth = stripeWidth
                            )
                            x += step
                        }
                    }
                }
            }
            Text(
                text = if (ratio >= 1f) {
                    "已超额 ${formatDistance(overKm)} km"
                } else {
                    "还差 ${formatDistance(uiState.weeklyRemainingKm)} km"
                },
                color = AppSecondaryText,
                fontSize = 11.sp,
                maxLines = 1
            )
        }
    }
}

private fun weightCaption(
    hasTodayWeight: Boolean,
    previousChange: Double?,
    latestWeight: Double?,
    latestDate: String?
): String {
    if (hasTodayWeight) {
        return when {
            previousChange == null -> "已记录今日体重"
            previousChange < 0.0 -> "较上次 ↓ ${formatWeight(abs(previousChange))} kg"
            previousChange > 0.0 -> "较上次 ↑ ${formatWeight(previousChange)} kg"
            else -> "较上次持平"
        }
    }
    if (latestWeight != null) {
        val date = latestDate?.let { if (it.length >= 10) it.substring(5, 10) else it }
        return "今天未记录 · 最新 ${formatWeight(latestWeight)} kg ${date ?: ""}".trim()
    }
    return "记录第一条体重"
}

private fun todayLabelText(): String {
    val now = System.currentTimeMillis()
    val date = AppDateFormats.format("M月d日", now, Locale.CHINA)
    val weekday = AppDateFormats.format("EEEE", now, Locale.CHINA)
    return "$date · $weekday"
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun HomeScreenPreview() {
    MyRunAppTheme(darkTheme = true) {
        HomeScreen(
            weightUiState = WeightUiState(
                weightCard = WeightCardUiState(
                    todayWeight = 77.20,
                    hasTodayWeight = true,
                    latestWeight = 77.20,
                    latestDate = "2026-10-26",
                    previousChange = -0.30,
                    totalChange = -1.80
                )
            ),
            exerciseUiState = ExerciseUiState(
                summary = ExerciseSummaryUiState(
                    // 预览用超额数据：今日 10.2 / 5 km = 204%，本周 40 / 20 km = 200%
                    todayDistanceKm = 10.20,
                    dailyGoalKm = 5.00,
                    dailyProgress = 1f,
                    weeklyDistanceKm = 40.00,
                    weeklyGoalKm = 20.00,
                    weeklyProgress = 1f,
                    weeklyRemainingKm = 0.00,
                    weeklyGoalCompleted = true,
                    monthlyDistanceKm = 65.80,
                    monthlyGoalKm = 80.00,
                    monthlyProgress = 0.8225f,
                    monthlyProgressPercent = 82,
                    monthlyGoalCompleted = false,
                    totalDistanceKm = 532.80,
                    checkInDays = 48,
                    streakDays = 5,
                    todayCaloriesKcal = 356,
                    weeklyCaloriesKcal = 1587,
                    monthlyCaloriesKcal = 6842,
                    checkedInToday = true
                )
            ),
            onExerciseClick = {},
            onStartOutdoorRunClick = {},
            onWeightClick = {}
        )
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun HomeScreenEmptyPreview() {
    MyRunAppTheme(darkTheme = true) {
        HomeScreen(
            weightUiState = WeightUiState(),
            exerciseUiState = ExerciseUiState(
                summary = ExerciseSummaryUiState(
                    dailyGoalKm = 5.00,
                    weeklyGoalKm = 20.00,
                    monthlyGoalKm = 80.00,
                    weeklyRemainingKm = 20.00
                )
            ),
            onExerciseClick = {},
            onStartOutdoorRunClick = {},
            onWeightClick = {}
        )
    }
}
