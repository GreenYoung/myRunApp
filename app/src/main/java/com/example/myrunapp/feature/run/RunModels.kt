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

data class RunTrackingUiState(
    val isTracking: Boolean = false,
    val hasLocationPermission: Boolean = false,
    val hasNotificationPermission: Boolean = true,
    val isServiceRunning: Boolean = false,
    val isSaving: Boolean = false,
    val gpsStatusText: String = "等待定位",
    val distanceKm: Double = 0.0,
    val durationSeconds: Long = 0L,
    val averagePaceText: String = "--'--\"/km",
    val caloriesKcal: Int = 0,
    val canSave: Boolean = false,
    val trackPoints: List<RunTrackPointUiModel> = emptyList(),
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
