package com.example.myrunapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.myrunapp.ui.theme.Accent
import com.example.myrunapp.ui.theme.AppBackground
import com.example.myrunapp.ui.theme.AppSecondaryText
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private val DatePickerGreen = Color(0xFF22C55E)
private val DatePickerDark = Color(0xFF111820)

@Composable
fun ExerciseDatePicker(
    selectedDateText: String,
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val initialSelected = remember(selectedDateText) { parseDateOrToday(selectedDateText) }
    val today = remember { startOfDate(Calendar.getInstance()) }
    var browsingMonth by remember(selectedDateText) {
        mutableStateOf(monthStart(initialSelected))
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF151E26), DatePickerDark),
                        start = Offset.Zero,
                        end = Offset.Infinite
                    ),
                    shape = RoundedCornerShape(18.dp)
                )
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ExerciseDatePickerHeader(
                month = browsingMonth,
                onPreviousMonth = {
                    browsingMonth = shiftMonth(browsingMonth, -1)
                },
                onNextMonth = {
                    browsingMonth = shiftMonth(browsingMonth, 1)
                }
            )
            ExerciseWeekHeader()
            ExerciseDateGrid(
                month = browsingMonth,
                selectedDate = initialSelected,
                today = today,
                onDateSelected = onDateSelected
            )
        }
    }
}

@Composable
private fun ExerciseDatePickerHeader(
    month: Calendar,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        DatePickerArrow("‹", onPreviousMonth)
        Text(
            text = "${month.get(Calendar.YEAR)}年${month.get(Calendar.MONTH) + 1}月",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        DatePickerArrow("›", onNextMonth)
    }
}

@Composable
private fun DatePickerArrow(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(AppBackground.copy(alpha = 0.55f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = Accent, fontSize = 24.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ExerciseWeekHeader() {
    Row(modifier = Modifier.fillMaxWidth()) {
        listOf("日", "一", "二", "三", "四", "五", "六").forEach { week ->
            Text(
                text = week,
                modifier = Modifier.weight(1f),
                color = AppSecondaryText,
                textAlign = TextAlign.Center,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun ExerciseDateGrid(
    month: Calendar,
    selectedDate: Calendar,
    today: Calendar,
    onDateSelected: (String) -> Unit
) {
    val daysInMonth = month.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOffset = monthStart(month).get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY
    val cells = 42

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(cells / 7) { rowIndex ->
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(7) { columnIndex ->
                    val cellIndex = rowIndex * 7 + columnIndex
                    val day = cellIndex - firstDayOffset + 1
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (day in 1..daysInMonth) {
                            val cellDate = dateInMonth(month, day)
                            ExerciseDateCell(
                                day = day,
                                isSelected = sameDate(cellDate, selectedDate),
                                isToday = sameDate(cellDate, today),
                                onClick = { onDateSelected(formatDate(cellDate)) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExerciseDateCell(
    day: Int,
    isSelected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) DatePickerGreen else Color.Transparent
    val borderColor = if (!isSelected && isToday) DatePickerGreen else Color.Transparent
    val textColor = if (isSelected) Color(0xFF06130E) else Color.White

    Box(
        modifier = Modifier
            .size(40.dp)
            .background(backgroundColor, CircleShape)
            .border(1.dp, borderColor, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = day.toString(),
            color = textColor,
            fontSize = 15.sp,
            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium
        )
    }
}

private fun parseDateOrToday(value: String): Calendar {
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        isLenient = false
    }
    return runCatching {
        val parsed = formatter.parse(value.trim())
        Calendar.getInstance().apply {
            time = parsed ?: Calendar.getInstance().time
        }
    }.getOrDefault(Calendar.getInstance()).let(::startOfDate)
}

private fun formatDate(date: Calendar): String {
    return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date.time)
}

private fun startOfDate(date: Calendar): Calendar {
    return Calendar.getInstance().apply {
        timeInMillis = date.timeInMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
}

private fun monthStart(date: Calendar): Calendar {
    return startOfDate(date).apply {
        set(Calendar.DAY_OF_MONTH, 1)
    }
}

private fun shiftMonth(date: Calendar, amount: Int): Calendar {
    return monthStart(date).apply {
        add(Calendar.MONTH, amount)
    }
}

private fun dateInMonth(month: Calendar, day: Int): Calendar {
    return monthStart(month).apply {
        set(Calendar.DAY_OF_MONTH, day)
    }
}

private fun sameDate(left: Calendar, right: Calendar): Boolean {
    return left.get(Calendar.YEAR) == right.get(Calendar.YEAR) &&
        left.get(Calendar.MONTH) == right.get(Calendar.MONTH) &&
        left.get(Calendar.DAY_OF_MONTH) == right.get(Calendar.DAY_OF_MONTH)
}
