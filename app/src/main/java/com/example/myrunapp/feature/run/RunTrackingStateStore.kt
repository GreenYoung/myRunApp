package com.example.myrunapp.feature.run

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface RunSaveResult {
    data class Success(val sessionId: Long) : RunSaveResult
    data class Failure(val message: String) : RunSaveResult
}

object RunTrackingStateStore {
    private val _state = MutableStateFlow(RunTrackingUiState())
    val state: StateFlow<RunTrackingUiState> = _state.asStateFlow()

    /**
     * 保存结果使用 StateFlow 而不是 SharedFlow：StateFlow 会保留最新值，
     * 即使 Service 在 ViewModel 开始等待之前就完成保存，结果也不会丢失。
     */
    private val _saveResult = MutableStateFlow<RunSaveResult?>(null)
    val saveResult: StateFlow<RunSaveResult?> = _saveResult.asStateFlow()

    fun update(transform: (RunTrackingUiState) -> RunTrackingUiState) {
        _state.value = transform(_state.value)
    }

    fun reset() {
        _state.value = RunTrackingUiState()
    }

    fun publishSaveResult(result: RunSaveResult) {
        _saveResult.value = result
    }

    fun clearSaveResult() {
        _saveResult.value = null
    }
}
