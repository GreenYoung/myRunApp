package com.example.myrunapp.feature.weight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
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

class WeightViewModel(
    private val weightDao: WeightDao,
    private val exerciseDao: ExerciseDao,
    private val targetWeightRepository: TargetWeightRepository
) : ViewModel() {
    private val inputState = MutableStateFlow(
        WeightUiState(inputDate = todayIsoDate())
    )

    val uiState = combine(
        weightDao.observeAllWeights(),
        exerciseDao.observeAllExercises(),
        targetWeightRepository.targetWeightKg,
        targetWeightRepository.heightCm,
        inputState
    ) { records, exercises, targetWeight, heightCm, input ->
        val points = records.map { WeightPoint(it.date, it.weightKg) }
        val latest = records.maxByOrNull { it.date }
        val first = records.minByOrNull { it.date }
        val previous = records
            .filter { latest != null && it.date < latest.date }
            .maxByOrNull { it.date }
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
        val weeklyRecords = latest?.date?.let { latestDate ->
            filterWeightPointsByRange(points, latestDate, WeightRange.DAYS_7)
        }.orEmpty()
        val weeklyChange = if (weeklyRecords.size >= 2) {
            weeklyRecords.last().weightKg - weeklyRecords.first().weightKg
        } else {
            null
        }
        val recentRecords = records
            .sortedByDescending { it.date }
            .take(5)
            .map { record ->
                val previousRecord = records
                    .filter { it.date < record.date }
                    .maxByOrNull { it.date }
                WeightRecordItem(
                    date = record.date,
                    weightKg = record.weightKg,
                    previousChange = previousRecord?.let { record.weightKg - it.weightKg },
                    note = record.note
                )
            }
        val trendRecords = records
            .sortedBy { it.date }
            .map { record ->
                val previousRecord = records
                    .filter { it.date < record.date }
                    .maxByOrNull { it.date }
                WeightTrendPointUiState(
                    date = record.date,
                    weightKg = record.weightKg,
                    previousChange = previousRecord?.let { record.weightKg - it.weightKg },
                    note = record.note
                )
            }
        val filteredTrendRecords = filterWeightTrendPointsByRange(
            records = trendRecords,
            latestDate = latest?.date,
            range = input.weightTrend.selectedRange
        )
        val today = todayIsoDate()
        val todayRecord = records.firstOrNull { it.date == today }
        input.copy(
            latestWeightKg = latest?.weightKg,
            latestDate = latest?.date,
            todayWeightKg = todayRecord?.weightKg,
            weightCard = WeightCardUiState(
                todayWeight = todayRecord?.weightKg,
                hasTodayWeight = todayRecord != null,
                latestWeight = latest?.weightKg,
                latestDate = latest?.date,
                previousChange = previousChange,
                totalChange = totalChange
            ),
            weightDetail = WeightDetailUiState(
                latestWeight = latest?.weightKg,
                previousChange = previousChange,
                totalChange = totalChange,
                weeklyChange = weeklyChange,
                firstWeight = first?.weightKg,
                targetWeight = targetWeight,
                remainingWeight = calculateRemainingWeight(latest?.weightKg, targetWeight),
                targetProgress = calculateTargetProgress(first?.weightKg, latest?.weightKg, targetWeight),
                latestDate = latest?.date,
                bmi = calculateBmiUiState(latest?.weightKg, heightCm),
                changeSpeed = buildWeightChangeSpeed(points, latest?.date, input.weightDetail.selectedRange),
                exerciseCorrelation = buildWeightExerciseCorrelation(points, exercises, latest?.date),
                selectedRange = input.weightDetail.selectedRange,
                chartRecords = filterWeightPointsByRange(points, latest?.date, input.weightDetail.selectedRange),
                recentRecords = recentRecords
            ),
            weightTrend = WeightTrendUiState(
                selectedRange = input.weightTrend.selectedRange,
                records = filteredTrendRecords,
                targetWeight = targetWeight,
                latestWeight = latest?.weightKg,
                remainingWeight = calculateRemainingWeight(latest?.weightKg, targetWeight),
                changeSpeed = buildWeightChangeSpeed(points, latest?.date, input.weightTrend.selectedRange)
            ),
            records = points
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

        val now = System.currentTimeMillis()
        viewModelScope.launch {
            val editingDate = current.editingWeightDate
            if (editingDate != null && editingDate != current.inputDate) {
                weightDao.deleteWeightByDate(editingDate)
            }
            val existing = weightDao.getWeightByDate(current.inputDate)
            weightDao.upsertWeight(
                WeightRecordEntity(
                    date = current.inputDate,
                    weightKg = validation.weightKg ?: return@launch,
                    note = current.inputNote.trim().takeIf { it.isNotEmpty() },
                    createdAt = existing?.createdAt ?: now,
                    updatedAt = now
                )
            )
            inputState.update {
                it.copy(
                    isRecordDialogVisible = false,
                    editingWeightDate = null,
                    inputWeight = "",
                    inputDate = todayIsoDate(),
                    inputNote = "",
                    inputError = null
                )
            }
        }
    }

    fun deleteWeight(record: WeightRecordItem) {
        viewModelScope.launch {
            weightDao.deleteWeightByDate(record.date)
        }
    }
}

class WeightViewModelFactory(
    private val weightDao: WeightDao,
    private val exerciseDao: ExerciseDao,
    private val targetWeightRepository: TargetWeightRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WeightViewModel::class.java)) {
            return WeightViewModel(weightDao, exerciseDao, targetWeightRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
