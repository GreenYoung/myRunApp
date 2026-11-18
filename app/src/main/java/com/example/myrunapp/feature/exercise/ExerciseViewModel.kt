package com.example.myrunapp.feature.exercise

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.example.myrunapp.core.data.AppDatabase
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val THUMBNAIL_TRACK_STRIDE = 10

private data class ExerciseStatsState(
    val summary: ExerciseSummaryUiState,
    val detail: ExerciseDetailUiState,
    val goal: ExerciseGoalUiState
)

class ExerciseViewModel(
    private val database: AppDatabase,
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

    private val selectedStatsRangeFlow = inputState
        .map { it.detail.selectedStatsRange }
        .distinctUntilChanged()

    /** 最近看过的 6 个统计周期，翻回头时直接命中，不用等数据库回流。 */
    private val statsDetailCache = object : LinkedHashMap<String, ExerciseStatsDetailUiState>(8, 0.75f, true) {
        override fun removeEldestEntry(
            eldest: MutableMap.MutableEntry<String, ExerciseStatsDetailUiState>?
        ): Boolean = size > 6
    }

    private val thumbnailTrackPointsFlow = combine(
        runDao.observeSampledTrackPoints(THUMBNAIL_TRACK_STRIDE),
        runDao.observeTrackPointEdges()
    ) { sampled, edges ->
        (sampled + edges)
            .distinctBy { it.id }
            .sortedBy { it.recordedAt }
    }

    private val statsState = combine(
        exerciseDao.observeAllExercises(),
        runDao.observeAllSessions(),
        thumbnailTrackPointsFlow,
        exerciseGoalRepository.goalSettings,
        selectedStatsRangeFlow
    ) { records, runSessions, thumbnailPoints, goalSettings, selectedRange ->
        val summary = buildExerciseSummary(
            records = records,
            weeklyGoal = goalSettings.weeklyDistanceGoalKm,
            monthlyGoal = goalSettings.monthlyDistanceGoalKm
        )
        ExerciseStatsState(
            summary = summary,
            detail = buildExerciseDetail(
                records = records,
                summary = summary,
                runSessions = runSessions,
                runTrackPoints = thumbnailPoints,
                selectedStatsRange = selectedRange
            ),
            goal = ExerciseGoalUiState(
                weeklyGoalKm = goalSettings.weeklyDistanceGoalKm,
                monthlyGoalKm = goalSettings.monthlyDistanceGoalKm
            )
        )
    }

    val uiState = combine(
        statsState,
        inputState
    ) { stats, input ->
        input.copy(
            summary = stats.summary,
            detail = stats.detail,
            goal = stats.goal
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
                editingOriginalStartTime = null,
                editingOriginalDurationSeconds = null,
                durationSelectionChanged = false,
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
        inputState.update {
            it.copy(
                isAddDialogVisible = false,
                editingExerciseRecordId = null,
                editingOriginalStartTime = null,
                editingOriginalDurationSeconds = null,
                durationSelectionChanged = false,
                inputError = null
            )
        }
    }

    fun showEditDialog(record: ExerciseRecordUiModel) {
        val durationMinutes = (record.durationSeconds / 60L).toInt().coerceIn(1, 300)
        inputState.update {
            it.copy(
                isAddDialogVisible = true,
                editingExerciseRecordId = record.id,
                editingOriginalStartTime = record.startTime,
                editingOriginalDurationSeconds = record.durationSeconds,
                durationSelectionChanged = false,
                inputType = record.type,
                inputDate = formatExerciseInputDate(record.startTime),
                inputDistanceKm = formatDistance(record.distanceKm),
                selectedDurationMinutes = durationMinutes,
                inputDurationHours = "0",
                inputDurationMinutes = durationMinutes.toString(),
                inputDurationSeconds = "0",
                inputInclinePercent = record.inclinePercent?.let(::formatInclineInput) ?: "",
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
                durationSelectionChanged = true,
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
        val key = statsDetailCacheKey(period, periodOffset)
        return exerciseDao.observeAllExercises().map { records ->
            buildExerciseStatsDetail(records, period, periodOffset).also { state ->
                statsDetailCache[key] = state
            }
        }
    }

    /** 翻回已看过的周期时先用缓存渲染，避免又闪一次 0.00 km 占位。 */
    fun cachedStatsDetail(
        period: ExerciseStatsPeriod,
        periodOffset: Int = 0
    ): ExerciseStatsDetailUiState? = statsDetailCache[statsDetailCacheKey(period, periodOffset)]

    private fun statsDetailCacheKey(period: ExerciseStatsPeriod, periodOffset: Int): String =
        "${period.name}:$periodOffset"

    fun saveExercise() {
        val current = uiState.value
        val originalDurationSeconds = current.editingOriginalDurationSeconds
        val keepOriginalDuration = current.editingExerciseRecordId != null &&
            !current.durationSelectionChanged &&
            originalDurationSeconds != null
        val selectedMinutes = current.selectedDurationMinutes
        if (!keepOriginalDuration) {
            if (selectedMinutes == null) {
                inputState.update { it.copy(inputError = "请选择运动时长") }
                return
            }
            if (selectedMinutes !in 1..300) {
                inputState.update { it.copy(inputError = "运动时长需在 1～300 分钟之间") }
                return
            }
        }

        val durationSeconds = if (keepOriginalDuration) {
            originalDurationSeconds
        } else {
            selectedMinutes!! * 60L
        }
        val validation = validateExerciseInput(
            date = current.inputDate,
            type = current.inputType,
            distanceKm = current.inputDistanceKm,
            hours = (durationSeconds / 3600).toString(),
            minutes = ((durationSeconds % 3600) / 60).toString(),
            seconds = (durationSeconds % 60).toString(),
            inclinePercent = current.inputInclinePercent
        )

        if (!validation.isValid) {
            inputState.update { it.copy(inputError = validation.error) }
            return
        }

        viewModelScope.launch {
            val recordDate = current.inputDate.trim().ifEmpty { todayExerciseDate() }
            val weightKg = weightDao.getLatestWeightOnOrBefore(recordDate)?.weightKg ?: 70.0
            val validDurationSeconds = validation.durationSeconds ?: return@launch
            val validDistanceKm = validation.distanceKm ?: return@launch
            val validInclinePercent = validation.inclinePercent ?: 0.0
            val caloriesKcal = estimateExerciseCalories(
                weightKg = weightKg,
                type = current.inputType,
                durationSeconds = validDurationSeconds,
                distanceKm = validDistanceKm,
                inclinePercent = validInclinePercent
            )
            val editingRecordId = current.editingExerciseRecordId
            val originalStartTime = current.editingOriginalStartTime
            val startTime = when {
                editingRecordId == null ->
                    exerciseDateWithCurrentClockTime(recordDate) ?: validation.startTime ?: return@launch
                originalStartTime != null ->
                    exerciseDateTimeWithOriginalClockTime(recordDate, originalStartTime)
                        ?: validation.startTime ?: return@launch
                else -> validation.startTime ?: return@launch
            }
            val entity = ExerciseRecordEntity(
                id = editingRecordId ?: 0L,
                type = current.inputType.name,
                startTime = startTime,
                durationSeconds = validDurationSeconds,
                distanceKm = validDistanceKm,
                caloriesKcal = caloriesKcal,
                inclinePercent = if (current.inputType == ExerciseType.TREADMILL) validInclinePercent else null
            )
            // 运动记录和跑步会话必须一起更新，避免只改到一半。
            database.withTransaction {
                if (editingRecordId == null) {
                    exerciseDao.insertExercise(
                        entity.copy(id = 0L)
                    )
                } else {
                    exerciseDao.updateExercise(entity)
                    runDao.getSessionByExerciseRecordId(editingRecordId)?.let { session ->
                        runDao.updateSession(
                            session.copy(
                                startTime = startTime,
                                endTime = startTime + validDurationSeconds * 1000L,
                                durationSeconds = validDurationSeconds,
                                distanceKm = validDistanceKm,
                                caloriesKcal = caloriesKcal
                            )
                        )
                    }
                }
            }
            inputState.update {
                it.copy(
                    isAddDialogVisible = false,
                    editingExerciseRecordId = null,
                    editingOriginalStartTime = null,
                    editingOriginalDurationSeconds = null,
                    durationSelectionChanged = false,
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
            database.withTransaction {
                runDao.deleteTrackPointsByExerciseRecordId(recordId)
                runDao.deleteSessionsByExerciseRecordId(recordId)
                exerciseDao.deleteExerciseById(recordId)
            }
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
    private val database: AppDatabase,
    private val exerciseDao: ExerciseDao,
    private val runDao: RunDao,
    private val weightDao: WeightDao,
    private val exerciseGoalRepository: ExerciseGoalRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExerciseViewModel::class.java)) {
            return ExerciseViewModel(database, exerciseDao, runDao, weightDao, exerciseGoalRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

private fun formatGoalInput(value: Double): String {
    return String.format(Locale.US, "%.2f", value)
}

private fun formatInclineInput(value: Double): String {
    return if (value == value.toLong().toDouble()) {
        value.toLong().toString()
    } else {
        String.format(Locale.US, "%.1f", value)
    }
}
