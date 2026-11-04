package com.example.myrunapp.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myrunapp.feature.exercise.ExerciseRecordUiModel
import com.example.myrunapp.feature.exercise.ExerciseType
import com.example.myrunapp.feature.exercise.ExerciseUiState
import com.example.myrunapp.feature.exercise.formatExerciseDurationDetail
import com.example.myrunapp.feature.exercise.todayExerciseDate
import com.example.myrunapp.feature.weight.WeightRecordItem
import com.example.myrunapp.feature.weight.WeightUiState
import com.example.myrunapp.feature.weight.formatWeight
import com.example.myrunapp.ui.components.AppDangerButton
import com.example.myrunapp.ui.components.AppDialogDateField
import com.example.myrunapp.ui.components.AppDialogError
import com.example.myrunapp.ui.components.AppDialogField
import com.example.myrunapp.ui.components.AppDialogMessage
import com.example.myrunapp.ui.components.AppDialogPickerField
import com.example.myrunapp.ui.components.AppDialogScaffold
import com.example.myrunapp.ui.components.AppPrimaryButton
import com.example.myrunapp.ui.components.AppSecondaryButton
import com.example.myrunapp.ui.components.ExerciseDatePicker
import com.example.myrunapp.ui.components.ExerciseDurationPicker

private val DialogButtonHeight = 48.dp
private val DialogButtonCorner = 14.dp

// ---------------------------------------------------------------------------
// 运动目标
// ---------------------------------------------------------------------------

@Composable
fun ExerciseGoalDialog(
    weeklyGoal: String,
    monthlyGoal: String,
    error: String?,
    onWeeklyGoalChange: (String) -> Unit,
    onMonthlyGoalChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    AppDialogScaffold(
        title = "运动目标",
        onDismiss = onDismiss,
        confirmText = "保存",
        onConfirm = onSave
    ) {
        ExerciseGoalFields(
            weeklyGoal = weeklyGoal,
            monthlyGoal = monthlyGoal,
            error = error,
            onWeeklyGoalChange = onWeeklyGoalChange,
            onMonthlyGoalChange = onMonthlyGoalChange
        )
    }
}

@Composable
fun ExerciseGoalFields(
    weeklyGoal: String,
    monthlyGoal: String,
    error: String?,
    onWeeklyGoalChange: (String) -> Unit,
    onMonthlyGoalChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AppDialogField(
            label = "周运动里程",
            value = weeklyGoal,
            onValueChange = onWeeklyGoalChange,
            unit = "km",
            placeholder = "例如 20"
        )
        AppDialogField(
            label = "月运动里程",
            value = monthlyGoal,
            onValueChange = onMonthlyGoalChange,
            unit = "km",
            placeholder = "例如 80"
        )
        error?.let { AppDialogError(it) }
    }
}

// ---------------------------------------------------------------------------
// 添加 / 编辑运动记录
// ---------------------------------------------------------------------------

@Composable
fun AddExerciseRecordDialog(
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

    AppDialogScaffold(
        title = if (uiState.editingExerciseRecordId != null) "编辑运动记录" else "添加运动记录",
        onDismiss = onDismiss,
        confirmText = "保存",
        onConfirm = onSave
    ) {
        AddExerciseRecordFields(
            date = uiState.inputDate.ifBlank { todayExerciseDate() },
            distanceKm = uiState.inputDistanceKm,
            inclinePercent = uiState.inputInclinePercent,
            durationLabel = durationLabel,
            error = uiState.inputError,
            isTreadmill = isTreadmill,
            onDateClick = { isDatePickerVisible = true },
            onDistanceChange = onDistanceChange,
            onInclineChange = onInclineChange,
            onDurationClick = { isDurationPickerVisible = true }
        )
    }

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
fun AddExerciseRecordFields(
    date: String,
    distanceKm: String,
    inclinePercent: String,
    durationLabel: String?,
    error: String?,
    isTreadmill: Boolean,
    onDateClick: () -> Unit,
    onDistanceChange: (String) -> Unit,
    onInclineChange: (String) -> Unit,
    onDurationClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // 日期固定在最上面，先选日子再填数据
        AppDialogDateField(
            label = "日期",
            value = date,
            onClick = onDateClick
        )
        if (isTreadmill) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AppDialogField(
                    label = "里程",
                    value = distanceKm,
                    onValueChange = onDistanceChange,
                    unit = "km",
                    placeholder = "例如 5.2",
                    modifier = Modifier.weight(1f)
                )
                AppDialogField(
                    label = "坡度",
                    value = inclinePercent,
                    onValueChange = onInclineChange,
                    unit = "%",
                    placeholder = "0",
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            AppDialogField(
                label = "里程",
                value = distanceKm,
                onValueChange = onDistanceChange,
                unit = "km",
                placeholder = "例如 5.2"
            )
        }
        AppDialogPickerField(
            label = "运动时长",
            value = durationLabel.orEmpty(),
            onClick = onDurationClick,
            placeholder = "请选择运动时长"
        )
        error?.let { AppDialogError(it) }
    }
}

