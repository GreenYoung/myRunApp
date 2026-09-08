package com.example.myrunapp.feature.run

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myrunapp.core.log.AppLogger
import com.example.myrunapp.core.log.LogTags
import com.example.myrunapp.feature.exercise.formatPace
import com.example.myrunapp.feature.run.data.RunDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private data class RunPermissionState(
    val hasLocationPermission: Boolean = false,
    val hasNotificationPermission: Boolean = true
)

class RunTrackingViewModel(
    private val appContext: Context,
    private val runDao: RunDao
) : ViewModel() {
    private val permissionState = MutableStateFlow(currentPermissionState())

    val uiState = combine(
        RunTrackingStateStore.state,
        permissionState
    ) { serviceState, permissions ->
        serviceState.copy(
            hasLocationPermission = permissions.hasLocationPermission,
            hasNotificationPermission = permissions.hasNotificationPermission
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RunTrackingStateStore.state.value.copy(
            hasLocationPermission = hasLocationPermission(),
            hasNotificationPermission = hasNotificationPermission()
        )
    )

    fun refreshPermission() {
        permissionState.value = currentPermissionState()
        RunTrackingStateStore.update {
            it.copy(
                hasLocationPermission = permissionState.value.hasLocationPermission,
                hasNotificationPermission = permissionState.value.hasNotificationPermission,
                errorMessage = null
            )
        }
    }

    fun startTracking() {
        refreshPermission()
        val permissions = permissionState.value
        val locationEnabled = isLocationEnabled()
        AppLogger.i(
            LogTags.RUN,
            "start button clicked hasLocation=${permissions.hasLocationPermission} hasNotification=${permissions.hasNotificationPermission} locationEnabled=$locationEnabled"
        )
        when {
            !permissions.hasLocationPermission -> {
                AppLogger.w(LogTags.RUN, "start blocked in ViewModel: missing location permission")
                RunTrackingStateStore.update {
                    it.copy(errorMessage = "需要精确位置权限才能准确记录运动轨迹")
                }
            }
            !permissions.hasNotificationPermission -> {
                AppLogger.w(LogTags.RUN, "start blocked in ViewModel: missing notification permission")
                RunTrackingStateStore.update {
                    it.copy(errorMessage = "需要通知权限才能在息屏时保持跑步记录")
                }
            }
            !locationEnabled -> {
                AppLogger.w(LogTags.RUN, "start blocked in ViewModel: location disabled")
                RunTrackingStateStore.update {
                    it.copy(
                        gpsStatusText = "请开启系统定位",
                        errorMessage = "请开启系统定位后再开始跑步"
                    )
                }
            }
            else -> {
                AppLogger.i(LogTags.RUN, "starting RunTrackingService")
                ContextCompat.startForegroundService(appContext, RunTrackingService.startIntent(appContext))
            }
        }
    }

    fun stopWithoutSaving() {
        appContext.startService(RunTrackingService.discardIntent(appContext))
    }

    fun pauseTracking() {
        AppLogger.i(LogTags.RUN, "pause requested from ViewModel")
        appContext.startService(RunTrackingService.pauseIntent(appContext))
    }

    fun resumeTracking() {
        AppLogger.i(LogTags.RUN, "resume requested from ViewModel")
        appContext.startService(RunTrackingService.resumeIntent(appContext))
    }

    fun finishTracking(onSaved: (Long) -> Unit) {
        if (uiState.value.isSaving) return
        if (!uiState.value.canSave) {
            AppLogger.w(
                LogTags.RUN,
                "finish blocked: canSave=false duration=${uiState.value.durationSeconds}s distance=${uiState.value.distanceKm} points=${uiState.value.trackPoints.size}"
            )
            RunTrackingStateStore.update {
                it.copy(errorMessage = "本次跑步数据太少，无法保存")
            }
            return
        }
        viewModelScope.launch {
            AppLogger.i(LogTags.RUN, "finish requested from ViewModel")
            appContext.startService(RunTrackingService.finishIntent(appContext))
            val sessionId = RunTrackingStateStore.savedSessionIds.first()
            AppLogger.i(LogTags.RUN, "saved session received sessionId=$sessionId")
            onSaved(sessionId)
        }
    }

    fun observeTrackDetail(sessionId: Long): Flow<RunTrackDetailUiState> {
        return combine(
            runDao.observeSession(sessionId),
            runDao.observeTrackPoints(sessionId)
        ) { session, points ->
            if (session == null) {
                RunTrackDetailUiState(sessionId = sessionId, isMissing = true)
            } else {
                RunTrackDetailUiState(
                    sessionId = session.id,
                    startTime = session.startTime,
                    distanceKm = session.distanceKm,
                    durationSeconds = session.durationSeconds,
                    paceText = formatPace(session.durationSeconds, session.distanceKm),
                    caloriesKcal = session.caloriesKcal,
                    points = points.map {
                        RunTrackPointUiModel(
                            latitude = it.latitude,
                            longitude = it.longitude,
                            coordinateSystem = it.coordinateSystem,
                            accuracyMeters = it.accuracyMeters,
                            altitudeMeters = it.altitudeMeters,
                            speedMetersPerSecond = it.speedMetersPerSecond,
                            recordedAt = it.recordedAt
                        )
                    }
                )
            }
        }
    }

    private fun currentPermissionState(): RunPermissionState {
        return RunPermissionState(
            hasLocationPermission = hasLocationPermission(),
            hasNotificationPermission = hasNotificationPermission()
        )
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }
}

class RunTrackingViewModelFactory(
    private val appContext: Context,
    private val runDao: RunDao
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RunTrackingViewModel::class.java)) {
            return RunTrackingViewModel(appContext, runDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
