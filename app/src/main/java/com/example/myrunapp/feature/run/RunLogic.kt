package com.example.myrunapp.feature.run

import android.location.Location
import android.os.Build
import android.os.SystemClock
import com.amap.api.location.AMapLocation
import com.example.myrunapp.core.log.AppLogger
import com.example.myrunapp.core.log.LogTags
import com.example.myrunapp.feature.run.data.RunTrackPointEntity
import java.util.Locale
import kotlin.math.roundToLong

data class GpsTrackFilterConfig(
    val maxAccuracyMeters: Float = 35f,
    val goodAccuracyMeters: Float = 15f,
    val excellentAccuracyMeters: Float = 10f,
    val startAnchorRequiredPoints: Int = 3,
    val startAnchorMaxAccuracyMeters: Float = 25f,
    val startAnchorStableRadiusMeters: Float = 20f,
    val startAnchorMaxWaitMs: Long = 8_000L,
    val maxLocationAgeMs: Long = 5_000L,
    val minIntervalMs: Long = 1_000L,
    val minDistanceMeters: Float = 3f,
    val mediumAccuracyMinDistanceMeters: Float = 4f,
    val poorAccuracyMinDistanceMeters: Float = 5f,
    val stationaryMinDistanceMeters: Float = 5f,
    val maxRunningSpeedMps: Float = 8.5f,
    val maxJumpDistanceMeters: Float = 50f,
    val maxJumpWindowMs: Long = 5_000L,
    val earlyTrackGuardDurationMs: Long = 30_000L,
    val earlyTrackGuardDistanceMeters: Float = 120f,
    val earlyTrackMaxJumpDistanceMeters: Float = 35f,
    val earlyTrackMaxSpeedMps: Float = 6.5f,
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
    START_ANCHOR_CALIBRATING,
    START_ANCHOR_UNSTABLE,
    EARLY_TRACK_GUARD,
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
    private var startAnchorStartedAt: Long = 0L
    private var acceptedStartedAt: Long = 0L
    private var distanceAfterStartAnchorMeters = 0f
    private var stationaryCandidateCount = 0
    private var movingCandidateCount = 0
    private var isStationary = false

    fun reset() {
        warmupCandidates.clear()
        lastAcceptedPoint = null
        suspectPoint = null
        startAnchorStartedAt = 0L
        acceptedStartedAt = 0L
        distanceAfterStartAnchorMeters = 0f
        stationaryCandidateCount = 0
        movingCandidateCount = 0
        isStationary = false
    }

    fun filter(location: Location): LocationFilterResult {
        val baseReject = baseRejectReason(location)
        if (baseReject != null) return LocationFilterResult.Rejected(baseReject)

        val nextPoint = if (location is AMapLocation) {
            location.toAmapRunTrackPoint()
        } else {
            location.toRunTrackPoint()
        }
        val previousPoint = lastAcceptedPoint

        if (previousPoint == null) {
            return handleStartAnchor(nextPoint)
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
        if (isInEarlyTrackGuard(nextPoint) &&
            (distanceMeters > config.earlyTrackMaxJumpDistanceMeters || speedMps > config.earlyTrackMaxSpeedMps)
        ) {
            suspectPoint = nextPoint
            AppLogger.d(
                LogTags.TRACK,
                "EARLY_TRACK_REJECT distance=${"%.2f".format(distanceMeters)}m speed=${"%.2f".format(speedMps)}mps " +
                    "accuracy=${nextPoint.accuracyMeters} guardDistance=${"%.2f".format(distanceAfterStartAnchorMeters)}m"
            )
            return LocationFilterResult.Pending(LocationRejectReason.EARLY_TRACK_GUARD)
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
        distanceAfterStartAnchorMeters += distanceMeters
        return LocationFilterResult.Accepted(nextPoint, distanceMeters)
    }

    private fun handleStartAnchor(nextPoint: RunTrackPointUiModel): LocationFilterResult {
        val accuracy = nextPoint.accuracyMeters
        if (accuracy == null || accuracy > config.startAnchorMaxAccuracyMeters) {
            AppLogger.d(
                LogTags.TRACK,
                "START_ANCHOR_REJECT reason=weak_accuracy accuracy=$accuracy max=${config.startAnchorMaxAccuracyMeters} " +
                    "lat=${nextPoint.latitude} lon=${nextPoint.longitude}"
            )
            return LocationFilterResult.Pending(LocationRejectReason.START_ANCHOR_CALIBRATING)
        }

        if (warmupCandidates.isEmpty()) {
            startAnchorStartedAt = nextPoint.recordedAt
        }
        warmupCandidates += nextPoint

        val stableCluster = warmupCandidates.filter { candidate ->
            distanceMetersBetween(candidate, nextPoint) <= config.startAnchorStableRadiusMeters
        }
        val waitedMs = (nextPoint.recordedAt - startAnchorStartedAt).coerceAtLeast(0L)

        AppLogger.d(
            LogTags.TRACK,
            "START_ANCHOR_CANDIDATE count=${warmupCandidates.size} stable=${stableCluster.size} waited=${waitedMs}ms " +
                "accuracy=$accuracy lat=${nextPoint.latitude} lon=${nextPoint.longitude}"
        )

        if (stableCluster.size >= config.startAnchorRequiredPoints) {
            val anchor = stableCluster.bestStartAnchorPoint()
            return acceptStartAnchor(anchor, "stable_cluster")
        }

        if (waitedMs >= config.startAnchorMaxWaitMs) {
            val best = warmupCandidates.bestStartAnchorPoint()
            if ((best.accuracyMeters ?: Float.MAX_VALUE) <= config.excellentAccuracyMeters) {
                return acceptStartAnchor(best, "timeout_best_accuracy")
            }
            AppLogger.d(
                LogTags.TRACK,
                "START_ANCHOR_WAIT reason=timeout_but_unstable count=${warmupCandidates.size} " +
                    "bestAccuracy=${best.accuracyMeters}"
            )
            return LocationFilterResult.Pending(LocationRejectReason.START_ANCHOR_UNSTABLE)
        }

        return LocationFilterResult.Pending(LocationRejectReason.START_ANCHOR_CALIBRATING)
    }

    private fun acceptStartAnchor(
        anchor: RunTrackPointUiModel,
        reason: String
    ): LocationFilterResult.Accepted {
        warmupCandidates.clear()
        lastAcceptedPoint = anchor
        acceptedStartedAt = anchor.recordedAt
        distanceAfterStartAnchorMeters = 0f
        updateMotionState(0f)
        AppLogger.i(
            LogTags.TRACK,
            "START_ANCHOR_ACCEPT reason=$reason accuracy=${anchor.accuracyMeters} " +
                "lat=${anchor.latitude} lon=${anchor.longitude}"
        )
        return LocationFilterResult.Accepted(anchor, 0f)
    }

    private fun List<RunTrackPointUiModel>.bestStartAnchorPoint(): RunTrackPointUiModel {
        return minWith(
            compareBy<RunTrackPointUiModel> { it.accuracyMeters ?: Float.MAX_VALUE }
                .thenByDescending { it.recordedAt }
        )
    }

    private fun isInEarlyTrackGuard(nextPoint: RunTrackPointUiModel): Boolean {
        if (acceptedStartedAt <= 0L) return false
        val elapsedMs = (nextPoint.recordedAt - acceptedStartedAt).coerceAtLeast(0L)
        return elapsedMs <= config.earlyTrackGuardDurationMs &&
            distanceAfterStartAnchorMeters <= config.earlyTrackGuardDistanceMeters
    }

    private fun baseRejectReason(location: Location): LocationRejectReason? {
        if (isMockLocation(location)) return LocationRejectReason.MOCK_LOCATION
        if (!location.hasAccuracy()) return LocationRejectReason.NO_ACCURACY
        if (location.accuracy > config.maxAccuracyMeters) return LocationRejectReason.POOR_ACCURACY

        val ageMs = location.locationAgeMs()
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

fun formatRunInfoDuration(durationSeconds: Long): String {
    return formatRunClock(durationSeconds)
}

fun formatRunInfoDistance(distanceKm: Double): String {
    return String.format(Locale.US, "%.2f 公里", distanceKm)
}

fun formatRunInfoPace(paceText: String): String {
    return paceText
        .replace("\"/km", "''/公里")
        .replace("/km", "/公里")
}

fun formatRunInfoCalories(caloriesKcal: Int): String {
    return "$caloriesKcal 大卡"
}

fun formatRunInfoCalories(caloriesText: String): String {
    return caloriesText.replace("kcal", "大卡")
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

fun AMapLocation.toAmapRunTrackPoint(): RunTrackPointUiModel {
    return RunTrackPointUiModel(
        latitude = latitude,
        longitude = longitude,
        accuracyMeters = if (accuracy > 0f) accuracy else null,
        altitudeMeters = if (hasAltitude()) altitude else null,
        speedMetersPerSecond = if (hasSpeed()) speed else null,
        recordedAt = normalizedLocationTime(),
        elapsedRealtimeNanos = 0L,
        coordinateSystem = CoordinateSystem.GCJ02
    )
}

private fun Location.detectCoordinateSystem(): String {
    return if (this is AMapLocation && isOffset) {
        CoordinateSystem.GCJ02
    } else {
        CoordinateSystem.WGS84
    }
}

private fun AMapLocation.normalizedLocationTime(): Long {
    val now = System.currentTimeMillis()
    return if (time > 0L && time <= now + 1_000L) time else now
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

fun Location.locationAgeMs(): Long {
    return if (this is AMapLocation) {
        val now = System.currentTimeMillis()
        if (time > 0L) now - time else 0L
    } else {
        (SystemClock.elapsedRealtimeNanos() - elapsedRealtimeNanos) / 1_000_000L
    }.coerceAtLeast(0L)
}

@Suppress("DEPRECATION")
private fun isMockLocation(location: Location): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        location.isMock
    } else {
        location.isFromMockProvider
    }
}
