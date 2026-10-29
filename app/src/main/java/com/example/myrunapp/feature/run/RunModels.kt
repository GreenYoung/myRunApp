package com.example.myrunapp.feature.run

data class RunTrackPointUiModel(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float?,
    val altitudeMeters: Double?,
    val speedMetersPerSecond: Float?,
    val recordedAt: Long,
    val elapsedRealtimeNanos: Long = 0L,
    val coordinateSystem: String = CoordinateSystem.WGS84
)

object CoordinateSystem {
    const val WGS84 = "WGS84"
    const val GCJ02 = "GCJ02"
}

internal const val RunMapRoadLevelAccuracyMeters = 30f
internal const val RunMapMaxDisplayAccuracyMeters = 80f

/**
 * 服务定位结果的轻量快照（GCJ-02）。地图页直接消费这一份数据来更新原生定位蓝点，
 * 避免追踪时再启动第二个 AMapLocationClient。
 */
data class RunMapLocationSnapshot(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val isRoadLevelReliable: Boolean,
    val isGpsLocation: Boolean,
    val recordedAt: Long
)

data class RunTrackingUiState(
    val isTracking: Boolean = false,
    val isPaused: Boolean = false,
    val hasLocationPermission: Boolean = false,
    val hasNotificationPermission: Boolean = true,
    val isServiceRunning: Boolean = false,
    val isSaving: Boolean = false,
    val hasRecoveredDraft: Boolean = false,
    val gpsStatusText: String = "等待定位",
    val distanceKm: Double = 0.0,
    val durationSeconds: Long = 0L,
    val averagePaceText: String = "--'--\"/km",
    val caloriesKcal: Int = 0,
    val canSave: Boolean = false,
    val trackPoints: List<RunTrackPointUiModel> = emptyList(),
    val mapLocation: RunMapLocationSnapshot? = null,
    val errorMessage: String? = null
)

data class RunTrackDetailUiState(
    val sessionId: Long = 0,
    val startTime: Long? = null,
    val distanceKm: Double = 0.0,
    val durationSeconds: Long = 0L,
    val paceText: String = "--'--\"/km",
    val caloriesKcal: Int = 0,
    val points: List<RunTrackPointUiModel> = emptyList(),
    val isMissing: Boolean = false
)
