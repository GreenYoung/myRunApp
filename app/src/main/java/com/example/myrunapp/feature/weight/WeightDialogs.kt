package com.example.myrunapp.feature.weight

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

private val WeightDialogControlHeight = 42.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WeightInputDialog(
    uiState: WeightUiState,
    onDismiss: () -> Unit,
    onDateChange: (String) -> Unit,
    onWeightChange: (String) -> Unit,
    onSave: () -> Unit
) {
    var isCalendarVisible by remember { mutableStateOf(false) }

    AlertDialog(
        modifier = Modifier.imePadding(),
        onDismissRequest = onDismiss,
        containerColor = AppSurface,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                WeightCompactInputField(
                    label = "体重 kg",
                    value = uiState.inputWeight,
                    onValueChange = onWeightChange,
                    keyboardType = KeyboardType.Decimal
                )
                DatePickerField(selectedDate = uiState.inputDate, onClick = { isCalendarVisible = true })
                uiState.inputError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = { WeightDialogButtonRow(onCancel = onDismiss, onConfirm = onSave) }
    )

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
private fun WeightDialogButtonRow(
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
            height = WeightDialogControlHeight
        )
        AppPrimaryButton(
            text = "保存",
            onClick = onConfirm,
            modifier = Modifier.weight(1f),
            height = WeightDialogControlHeight
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TargetWeightDialog(
    value: String,
    heightCm: String,
    error: String?,
    onValueChange: (String) -> Unit,
    onHeightCmChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        modifier = Modifier.imePadding(),
        onDismissRequest = onDismiss,
        containerColor = AppSurface,
        title = { Text("设置目标体重", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AppInputField(
                    label = "目标体重 kg",
                    value = value,
                    onValueChange = onValueChange,
                    keyboardType = KeyboardType.Decimal
                )
                AppInputField(
                    label = "身高 cm（用于 BMI）",
                    value = heightCm,
                    onValueChange = onHeightCmChange,
                    keyboardType = KeyboardType.Decimal
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = { AppDialogButtonRow(onCancel = onDismiss, onConfirm = onSave, confirmText = "保存") }
    )
}

@Composable
private fun WeightCompactInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType
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
        modifier = Modifier
            .fillMaxWidth()
            .height(WeightDialogControlHeight)
            .background(AppBackground, RoundedCornerShape(8.dp))
            .border(1.dp, AppGrid, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
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

@Composable
private fun DatePickerField(
    selectedDate: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(WeightDialogControlHeight)
            .background(AppBackground, RoundedCornerShape(8.dp))
            .border(1.dp, AppGrid, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(selectedDate, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Text("📅", color = Accent, fontSize = 15.sp)
    }
}
