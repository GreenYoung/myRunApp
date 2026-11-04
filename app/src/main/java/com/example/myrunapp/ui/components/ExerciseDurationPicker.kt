package com.example.myrunapp.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.myrunapp.ui.theme.AppSecondaryText
import kotlin.math.abs

private val DurationAccent = Color(0xFF22C55E)
private val DurationPickerDark = Color(0xFF111820)

/** 运动时长滚轮弹窗：1～300 分钟。 */
@Composable
fun ExerciseDurationPicker(
    selectedMinutes: Int?,
    onDurationSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF151E26), DurationPickerDark),
                        start = Offset.Zero,
                        end = Offset.Infinite
                    ),
                    shape = RoundedCornerShape(18.dp)
                )
                .padding(horizontal = 16.dp, vertical = 22.dp),
            contentAlignment = Alignment.Center
        ) {
            DurationWheel(
                initialMinutes = selectedMinutes ?: 30,
                onDurationSelected = onDurationSelected
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DurationWheel(
    initialMinutes: Int,
    onDurationSelected: (Int) -> Unit
) {
    val safeInitial = initialMinutes.coerceIn(1, 300)
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = safeInitial - 1
    )
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val centeredMinutes = centeredDurationMinutes(listState) ?: safeInitial

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        contentAlignment = Alignment.Center
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            state = listState,
            flingBehavior = flingBehavior,
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(vertical = 88.dp)
        ) {
            items(300) { index ->
                val minutes = index + 1
                DurationWheelItem(
                    minutes = minutes,
                    isCentered = minutes == centeredMinutes,
                    distanceFromCenter = abs(minutes - centeredMinutes),
                    onClick = {
                        if (minutes == centeredMinutes) {
                            onDurationSelected(minutes)
                        }
                    }
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            HorizontalDivider(color = DurationAccent.copy(alpha = 0.35f), thickness = 1.dp)
            HorizontalDivider(color = DurationAccent.copy(alpha = 0.35f), thickness = 1.dp)
        }
    }
}

@Composable
private fun DurationWheelItem(
    minutes: Int,
    isCentered: Boolean,
    distanceFromCenter: Int,
    onClick: () -> Unit
) {
    val alpha = when {
        isCentered -> 1f
        distanceFromCenter == 1 -> 0.78f
        distanceFromCenter == 2 -> 0.48f
        else -> 0.25f
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (isCentered) "$minutes 分钟" else minutes.toString(),
            color = if (isCentered) DurationAccent else AppSecondaryText.copy(alpha = alpha),
            fontSize = if (isCentered) 22.sp else 17.sp,
            fontWeight = if (isCentered) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}

private fun centeredDurationMinutes(listState: LazyListState): Int? {
    val layoutInfo = listState.layoutInfo
    val visibleItems = layoutInfo.visibleItemsInfo
    if (visibleItems.isEmpty()) return null

    val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
    val centeredItem = visibleItems.minByOrNull { item ->
        abs((item.offset + item.size / 2) - viewportCenter)
    } ?: return null

    return (centeredItem.index + 1).coerceIn(1, 300)
}
