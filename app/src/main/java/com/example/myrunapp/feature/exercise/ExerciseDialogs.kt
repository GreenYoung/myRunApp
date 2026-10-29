package com.example.myrunapp.feature.exercise

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.myrunapp.ui.components.AppDangerButton
import com.example.myrunapp.ui.components.AppDialogButtonRow
import com.example.myrunapp.ui.components.AppInputField
import com.example.myrunapp.ui.components.AppPrimaryButton
import com.example.myrunapp.ui.components.AppSecondaryButton
import com.example.myrunapp.ui.components.ExerciseDatePicker
import com.example.myrunapp.ui.theme.Accent
import com.example.myrunapp.ui.theme.AppBackground
import com.example.myrunapp.ui.theme.AppGrid
import com.example.myrunapp.ui.theme.AppSecondaryText
import com.example.myrunapp.ui.theme.AppSurface
import kotlin.math.abs

private val CardDark = Color(0xFF111820)
internal val ExerciseDialogControlHeight = 42.dp

@Composable
internal fun ExerciseGoalDialog(
    weeklyGoal: String,
    monthlyGoal: String,
    error: String?,
    onWeeklyGoalChange: (String) -> Unit,
    onMonthlyGoalChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppSurface,
        title = {
            Text(
                text = "运动目标",
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                ExerciseGoalInputField(
                    label = "周运动里程",
                    value = weeklyGoal,
                    onValueChange = onWeeklyGoalChange
                )
                ExerciseGoalInputField(
                    label = "月运动里程",
                    value = monthlyGoal,
                    onValueChange = onMonthlyGoalChange
                )
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            AppDialogButtonRow(
                onCancel = onDismiss,
                onConfirm = onSave,
                confirmText = "保存"
            )
        }
    )
}

@Composable
private fun ExerciseGoalInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppInputField(
            label = label,
            value = value,
            onValueChange = onValueChange,
            keyboardType = KeyboardType.Decimal,
            modifier = Modifier.weight(1f)
        )
        Text("km", color = AppSecondaryText, fontWeight = FontWeight.Medium)
    }
}

@Composable
internal fun ExerciseRecordActionDialog(
    record: ExerciseRecordUiModel,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppSurface,
        title = {
            Text("记录操作", color = Color.White, fontWeight = FontWeight.Bold)
        },
        text = {
            Text(
                text = "${record.dateTimeText} · ${record.typeText} · ${record.distanceText}",
                color = AppSecondaryText,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AppSecondaryButton(
                    text = "取消",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    height = 52.dp
                )
                AppPrimaryButton(
                    text = "编辑",
                    onClick = onEdit,
                    modifier = Modifier.weight(1f),
                    height = 52.dp
                )
                AppDangerButton(text = "删除", onClick = onDelete, modifier = Modifier.weight(1f))
            }
        }
    )
}

