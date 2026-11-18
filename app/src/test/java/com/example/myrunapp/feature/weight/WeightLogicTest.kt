package com.example.myrunapp.feature.weight

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

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

    @Test
    fun filterWeightPointsByRange_anchorsWindowToToday() {
        val today = todayIsoDate()
        val twoDaysAgo = isoDaysAgo(2)
        val tenDaysAgo = isoDaysAgo(10)
        val points = listOf(
            WeightPoint(tenDaysAgo, 70.0),
            WeightPoint(twoDaysAgo, 71.0),
            WeightPoint(today, 72.0)
        )

        val filtered = filterWeightPointsByRange(points, WeightRange.DAYS_7)

        assertEquals(listOf(twoDaysAgo, today), filtered.map { it.date })
    }

    @Test
    fun filterWeightPointsByRange_allKeepsEveryPoint() {
        val points = listOf(
            WeightPoint(isoDaysAgo(100), 70.0),
            WeightPoint(todayIsoDate(), 72.0)
        )

        val filtered = filterWeightPointsByRange(points, WeightRange.ALL)

        assertEquals(points, filtered)
    }

    @Test
    fun calculateBmiUiState_usesWhoThresholds() {
        // 身高固定 200cm（2m），BMI = 体重 / 4，阈值正好落在整数上
        assertEquals("偏低", calculateBmiUiState(70.0, 200.0).category)
        assertEquals("正常", calculateBmiUiState(74.0, 200.0).category)   // 18.5
        assertEquals("正常", calculateBmiUiState(99.6, 200.0).category)   // 24.9
        assertEquals("偏高", calculateBmiUiState(100.0, 200.0).category)  // 25.0
        assertEquals("偏高", calculateBmiUiState(119.6, 200.0).category)  // 29.9
        assertEquals("肥胖", calculateBmiUiState(120.0, 200.0).category)  // 30.0
    }

    @Test
    fun calculateBmiUiState_withoutHeightReturnsNoBmi() {
        val result = calculateBmiUiState(72.0, null)

        assertEquals(null, result.bmi)
        assertEquals("设置身高后查看 BMI", result.category)
    }

    @Test
    fun targetProgress_supportsGainingWeight() {
        // 起始 60 → 目标 65（增重），当前 63 → 完成 60%
        assertEquals(0.6f, calculateTargetProgress(60.0, 63.0, 65.0)!!, 0.001f)
        assertEquals("还需增 2.00 kg", targetProgressText(60.0, 63.0, 65.0))
    }

    @Test
    fun targetProgress_lossGoal_reportsRemaining() {
        assertEquals("还需减 5.00 kg", targetProgressText(80.0, 75.0, 70.0))
        assertEquals(0.5f, calculateTargetProgress(80.0, 75.0, 70.0)!!, 0.001f)
    }

    @Test
    fun targetProgress_lossGoal_overshootCountsAsAchieved() {
        assertEquals(1f, calculateTargetProgress(80.0, 69.5, 70.0)!!, 0.001f)
        assertEquals("已达成", targetProgressText(80.0, 69.5, 70.0))
        assertEquals(0.5, calculateRemainingWeight(69.5, 70.0)!!, 0.001)
    }

    @Test
    fun targetProgress_gainGoal_overshootCountsAsAchieved() {
        assertEquals("已达成", targetProgressText(60.0, 66.0, 65.0))
        assertEquals(1f, calculateTargetProgress(60.0, 66.0, 65.0)!!, 0.001f)
    }

    private fun isoDaysAgo(days: Int): String {
        return Calendar.getInstance().run {
            add(Calendar.DAY_OF_YEAR, -days)
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(time)
        }
    }
}
