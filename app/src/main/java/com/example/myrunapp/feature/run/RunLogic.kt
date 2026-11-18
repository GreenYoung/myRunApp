package com.example.myrunapp.feature.run

import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.SystemClock
import com.amap.api.location.AMapLocation
import com.example.myrunapp.core.log.AppLogger
import com.example.myrunapp.core.log.LogTags
import com.example.myrunapp.feature.run.data.RunDraftPointEntity
import com.example.myrunapp.feature.run.data.RunTrackPointEntity
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.sqrt

data class GpsTrackFilterConfig(
    val maxAccuracyMeters: Float = 35f,
    val goodAccuracyMeters: Float = 15f,
    val excellentAccuracyMeters: Float = 10f,
    // 起点锚点：先要求高精度点聚簇，超时后才退而求其次接受中等精度点
    val startAnchorRequiredPoints: Int = 3,
    val startAnchorMaxAccuracyMeters: Float = 12f,
    val startAnchorFallbackAccuracyMeters: Float = 20f,
    val startAnchorStableRadiusMeters: Float = 10f,
    val startAnchorMaxWaitMs: Long = 8_000L,
    val maxLocationAgeMs: Long = 5_000L,
    val minIntervalMs: Long = 1_000L,
    val minDistanceMeters: Float = 3f,
    val mediumAccuracyMinDistanceMeters: Float = 4f,
    val poorAccuracyMinDistanceMeters: Float = 5f,
    val stationaryMinDistanceMeters: Float = 5f,
    // 8.5 m/s（30.6 km/h）会把 160 m 的网络定位漂移当成合法位移，按跑步上限收紧到 5.5 m/s（3'02"/km）
    val maxRunningSpeedMps: Float = 5.5f,
    val maxJumpDistanceMeters: Float = 50f,
    val earlyTrackGuardDurationMs: Long = 30_000L,
    val earlyTrackGuardDistanceMeters: Float = 120f,
    val earlyTrackMaxJumpDistanceMeters: Float = 35f,
    val earlyTrackMaxSpeedMps: Float = 5f,
    val poorSpeedAccuracyMps: Float = 2.0f,
    val stationarySpeedMps: Float = 0.5f,
    val movingResumeSpeedMps: Float = 0.8f,
    val stationaryEnterCount: Int = 3,
    val movingResumeCount: Int = 2,
    // 定位中断（客户端重启/长时间静默）后的恢复期：更严格的精度与速度门槛 + 稳定簇确认
    val recoveryMaxAccuracyMeters: Float = 15f,
    val recoveryMaxSpeedMps: Float = 5f,
    val recoveryStableRadiusMeters: Float = 15f,
    val recoveryRequiredPoints: Int = 2,
    val recoveryMaxWaitMs: Long = 20_000L,
    // 恢复后的一段时间内继续收紧跳跃限制，避免漂移点被当作真实位移
    val recoveryGuardDurationMs: Long = 60_000L,
    val recoveryGuardMaxJumpDistanceMeters: Float = 30f,
    val recoveryGuardMaxSpeedMps: Float = 5f,
    // 超过该时长的静默视为定位中断：恢复点先待定，确认成簇后才决定这段弦长要不要计入里程
    val longGapIgnoreDistanceMs: Long = 12_000L,
    // 断档弦长补记上限：断档不超过 60 s 且弦长速度不超过阈值时才补记，避免慢跑掉星少算里程
    val recoveryChordMaxGapMs: Long = 60_000L,
    val recoveryChordMaxSpeedMps: Float = 5.5f,
    // 弦长阈值跟着跑者自己的配速走：慢跑 1.1 m/s 时阈值 2.5 m/s（9 km/h），
    // 这样"断档 16 s 却前进 69 m（4.3 m/s）"这种漂移不会被当成位移，而 1–2 m/s 的真实断档照样补记
    val recoveryChordPaceFactor: Float = 2.0f,
    val recoveryChordMinSpeedMps: Float = 2.5f,
    // 只把 GPS 结果当作轨迹点；网络定位（前次/WiFi/基站/缓存）只用于地图展示
    val gpsOnly: Boolean = true
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
    RECOVERY_CALIBRATING,
    RECOVERY_GUARD,
    NON_GPS_LOCATION,
    MOCK_LOCATION
}

