package com.example.myrunapp.feature.exercise

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myrunapp.feature.exercise.data.ExerciseDao
import com.example.myrunapp.feature.exercise.data.ExerciseGoalRepository
import com.example.myrunapp.feature.exercise.data.ExerciseRecordEntity
import com.example.myrunapp.feature.run.data.RunDao
import com.example.myrunapp.feature.weight.data.WeightDao
import java.util.Locale
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ExerciseViewModel(
    private val exerciseDao: ExerciseDao,
    private val runDao: RunDao,
    private val weightDao: WeightDao,
    private val exerciseGoalRepository: ExerciseGoalRepository
) : ViewModel() {
    private val inputState = MutableStateFlow(
        ExerciseUiState(
            inputDate = todayExerciseDate(),
            inputDurationSeconds = "0"
        )
    )

    val uiState = combine(
        exerciseDao.observeAllExercises(),
        runDao.observeAllSessions(),
        runDao.observeAllTrackPoints(),
        exerciseGoalRepository.goalSettings,
        inputState
    ) { records, runSessions, runTrackPoints, goalSettings, input ->
        input.copy(
            summary = buildExerciseSummary(
                records = records,
                weeklyGoal = goalSettings.weeklyDistanceGoalKm,
                monthlyGoal = goalSettings.monthlyDistanceGoalKm
            ),
            detail = buildExerciseDetail(
                records = records,
                runSessions = runSessions,
                runTrackPoints = runTrackPoints,
                selectedStatsRange = input.detail.selectedStatsRange
            ),
            goal = ExerciseGoalUiState(
                weeklyGoalKm = goalSettings.weeklyDistanceGoalKm,
                monthlyGoalKm = goalSettings.monthlyDistanceGoalKm
            )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ExerciseUiState(
            inputDate = todayExerciseDate(),
            inputDurationSeconds = "0"
        )
    )

    fun showAddDialog() {
        inputState.update {
            it.copy(
                isAddDialogVisible = true,
                editingExerciseRecordId = null,
                inputType = ExerciseType.TREADMILL,
                inputDate = todayExerciseDate(),
                inputDistanceKm = "",
                selectedDurationMinutes = null,
                inputDurationHours = "0",
                inputDurationMinutes = "",
                inputDurationSeconds = "0",
                inputInclinePercent = "",
                inputError = null
            )
        }
    }

    fun hideAddDialog() {
        inputState.update { it.copy(isAddDialogVisible = false, editingExerciseRecordId = null, inputError = null) }
    }

    fun showEditDialog(record: ExerciseRecordUiModel) {
        val durationMinutes = (record.durationSeconds / 60L).toInt().coerceIn(1, 300)
        inputState.update {
            it.copy(
                isAddDialogVisible = true,
                editingExerciseRecordId = record.id,
                inputType = record.type,
                inputDate = formatExerciseInputDate(record.startTime),
                inputDistanceKm = formatDistance(record.distanceKm),
                selectedDurationMinutes = durationMinutes,
                inputDurationHours = "0",
                inputDurationMinutes = durationMinutes.toString(),
                inputDurationSeconds = "0",
                inputInclinePercent = "",
                inputError = null
            )
        }
    }

    fun showGoalDialog() {
        val goal = uiState.value.goal
        inputState.update {
            it.copy(
                isGoalDialogVisible = true,
                inputWeeklyGoalKm = formatGoalInput(goal.weeklyGoalKm),
                inputMonthlyGoalKm = formatGoalInput(goal.monthlyGoalKm),
                goalInputError = null
            )
        }
    }

    fun hideGoalDialog() {
        inputState.update { it.copy(isGoalDialogVisible = false, goalInputError = null) }
    }

    fun onTypeChange(type: ExerciseType) {
        inputState.update { it.copy(inputType = type, inputError = null) }
    }

    fun onDateChange(value: String) {
        inputState.update { it.copy(inputDate = value, inputError = null) }
    }

    fun onDistanceChange(value: String) {
        inputState.update { it.copy(inputDistanceKm = value, inputError = null) }
    }

    fun onDurationHoursChange(value: String) {
        inputState.update { it.copy(inputDurationHours = value, inputError = null) }
    }

    fun onDurationMinutesChange(value: String) {
        inputState.update { it.copy(inputDurationMinutes = value, inputError = null) }
    }

    fun onDurationSelected(minutes: Int) {
        inputState.update {
            it.copy(
                selectedDurationMinutes = minutes,
                inputDurationHours = "0",
                inputDurationMinutes = minutes.toString(),
                inputDurationSeconds = "0",
                inputError = null
            )
        }
    }

    fun onDurationSecondsChange(value: String) {
        inputState.update { it.copy(inputDurationSeconds = value, inputError = null) }
    }

    fun onInclineChange(value: String) {
        inputState.update { it.copy(inputInclinePercent = value, inputError = null) }
    }

    fun onWeeklyGoalChange(value: String) {
        inputState.update { it.copy(inputWeeklyGoalKm = value, goalInputError = null) }
    }

    fun onMonthlyGoalChange(value: String) {
        inputState.update { it.copy(inputMonthlyGoalKm = value, goalInputError = null) }
    }

    fun onStatsRangeChange(range: ExerciseStatsRange) {
        inputState.update {
            it.copy(
                detail = it.detail.copy(selectedStatsRange = range)
            )
        }
    }

    fun observeStatsDetail(
        period: ExerciseStatsPeriod,
        periodOffset: Int = 0
    ): Flow<ExerciseStatsDetailUiState> {
        return exerciseDao.observeAllExercises().map { records ->
            buildExerciseStatsDetail(records, period, periodOffset)
        }
    }

    fun saveExercise() {
        val current = uiState.value
        val selectedMinutes = current.selectedDurationMinutes
        if (selectedMinutes == null) {
            inputState.update { it.copy(inputError = "请选择运动时长") }
            return
        }
        if (selectedMinutes !in 1..300) {
            inputState.update { it.copy(inputError = "运动时长需在 1～300 分钟之间") }
            return
        }

        val durationHours = selectedMinutes / 60
        val durationMinutes = selectedMinutes % 60
        val validation = validateExerciseInput(
            date = current.inputDate,
            type = current.inputType,
            distanceKm = current.inputDistanceKm,
            hours = durationHours.toString(),
            minutes = durationMinutes.toString(),
            seconds = "0",
            inclinePercent = current.inputInclinePercent
        )

        if (!validation.isValid) {
            inputState.update { it.copy(inputError = validation.error) }
            return
        }

        viewModelScope.launch {
            val recordDate = current.inputDate.trim().ifEmpty { todayExerciseDate() }
            val weightKg = weightDao.getLatestWeightOnOrBefore(recordDate)?.weightKg ?: 70.0
            val caloriesKcal = estimateExerciseCalories(
                weightKg = weightKg,
                type = current.inputType,
                durationSeconds = validation.durationSeconds ?: return@launch,
                distanceKm = validation.distanceKm ?: return@launch,
                inclinePercent = validation.inclinePercent ?: 0.0
            )
            val startTime = if (
                current.editingExerciseRecordId == null &&
                current.inputType == ExerciseType.TREADMILL
            ) {
                exerciseDateWithCurrentClockTime(recordDate) ?: validation.startTime ?: return@launch
            } else {
                validation.startTime ?: return@launch
            }
            val entity = ExerciseRecordEntity(
                id = current.editingExerciseRecordId ?: 0L,
                type = current.inputType.name,
                startTime = startTime,
                durationSeconds = validation.durationSeconds,
                distanceKm = validation.distanceKm,
                caloriesKcal = caloriesKcal
            )
            if (current.editingExerciseRecordId == null) {
                exerciseDao.insertExercise(
                    entity.copy(id = 0L)
                )
            } else {
                exerciseDao.updateExercise(entity)
            }
            inputState.update {
                it.copy(
                    isAddDialogVisible = false,
                    editingExerciseRecordId = null,
                    inputDistanceKm = "",
                    selectedDurationMinutes = null,
                    inputDurationHours = "0",
                    inputDurationMinutes = "",
                    inputDurationSeconds = "0",
                    inputInclinePercent = "",
                    inputError = null
                )
            }
        }
    }

    fun deleteExercise(recordId: Long) {
        viewModelScope.launch {
            runDao.deleteTrackPointsByExerciseRecordId(recordId)
            runDao.deleteSessionsByExerciseRecordId(recordId)
            exerciseDao.deleteExerciseById(recordId)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeRecordDetail(recordId: Long): Flow<ExerciseRecordDetailUiState> {
        return combine(
            exerciseDao.observeExerciseById(recordId),
            runDao.observeSessionByExerciseRecordId(recordId).flatMapLatest { session ->
                if (session == null) {
                    flowOf(session to emptyList())
                } else {
                    runDao.observeTrackPoints(session.id).map { points -> session to points }
                }
            }
        ) { record, sessionAndPoints ->
            if (record == null) {
                ExerciseRecordDetailUiState(isMissing = true)
            } else {
                val (session, points) = sessionAndPoints
                ExerciseRecordDetailUiState(
                    record = record.toExerciseRecordUiModel(
                        runSession = session,
                        trackPoints = points
                    ),
                    runSessionId = session?.id,
                    trackPoints = record.toExerciseRecordUiModel(session, points).trackPoints,
                    isMissing = false
                )
            }
        }
    }

    fun saveExerciseGoals() {
        val current = uiState.value
        val weeklyInput = current.inputWeeklyGoalKm.trim()
        val monthlyInput = current.inputMonthlyGoalKm.trim()
        val weeklyGoal = if (weeklyInput.isEmpty()) {
            current.goal.weeklyGoalKm
        } else {
            weeklyInput.toDoubleOrNull()
        }
        val monthlyGoal = if (monthlyInput.isEmpty()) {
            current.goal.monthlyGoalKm
        } else {
            monthlyInput.toDoubleOrNull()
        }

        when {
            weeklyGoal == null || monthlyGoal == null -> {
                inputState.update { it.copy(goalInputError = "请输入合法运动目标") }
            }
            weeklyGoal <= 0.0 || weeklyGoal > 500.0 -> {
                inputState.update { it.copy(goalInputError = "周运动里程需大于 0 且不超过 500 km") }
            }
            monthlyGoal <= 0.0 || monthlyGoal > 2000.0 -> {
                inputState.update { it.copy(goalInputError = "月运动里程需大于 0 且不超过 2000 km") }
            }
            monthlyGoal < weeklyGoal -> {
                inputState.update { it.copy(goalInputError = "月运动里程不能小于周运动里程") }
            }
            else -> {
                viewModelScope.launch {
                    exerciseGoalRepository.saveGoals(weeklyGoal, monthlyGoal)
                    inputState.update {
                        it.copy(
                            isGoalDialogVisible = false,
                            inputWeeklyGoalKm = "",
                            inputMonthlyGoalKm = "",
                            goalInputError = null
                        )
                    }
                }
            }
        }
    }
}

class ExerciseViewModelFactory(
    private val exerciseDao: ExerciseDao,
    private val runDao: RunDao,
    private val weightDao: WeightDao,
    private val exerciseGoalRepository: ExerciseGoalRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExerciseViewModel::class.java)) {
            return ExerciseViewModel(exerciseDao, runDao, weightDao, exerciseGoalRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

private fun formatGoalInput(value: Double): String {
    return String.format(Locale.US, "%.2f", value)
}
