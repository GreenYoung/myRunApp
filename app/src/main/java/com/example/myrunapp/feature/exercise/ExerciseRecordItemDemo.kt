package com.example.myrunapp.feature.exercise

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myrunapp.ui.theme.AppBackground
import com.example.myrunapp.ui.theme.AppSecondaryText
import com.example.myrunapp.ui.theme.MyRunAppTheme

// ==== 仅为效果预览的 Demo，不参与正式页面逻辑 ====

private val DemoCardBackground = Color(0xFF141C24)
private val DemoCardBorder = Color.White.copy(alpha = 0.05f)
private val DemoDivider = Color(0x14FFFFFF)
private val DemoThumbnailBackground = Color(0xB30C1218)
private val DemoThumbnailBorder = Color.White.copy(alpha = 0.06f)
private val DemoCyan = Color(0xFF38BDF8)
private val DemoNumbers = TextStyle(fontFeatureSettings = "tnum")

private enum class DemoTypePlacement {
    Current,
    RightStack,
    DateRowEnd,
    ReplaceChevron
}

private data class DemoRecord(
    val date: String,
    val typeText: String,
    val outdoor: Boolean,
    val distance: String,
    val metrics: String
)

private val demoRecords = listOf(
    DemoRecord("12月28日 07:12 周日", "户外", true, "5.20 km", "33:00 · 6'23\" · 356 kcal"),
    DemoRecord("12月27日 18:45 周六", "跑步机", false, "3.00 km", "21:30 · 7'10\" · 192 kcal")
)

@Preview(name = "运动记录条目 Demo · 类型位置", showBackground = true, widthDp = 400, heightDp = 1000)
@Composable
private fun ExerciseRecordTypePlacementDemoPreview() {
    MyRunAppTheme(darkTheme = true) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBackground)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DemoSectionLabel(text = "当前样式 · 类型紧跟在日期后")
            DemoMonthCard(placement = DemoTypePlacement.Current)
            DemoSectionLabel(text = "方案 A · 类型移到右侧，与箭头同列右对齐（已采用）")
            DemoMonthCard(placement = DemoTypePlacement.RightStack)
            DemoSectionLabel(text = "方案 B · 类型移到日期行最右端，箭头保留")
            DemoMonthCard(placement = DemoTypePlacement.DateRowEnd)
            DemoSectionLabel(text = "方案 C · 类型替代箭头位置")
            DemoMonthCard(placement = DemoTypePlacement.ReplaceChevron)
        }
    }
}

@Composable
private fun DemoSectionLabel(text: String) {
    Text(
        text = text,
        color = AppSecondaryText,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun DemoMonthCard(placement: DemoTypePlacement) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(DemoCardBackground)
            .border(1.dp, DemoCardBorder, RoundedCornerShape(20.dp))
            .padding(bottom = 6.dp)
    ) {
        DemoMonthHeader()
        demoRecords.forEachIndexed { index, record ->
            if (index > 0) {
                HorizontalDivider(
                    color = DemoDivider,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            DemoRecordRow(record = record, placement = placement)
        }
    }
}

@Composable
private fun DemoMonthHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("十二月", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Text("2026", color = AppSecondaryText.copy(alpha = 0.7f), fontSize = 11.sp)
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                "65.80 km",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                style = DemoNumbers
            )
            Text("累计 12 次 · 7.4 小时", color = AppSecondaryText, fontSize = 11.sp)
        }
    }
}

@Composable
private fun DemoRecordRow(
    record: DemoRecord,
    placement: DemoTypePlacement
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val thumbnailModifier = Modifier
            .width(76.dp)
            .fillMaxHeight()
        if (record.outdoor) {
            DemoTrackThumbnail(modifier = thumbnailModifier)
        } else {
            DemoTypeBadge(typeText = record.typeText, modifier = thumbnailModifier)
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = if (placement == DemoTypePlacement.DateRowEnd) {
                    Modifier.fillMaxWidth()
                } else {
                    Modifier
                },
                horizontalArrangement = if (placement == DemoTypePlacement.DateRowEnd) {
                    Arrangement.SpaceBetween
                } else {
                    Arrangement.spacedBy(8.dp)
                },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = record.date,
                    color = Color.White.copy(alpha = 0.86f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (placement == DemoTypePlacement.Current || placement == DemoTypePlacement.DateRowEnd) {
                    DemoTypeCapsule(record = record)
                }
            }
            Text(
                text = record.distance,
                color = Color.White,
                fontSize = 23.sp,
                lineHeight = 26.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                style = DemoNumbers
            )
            Text(
                text = record.metrics,
                color = AppSecondaryText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                style = DemoNumbers
            )
        }
        when (placement) {
            DemoTypePlacement.RightStack -> {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DemoTypeCapsule(record = record)
                    DemoChevron()
                }
            }
            DemoTypePlacement.ReplaceChevron -> DemoTypeCapsule(record = record)
            else -> DemoChevron()
        }
    }
}

@Composable
private fun DemoChevron() {
    Text(
        text = "›",
        color = AppSecondaryText.copy(alpha = 0.45f),
        fontSize = 20.sp,
        fontWeight = FontWeight.Light
    )
}

@Composable
private fun DemoTypeCapsule(record: DemoRecord) {
    val color = if (record.outdoor) ExerciseGreen else DemoCyan
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
private fun DemoTypeBadge(
    typeText: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(DemoThumbnailBackground)
            .border(1.dp, DemoThumbnailBorder, RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text("🏃", fontSize = 20.sp)
            Text(
                text = typeText,
                color = AppSecondaryText,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun DemoTrackThumbnail(modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(DemoThumbnailBackground)
            .border(1.dp, DemoThumbnailBorder, RoundedCornerShape(14.dp))
            .padding(8.dp)
    ) {
        val points = listOf(
            0.05f to 0.88f,
            0.20f to 0.58f,
            0.38f to 0.70f,
            0.54f to 0.32f,
            0.72f to 0.46f,
            0.92f to 0.10f
        )
        val path = Path()
        points.forEachIndexed { index, (x, y) ->
            val px = x * size.width
            val py = y * size.height
            if (index == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        drawPath(
            path = path,
            color = ExerciseGreen,
            style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round)
        )
        drawCircle(DemoCyan, radius = 3.dp.toPx(), center = Offset(0.05f * size.width, 0.88f * size.height))
        drawCircle(ExerciseGreen, radius = 3.dp.toPx(), center = Offset(0.92f * size.width, 0.10f * size.height))
    }
}
