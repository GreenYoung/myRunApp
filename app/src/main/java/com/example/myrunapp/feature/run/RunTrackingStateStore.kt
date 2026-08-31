package com.example.myrunapp.feature.run

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

object RunTrackingStateStore {
    private val _state = MutableStateFlow(RunTrackingUiState())
    val state: StateFlow<RunTrackingUiState> = _state.asStateFlow()

    private val _savedSessionIds = MutableSharedFlow<Long>(extraBufferCapacity = 1)
    val savedSessionIds: SharedFlow<Long> = _savedSessionIds.asSharedFlow()

    fun update(transform: (RunTrackingUiState) -> RunTrackingUiState) {
        _state.value = transform(_state.value)
    }

    fun reset() {
        _state.value = RunTrackingUiState()
    }

    fun emitSavedSessionId(sessionId: Long) {
        _savedSessionIds.tryEmit(sessionId)
    }
}
