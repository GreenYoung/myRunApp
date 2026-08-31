package com.example.myrunapp.feature.run

import android.location.Location
import android.os.Build
import android.os.SystemClock
import com.amap.api.location.AMapLocation
import com.example.myrunapp.feature.run.data.RunTrackPointEntity
import kotlin.math.roundToLong

data class GpsTrackFilterConfig(
    val maxAccuracyMeters: Float = 30f,
    val goodAccuracyMeters: Float = 15f,
    val excellentAccuracyMeters: Float = 10f,
    val warmupAcceptedPoints: Int = 2,
    val warmupMaxAccuracyMeters: Float = 30f,
    val maxLocationAgeMs: Long = 5_000L,
    val minIntervalMs: Long = 1_000L,
    val minDistanceMeters: Float = 3f,
    val mediumAccuracyMinDistanceMeters: Float = 4f,
    val poorAccuracyMinDistanceMeters: Float = 5f,
    val stationaryMinDistanceMeters: Float = 5f,
    val maxRunningSpeedMps: Float = 8.5f,
    val maxJumpDistanceMeters: Float = 50f,
    val maxJumpWindowMs: Long = 5_000L,
    val poorSpeedAccuracyMps: Float = 2.0f,
    val stationarySpeedMps: Float = 0.5f,
    val movingResumeSpeedMps: Float = 0.8f,
    val stationaryEnterCount: Int = 3,
    val movingResumeCount: Int = 2
)

enum class LocationRejectReason {
    NO_ACCURACY,
    POOR_ACCURACY,
    STALE_LOCATION,
    INVALID_TIME,
    TOO_FREQUENT,
    TOO_CLOSE,
    IMPOSSIBLE_SPEED,
    LARGE_JUMP,
    SUSPECT_SPIKE,
    MOCK_LOCATION
}

sealed interface LocationFilterResult {
    data class Accepted(
        val point: RunTrackPointUiModel,
        val distanceFromPreviousMeters: Float
    ) : LocationFilterResult

    data class Rejected(
        val reason: LocationRejectReason
    ) : LocationFilterResult

    data class Pending(
        val reason: LocationRejectReason
    ) : LocationFilterResult
}

class GpsTrackFilter(
    private val config: GpsTrackFilterConfig = GpsTrackFilterConfig()
) {
    private val warmupCandidates = mutableListOf<RunTrackPointUiModel>()
    private var lastAcceptedPoint: RunTrackPointUiModel? = null
    private var suspectPoint: RunTrackPointUiModel? = null
    private var stationaryCandidateCount = 0
    private var movingCandidateCount = 0
    private var isStationary = false

    fun reset() {
        warmupCandidates.clear()
        lastAcceptedPoint = null
        suspectPoint = null
        stationaryCandidateCount = 0
        movingCandidateCount = 0
        isStationary = false
    }

    fun filter(location: Location): LocationFilterResult {
        val baseReject = baseRejectReason(location)
        if (baseReject != null) return LocationFilterResult.Rejected(baseReject)

        val nextPoint = location.toRunTrackPoint()
        val previousPoint = lastAcceptedPoint

        if (previousPoint == null) {
            warmupCandidates += nextPoint
            if (warmupCandidates.size >= config.warmupAcceptedPoints &&
                nextPoint.accuracyMeters != null &&
                nextPoint.accuracyMeters <= config.warmupMaxAccuracyMeters
            ) {
                warmupCandidates.clear()
                lastAcceptedPoint = nextPoint
                updateMotionState(0f)
                return LocationFilterResult.Accepted(nextPoint, 0f)
            }
            return LocationFilterResult.Pending(LocationRejectReason.SUSPECT_SPIKE)
        }

        val suspect = suspectPoint
        if (suspect != null) {
            val acDistance = distanceMetersBetween(previousPoint, nextPoint)
            val acDeltaMs = deltaTimeMs(previousPoint, nextPoint)
            if (acDeltaMs > 0L && acDistance < dynamicMinDistanceMeters(nextPoint)) {
                suspectPoint = null
                return LocationFilterResult.Rejected(LocationRejectReason.SUSPECT_SPIKE)
            }
            suspectPoint = null
        }

        val deltaMs = deltaTimeMs(previousPoint, nextPoint)
        if (deltaMs <= 0L) return LocationFilterResult.Rejected(LocationRejectReason.INVALID_TIME)
        if (deltaMs < config.minIntervalMs) return LocationFilterResult.Rejected(LocationRejectReason.TOO_FREQUENT)

        val distanceMeters = distanceMetersBetween(previousPoint, nextPoint)
        val speedMps = distanceMeters / (deltaMs / 1000f)
        val minDistance = dynamicMinDistanceMeters(nextPoint)

        if (distanceMeters < minDistance) {
            updateMotionState(speedMps)
            return LocationFilterResult.Rejected(LocationRejectReason.TOO_CLOSE)
        }
        if (deltaMs <= config.maxJumpWindowMs && distanceMeters > config.maxJumpDistanceMeters) {
            suspectPoint = nextPoint
            return LocationFilterResult.Pending(LocationRejectReason.LARGE_JUMP)
        }
        if (speedMps > config.maxRunningSpeedMps) {
            suspectPoint = nextPoint
            return LocationFilterResult.Pending(LocationRejectReason.IMPOSSIBLE_SPEED)
        }

        updateMotionState(speedMps)
        lastAcceptedPoint = nextPoint
        return LocationFilterResult.Accepted(nextPoint, distanceMeters)
    }

    private fun baseRejectReason(location: Location): LocationRejectReason? {
        if (isMockLocation(location)) return LocationRejectReason.MOCK_LOCATION
        if (!location.hasAccuracy()) return LocationRejectReason.NO_ACCURACY
        if (location.accuracy > config.maxAccuracyMeters) return LocationRejectReason.POOR_ACCURACY

        val ageMs = ((SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos) / 1_000_000L)
        if (ageMs > config.maxLocationAgeMs) return LocationRejectReason.STALE_LOCATION

        return null
    }

    private fun dynamicMinDistanceMeters(point: RunTrackPointUiModel): Float {
        if (isStationary) return config.stationaryMinDistanceMeters
        val accuracy = point.accuracyMeters ?: return config.poorAccuracyMinDistanceMeters
        return when {
            accuracy <= config.excellentAccuracyMeters -> config.minDistanceMeters
            accuracy <= config.goodAccuracyMeters -> config.mediumAccuracyMinDistanceMeters
            else -> config.poorAccuracyMinDistanceMeters
        }
    }

    private fun updateMotionState(speedMps: Float) {
        if (speedMps < config.stationarySpeedMps) {
            stationaryCandidateCount += 1
            movingCandidateCount = 0
            if (stationaryCandidateCount >= config.stationaryEnterCount) {
                isStationary = true
            }
        } else if (speedMps >= config.movingResumeSpeedMps) {
            movingCandidateCount += 1
            stationaryCandidateCount = 0
            if (movingCandidateCount >= config.movingResumeCount) {
                isStationary = false
            }
        }
    }
}

