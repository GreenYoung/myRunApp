package com.example.myrunapp.feature.run

import com.example.myrunapp.feature.run.LocationFilterResult.Accepted
import com.example.myrunapp.feature.run.LocationFilterResult.Pending
import com.example.myrunapp.feature.run.LocationFilterResult.Rejected
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos

/**
 * 轨迹过滤器的回归测试。
 *
 * 场景来自 2026-10-30 的真实日志：
 * - 12:04 那次（正常慢跑 3.05 km / 36 分）不能有任何点被新规则误删；
 * - 17:03 那次在 17:06:23 / 17:07:07 被 WiFi 定位（accuracy 30–33 m）带出去 163 m 又跳回来，
 *   以及 17:04:34 断档 16 s 后直接采信首点造成的 69 m 尖刺。
 */
class GpsTrackFilterTest {

    private val baseLatitude = 30.4855
    private val baseLongitude = 114.5598
    private val metersPerDegreeLatitude = 111_320.0
    private val metersPerDegreeLongitude = 111_320.0 * cos(Math.toRadians(baseLatitude))
    private val startTimeMs = 1_790_000_000_000L

    private fun sample(
        offsetMs: Long,
        eastMeters: Double,
        northMeters: Double,
        accuracyMeters: Float = 5f,
        isGps: Boolean = true,
        speedMps: Float = 1.1f
    ): RunLocationSample {
        return RunLocationSample(
            latitude = baseLatitude + northMeters / metersPerDegreeLatitude,
            longitude = baseLongitude + eastMeters / metersPerDegreeLongitude,
            accuracyMeters = accuracyMeters,
            speedMetersPerSecond = speedMps,
            altitudeMeters = null,
            recordedAt = startTimeMs + offsetMs,
            elapsedRealtimeNanos = 0L,
            ageMs = 0L,
            coordinateSystem = CoordinateSystem.GCJ02,
            isGps = isGps,
            isMock = false
        )
    }

    /** 以 1.1 m/s 的慢跑速度铺 count 个点，间隔 3 s（每次前进 3.3 m）。 */
    private fun feedSlowJog(filter: GpsTrackFilter, count: Int, startIndex: Int = 0) {
        for (index in startIndex until startIndex + count) {
            filter.filterSample(sample(index * 3_000L, index * 3.3, 0.0))
        }
    }

    private fun LocationFilterResult.reason(): LocationRejectReason {
        return when (this) {
            is Rejected -> this.reason
            is Pending -> this.reason
            is Accepted -> error("expected rejection but point was accepted")
        }
    }

    @Test
    fun slowJog_withHealthyGps_keepsEveryPoint() {
        val filter = GpsTrackFilter()
        val results = (0 until 60).map { index ->
            filter.filterSample(sample(index * 3_000L, index * 3.3, 0.0))
        }

        val accepted = results.filterIsInstance<Accepted>()
        val distance = accepted.sumOf { it.distanceFromPreviousMeters.toDouble() }

        // 第 1–2 个点用于起点锚定，第 3 个点被接受；之后每个点都保留
        assertEquals(58, accepted.size)
        // 58 个点 => 57 段 * 3.3 m ≈ 188 m，与 12:04 那次的"每段 3–5 m"一致
        assertTrue("distance=$distance", distance in 185.0..192.0)
    }

    @Test
    fun slowJog_shortGap_countsChordSoDistanceIsNotLost() {
        val filter = GpsTrackFilter()
        feedSlowJog(filter, 11) // t=0..30 s

        // 掉星 20 s（t=30 s -> t=50 s），慢跑期间前进 22 m
        val firstAfterGap = filter.filterSample(sample(50_000L, 55.0, 0.0))
        assertTrue("首个恢复点应待定：$firstAfterGap", firstAfterGap is Pending)

        // 第二个点确认了位置，弦长（约 25 m / 23 s = 1.1 m/s）应补记进里程
        val confirmed = filter.filterSample(sample(53_000L, 58.3, 0.0))
        assertTrue("恢复点应被接受：$confirmed", confirmed is Accepted)
        val countedChord = (confirmed as Accepted).distanceFromPreviousMeters
        assertTrue("断档弦长应补记，实际 $countedChord m", countedChord in 20f..30f)
    }

    @Test
    fun networkFix_isRejectedForTrackEvenAtHighAccuracy() {
        val filter = GpsTrackFilter()
        val weak = filter.filterSample(sample(0L, 0.0, 0.0, accuracyMeters = 33f, isGps = false))
        assertEquals(LocationRejectReason.NON_GPS_LOCATION, weak.reason())

        // 即使精度很好也不该进轨迹：网络定位在跑动场景的偏差无法用 accuracy 判断
        feedSlowJog(filter, 11)
        val goodWifi = filter.filterSample(sample(33_000L, 36.3, 0.0, accuracyMeters = 8f, isGps = false))
        assertEquals(LocationRejectReason.NON_GPS_LOCATION, goodWifi.reason())
    }

    @Test
    fun wifiSpike_isNeverAcceptedEvenAfterItLooksSlowEnough() {
        val filter = GpsTrackFilter()
        feedSlowJog(filter, 12) // 最后一个点 t=33 s，east=36.3 m

        // 17:06:14 的真实场景：163 m 外的网络定位，第一次因速度被拒，20 s 后会被"速度降到 8 m/s 以下"放行
        val first = filter.filterSample(sample(39_000L, -120.0, 110.0, accuracyMeters = 33f, isGps = false))
        assertEquals(LocationRejectReason.NON_GPS_LOCATION, first.reason())

        val later = filter.filterSample(sample(59_000L, -120.0, 110.0, accuracyMeters = 33f, isGps = false))
        assertEquals(LocationRejectReason.NON_GPS_LOCATION, later.reason())
    }

