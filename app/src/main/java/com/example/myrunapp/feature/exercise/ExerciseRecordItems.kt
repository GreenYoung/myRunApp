package com.example.myrunapp.feature.exercise

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myrunapp.R
import com.example.myrunapp.core.util.AppDateFormats
import com.example.myrunapp.feature.run.RunTrackPointUiModel
import com.example.myrunapp.feature.run.formatRunClock
import com.example.myrunapp.ui.components.AppPressRippleColor
import com.example.myrunapp.ui.theme.AppSecondaryText
import java.util.Locale
import kotlin.math.roundToLong

internal data class ExerciseMonthGroup(
    val year: Int,
    val month: Int,
    val records: List<ExerciseRecordUiModel>,
    val totalDistanceKm: Double,
    val totalDurationSeconds: Long,
    val totalCaloriesKcal: Int
)

private val ExerciseCardBackground = Color(0xFF141C24)
private val ExerciseCardBorder = Color.White.copy(alpha = 0.05f)
private val ExerciseDivider = Color(0x14FFFFFF)
private val ExerciseThumbnailBackground = Color(0xB30C1218)
private val ExerciseThumbnailBorder = Color.White.copy(alpha = 0.06f)
private val ExerciseCyan = Color(0xFF38BDF8)

@Composable
internal fun ExerciseMonthCard(
    group: ExerciseMonthGroup,
    monthlyGoalKm: Double,
    onRecordClick: (Long) -> Unit,
    onLongClick: (ExerciseRecordUiModel) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(ExerciseCardBackground)
            .border(1.dp, ExerciseCardBorder, RoundedCornerShape(20.dp))
            .padding(bottom = 6.dp)
    ) {
        ExerciseMonthCardHeader(group = group, monthlyGoalKm = monthlyGoalKm)
        group.records.forEachIndexed { index, record ->
            if (index > 0) {
                HorizontalDivider(
                    color = ExerciseDivider,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            ExerciseRecordRow(
                record = record,
                onClick = { onRecordClick(record.id) },
                onLongClick = { onLongClick(record) }
            )
        }
    }
}

@Composable
private fun ExerciseMonthCardHeader(
    group: ExerciseMonthGroup,
    monthlyGoalKm: Double
) {
    Column(
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = monthChineseName(group.month),
                    color = Color.White,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = group.year.toString(),
                    color = AppSecondaryText.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "${formatDistance(group.totalDistanceKm)} km",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    style = ExerciseTabularNumbers
                )
                Text(
                    text = "累计 ${group.records.size} 次 · ${formatHoursText(group.totalDurationSeconds)}",
                    color = AppSecondaryText,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (monthlyGoalKm > 0.0) {
            val ratio = group.totalDistanceKm / monthlyGoalKm
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "月目标 ${formatDistance(monthlyGoalKm)} km",
                        color = AppSecondaryText.copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${(ratio * 100).toInt()}%",
                        color = ExerciseGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.07f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(ratio.coerceIn(0.0, 1.0).toFloat())
                            .height(3.dp)
                            .clip(RoundedCornerShape(50))
                            .background(ExerciseGreen)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ExerciseRecordRow(
    record: ExerciseRecordUiModel,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = ripple(color = AppPressRippleColor),
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val thumbnailModifier = Modifier
            .width(76.dp)
            .fillMaxHeight()
        when {
            record.isOutdoorRun && record.hasTrack -> TrackThumbnail(
                points = record.trackPoints,
                modifier = thumbnailModifier
            )

            record.isIndoorRun -> IndoorRunThumbnail(
                modifier = thumbnailModifier
            )

            else -> ExerciseTypeBadge(
                record = record,
                modifier = thumbnailModifier
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = formatHistoryRecordDate(record.startTime),
                color = Color.White.copy(alpha = 0.86f),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Text(
                text = record.distanceText,
                color = Color.White,
                fontSize = 23.sp,
                lineHeight = 26.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                style = ExerciseTabularNumbers
            )
            // 三段元信息：左段可压缩（省略号），热量单独一段不压缩，
            // 保证"配速多一位数字"时也不会把 kcal 挤没。
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${formatRunClock(record.durationSeconds)} · ${record.paceText.removeSuffix("/km")}",
                    color = AppSecondaryText,
                    fontSize = 12.sp,
                    letterSpacing = 0.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    style = ExerciseTabularNumbers,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Text(
                    text = " · ${record.caloriesText}",
                    color = AppSecondaryText,
                    fontSize = 12.sp,
                    letterSpacing = 0.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    softWrap = false,
                    style = ExerciseTabularNumbers
                )
            }
        }
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ExerciseTypeCapsule(record = record)
            Text(
                text = "›",
                color = AppSecondaryText.copy(alpha = 0.45f),
                fontSize = 20.sp,
                fontWeight = FontWeight.Light
            )
        }
    }
}

@Composable
private fun ExerciseTypeCapsule(record: ExerciseRecordUiModel) {
    val color = if (record.isOutdoorRun) ExerciseGreen else ExerciseCyan
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 9.dp, vertical = 3.dp)
    ) {
        Text(
            text = record.typeText,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun ExerciseTypeBadge(
    record: ExerciseRecordUiModel,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(ExerciseThumbnailBackground)
            .border(1.dp, ExerciseThumbnailBorder, RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text("🏃", fontSize = 20.sp)
            Text(
                text = record.typeText,
                color = AppSecondaryText,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

/**
 * 室内跑缩略图：深色圆角容器 + 彩色跑步插画，替换原来的 emoji 徽章。
 * 与户外跑的轨迹缩略图共用同一块视觉区域和同一种容器样式，容器内只放图形不放文字
 * （「室内」类型已由行右侧的胶囊标出）。
 */
@Composable
private fun IndoorRunThumbnail(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(ExerciseThumbnailBackground)
            .border(1.dp, ExerciseThumbnailBorder, RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.ic_indoor_run),
            contentDescription = "室内跑",
            modifier = Modifier.size(48.dp),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun TrackThumbnail(
    points: List<RunTrackPointUiModel>,
    modifier: Modifier = Modifier
) {
    val validPoints = points.filter {
        it.latitude in -90.0..90.0 &&
            it.longitude in -180.0..180.0 &&
            !(it.latitude == 0.0 && it.longitude == 0.0)
    }
    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(ExerciseThumbnailBackground)
            .border(1.dp, ExerciseThumbnailBorder, RoundedCornerShape(14.dp))
            .padding(8.dp)
    ) {
        if (validPoints.size < 2) return@Canvas

        val minLat = validPoints.minOf { it.latitude }
        val maxLat = validPoints.maxOf { it.latitude }
        val minLng = validPoints.minOf { it.longitude }
        val maxLng = validPoints.maxOf { it.longitude }
        val latSpan = (maxLat - minLat).takeIf { it > 0.0 } ?: 0.0001
        val lngSpan = (maxLng - minLng).takeIf { it > 0.0 } ?: 0.0001

        fun offsetOf(point: RunTrackPointUiModel): Offset {
            val x = ((point.longitude - minLng) / lngSpan).toFloat() * size.width
            val y = size.height - ((point.latitude - minLat) / latSpan).toFloat() * size.height
            return Offset(x, y)
        }

        val offsets = validPoints.map(::offsetOf)
        val path = Path().apply {
            moveTo(offsets.first().x, offsets.first().y)
            offsets.drop(1).forEach { lineTo(it.x, it.y) }
        }
        drawPath(
            path = path,
            color = ExerciseGreen,
            style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round)
        )
        drawCircle(color = ExerciseCyan, radius = 3.dp.toPx(), center = offsets.first())
        drawCircle(color = ExerciseGreen, radius = 3.dp.toPx(), center = offsets.last())
    }
}

internal fun List<ExerciseRecordUiModel>.toExerciseMonthGroups(): List<ExerciseMonthGroup> {
    return groupBy { record ->
        val calendar = CalendarHolder.calendarFor(record.startTime)
        calendar.get(java.util.Calendar.YEAR) to (calendar.get(java.util.Calendar.MONTH) + 1)
    }.map { (yearMonth, records) ->
        ExerciseMonthGroup(
            year = yearMonth.first,
            month = yearMonth.second,
            records = records.sortedByDescending { it.startTime },
            totalDistanceKm = records.sumOf { it.distanceKm },
            totalDurationSeconds = records.sumOf { it.durationSeconds },
            totalCaloriesKcal = records.sumOf { caloriesTextToInt(it.caloriesText) }
        )
    }.sortedWith(
        compareByDescending<ExerciseMonthGroup> { it.year }
            .thenByDescending { it.month }
    )
}

private fun formatHistoryRecordDate(startTime: Long): String {
    val monthDay = AppDateFormats.format("M月d日", startTime, Locale.CHINA)
    val time = AppDateFormats.format(AppDateFormats.TIME_OF_DAY, startTime, Locale.CHINA)
    val week = AppDateFormats.format("E", startTime, Locale.CHINA)
    return "$monthDay $time $week"
}

internal fun monthChineseName(month: Int): String {
    return when (month) {
        1 -> "一月"
        2 -> "二月"
        3 -> "三月"
        4 -> "四月"
        5 -> "五月"
        6 -> "六月"
        7 -> "七月"
        8 -> "八月"
        9 -> "九月"
        10 -> "十月"
        11 -> "十一月"
        12 -> "十二月"
        else -> "${month}月"
    }
}

internal fun formatHoursText(totalDurationSeconds: Long): String {
    val tenths = (totalDurationSeconds / 360.0).roundToLong()
    return if (tenths % 10L == 0L) {
        "${tenths / 10} 小时"
    } else {
        String.format(Locale.US, "%.1f 小时", tenths / 10.0)
    }
}

private fun caloriesTextToInt(value: String): Int {
    return value.filter { it.isDigit() }.toIntOrNull() ?: 0
}

private object CalendarHolder {
    fun calendarFor(timeMillis: Long): java.util.Calendar {
        return java.util.Calendar.getInstance().apply {
            timeInMillis = timeMillis
        }
    }
}