fun distanceMetersBetween(from: RunTrackPointUiModel, to: RunTrackPointUiModel): Float {
    val result = FloatArray(1)
    Location.distanceBetween(
        from.latitude,
        from.longitude,
        to.latitude,
        to.longitude,
        result
    )
    return result[0]
}

fun formatRunClock(durationSeconds: Long): String {
    val hours = durationSeconds / 3600
    val minutes = (durationSeconds % 3600) / 60
    val seconds = durationSeconds % 60
    return "%02d:%02d:%02d".format(hours, minutes, seconds)
}

fun RunTrackPointUiModel.toEntity(sessionId: Long): RunTrackPointEntity {
    return RunTrackPointEntity(
        sessionId = sessionId,
        latitude = latitude,
        longitude = longitude,
        coordinateSystem = coordinateSystem,
        accuracyMeters = accuracyMeters,
        altitudeMeters = altitudeMeters,
        speedMetersPerSecond = speedMetersPerSecond,
        recordedAt = recordedAt
    )
}

fun Location.toRunTrackPoint(): RunTrackPointUiModel {
    return RunTrackPointUiModel(
        latitude = latitude,
        longitude = longitude,
        coordinateSystem = detectCoordinateSystem(),
        accuracyMeters = if (hasAccuracy()) accuracy else null,
        altitudeMeters = if (hasAltitude()) altitude else null,
        speedMetersPerSecond = if (hasSpeed()) speed else null,
        recordedAt = if (time > 0L) time else System.currentTimeMillis(),
        elapsedRealtimeNanos = elapsedRealtimeNanos
    )
}

private fun Location.detectCoordinateSystem(): String {
    return if (this is AMapLocation && isOffset) {
        CoordinateSystem.GCJ02
    } else {
        CoordinateSystem.WGS84
    }
}

fun durationSecondsSince(startTime: Long, now: Long = System.currentTimeMillis()): Long {
    return ((now - startTime).coerceAtLeast(0L) / 1000.0).roundToLong()
}

fun deltaTimeMs(from: RunTrackPointUiModel, to: RunTrackPointUiModel): Long {
    return if (from.elapsedRealtimeNanos > 0L && to.elapsedRealtimeNanos > 0L) {
        (to.elapsedRealtimeNanos - from.elapsedRealtimeNanos) / 1_000_000L
    } else {
        to.recordedAt - from.recordedAt
    }
}

@Suppress("DEPRECATION")
private fun isMockLocation(location: Location): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        location.isMock
    } else {
        location.isFromMockProvider
    }
}