// ---------------------------------------------------------------------------
// 体重记录 / 目标体重
// ---------------------------------------------------------------------------

@Composable
fun WeightInputDialog(
    uiState: WeightUiState,
    onDismiss: () -> Unit,
    onDateChange: (String) -> Unit,
    onWeightChange: (String) -> Unit,
    onSave: () -> Unit
) {
    var isCalendarVisible by remember { mutableStateOf(false) }

    AppDialogScaffold(
        title = "记录体重",
        onDismiss = onDismiss,
        confirmText = "保存",
        onConfirm = onSave
    ) {
        WeightInputFields(
            weight = uiState.inputWeight,
            date = uiState.inputDate,
            error = uiState.inputError,
            onWeightChange = onWeightChange,
            onDateClick = { isCalendarVisible = true }
        )
    }

    if (isCalendarVisible) {
        ExerciseDatePicker(
            selectedDateText = uiState.inputDate,
            onDateSelected = {
                onDateChange(it)
                isCalendarVisible = false
            },
            onDismiss = { isCalendarVisible = false }
        )
    }
}

@Composable
fun WeightInputFields(
    weight: String,
    date: String,
    error: String?,
    onWeightChange: (String) -> Unit,
    onDateClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // 日期统一放在其它输入项之前
        AppDialogDateField(
            label = "日期",
            value = date,
            onClick = onDateClick
        )
        AppDialogField(
            label = "体重",
            value = weight,
            onValueChange = onWeightChange,
            unit = "kg",
            placeholder = "例如 72.5"
        )
        error?.let { AppDialogError(it) }
    }
}

@Composable
fun TargetWeightDialog(
    value: String,
    heightCm: String,
    error: String?,
    onValueChange: (String) -> Unit,
    onHeightCmChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    AppDialogScaffold(
        title = "设置目标体重",
        onDismiss = onDismiss,
        confirmText = "保存",
        onConfirm = onSave
    ) {
        TargetWeightFields(
            value = value,
            heightCm = heightCm,
            error = error,
            onValueChange = onValueChange,
            onHeightCmChange = onHeightCmChange
        )
    }
}

@Composable
fun TargetWeightFields(
    value: String,
    heightCm: String,
    error: String?,
    onValueChange: (String) -> Unit,
    onHeightCmChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AppDialogField(
            label = "目标体重",
            value = value,
            onValueChange = onValueChange,
            unit = "kg",
            placeholder = "例如 70.0"
        )
        AppDialogField(
            label = "身高（用于计算 BMI）",
            value = heightCm,
            onValueChange = onHeightCmChange,
            unit = "cm",
            placeholder = "例如 175"
        )
        error?.let { AppDialogError(it) }
    }
}

// ---------------------------------------------------------------------------
// 记录操作 / 删除确认
// ---------------------------------------------------------------------------

@Composable
fun ExerciseRecordActionDialog(
    record: ExerciseRecordUiModel,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    AppDialogScaffold(
        title = "记录操作",
        onDismiss = onDismiss,
        confirmText = "编辑",
        onConfirm = onEdit,
        actions = {
            AppSecondaryButton(
                text = "取消",
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                height = DialogButtonHeight,
                cornerRadius = DialogButtonCorner
            )
            AppPrimaryButton(
                text = "编辑",
                onClick = onEdit,
                modifier = Modifier.weight(1f),
                height = DialogButtonHeight,
                cornerRadius = DialogButtonCorner
            )
            AppDangerButton(
                text = "删除",
                onClick = onDelete,
                modifier = Modifier.weight(1f),
                height = DialogButtonHeight,
                cornerRadius = DialogButtonCorner
            )
        }
    ) {
        AppDialogMessage("${record.dateTimeText} · ${record.typeText} · ${record.distanceText}")
    }
}

