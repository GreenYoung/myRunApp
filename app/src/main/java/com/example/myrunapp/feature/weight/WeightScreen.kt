package com.example.myrunapp.feature.weight

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.myrunapp.ui.components.AppPageTopBar
import com.example.myrunapp.ui.components.AppPrimaryButton
import com.example.myrunapp.ui.components.AppSecondaryButton
import com.example.myrunapp.ui.components.PageTopSpacing
import com.example.myrunapp.ui.theme.AppBackground
import com.example.myrunapp.ui.theme.AppSecondaryText
import com.example.myrunapp.ui.theme.MyRunAppTheme
import java.util.Locale
import kotlin.math.abs

internal val DetailGreen = Color(0xFF22C55E)
internal val DetailRed = Color(0xFFF87171)

@Composable
fun WeightRoute(
    viewModel: WeightViewModel,
    onBack: () -> Unit,
    onTrendClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    WeightDetailScreen(
        uiState = uiState,
        onBack = onBack,
        onRecordClick = viewModel::showRecordDialog,
        onEditRecord = viewModel::showEditRecordDialog,
        onDeleteRecord = viewModel::deleteWeight,
        onDismissRecordDialog = viewModel::hideRecordDialog,
        onDateChange = viewModel::onInputDateChange,
        onWeightChange = viewModel::onInputWeightChange,
        onSaveWeight = viewModel::saveWeight,
        onRangeChange = viewModel::onRangeChange,
        onTrendClick = onTrendClick,
        onTargetClick = viewModel::showTargetDialog,
        onDismissTargetDialog = viewModel::hideTargetDialog,
        onTargetWeightChange = viewModel::onTargetWeightChange,
        onHeightCmChange = viewModel::onHeightCmChange,
        onSaveTargetWeight = viewModel::saveTargetWeight,
        modifier = modifier
    )
}

@Composable
fun WeightDetailScreen(
    uiState: WeightUiState,
    onBack: () -> Unit,
    onRecordClick: () -> Unit,
    onEditRecord: (WeightRecordItem) -> Unit,
    onDeleteRecord: (WeightRecordItem) -> Unit,
    onDismissRecordDialog: () -> Unit,
    onDateChange: (String) -> Unit,
    onWeightChange: (String) -> Unit,
    onSaveWeight: () -> Unit,
    onRangeChange: (WeightRange) -> Unit,
    onTrendClick: () -> Unit,
    onTargetClick: () -> Unit,
    onDismissTargetDialog: () -> Unit,
    onTargetWeightChange: (String) -> Unit,
    onHeightCmChange: (String) -> Unit,
    onSaveTargetWeight: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 23.dp, end = 23.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                top = PageTopSpacing,
                bottom = 28.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { WeightDetailTopBar(onBack = onBack) }
            item { LatestWeightCard(uiState.weightDetail) }
            item { WeightOverviewCard(uiState.weightDetail) }
            item { WeightExerciseCorrelationCard(uiState.weightDetail.exerciseCorrelation) }
            item { WeightTrendEntryCard(uiState = uiState.weightDetail, onClick = onTrendClick) }
            item {
                RecentWeightRecords(
                    records = uiState.weightDetail.recentRecords,
                    onEditRecord = onEditRecord,
                    onDeleteRecord = onDeleteRecord
                )
            }
            item {
                WeightActionSection(
                    targetWeight = uiState.weightDetail.targetWeight,
                    onTargetClick = onTargetClick,
                    onRecordClick = onRecordClick
                )
            }
        }

        if (uiState.isRecordDialogVisible) {
            WeightInputDialog(
                uiState = uiState,
                onDismiss = onDismissRecordDialog,
                onDateChange = onDateChange,
                onWeightChange = onWeightChange,
                onSave = onSaveWeight
            )
        }

        if (uiState.isTargetDialogVisible) {
            TargetWeightDialog(
                value = uiState.inputTargetWeight,
                heightCm = uiState.inputHeightCm,
                error = uiState.targetInputError,
                onValueChange = onTargetWeightChange,
                onHeightCmChange = onHeightCmChange,
                onDismiss = onDismissTargetDialog,
                onSave = onSaveTargetWeight
            )
        }
    }
}

