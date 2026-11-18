package com.example.myrunapp.feature.run

import org.junit.Assert.assertEquals
import org.junit.Test

class RunTimingTest {
    @Test
    fun plainRun_countsElapsedSinceStart() {
        val seconds = computeActiveDurationSeconds(
            baseDurationMs = 0L,
            startElapsedMs = 1_000L,
            totalPausedElapsedMs = 0L,
            pausedAtElapsedMs = 0L,
            nowElapsedMs = 61_000L
        )

        assertEquals(60L, seconds)
    }

    @Test
    fun pausedRun_freezesAtPauseMoment() {
        // 30s 时暂停，当前已过去 10 分钟：时长必须停在 30s
        val seconds = computeActiveDurationSeconds(
            baseDurationMs = 0L,
            startElapsedMs = 1_000L,
            totalPausedElapsedMs = 0L,
            pausedAtElapsedMs = 31_000L,
            nowElapsedMs = 601_000L
        )

        assertEquals(30L, seconds)
    }

    @Test
    fun resumedRun_excludesPausedDuration() {
        // 跑了 60s → 暂停 20s → 又跑 40s
        val seconds = computeActiveDurationSeconds(
            baseDurationMs = 0L,
            startElapsedMs = 1_000L,
            totalPausedElapsedMs = 20_000L,
            pausedAtElapsedMs = 0L,
            nowElapsedMs = 121_000L
        )

        assertEquals(100L, seconds)
    }

    @Test
    fun recoveredDraft_continuesFromBaseDuration() {
        // 草稿里已记录 120s，恢复后本段又跑了 5s
        val seconds = computeActiveDurationSeconds(
            baseDurationMs = 120_000L,
            startElapsedMs = 10_000L,
            totalPausedElapsedMs = 0L,
            pausedAtElapsedMs = 0L,
            nowElapsedMs = 15_000L
        )

        assertEquals(125L, seconds)
    }

    @Test
    fun clockGoingBackwards_neverReturnsNegative() {
        val seconds = computeActiveDurationSeconds(
            baseDurationMs = 0L,
            startElapsedMs = 50_000L,
            totalPausedElapsedMs = 0L,
            pausedAtElapsedMs = 0L,
            nowElapsedMs = 10_000L
        )

        assertEquals(0L, seconds)
    }

    @Test
    fun pausedTimeLargerThanActiveTime_isClampedToBase() {
        // 累计暂停比已过去的单调时间还长（异常数据）：active 段按 0 处理，不能出现负增量
        val seconds = computeActiveDurationSeconds(
            baseDurationMs = 60_000L,
            startElapsedMs = 0L,
            totalPausedElapsedMs = 90_000L,
            pausedAtElapsedMs = 0L,
            nowElapsedMs = 50_000L
        )

        assertEquals(60L, seconds)
    }
}