@Composable
fun DeleteExerciseRecordDialog(
    record: ExerciseRecordUiModel,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    AppDialogScaffold(
        title = "删除运动记录",
        onDismiss = onDismiss,
        confirmText = "删除",
        onConfirm = onDelete,
        actions = {
            AppSecondaryButton(
                text = "取消",
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                height = DialogButtonHeight,
                cornerRadius = DialogButtonCorner
            )
            AppDangerButton(
                text = "删除",
                onClick = onDelete,
                modifier = Modifier.weight(1f),
                height = DialogButtonHeight,
                cornerRadius = DialogButtonCorner
            )
        }
    ) {
        AppDialogMessage("确认删除这条运动记录？\n${record.dateTimeText} · ${record.typeText} · ${record.distanceText}")
    }
}

@Composable
fun WeightRecordActionDialog(
    record: WeightRecordItem,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    AppDialogScaffold(
        title = "记录操作",
        onDismiss = onDismiss,
        confirmText = "编辑",
        onConfirm = onEdit,
        actions = {
            AppSecondaryButton(
                text = "取消",
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                height = DialogButtonHeight,
                cornerRadius = DialogButtonCorner
            )
            AppPrimaryButton(
                text = "编辑",
                onClick = onEdit,
                modifier = Modifier.weight(1f),
                height = DialogButtonHeight,
                cornerRadius = DialogButtonCorner
            )
            AppDangerButton(
                text = "删除",
                onClick = onDelete,
                modifier = Modifier.weight(1f),
                height = DialogButtonHeight,
                cornerRadius = DialogButtonCorner
            )
        }
    ) {
        AppDialogMessage("${record.date} · ${formatWeight(record.weightKg)} kg")
    }
}

@Composable
fun DeleteWeightRecordDialog(
    record: WeightRecordItem,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    AppDialogScaffold(
        title = "删除体重记录",
        onDismiss = onDismiss,
        confirmText = "删除",
        onConfirm = onDelete,
        actions = {
            AppSecondaryButton(
                text = "取消",
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                height = DialogButtonHeight,
                cornerRadius = DialogButtonCorner
            )
            AppDangerButton(
                text = "删除",
                onClick = onDelete,
                modifier = Modifier.weight(1f),
                height = DialogButtonHeight,
                cornerRadius = DialogButtonCorner
            )
        }
    ) {
        AppDialogMessage("${record.date} · ${formatWeight(record.weightKg)} kg")
    }
}

// ---------------------------------------------------------------------------
// 跑步相关确认框
// ---------------------------------------------------------------------------

@Composable
fun BatteryOptimizationDialog(
    onConfirm: () -> Unit,
    onDecline: () -> Unit,
    onCancel: () -> Unit
) {
    AppDialogScaffold(
        title = "开启后台持续定位",
        onDismiss = onCancel,
        confirmText = "去设置",
        onConfirm = onConfirm,
        actions = {
            AppSecondaryButton(
                text = "暂不",
                onClick = onDecline,
                modifier = Modifier.weight(1f),
                height = DialogButtonHeight,
                cornerRadius = DialogButtonCorner
            )
            AppPrimaryButton(
                text = "去设置",
                onClick = onConfirm,
                modifier = Modifier.weight(1f),
                height = DialogButtonHeight,
                cornerRadius = DialogButtonCorner
            )
        }
    ) {
        AppDialogMessage("锁屏后系统可能冻结定位和计时，导致轨迹中断。建议把 MyRun 加入电池优化白名单，保证跑步全程持续记录。")
    }
}

@Composable
fun DiscardRecoveredRunDialog(
    onConfirm: () -> Unit,
    onDecline: () -> Unit,
    onDismiss: () -> Unit
) {
    AppDialogScaffold(
        title = "放弃未完成的跑步记录？",
        onDismiss = onDismiss,
        confirmText = "放弃",
        onConfirm = onConfirm,
        actions = {
            AppSecondaryButton(
                text = "保留记录",
                onClick = onDecline,
                modifier = Modifier.weight(1f),
                height = DialogButtonHeight,
                cornerRadius = DialogButtonCorner
            )
            AppDangerButton(
                text = "放弃",
                onClick = onConfirm,
                modifier = Modifier.weight(1f),
                height = DialogButtonHeight,
                cornerRadius = DialogButtonCorner
            )
        }
    ) {
        AppDialogMessage("未完成的跑步记录将被删除，且无法恢复。")
    }
}
