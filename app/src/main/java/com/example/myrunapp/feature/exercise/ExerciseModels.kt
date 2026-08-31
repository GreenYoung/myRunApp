package com.example.myrunapp.feature.exercise

import com.example.myrunapp.feature.run.RunTrackPointUiModel

enum class ExerciseType {
    TREADMILL,
    OUTDOOR_RUNNING
}

enum class ExerciseStatsRange {
    WEEK,
    MONTH,
    YEAR,
    ALL
}

data class ExerciseSummaryUiState(
    val todayDistanceKm: Double = 0.0,
    val dailyGoalKm: Double = 8.0,
    val dailyProgress: Float = 0f,
    val weeklyDistanceKm: Double = 0.0,
    val weeklyGoalKm: Double = 40.0,
    val weeklyProgress: Float = 0f,
    val weeklyRemainingKm: Double = 40.0,
    val weeklyGoalCompleted: Boolean = false,
    val monthlyDistanceKm: Double = 0.0,
    val monthlyGoalKm: Double = 100.0,
    val monthlyProgress: Float = 0f,
    val monthlyProgressPercent: Int = 0,
    val monthlyGoalCompleted: Boolean = false,
    val totalDistanceKm: Double = 0.0,
    val checkInDays: Int = 0,
    val streakDays: Int = 0,
    val todayCaloriesKcal: Int = 0,
    val weeklyCaloriesKcal: Int = 0,
    val monthlyCaloriesKcal: Int = 0,
    val checkedInToday: Boolean = false
)

data class ExerciseRecordUiModel(
    val id: Long,
    val type: ExerciseType,
    val startTime: Long,
    val durationSeconds: Long,
    val distanceKm: Double,
    val typeText: String,
    val dateTimeText: String,
    val distanceText: String,
    val durationText: String,
    val paceText: String,
    val caloriesText: String,
    val runSessionId: Long? = null,
    val trackPoints: List<RunTrackPointUiModel> = emptyList()
)

data class ExerciseRecordDetailUiState(
    val record: ExerciseRecordUiModel? = null,
    val runSessionId: Long? = null,
    val trackPoints: List<RunTrackPointUiModel> = emptyList(),
    val isMissing: Boolean = false
)

val ExerciseRecordUiModel.hasTrack: Boolean
    get() = runSessionId != null && trackPoints.size >= 2

val ExerciseRecordUiModel.isOutdoorRun: Boolean
    get() = type == ExerciseType.OUTDOOR_RUNNING

val ExerciseRecordDetailUiState.isOutdoorRun: Boolean
    get() = record?.type == ExerciseType.OUTDOOR_RUNNING

val ExerciseRecordDetailUiState.hasTrack: Boolean
    get() = runSessionId != null && trackPoints.size >= 2

data class ExerciseDetailUiState(
    val totalDistanceKm: Double = 0.0,
    val checkInDays: Int = 0,
    val weeklyDistanceKm: Double = 0.0,
    val monthlyDistanceKm: Double = 0.0,
    val totalCaloriesKcal: Int = 0,
    val monthlyCaloriesKcal: Int = 0,
    val selectedStatsRange: ExerciseStatsRange = ExerciseStatsRange.WEEK,
    val rangeStats: ExerciseRangeStatsUiState = ExerciseRangeStatsUiState(),
    val records: List<ExerciseRecordUiModel> = emptyList()
)

data class ExerciseRangeStatsUiState(
    val range: ExerciseStatsRange = ExerciseStatsRange.WEEK,
    val totalDistanceKm: Double = 0.0,
    val totalCaloriesKcal: Int = 0,
    val checkInDays: Int = 0,
    val averagePaceText: String = "--'--\"/km",
    val longestDistanceKm: Double = 0.0
)

data class ExerciseGoalUiState(
    val weeklyGoalKm: Double = 40.0,
    val monthlyGoalKm: Double = 100.0
)

data class ExerciseUiState(
    val summary: ExerciseSummaryUiState = ExerciseSummaryUiState(),
    val detail: ExerciseDetailUiState = ExerciseDetailUiState(),
    val goal: ExerciseGoalUiState = ExerciseGoalUiState(),
    val isAddDialogVisible: Boolean = false,
    val editingExerciseRecordId: Long? = null,
    val isGoalDialogVisible: Boolean = false,
    val inputType: ExerciseType = ExerciseType.TREADMILL,
    val inputDate: String = "",
    val inputDistanceKm: String = "",
    val selectedDurationMinutes: Int? = null,
    val inputDurationHours: String = "",
    val inputDurationMinutes: String = "",
    val inputDurationSeconds: String = "",
    val inputInclinePercent: String = "",
    val inputWeeklyGoalKm: String = "",
    val inputMonthlyGoalKm: String = "",
    val goalInputError: String? = null,
    val inputError: String? = null
)

data class ExerciseValidationResult(
    val startTime: Long? = null,
    val durationSeconds: Long? = null,
    val distanceKm: Double? = null,
    val inclinePercent: Double? = null,
    val error: String? = null
) {
    val isValid: Boolean = error == null &&
        startTime != null &&
        durationSeconds != null &&
        distanceKm != null &&
        inclinePercent != null
}
