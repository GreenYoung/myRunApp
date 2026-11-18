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
import com.example.myrunapp.feature.run.data.RunDraftDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

private const val SaveResultTimeoutMs = 30_000L

internal data class RunPermissionState(
    val hasLocationPermission: Boolean = false,
    val hasNotificationPermission: Boolean = true
)

class RunTrackingViewModel(
    private val appContext: Context,
    private val runDao: RunDao,
    private val runDraftDao: RunDraftDao
) : ViewModel() {
    private val permissionState = MutableStateFlow(currentPermissionState())
    private var finishRequestPending = false

    init {
        restoreDraftIfNeeded()
    }

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

    /**
     * 刷新并**返回**最新权限状态。
     *
     * Screen 里不能在 `refreshPermission()` 之后再去读 `uiState` 快照——那是进入组合时的旧值，
     * 会导致"权限已授予却再次弹窗"。需要立即判断时用这个函数。
     */
    internal fun refreshPermissionNow(): RunPermissionState {
        refreshPermission()
        return permissionState.value
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
                // 通知权限被拒不阻断：Android 13+ 前台服务本身不依赖它，只是通知栏看不到进度。
                // 页面顶部的 GPS 状态条会提示"需要通知权限才能在息屏时保持跑步记录"。
                if (!permissions.hasNotificationPermission) {
                    AppLogger.w(LogTags.RUN, "notification permission missing, start anyway")
                }
                AppLogger.i(LogTags.RUN, "starting RunTrackingService")
                ContextCompat.startForegroundService(appContext, RunTrackingService.startIntent(appContext))
            }
        }
    }

    fun stopWithoutSaving() {
        appContext.startService(RunTrackingService.discardIntent(appContext))
    }

    fun continueRecoveredDraft() {
        if (uiState.value.isTracking) return
        AppLogger.i(LogTags.RUN, "continue recovered draft requested")
        RunTrackingStateStore.update { it.copy(errorMessage = null) }
        val started = runCatching {
            ContextCompat.startForegroundService(appContext, RunTrackingService.continueDraftIntent(appContext))
        }.isSuccess
        if (!started) {
            RunTrackingStateStore.update {
                it.copy(errorMessage = "无法恢复跑步记录，请重试")
            }
        }
    }

    fun saveRecoveredDraft(onSaved: (Long) -> Unit) {
        if (finishRequestPending || uiState.value.isSaving) return
        if (!uiState.value.hasRecoveredDraft) return
        finishRequestPending = true
        viewModelScope.launch {
            try {
                AppLogger.i(LogTags.RUN, "save recovered draft requested")
                RunTrackingStateStore.clearSaveResult()
                val started = runCatching {
                    appContext.startService(RunTrackingService.saveDraftIntent(appContext))
                }.isSuccess
                if (!started) {
                    RunTrackingStateStore.update {
                        it.copy(isSaving = false, errorMessage = "无法启动保存服务，请重试")
                    }
                    return@launch
                }
                val result = withTimeoutOrNull(SaveResultTimeoutMs) {
                    RunTrackingStateStore.saveResult.filterNotNull().first()
                }
                RunTrackingStateStore.clearSaveResult()
                when (result) {
                    is RunSaveResult.Success -> {
                        AppLogger.i(LogTags.RUN, "recovered draft saved sessionId=${result.sessionId}")
                        RunTrackingStateStore.reset()
                        onSaved(result.sessionId)
                    }
                    is RunSaveResult.Failure -> {
                        RunTrackingStateStore.update {
                            it.copy(isSaving = false, errorMessage = result.message)
                        }
                    }
                    null -> {
                        RunTrackingStateStore.update {
                            it.copy(isSaving = false, errorMessage = "保存超时，请重试")
                        }
                    }
                }
            } finally {
                finishRequestPending = false
            }
        }
    }

    fun discardRecoveredDraft() {
        if (uiState.value.isTracking) return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                runCatching {
                    runDraftDao.clearPoints()
                    runDraftDao.clearDraft()
                }.onFailure {
                    AppLogger.e(LogTags.DATABASE, "clear recovered run draft failed", it)
                }
            }
            AppLogger.i(LogTags.RUN, "recovered draft discarded")
            RunTrackingStateStore.reset()
        }
    }

    private fun restoreDraftIfNeeded() {
        viewModelScope.launch {
            val current = RunTrackingStateStore.state.value
            if (current.isTracking || current.isServiceRunning || current.hasRecoveredDraft) return@launch
            val draft = withContext(Dispatchers.IO) { runDraftDao.getDraft() } ?: return@launch
            if (draft.durationSeconds <= 0L && draft.totalDistanceMeters <= 0.0) {
                withContext(Dispatchers.IO) {
                    runCatching {
                        runDraftDao.clearPoints()
                        runDraftDao.clearDraft()
                    }
                }
                return@launch
            }
            val points = withContext(Dispatchers.IO) { runDraftDao.getPoints() }
            val distanceKm = draft.totalDistanceMeters / 1000.0
            AppLogger.i(
                LogTags.RUN,
                "recovered unfinished run draft startTime=${draft.startTime} duration=${draft.durationSeconds}s " +
                    "distance=${distanceKm}km points=${points.size}"
            )
            RunTrackingStateStore.update {
                if (it.isTracking || it.isServiceRunning) return@update it
                it.copy(
                    isTracking = false,
                    isPaused = false,
                    isServiceRunning = false,
                    isSaving = false,
                    hasRecoveredDraft = true,
                    gpsStatusText = "发现未完成的跑步记录",
                    distanceKm = distanceKm,
                    durationSeconds = draft.durationSeconds,
                    averagePaceText = formatPace(draft.durationSeconds, distanceKm),
                    caloriesKcal = draft.caloriesKcal,
                    canSave = points.size >= 2 && draft.durationSeconds > 0L && distanceKm > 0.0,
                    trackPoints = points.map { it.toRunTrackPointUiModel() },
                    errorMessage = null
                )
            }
        }
    }

    fun pauseTracking() {
        AppLogger.i(LogTags.RUN, "pause requested from ViewModel")
        appContext.startService(RunTrackingService.pauseIntent(appContext))
    }

    fun resumeTracking() {
        AppLogger.i(LogTags.RUN, "resume requested from ViewModel")
        appContext.startService(RunTrackingService.resumeIntent(appContext))
    }

    fun finishTracking(onSaved: (Long) -> Unit, onDiscarded: () -> Unit) {
        if (finishRequestPending || uiState.value.isSaving) return
        if (!uiState.value.canSave) {
            AppLogger.i(
                LogTags.RUN,
                "finish with too little data, discarding run duration=${uiState.value.durationSeconds}s " +
                    "distance=${uiState.value.distanceKm} points=${uiState.value.trackPoints.size}"
            )
            stopWithoutSaving()
            onDiscarded()
            return
        }
        finishRequestPending = true
        viewModelScope.launch {
            try {
                AppLogger.i(LogTags.RUN, "finish requested from ViewModel")
                RunTrackingStateStore.clearSaveResult()
                val started = runCatching {
                    appContext.startService(RunTrackingService.finishIntent(appContext))
                }.isSuccess
                if (!started) {
                    RunTrackingStateStore.update {
                        it.copy(isSaving = false, errorMessage = "无法启动保存服务，请重试")
                    }
                    return@launch
                }
                val result = withTimeoutOrNull(SaveResultTimeoutMs) {
                    RunTrackingStateStore.saveResult.filterNotNull().first()
                }
                RunTrackingStateStore.clearSaveResult()
                when (result) {
                    is RunSaveResult.Success -> {
                        AppLogger.i(LogTags.RUN, "saved session received sessionId=${result.sessionId}")
                        RunTrackingStateStore.reset()
                        onSaved(result.sessionId)
                    }
                    is RunSaveResult.Failure -> {
                        AppLogger.w(LogTags.RUN, "save failed: ${result.message}")
                        RunTrackingStateStore.update {
                            it.copy(isSaving = false, errorMessage = result.message)
                        }
                    }
                    null -> {
                        AppLogger.w(LogTags.RUN, "save result timeout")
                        RunTrackingStateStore.update {
                            it.copy(isSaving = false, errorMessage = "保存超时，请重试")
                        }
                    }
                }
            } finally {
                finishRequestPending = false
            }
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
    private val runDao: RunDao,
    private val runDraftDao: RunDraftDao
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RunTrackingViewModel::class.java)) {
            return RunTrackingViewModel(appContext, runDao, runDraftDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