/**
 * 过滤器需要的最小定位信息。与 android.location.Location 解耦，
 * 便于在 JVM 单元测试里直接回放真实日志中的定位序列。
 */
data class RunLocationSample(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float?,
    val speedMetersPerSecond: Float?,
    val altitudeMeters: Double?,
    val recordedAt: Long,
    val elapsedRealtimeNanos: Long,
    val ageMs: Long,
    val coordinateSystem: String,
    val isGps: Boolean,
    val isMock: Boolean
)

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
    private val recoveryCandidates = mutableListOf<RunTrackPointUiModel>()
    private var isRecovering = false
    private var recoveryStartedAt = 0L
    private var recoveryGuardUntilMs = 0L
    /** 最近一段正常相邻点的平均速度（指数平滑），用于判断断档弦长是否与本人配速匹配。 */
    private var recentPaceMps = 0f

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
        recoveryCandidates.clear()
        isRecovering = false
        recoveryStartedAt = 0L
        recoveryGuardUntilMs = 0L
        recentPaceMps = 0f
    }

    /**
     * 定位客户端重建后调用：下一个被接受的点必须通过恢复期校验（精度、速度、稳定簇），
     * 与中断前最后一个点之间的弦长要等确认后再按配速判断是否补记。
     */
    fun beginRecovery() {
        if (lastAcceptedPoint == null || isRecovering) return
        isRecovering = true
        recoveryCandidates.clear()
        recoveryStartedAt = 0L
        suspectPoint = null
        AppLogger.i(
            LogTags.TRACK,
            "FILTER_RECOVERY_BEGIN lastLat=${lastAcceptedPoint?.latitude} lastLon=${lastAcceptedPoint?.longitude} " +
                "accuracy=${lastAcceptedPoint?.accuracyMeters}"
        )
    }

    fun filter(location: Location): LocationFilterResult {
        return filterSample(location.toRunLocationSample())
    }

    internal fun filterSample(sample: RunLocationSample): LocationFilterResult {
        val baseReject = baseRejectReason(sample)
        if (baseReject != null) return LocationFilterResult.Rejected(baseReject)

        val nextPoint = sample.toRunTrackPoint()
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

        // 超过 12 s 的静默（GPS 掉星、后台冻结、客户端重启）一律走恢复期：
        // 首点先待定，凑够稳定簇后才落盘，弦长在确认后按合理性补记
        if (isRecovering || deltaMs >= config.longGapIgnoreDistanceMs) {
            if (!isRecovering) {
                AppLogger.w(
                    LogTags.TRACK,
                    "FILTER_LONG_GAP deltaTime=${deltaMs}ms chord=${"%.2f".format(distanceMetersBetween(previousPoint, nextPoint))}m"
                )
                beginRecovery()
            }
            return handleRecovery(previousPoint, nextPoint, deltaMs)
        }

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
        if (recoveryGuardUntilMs > 0L) {
            if (nextPoint.recordedAt <= recoveryGuardUntilMs) {
                if (distanceMeters > config.recoveryGuardMaxJumpDistanceMeters ||
                    speedMps > config.recoveryGuardMaxSpeedMps
                ) {
                    suspectPoint = nextPoint
                    AppLogger.d(
                        LogTags.TRACK,
                        "RECOVERY_GUARD_REJECT distance=${"%.2f".format(distanceMeters)}m speed=${"%.2f".format(speedMps)}mps " +
                            "accuracy=${nextPoint.accuracyMeters} remainingMs=${recoveryGuardUntilMs - nextPoint.recordedAt}"
                    )
                    return LocationFilterResult.Pending(LocationRejectReason.RECOVERY_GUARD)
                }
            } else {
                recoveryGuardUntilMs = 0L
            }
        }
        // 断档已在上面走恢复期，这里剩下的间隔都 < 12 s，仍是"单跳超过 50 m 就不可能是跑步"。
        // 不再像原来那样只在 5 s 窗口内判定，否则间隔一长就会发现不了 160 m 的漂移。
        if (distanceMeters > config.maxJumpDistanceMeters) {
            suspectPoint = nextPoint
            return LocationFilterResult.Pending(LocationRejectReason.LARGE_JUMP)
        }
        if (speedMps > config.maxRunningSpeedMps) {
            suspectPoint = nextPoint
            return LocationFilterResult.Pending(LocationRejectReason.IMPOSSIBLE_SPEED)
        }

        updateMotionState(speedMps)
        // 优先用定位自带的 speed（更平滑），没有时退回相邻点位移速度
        val paceSample = nextPoint.speedMetersPerSecond?.takeIf { it in 0.2f..8f } ?: speedMps
        recentPaceMps = if (recentPaceMps <= 0f) paceSample else recentPaceMps * 0.7f + paceSample * 0.3f
        lastAcceptedPoint = nextPoint
        distanceAfterStartAnchorMeters += distanceMeters
        return LocationFilterResult.Accepted(nextPoint, distanceMeters)
    }

    private fun handleStartAnchor(nextPoint: RunTrackPointUiModel): LocationFilterResult {
        val accuracy = nextPoint.accuracyMeters
        if (accuracy == null || accuracy > config.startAnchorFallbackAccuracyMeters) {
            AppLogger.d(
                LogTags.TRACK,
                "START_ANCHOR_REJECT reason=weak_accuracy accuracy=$accuracy max=${config.startAnchorFallbackAccuracyMeters} " +
                    "lat=${nextPoint.latitude} lon=${nextPoint.longitude}"
            )
            return LocationFilterResult.Pending(LocationRejectReason.START_ANCHOR_CALIBRATING)
        }

        if (warmupCandidates.isEmpty()) {
            startAnchorStartedAt = nextPoint.recordedAt
        }
        warmupCandidates += nextPoint

        // 只有高精度点才参与"快速起点"聚簇；中等精度点留作超时兜底
        val accurateCluster = warmupCandidates
            .filter { (it.accuracyMeters ?: Float.MAX_VALUE) <= config.startAnchorMaxAccuracyMeters }
            .filter { candidate -> distanceMetersBetween(candidate, nextPoint) <= config.startAnchorStableRadiusMeters }
        val waitedMs = (nextPoint.recordedAt - startAnchorStartedAt).coerceAtLeast(0L)

        AppLogger.d(
            LogTags.TRACK,
            "START_ANCHOR_CANDIDATE count=${warmupCandidates.size} stable=${accurateCluster.size} waited=${waitedMs}ms " +
                "accuracy=$accuracy lat=${nextPoint.latitude} lon=${nextPoint.longitude}"
        )

        if (accurateCluster.size >= config.startAnchorRequiredPoints) {
            val anchor = accurateCluster.bestStartAnchorPoint()
            return acceptStartAnchor(anchor, "stable_cluster")
        }

        if (waitedMs >= config.startAnchorMaxWaitMs) {
            // 超时兜底不能只看"单点精度最高"：弱信号下常常有一个精度数字好看、位置却偏出 50 m 的点，
            // 选它会把整段轨迹平移。这里改选邻点最多的那个点（即最可能代表真实位置的点）。
            val best = warmupCandidates.densestStartAnchorPoint()
            if ((best.accuracyMeters ?: Float.MAX_VALUE) <= config.startAnchorFallbackAccuracyMeters) {
                return acceptStartAnchor(best, "timeout_fallback_cluster")
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

    /** 在与最新点同一簇的候选里挑邻点最多、精度最好、时间最新的那个点。 */
    private fun List<RunTrackPointUiModel>.densestStartAnchorPoint(): RunTrackPointUiModel {
        val latest = maxBy { it.recordedAt }
        val nearLatest = filter { point ->
            distanceMetersBetween(point, latest) <= config.startAnchorStableRadiusMeters
        }
        fun neighborCount(point: RunTrackPointUiModel): Int = count { other ->
            distanceMetersBetween(point, other) <= config.startAnchorStableRadiusMeters
        }
        return nearLatest.maxWith(
            compareBy<RunTrackPointUiModel> { neighborCount(it) }
                .thenBy { -(it.accuracyMeters ?: Float.MAX_VALUE) }
                .thenBy { it.recordedAt }
        )
    }

    /**
     * 恢复期：只接受高精度、速度合理且连续多个点聚集（稳定簇）的候选点。
     * 确认后把它作为新的锚点，并按"断档时长 × 跑步速度"校验这段弦长是否补记进里程，
     * 随后进入 60s 的跳跃收紧窗口。
     */
    private fun handleRecovery(
        previousPoint: RunTrackPointUiModel,
        nextPoint: RunTrackPointUiModel,
        deltaMs: Long
    ): LocationFilterResult {
        val accuracy = nextPoint.accuracyMeters
        if (accuracy == null || accuracy > config.recoveryMaxAccuracyMeters) {
            AppLogger.d(
                LogTags.TRACK,
                "RECOVERY_REJECT reason=weak_accuracy accuracy=$accuracy max=${config.recoveryMaxAccuracyMeters}"
            )
            return LocationFilterResult.Pending(LocationRejectReason.RECOVERY_CALIBRATING)
        }

        val distanceMeters = distanceMetersBetween(previousPoint, nextPoint)
        val speedMps = distanceMeters / (deltaMs / 1000f)
        val speedOk = speedMps <= config.recoveryMaxSpeedMps
        if (recoveryCandidates.isEmpty()) {
            recoveryStartedAt = nextPoint.recordedAt
        }
        if (!speedOk) {
            AppLogger.d(
                LogTags.TRACK,
                "RECOVERY_REJECT reason=speed speed=${"%.2f".format(speedMps)}mps distance=${"%.2f".format(distanceMeters)}m " +
                    "deltaTime=${deltaMs}ms accuracy=$accuracy"
            )
            if (nextPoint.recordedAt - recoveryStartedAt < config.recoveryMaxWaitMs) {
                return LocationFilterResult.Pending(LocationRejectReason.RECOVERY_CALIBRATING)
            }
        }

        recoveryCandidates += nextPoint

        val stableCluster = recoveryCandidates.filter { candidate ->
            distanceMetersBetween(candidate, nextPoint) <= config.recoveryStableRadiusMeters
        }
        val waitedMs = (nextPoint.recordedAt - recoveryStartedAt).coerceAtLeast(0L)

        AppLogger.d(
            LogTags.TRACK,
            "RECOVERY_CANDIDATE count=${recoveryCandidates.size} stable=${stableCluster.size} waited=${waitedMs}ms " +
                "accuracy=$accuracy lat=${nextPoint.latitude} lon=${nextPoint.longitude}"
        )

        if (speedOk && stableCluster.size >= config.recoveryRequiredPoints) {
            return acceptRecoveryAnchor(nextPoint, "stable_cluster", waitedMs)
        }

        if (waitedMs >= config.recoveryMaxWaitMs) {
            val best = recoveryCandidates.bestStartAnchorPoint()
            if ((best.accuracyMeters ?: Float.MAX_VALUE) <= config.excellentAccuracyMeters) {
                return acceptRecoveryAnchor(best, "timeout_best_accuracy", waitedMs)
            }
            AppLogger.d(
                LogTags.TRACK,
                "RECOVERY_WAIT reason=timeout_but_unstable count=${recoveryCandidates.size} bestAccuracy=${best.accuracyMeters}"
            )
        }

        return LocationFilterResult.Pending(LocationRejectReason.RECOVERY_CALIBRATING)
    }

    private fun acceptRecoveryAnchor(
        anchor: RunTrackPointUiModel,
        reason: String,
        waitedMs: Long
    ): LocationFilterResult.Accepted {
        val previous = lastAcceptedPoint
        val gapMs = previous?.let { deltaTimeMs(it, anchor) } ?: 0L
        val chordMeters = previous?.let { distanceMetersBetween(it, anchor) } ?: 0f
        // 断档弦长不直接丢弃：位置被稳定簇确认后，只要位移与断档时长匹配本人配速（且断档 ≤ 60 s）就补记，
        // 这样慢跑掉星 20 s（约 20–40 m）的里程不会丢，而 163 m/20 s 的漂移仍然进不来。
        val chordSpeedMps = if (gapMs > 0L) chordMeters / (gapMs / 1000f) else 0f
        // 允许的弦长速度随本人配速浮动：慢跑时不会被 160 m 的漂移骗到，冲刺跑者也不会被误伤
        val chordSpeedLimitMps = (recentPaceMps * config.recoveryChordPaceFactor)
            .coerceIn(config.recoveryChordMinSpeedMps, config.recoveryChordMaxSpeedMps)
        val countedChordMeters = if (
            gapMs in 1..config.recoveryChordMaxGapMs &&
            chordSpeedMps <= chordSpeedLimitMps
        ) {
            chordMeters
        } else {
            0f
        }
        val candidateCount = recoveryCandidates.size
        recoveryCandidates.clear()
        isRecovering = false
        recoveryStartedAt = 0L
        suspectPoint = null
        lastAcceptedPoint = anchor
        recoveryGuardUntilMs = anchor.recordedAt + config.recoveryGuardDurationMs
        updateMotionState(0f)
        AppLogger.i(
            LogTags.TRACK,
            "RECOVERY_ANCHOR_ACCEPT reason=$reason candidates=$candidateCount waited=${waitedMs}ms " +
                "accuracy=${anchor.accuracyMeters} gap=${gapMs}ms chord=${"%.2f".format(chordMeters)}m " +
                "chordSpeed=${"%.2f".format(chordSpeedMps)}mps limit=${"%.2f".format(chordSpeedLimitMps)}mps " +
                "counted=${"%.2f".format(countedChordMeters)}m " +
                "lat=${anchor.latitude} lon=${anchor.longitude}"
        )
        return LocationFilterResult.Accepted(anchor, countedChordMeters)
    }

    private fun isInEarlyTrackGuard(nextPoint: RunTrackPointUiModel): Boolean {
        if (acceptedStartedAt <= 0L) return false
        val elapsedMs = (nextPoint.recordedAt - acceptedStartedAt).coerceAtLeast(0L)
        return elapsedMs <= config.earlyTrackGuardDurationMs &&
            distanceAfterStartAnchorMeters <= config.earlyTrackGuardDistanceMeters
    }

    private fun baseRejectReason(sample: RunLocationSample): LocationRejectReason? {
        if (sample.isMock) return LocationRejectReason.MOCK_LOCATION
        // 网络定位（前次/WiFi/基站/缓存）在跑动场景下误差可达上百米，只展示不落轨迹
        if (config.gpsOnly && !sample.isGps) return LocationRejectReason.NON_GPS_LOCATION
        if (sample.accuracyMeters == null) return LocationRejectReason.NO_ACCURACY
        if (sample.accuracyMeters > config.maxAccuracyMeters) return LocationRejectReason.POOR_ACCURACY
        if (sample.ageMs > config.maxLocationAgeMs) return LocationRejectReason.STALE_LOCATION

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

private const val EARTH_RADIUS_METERS = 6371008.8

fun distanceMetersBetween(from: RunTrackPointUiModel, to: RunTrackPointUiModel): Float {
    return distanceMetersBetween(from.latitude, from.longitude, to.latitude, to.longitude)
}

/** Haversine 距离（米）。纯 Kotlin 实现，保证轨迹过滤逻辑可以在 JVM 单元测试里回放。 */
fun distanceMetersBetween(fromLatitude: Double, fromLongitude: Double, toLatitude: Double, toLongitude: Double): Float {
    val dLat = Math.toRadians(toLatitude - fromLatitude)
    val dLon = Math.toRadians(toLongitude - fromLongitude)
    val lat1 = Math.toRadians(fromLatitude)
    val lat2 = Math.toRadians(toLatitude)
    val sinHalfLat = sin(dLat / 2.0)
    val sinHalfLon = sin(dLon / 2.0)
    val a = sinHalfLat * sinHalfLat + cos(lat1) * cos(lat2) * sinHalfLon * sinHalfLon
    val c = 2.0 * atan2(sqrt(a), sqrt(1.0 - a))
    return (EARTH_RADIUS_METERS * c).toFloat()
}

fun formatRunClock(durationSeconds: Long): String {
    val hours = durationSeconds / 3600
    val minutes = (durationSeconds % 3600) / 60
    val seconds = durationSeconds % 60
    return "%02d:%02d:%02d".format(hours, minutes, seconds)
}

/**
 * 跑步时长计算：只依赖单调时钟（elapsedRealtime），墙上时钟怎么跳都不影响结果。
 *
 * @param baseDurationMs 草稿恢复时已经累计的时长（新跑步为 0）
 * @param startElapsedMs 本段计时基准
 * @param totalPausedElapsedMs 本段累计暂停时长
 * @param pausedAtElapsedMs 本次暂停开始的时刻；0 表示当前未暂停（此时用 nowElapsedMs 计时）
 * @param nowElapsedMs 当前单调时钟读数
 */
fun computeActiveDurationSeconds(
    baseDurationMs: Long,
    startElapsedMs: Long,
    totalPausedElapsedMs: Long,
    pausedAtElapsedMs: Long,
    nowElapsedMs: Long
): Long {
    val effectiveNow = if (pausedAtElapsedMs > 0L) pausedAtElapsedMs else nowElapsedMs
    val activeMs = (effectiveNow - startElapsedMs - totalPausedElapsedMs).coerceAtLeast(0L)
    return (baseDurationMs + activeMs) / 1000L
}

fun formatRunInfoDuration(durationSeconds: Long): String {
    return formatRunClock(durationSeconds)
}

fun formatRunInfoDistance(distanceKm: Double): String {
    return String.format(Locale.US, "%.2f", distanceKm)
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

fun RunTrackPointUiModel.toDraftEntity(): RunDraftPointEntity {
    return RunDraftPointEntity(
        latitude = latitude,
        longitude = longitude,
        coordinateSystem = coordinateSystem,
        accuracyMeters = accuracyMeters,
        altitudeMeters = altitudeMeters,
        speedMetersPerSecond = speedMetersPerSecond,
        recordedAt = recordedAt
    )
}

fun RunDraftPointEntity.toRunTrackPointUiModel(): RunTrackPointUiModel {
    return RunTrackPointUiModel(
        latitude = latitude,
        longitude = longitude,
        accuracyMeters = accuracyMeters,
        altitudeMeters = altitudeMeters,
        speedMetersPerSecond = speedMetersPerSecond,
        recordedAt = recordedAt,
        coordinateSystem = coordinateSystem
    )
}

fun Location.toRunLocationSample(): RunLocationSample {
    val amapLocation = this as? AMapLocation
    return RunLocationSample(
        latitude = latitude,
        longitude = longitude,
        accuracyMeters = when {
            amapLocation != null -> if (amapLocation.accuracy > 0f) amapLocation.accuracy else null
            hasAccuracy() -> accuracy
            else -> null
        },
        speedMetersPerSecond = if (hasSpeed()) speed else null,
        altitudeMeters = if (hasAltitude()) altitude else null,
        recordedAt = if (amapLocation != null) {
            amapLocation.normalizedLocationTime()
        } else {
            if (time > 0L) time else System.currentTimeMillis()
        },
        elapsedRealtimeNanos = if (amapLocation != null) 0L else elapsedRealtimeNanos,
        ageMs = locationAgeMs(),
        coordinateSystem = detectCoordinateSystem(),
        isGps = if (amapLocation != null) {
            amapLocation.locationType == AMapLocation.LOCATION_TYPE_GPS
        } else {
            provider == LocationManager.GPS_PROVIDER
        },
        isMock = isMockLocation(this)
    )
}

fun RunLocationSample.toRunTrackPoint(): RunTrackPointUiModel {
    return RunTrackPointUiModel(
        latitude = latitude,
        longitude = longitude,
        accuracyMeters = accuracyMeters,
        altitudeMeters = altitudeMeters,
        speedMetersPerSecond = speedMetersPerSecond,
        recordedAt = recordedAt,
        elapsedRealtimeNanos = elapsedRealtimeNanos,
        coordinateSystem = coordinateSystem
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