@Composable
private fun WeightDetailTopBar(onBack: () -> Unit) {
    AppPageTopBar(title = "体重详情", showBackButton = true, onBackClick = onBack)
}

@Composable
private fun WeightActionSection(
    targetWeight: Double?,
    onTargetClick: () -> Unit,
    onRecordClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AppSecondaryButton(
            text = "目标体重\n${targetWeight?.let { "${formatWeight(it)} kg" } ?: "未设置"}",
            onClick = onTargetClick,
            modifier = Modifier.weight(1f),
            height = 58.dp
        )
        AppPrimaryButton(text = "+ 记录体重", onClick = onRecordClick, modifier = Modifier.weight(1f), height = 58.dp)
    }
}

internal fun Double?.toArrowChangeText(unit: Boolean): String {
    if (this == null) return "--"
    val suffix = if (unit) " kg" else ""
    val value = formatWeight(abs(this))
    return when {
        this < 0.0 -> "↓ $value$suffix"
        this > 0.0 -> "↑ $value$suffix"
        else -> "0.00$suffix"
    }
}

internal fun Double?.toSignedPlainText(): String {
    if (this == null) return "--"
    return String.format(Locale.US, "%+.2f kg", this)
}

internal fun Double?.toChangeColor(): Color {
    return when {
        this == null -> AppSecondaryText
        this < 0.0 -> DetailGreen
        this > 0.0 -> DetailRed
        else -> AppSecondaryText
    }
}

@Preview(showBackground = true)
@Composable
private fun WeightDetailScreenPreview() {
    MyRunAppTheme(darkTheme = true) {
        WeightDetailScreen(
            uiState = WeightUiState(
                weightDetail = WeightDetailUiState(
                    latestWeight = 77.20,
                    previousChange = -0.30,
                    totalChange = -1.80,
                    weeklyChange = -0.60,
                    firstWeight = 79.00,
                    targetWeight = 72.00,
                    remainingWeight = 5.20,
                    targetProgress = 0.35f,
                    latestDate = "2026-08-20",
                    bmi = WeightBmiUiState(heightCm = 178.0, bmi = 24.4, category = "偏高"),
                    changeSpeed = WeightChangeSpeedUiState(weeklyChangeKg = -0.42, description = "↓ 0.42 kg/周"),
                    exerciseCorrelation = WeightExerciseCorrelationUiState(
                        sevenDayDistanceKm = 12.3,
                        sevenDayCaloriesKcal = 760,
                        sevenDayWeightChangeKg = -0.4,
                        thirtyDayDistanceKm = 48.2,
                        thirtyDayWeightChangeKg = -1.2,
                        insight = "最近运动后体重呈下降趋势"
                    ),
                    chartRecords = listOf(
                        WeightPoint("2026-08-16", 78.0),
                        WeightPoint("2026-08-17", 77.8),
                        WeightPoint("2026-08-18", 77.6),
                        WeightPoint("2026-08-19", 77.5),
                        WeightPoint("2026-08-20", 77.2)
                    ),
                    recentRecords = listOf(
                        WeightRecordItem("2026-08-20", 77.2, -0.3, "跑后称重"),
                        WeightRecordItem("2026-08-19", 77.5, -0.1)
                    )
                )
            ),
            onBack = {},
            onRecordClick = {},
            onEditRecord = {},
            onDeleteRecord = {},
            onDismissRecordDialog = {},
            onDateChange = {},
            onWeightChange = {},
            onSaveWeight = {},
            onRangeChange = {},
            onTrendClick = {},
            onTargetClick = {},
            onDismissTargetDialog = {},
            onTargetWeightChange = {},
            onHeightCmChange = {},
            onSaveTargetWeight = {}
        )
    }
}