    @Test
    fun gpsRecoveryPoint_farFromPath_isNotWrittenIntoTheTrack() {
        val filter = GpsTrackFilter()
        feedSlowJog(filter, 12) // 最后接受点 t=33 s，east=36.3 m

        // GPS 恢复后的第一个点偏了 163 m：应停在恢复期待定，而不是直接连线
        val far = filter.filterSample(sample(53_000L, 199.3, 0.0, accuracyMeters = 10f))
        assertTrue("偏移点应待定：$far", far is Pending)

        // 随后的真实 GPS 点把位置确认下来：弦长约 6.7 m，被补记
        val near1 = filter.filterSample(sample(56_000L, 43.0, 0.0, accuracyMeters = 6f))
        assertTrue(near1 is Pending)
        val near2 = filter.filterSample(sample(59_000L, 46.3, 0.0, accuracyMeters = 6f))
        assertTrue("稳定簇确认后应接受：$near2", near2 is Accepted)

        val distance = (near2 as Accepted).distanceFromPreviousMeters
        assertTrue("只应补记真实位移，实际 $distance m", distance in 5f..15f)
    }

    @Test
    fun singleJumpLongerThan50m_isRejected() {
        val filter = GpsTrackFilter()
        feedSlowJog(filter, 13) // t=0..36 s，走出 30 s 早期保护期

        val jump = filter.filterSample(sample(46_000L, 39.6 + 60.0, 0.0))
        assertEquals(LocationRejectReason.LARGE_JUMP, jump.reason())
    }

    @Test
    fun speedAboveRunningLimit_isRejected() {
        val filter = GpsTrackFilter()
        feedSlowJog(filter, 13)

        // 5 s 走 30 m = 6 m/s，超过 5.5 m/s（3'02"/km）的跑步上限
        val tooFast = filter.filterSample(sample(41_000L, 39.6 + 30.0, 0.0))
        assertEquals(LocationRejectReason.IMPOSSIBLE_SPEED, tooFast.reason())
    }

    @Test
    fun slowJog_gapChordFarFasterThanOwnPace_isNotCounted() {
        val filter = GpsTrackFilter()
        feedSlowJog(filter, 13) // t=0..36 s，east=39.6 m，配速 1.1 m/s

        // 断档 16 s，GPS 回来时却"前进"了 69 m（4.3 m/s），与本人配速不符
        val first = filter.filterSample(sample(52_000L, 108.6, 0.0, accuracyMeters = 6f))
        assertTrue("首个恢复点应待定：$first", first is Pending)
        val confirmed = filter.filterSample(sample(55_000L, 111.9, 0.0, accuracyMeters = 6f))
        assertTrue("恢复点应被接受：$confirmed", confirmed is Accepted)

        // 位置被确认，但这 69 m 不算里程（否则地图上会多出一条 69 m 的直线切角）
        assertEquals(0f, (confirmed as Accepted).distanceFromPreviousMeters, 0.5f)
    }

    @Test
    fun fasterRunner_keepsGapChordThatMatchesOwnPace() {
        val filter = GpsTrackFilter()
        // 3.3 m/s（5'03"/km）的跑者，3 s 一个点
        for (index in 0..12) {
            filter.filterSample(sample(index * 3_000L, index * 9.9, 0.0, speedMps = 3.3f))
        }

        // 断档 15 s，期间前进 45 m，折算 3.0 m/s —— 与本人配速一致，应补记
        val first = filter.filterSample(sample(51_000L, 163.8, 0.0, speedMps = 3.3f))
        assertTrue(first is Pending)
        val confirmed = filter.filterSample(sample(54_000L, 173.7, 0.0, speedMps = 3.3f))
        assertTrue("恢复点应被接受：$confirmed", confirmed is Accepted)

        val counted = (confirmed as Accepted).distanceFromPreviousMeters
        assertTrue("快跑者的合理断档位移应保留，实际 $counted m", counted in 45f..60f)
    }

    @Test
    fun startAnchor_requiresThreeAccuratePoints() {
        val filter = GpsTrackFilter()
        assertTrue(filter.filterSample(sample(0L, 0.0, 0.0)) is Pending)
        assertTrue(filter.filterSample(sample(3_000L, 3.3, 0.0)) is Pending)
        assertTrue(filter.filterSample(sample(6_000L, 6.6, 0.0)) is Accepted)
    }

    @Test
    fun startAnchor_acceptsMediumAccuracyAfterTimeout() {
        val filter = GpsTrackFilter()
        // 精度 15 m：不满足 12 m 的快速聚簇，但 8 s 超时后允许兜底
        assertTrue(filter.filterSample(sample(0L, 0.0, 0.0, accuracyMeters = 15f)) is Pending)
        assertTrue(filter.filterSample(sample(3_000L, 3.3, 0.0, accuracyMeters = 15f)) is Pending)
        assertTrue(filter.filterSample(sample(6_000L, 6.6, 0.0, accuracyMeters = 15f)) is Pending)
        assertTrue(filter.filterSample(sample(9_000L, 9.9, 0.0, accuracyMeters = 15f)) is Accepted)
    }

    @Test
    fun startAnchor_rejectsNetworkFixAtStart() {
        val filter = GpsTrackFilter()
        val result = filter.filterSample(sample(0L, 0.0, 0.0, accuracyMeters = 33f, isGps = false))
        assertEquals(LocationRejectReason.NON_GPS_LOCATION, result.reason())
    }
}