@Composable
internal fun DeleteExerciseRecordDialog(
    record: ExerciseRecordUiModel,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppSurface,
        title = {
            Text("删除运动记录", color = Color.White, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("确认删除这条运动记录？", color = Color.White)
                Text(
                    text = "${record.dateTimeText} · ${record.typeText} · ${record.distanceText}",
                    color = AppSecondaryText,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AppSecondaryButton(
                    text = "取消",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    height = 52.dp
                )
                AppDangerButton(text = "删除", onClick = onDelete, modifier = Modifier.weight(1f))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddExerciseRecordDialog(
    uiState: ExerciseUiState,
    onDismiss: () -> Unit,
    onDateChange: (String) -> Unit,
    onDistanceChange: (String) -> Unit,
    onDurationSelected: (Int) -> Unit,
    onInclineChange: (String) -> Unit,
    onSave: () -> Unit
) {
    var isDatePickerVisible by remember { mutableStateOf(false) }
    var isDurationPickerVisible by remember { mutableStateOf(false) }
    val isTreadmill = uiState.inputType == ExerciseType.TREADMILL
    val durationLabel = when {
        uiState.editingExerciseRecordId != null &&
            !uiState.durationSelectionChanged &&
            uiState.editingOriginalDurationSeconds != null ->
            formatExerciseDurationDetail(uiState.editingOriginalDurationSeconds)
        uiState.selectedDurationMinutes != null -> "${uiState.selectedDurationMinutes} 分钟"
        else -> null
    }

    AlertDialog(
        modifier = Modifier.imePadding(),
        onDismissRequest = onDismiss,
        containerColor = AppSurface,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ExerciseDateField(
                    value = uiState.inputDate.ifBlank { todayExerciseDate() },
                    onClick = { isDatePickerVisible = true }
                )
                if (isTreadmill) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        InputField(
                            label = "里程 km",
                            value = uiState.inputDistanceKm,
                            onValueChange = onDistanceChange,
                            keyboardType = KeyboardType.Decimal,
                            modifier = Modifier.weight(1f),
                            height = ExerciseDialogControlHeight
                        )
                        InputField(
                            label = "坡度 %",
                            value = uiState.inputInclinePercent,
                            onValueChange = onInclineChange,
                            keyboardType = KeyboardType.Decimal,
                            modifier = Modifier.weight(1f),
                            height = ExerciseDialogControlHeight
                        )
                    }
                } else {
                    InputField(
                        label = "里程 km",
                        value = uiState.inputDistanceKm,
                        onValueChange = onDistanceChange,
                        keyboardType = KeyboardType.Decimal,
                        height = ExerciseDialogControlHeight
                    )
                }
                ExerciseDurationField(
                    label = durationLabel,
                    onClick = { isDurationPickerVisible = true }
                )
                uiState.inputError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = { ExerciseDialogButtonRow(onCancel = onDismiss, onConfirm = onSave) }
    )

    if (isDatePickerVisible) {
        ExerciseDatePicker(
            selectedDateText = uiState.inputDate,
            onDateSelected = {
                onDateChange(it)
                isDatePickerVisible = false
            },
            onDismiss = { isDatePickerVisible = false }
        )
    }

    if (isDurationPickerVisible) {
        ExerciseDurationPicker(
            selectedMinutes = uiState.selectedDurationMinutes,
            onDurationSelected = {
                onDurationSelected(it)
                isDurationPickerVisible = false
            },
            onDismiss = { isDurationPickerVisible = false }
        )
    }
}

@Composable
private fun ExerciseDateField(value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(ExerciseDialogControlHeight)
            .background(AppBackground, RoundedCornerShape(8.dp))
            .border(1.dp, AppGrid, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Text("📅", color = Accent, fontSize = 15.sp)
    }
}

@Composable
private fun ExerciseDurationField(label: String?, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(ExerciseDialogControlHeight)
            .background(AppBackground, RoundedCornerShape(8.dp))
            .border(1.dp, AppGrid, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label ?: "请选择运动时长",
            color = if (label == null) AppSecondaryText else Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        Text("›", color = Accent, fontSize = 21.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ExerciseDialogButtonRow(
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AppSecondaryButton(
            text = "取消",
            onClick = onCancel,
            modifier = Modifier.weight(1f),
            height = ExerciseDialogControlHeight
        )
        AppPrimaryButton(
            text = "保存",
            onClick = onConfirm,
            modifier = Modifier.weight(1f),
            height = ExerciseDialogControlHeight
        )
    }
}

@Composable
private fun ExerciseDurationPicker(
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
                        colors = listOf(Color(0xFF151E26), CardDark),
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
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 88.dp)
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
            HorizontalDivider(color = ExerciseGreen.copy(alpha = 0.35f), thickness = 1.dp)
            HorizontalDivider(color = ExerciseGreen.copy(alpha = 0.35f), thickness = 1.dp)
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
            color = if (isCentered) ExerciseGreen else AppSecondaryText.copy(alpha = alpha),
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

@Composable
private fun InputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType,
    modifier: Modifier = Modifier,
    height: Dp = ExerciseDialogControlHeight
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            color = Color.White,
            fontSize = 14.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Medium
        ),
        cursorBrush = SolidColor(Accent),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(AppBackground, RoundedCornerShape(8.dp))
            .border(1.dp, AppGrid, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isBlank()) {
                    Text(
                        text = label,
                        color = AppSecondaryText,
                        fontSize = 14.sp,
                        lineHeight = 16.sp,
                        maxLines = 1
                    )
                }
                innerTextField()
            }
        }
    )
}
