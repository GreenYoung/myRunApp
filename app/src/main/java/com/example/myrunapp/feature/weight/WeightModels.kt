package com.example.myrunapp.feature.weight

data class WeightPoint(
    val date: String,
    val weightKg: Double
)

data class WeightRecordItem(
    val date: String,
    val weightKg: Double,
    val previousChange: Double?
)

data class WeightTrendPointUiState(
    val date: String,
    val weightKg: Double,
    val previousChange: Double?
)

data class WeightAxisRange(
    val min: Double,
    val max: Double
)

data class WeightValidationResult(
    val weightKg: Double? = null,
    val error: String? = null
) {
    val isValid: Boolean = error == null && weightKg != null
}

data class WeightCardUiState(
    val todayWeight: Double? = null,
    val hasTodayWeight: Boolean = false,
    val latestWeight: Double? = null,
    val latestDate: String? = null,
    val previousChange: Double? = null,
    val totalChange: Double? = null
)

enum class WeightRange {
    DAYS_7,
    DAYS_30,
    DAYS_90,
    ALL
}

data class WeightDetailUiState(
    val latestWeight: Double? = null,
    val previousChange: Double? = null,
    val totalChange: Double? = null,
    val weeklyChange: Double? = null,
    val firstWeight: Double? = null,
    val targetWeight: Double? = null,
    val remainingWeight: Double? = null,
    val targetProgress: Float? = null,
    val latestDate: String? = null,
    val selectedRange: WeightRange = WeightRange.DAYS_30,
    val chartRecords: List<WeightPoint> = emptyList(),
    val recentRecords: List<WeightRecordItem> = emptyList()
)

data class WeightTrendUiState(
    val selectedRange: WeightRange = WeightRange.DAYS_7,
    val records: List<WeightTrendPointUiState> = emptyList(),
    val targetWeight: Double? = null
)

data class WeightUiState(
    val latestWeightKg: Double? = null,
    val latestDate: String? = null,
    val todayWeightKg: Double? = null,
    val weightCard: WeightCardUiState = WeightCardUiState(),
    val weightDetail: WeightDetailUiState = WeightDetailUiState(),
    val weightTrend: WeightTrendUiState = WeightTrendUiState(),
    val records: List<WeightPoint> = emptyList(),
    val isRecordDialogVisible: Boolean = false,
    val editingWeightDate: String? = null,
    val inputDate: String = "",
    val inputWeight: String = "",
    val inputError: String? = null,
    val isTargetDialogVisible: Boolean = false,
    val inputTargetWeight: String = "",
    val targetInputError: String? = null
)
