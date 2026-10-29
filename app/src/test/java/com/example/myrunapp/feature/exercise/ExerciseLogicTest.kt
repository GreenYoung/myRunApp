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
}
