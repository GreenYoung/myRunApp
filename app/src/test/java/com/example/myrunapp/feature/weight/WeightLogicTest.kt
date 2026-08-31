package com.example.myrunapp.feature.weight

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WeightLogicTest {
    @Test
    fun validateWeightInput_acceptsTwoDecimalWeight() {
        val result = validateWeightInput("2026-08-20", "77.25")

        assertTrue(result.isValid)
        assertEquals(77.25, result.weightKg ?: 0.0, 0.0)
    }

    @Test
    fun validateWeightInput_rejectsOutOfRangeWeight() {
        val result = validateWeightInput("2026-08-20", "2999")

        assertFalse(result.isValid)
        assertEquals("请输入 30.00～300.00 kg 之间的体重", result.error)
    }

    @Test
    fun validateWeightInput_rejectsMoreThanTwoDecimals() {
        val result = validateWeightInput("2026-08-20", "77.222")

        assertFalse(result.isValid)
    }

    @Test
    fun validateWeightInput_rejectsInvalidDate() {
        val result = validateWeightInput("2026-02-30", "77.2")

        assertFalse(result.isValid)
        assertEquals("请输入合法日期", result.error)
    }

    @Test
    fun calculateWeightAxisRange_keepsMinimumSpanForSinglePoint() {
        val range = calculateWeightAxisRange(
            listOf(WeightPoint("2026-08-20", 77.25))
        )

        assertTrue(range.max - range.min >= 2.0)
        assertTrue(range.min < 77.25)
        assertTrue(range.max > 77.25)
    }

    @Test
    fun buildIsoDate_clampsDayToMonth() {
        val result = buildIsoDate(2026, 2, 31)

        assertEquals("2026-02-28", result)
    }

    @Test
    fun changeIsoDateDay_keepsMonthAndCurrentYearFormat() {
        val result = changeIsoDateDay("${currentYear()}-08-20", 9)

        assertEquals("${currentYear()}-08-09", result)
    }
}
