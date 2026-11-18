package com.example.myrunapp.feature.exercise

import com.example.myrunapp.feature.exercise.data.ExerciseRecordEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ExerciseLogicTest {
    @Test
    fun statsDetail_disablesPreviousAtEarliestPeriod() {
        val state = buildExerciseStatsDetail(
            records = listOf(recordAt(yearsAgo(3))),
            period = ExerciseStatsPeriod.YEAR,
            periodOffset = -10
        )

        assertEquals(-3, state.periodOffset)
        assertFalse(state.canGoPrevious)
        assertTrue(state.canGoNext)
    }

    @Test
    fun statsDetail_allowsPreviousWithinRecordedRange() {
        val state = buildExerciseStatsDetail(
            records = listOf(recordAt(yearsAgo(3))),
            period = ExerciseStatsPeriod.YEAR,
            periodOffset = -1
        )

        assertEquals(-1, state.periodOffset)
        assertTrue(state.canGoPrevious)
        assertTrue(state.canGoNext)
    }

    @Test
    fun statsDetail_disablesNavigationWhenNoRecords() {
        val state = buildExerciseStatsDetail(
            records = emptyList(),
            period = ExerciseStatsPeriod.MONTH
        )

        assertEquals(0, state.periodOffset)
        assertFalse(state.canGoPrevious)
        assertFalse(state.canGoNext)
    }

    @Test
    fun statsDetail_labelsCurrentWeek() {
        val state = buildExerciseStatsDetail(
            records = listOf(recordAt(daysAgo(10))),
            period = ExerciseStatsPeriod.WEEK,
            periodOffset = 0
        )

        assertEquals("本周", state.periodLabel)
    }

    @Test
    fun statsDetail_labelsPreviousWeek() {
        val state = buildExerciseStatsDetail(
            records = listOf(recordAt(daysAgo(10))),
            period = ExerciseStatsPeriod.WEEK,
            periodOffset = -1
        )

        assertEquals(-1, state.periodOffset)
        assertTrue(Regex("上周 · \\d{2}\\.\\d{2}-\\d{2}\\.\\d{2}").matches(state.periodLabel))
    }

    @Test
    fun statsDetail_labelsDeeperWeek() {
        val state = buildExerciseStatsDetail(
            records = listOf(recordAt(daysAgo(30))),
            period = ExerciseStatsPeriod.WEEK,
            periodOffset = -3
        )

        assertEquals(-3, state.periodOffset)
        assertTrue(Regex("3周前 · \\d{2}\\.\\d{2}-\\d{2}\\.\\d{2}").matches(state.periodLabel))
    }

    @Test
    fun statsDetail_labelsPreviousMonth() {
        val state = buildExerciseStatsDetail(
            records = listOf(recordAt(daysAgo(40))),
            period = ExerciseStatsPeriod.MONTH,
            periodOffset = -1
        )

        assertEquals(-1, state.periodOffset)
        assertTrue(Regex("上月 · \\d{4}\\.\\d{2}").matches(state.periodLabel))
    }

    @Test
    fun statsDetail_labelsPreviousYear() {
        val state = buildExerciseStatsDetail(
            records = listOf(recordAt(yearsAgo(2))),
            period = ExerciseStatsPeriod.YEAR,
            periodOffset = -1
        )

        assertEquals(-1, state.periodOffset)
        assertTrue(Regex("去年 · \\d{4}").matches(state.periodLabel))
    }

    @Test
    fun statsDetail_labelFollowsClampedOffset() {
        val state = buildExerciseStatsDetail(
            records = listOf(recordAt(daysAgo(10))),
            period = ExerciseStatsPeriod.WEEK,
            periodOffset = -99
        )

        assertTrue(state.periodOffset < 0)
        assertFalse(state.canGoPrevious)
        val expectedPrefix = if (state.periodOffset == -1) {
            "上周"
        } else {
            "${-state.periodOffset}周前"
        }
        assertTrue(state.periodLabel.startsWith("$expectedPrefix · "))
    }

    @Test
    fun rangeText_currentWeek_showsSingleDayWhenStartEqualsEnd() {
        val day = millisOf(2026, 11, 16)

        val text = buildStatsRangeText(
            period = ExerciseStatsPeriod.WEEK,
            startMillis = day,
            endMillis = day,
            isCurrentPeriod = true,
            nowMillis = day
        )

        assertEquals("11月16日", text)
    }

    @Test
    fun rangeText_currentWeek_showsShortRange() {
        val text = buildStatsRangeText(
            period = ExerciseStatsPeriod.WEEK,
            startMillis = millisOf(2026, 11, 16),
            endMillis = millisOf(2026, 11, 17),
            isCurrentPeriod = true,
            nowMillis = millisOf(2026, 11, 17)
        )

        assertEquals("11月16日 - 11月17日", text)
    }

    @Test
    fun rangeText_previousYearWeek_prefixesYear() {
        val text = buildStatsRangeText(
            period = ExerciseStatsPeriod.WEEK,
            startMillis = millisOf(2025, 10, 20),
            endMillis = millisOf(2025, 10, 26),
            isCurrentPeriod = false,
            nowMillis = millisOf(2026, 11, 17)
        )

        assertEquals("2025年10月20日 - 10月26日", text)
    }

    @Test
    fun rangeText_crossYearWeek_prefixesBothYears() {
        val text = buildStatsRangeText(
            period = ExerciseStatsPeriod.WEEK,
            startMillis = millisOf(2025, 12, 29),
            endMillis = millisOf(2026, 1, 4),
            isCurrentPeriod = false,
            nowMillis = millisOf(2026, 11, 17)
        )

        assertEquals("2025年12月29日 - 2026年1月4日", text)
    }

    @Test
    fun rangeText_pastYear_showsYearOnly() {
        val text = buildStatsRangeText(
            period = ExerciseStatsPeriod.YEAR,
            startMillis = millisOf(2025, 1, 1),
            endMillis = millisOf(2025, 12, 31),
            isCurrentPeriod = false,
            nowMillis = millisOf(2026, 11, 17)
        )

        assertEquals("2025年", text)
    }

    @Test
    fun rangeText_currentYear_appendsThroughDate() {
        val text = buildStatsRangeText(
            period = ExerciseStatsPeriod.YEAR,
            startMillis = millisOf(2026, 1, 1),
            endMillis = millisOf(2026, 11, 17),
            isCurrentPeriod = true,
            nowMillis = millisOf(2026, 11, 17)
        )

        assertEquals("2026年（至 11月17日）", text)
    }

    @Test
    fun rangeText_currentMonth_showsDayRange() {
        val text = buildStatsRangeText(
            period = ExerciseStatsPeriod.MONTH,
            startMillis = millisOf(2026, 11, 1),
            endMillis = millisOf(2026, 11, 17),
            isCurrentPeriod = true,
            nowMillis = millisOf(2026, 11, 17)
        )

        assertEquals("11月1日 - 11月17日", text)
    }

    @Test
    fun rangeText_pastYearMonth_prefixesYear() {
        val text = buildStatsRangeText(
            period = ExerciseStatsPeriod.MONTH,
            startMillis = millisOf(2025, 10, 1),
            endMillis = millisOf(2025, 10, 31),
            isCurrentPeriod = false,
            nowMillis = millisOf(2026, 11, 17)
        )

        assertEquals("2025年10月1日 - 10月31日", text)
    }

    private fun yearsAgo(years: Int): Long {
        return Calendar.getInstance().apply {
            add(Calendar.YEAR, -years)
        }.timeInMillis
    }

    private fun recordAt(startTime: Long): ExerciseRecordEntity {
        return ExerciseRecordEntity(
            type = "TREADMILL",
            startTime = startTime,
            durationSeconds = 1800L,
            distanceKm = 5.0,
            caloriesKcal = 300
        )
    }

    private fun daysAgo(days: Int): Long {
        return Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -days)
        }.timeInMillis
    }

    /** 用固定年月日构造当天 00:00 的时间戳，避免用例受真实"今天"影响。 */
    private fun millisOf(year: Int, month: Int, day: Int): Long {
        return Calendar.getInstance().apply {
            clear()
            set(year, month - 1, day, 0, 0, 0)
        }.timeInMillis
    }
}
