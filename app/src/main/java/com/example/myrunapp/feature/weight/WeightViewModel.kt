package com.example.myrunapp.feature.weight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.example.myrunapp.core.data.AppDatabase
import com.example.myrunapp.feature.exercise.data.ExerciseDao
import com.example.myrunapp.feature.weight.data.WeightDao
import com.example.myrunapp.feature.weight.data.WeightRecordEntity
import com.example.myrunapp.feature.weight.data.TargetWeightRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private data class WeightDataState(
    val points: List<WeightPoint>,
    val latest: WeightRecordEntity?,
    val first: WeightRecordEntity?,
    val previousChange: Double?,
    val totalChange: Double?,
    val weeklyChange: Double?,
    val recentRecords: List<WeightRecordItem>,
    val trendRecords: List<WeightTrendPointUiState>,
    val exerciseCorrelation: WeightExerciseCorrelationUiState,
    val todayRecord: WeightRecordEntity?,
    val targetWeight: Double?,
    val heightCm: Double?
)

class WeightViewModel(
    private val database: AppDatabase,
    private val weightDao: WeightDao,
    private val exerciseDao: ExerciseDao,
    private val targetWeightRepository: TargetWeightRepository
) : ViewModel() {
    private val inputState = MutableStateFlow(
        WeightUiState(inputDate = todayIsoDate())
    )

    private val weightDataState = combine(
        weightDao.observeAllWeights(),
        exerciseDao.observeAllExercises(),
        targetWeightRepository.targetWeightKg,
        targetWeightRepository.heightCm
    ) { records, exercises, targetWeight, heightCm ->
        val today = todayIsoDate()
        val statsRecords = records.filter { it.date <= today }.sortedBy { it.date }
        val points = statsRecords.map { WeightPoint(it.date, it.weightKg) }
        val latest = statsRecords.lastOrNull()
        val first = statsRecords.firstOrNull()
        val previous = statsRecords.dropLast(1).lastOrNull()
        val previousChange = if (latest != null && previous != null) {
            latest.weightKg - previous.weightKg
        } else {
            null
        }
        val totalChange = if (latest != null && first != null && first.date != latest.date) {
            latest.weightKg - first.weightKg
        } else {
            null
        }
        val weeklyRecords = filterWeightPointsByRange(points, WeightRange.DAYS_7)
        val weeklyChange = if (weeklyRecords.size >= 2) {
            weeklyRecords.last().weightKg - weeklyRecords.first().weightKg
        } else {
            null
        }
        val changeByDate = statsRecords
            .sortedBy { it.date }
            .zipWithNext { previousRecord, currentRecord ->
                currentRecord.date to (currentRecord.weightKg - previousRecord.weightKg)
            }
            .toMap()
        val trendRecords = statsRecords.map { record ->
            WeightTrendPointUiState(
                date = record.date,
                weightKg = record.weightKg,
                previousChange = changeByDate[record.date],
                note = record.note
            )
        }
        val recentRecords = statsRecords.asReversed().map { record ->
            WeightRecordItem(
                date = record.date,
                weightKg = record.weightKg,
                previousChange = changeByDate[record.date],
                note = record.note
            )
        }
        WeightDataState(
            points = points,
            latest = latest,
            first = first,
            previousChange = previousChange,
            totalChange = totalChange,
            weeklyChange = weeklyChange,
            recentRecords = recentRecords,
            trendRecords = trendRecords,
            exerciseCorrelation = buildWeightExerciseCorrelation(points, exercises),
            todayRecord = statsRecords.firstOrNull { it.date == today },
            targetWeight = targetWeight,
            heightCm = heightCm
        )
    }

    val uiState = combine(
        weightDataState,
        inputState
    ) { data, input ->
        val latest = data.latest
        input.copy(
            latestWeightKg = latest?.weightKg,
            latestDate = latest?.date,
            todayWeightKg = data.todayRecord?.weightKg,
            weightCard = WeightCardUiState(
                todayWeight = data.todayRecord?.weightKg,
                hasTodayWeight = data.todayRecord != null,
                latestWeight = latest?.weightKg,
                latestDate = latest?.date,
                previousChange = data.previousChange,
                totalChange = data.totalChange
            ),
            weightDetail = WeightDetailUiState(
                latestWeight = latest?.weightKg,
                previousChange = data.previousChange,
                totalChange = data.totalChange,
                weeklyChange = data.weeklyChange,
                firstWeight = data.first?.weightKg,
                targetWeight = data.targetWeight,
                remainingWeight = calculateRemainingWeight(latest?.weightKg, data.targetWeight),
                targetProgress = calculateTargetProgress(data.first?.weightKg, latest?.weightKg, data.targetWeight),
                latestDate = latest?.date,
                bmi = calculateBmiUiState(latest?.weightKg, data.heightCm),
                changeSpeed = buildWeightChangeSpeed(data.points, input.weightDetail.selectedRange),
                exerciseCorrelation = data.exerciseCorrelation,
                selectedRange = input.weightDetail.selectedRange,
                chartRecords = filterWeightPointsByRange(data.points, input.weightDetail.selectedRange),
                recentRecords = data.recentRecords
            ),
            weightTrend = WeightTrendUiState(
                selectedRange = input.weightTrend.selectedRange,
                records = filterWeightTrendPointsByRange(
                    records = data.trendRecords,
                    range = input.weightTrend.selectedRange
                ),
                targetWeight = data.targetWeight,
                latestWeight = latest?.weightKg,
                remainingWeight = calculateRemainingWeight(latest?.weightKg, data.targetWeight),
                changeSpeed = buildWeightChangeSpeed(data.points, input.weightTrend.selectedRange)
            ),
            records = data.points
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = WeightUiState(inputDate = todayIsoDate())
    )

    fun showRecordDialog() {
        inputState.update {
            it.copy(
                isRecordDialogVisible = true,
                editingWeightDate = null,
                inputDate = todayIsoDate(),
                inputWeight = "",
                inputNote = "",
                inputError = null
            )
        }
    }

    fun showEditRecordDialog(record: WeightRecordItem) {
        inputState.update {
            it.copy(
                isRecordDialogVisible = true,
                editingWeightDate = record.date,
                inputDate = record.date,
                inputWeight = formatWeight(record.weightKg),
                inputNote = record.note.orEmpty(),
                inputError = null
            )
        }
    }

    fun hideRecordDialog() {
        inputState.update {
            it.copy(
                isRecordDialogVisible = false,
                editingWeightDate = null,
                inputError = null
            )
        }
    }

    fun onInputDateChange(value: String) {
        inputState.update { it.copy(inputDate = value, inputError = null) }
    }

    fun onInputWeightChange(value: String) {
        inputState.update { it.copy(inputWeight = value, inputError = null) }
    }

    fun onInputNoteChange(value: String) {
        inputState.update { it.copy(inputNote = value.take(100), inputError = null) }
    }

    fun onRangeChange(range: WeightRange) {
        inputState.update {
            it.copy(
                weightDetail = it.weightDetail.copy(selectedRange = range),
                weightTrend = it.weightTrend.copy(selectedRange = range)
            )
        }
    }

    fun showTargetDialog() {
        val targetWeight = uiState.value.weightDetail.targetWeight
        val heightCm = uiState.value.weightDetail.bmi.heightCm
        inputState.update {
            it.copy(
                isTargetDialogVisible = true,
                inputTargetWeight = targetWeight?.let(::formatWeight) ?: "",
                inputHeightCm = heightCm?.let(::formatHeightCm) ?: "",
                targetInputError = null
            )
        }
    }

    fun hideTargetDialog() {
        inputState.update {
            it.copy(
                isTargetDialogVisible = false,
                targetInputError = null
            )
        }
    }

    fun onTargetWeightChange(value: String) {
        inputState.update { it.copy(inputTargetWeight = value, targetInputError = null) }
    }

    fun onHeightCmChange(value: String) {
        inputState.update { it.copy(inputHeightCm = value, targetInputError = null) }
    }

    fun saveTargetWeight() {
        val current = uiState.value
        val validation = validateWeightSettingsInput(current.inputTargetWeight, current.inputHeightCm)

        if (!validation.isValid) {
            inputState.update { it.copy(targetInputError = validation.error) }
            return
        }

        viewModelScope.launch {
            targetWeightRepository.saveWeightSettings(
                targetWeightKg = validation.targetWeightKg ?: return@launch,
                heightCm = validation.heightCm
            )
            inputState.update {
                it.copy(
                    isTargetDialogVisible = false,
                    inputTargetWeight = "",
                    inputHeightCm = "",
                    targetInputError = null
                )
            }
        }
    }

    fun saveWeight() {
        val current = uiState.value
        val validation = validateWeightInput(current.inputDate, current.inputWeight)

        if (!validation.isValid) {
            inputState.update { it.copy(inputError = validation.error) }
            return
        }

        val weightKg = validation.weightKg ?: return
        viewModelScope.launch {
            // 目标日期已有一条不同日期的记录时，先确认再覆盖，别静默丢数据。
            val existing = weightDao.getWeightByDate(current.inputDate)
            val editingSameDate = current.editingWeightDate == current.inputDate
            if (existing != null && !editingSameDate) {
                inputState.update { it.copy(pendingOverwriteDate = current.inputDate) }
                return@launch
            }
            persistWeight(weightKg)
        }
    }

    /** 用户确认覆盖同日记录。 */
    fun confirmOverwriteWeight() {
        val current = uiState.value
        val weightKg = validateWeightInput(current.inputDate, current.inputWeight).weightKg
        inputState.update { it.copy(pendingOverwriteDate = null) }
        if (weightKg == null) return
        viewModelScope.launch { persistWeight(weightKg) }
    }

    fun cancelOverwriteWeight() {
        inputState.update { it.copy(pendingOverwriteDate = null) }
    }

    private suspend fun persistWeight(weightKg: Double) {
        val now = System.currentTimeMillis()
        val current = uiState.value
        val editingDate = current.editingWeightDate
        // 换日期时"删除旧日期 + 写入新日期"必须作为整体成功或失败。
        database.withTransaction {
            if (editingDate != null && editingDate != current.inputDate) {
                weightDao.deleteWeightByDate(editingDate)
            }
            val existing = weightDao.getWeightByDate(current.inputDate)
            weightDao.upsertWeight(
                WeightRecordEntity(
                    date = current.inputDate,
                    weightKg = weightKg,
                    note = current.inputNote.trim().takeIf { it.isNotEmpty() },
                    createdAt = existing?.createdAt ?: now,
                    updatedAt = now
                )
            )
        }
        inputState.update {
            it.copy(
                isRecordDialogVisible = false,
                editingWeightDate = null,
                inputWeight = "",
                inputDate = todayIsoDate(),
                inputNote = "",
                inputError = null,
                pendingOverwriteDate = null
            )
        }
    }

    fun deleteWeight(record: WeightRecordItem) {
        viewModelScope.launch {
            weightDao.deleteWeightByDate(record.date)
        }
    }
}

class WeightViewModelFactory(
    private val database: AppDatabase,
    private val weightDao: WeightDao,
    private val exerciseDao: ExerciseDao,
    private val targetWeightRepository: TargetWeightRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WeightViewModel::class.java)) {
            return WeightViewModel(database, weightDao, exerciseDao, targetWeightRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
