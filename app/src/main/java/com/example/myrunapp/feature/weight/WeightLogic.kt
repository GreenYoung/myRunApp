package com.example.myrunapp.feature.weight

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private const val DATE_PATTERN = "yyyy-MM-dd"
private const val WEIGHT_ERROR = "请输入 30.00～300.00 kg 之间的体重"
private const val TARGET_WEIGHT_ERROR = "请输入 30.00～300.00 kg 之间的目标体重"

fun todayIsoDate(): String {
    return SimpleDateFormat(DATE_PATTERN, Locale.US).format(Calendar.getInstance().time)
}

fun currentYear(): Int {
    return Calendar.getInstance().get(Calendar.YEAR)
}

fun yearOfIsoDate(value: String): Int {
    return value.substringOrNull(0, 4)?.toIntOrNull() ?: currentYear()
}

fun monthOfIsoDate(value: String): Int {
    return value.substringOrNull(5, 7)?.toIntOrNull()?.coerceIn(1, 12)
        ?: Calendar.getInstance().get(Calendar.MONTH) + 1
}

fun dayOfIsoDate(value: String): Int {
    return value.substringOrNull(8, 10)?.toIntOrNull()?.coerceIn(1, 31)
        ?: Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
}

fun daysInMonth(year: Int, month: Int): Int {
    return Calendar.getInstance().run {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month - 1)
        set(Calendar.DAY_OF_MONTH, 1)
        getActualMaximum(Calendar.DAY_OF_MONTH)
    }
}

fun buildIsoDate(year: Int, month: Int, day: Int): String {
    val safeMonth = month.coerceIn(1, 12)
    val safeDay = day.coerceIn(1, daysInMonth(year, safeMonth))
    return String.format(Locale.US, "%04d-%02d-%02d", year, safeMonth, safeDay)
}

fun firstDayOffsetOfMonth(year: Int, month: Int): Int {
    return Calendar.getInstance().run {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month - 1)
        set(Calendar.DAY_OF_MONTH, 1)
        (get(Calendar.DAY_OF_WEEK) + 5) % 7
    }
}

fun previousMonth(year: Int, month: Int): YearMonth {
    return if (month == 1) YearMonth(year - 1, 12) else YearMonth(year, month - 1)
}

fun nextMonth(year: Int, month: Int): YearMonth {
    return if (month == 12) YearMonth(year + 1, 1) else YearMonth(year, month + 1)
}

fun changeIsoDateMonth(value: String, month: Int): String {
    val year = currentYear()
    val day = dayOfIsoDate(value)
    return buildIsoDate(year, month, day)
}

fun changeIsoDateDay(value: String, day: Int): String {
    return buildIsoDate(currentYear(), monthOfIsoDate(value), day)
}

fun isValidIsoDate(value: String): Boolean {
    if (!Regex("""\d{4}-\d{2}-\d{2}""").matches(value)) return false
    return try {
        SimpleDateFormat(DATE_PATTERN, Locale.US).apply {
            isLenient = false
        }.parse(value)
        true
    } catch (_: ParseException) {
        false
    }
}

fun validateWeightInput(date: String, weight: String): WeightValidationResult {
    if (!isValidIsoDate(date.trim())) {
        return WeightValidationResult(error = "请输入合法日期")
    }

    val trimmedWeight = weight.trim()
    if (!Regex("""\d+(\.\d{1,2})?""").matches(trimmedWeight)) {
        return WeightValidationResult(error = WEIGHT_ERROR)
    }

    val value = trimmedWeight.toDoubleOrNull()
        ?: return WeightValidationResult(error = WEIGHT_ERROR)

    if (value < 30.0 || value > 300.0) {
        return WeightValidationResult(error = WEIGHT_ERROR)
    }

    val normalized = BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).toDouble()
    return WeightValidationResult(weightKg = normalized)
}

fun validateTargetWeightInput(weight: String): WeightValidationResult {
    val trimmedWeight = weight.trim()
    if (!Regex("""\d+(\.\d{1,2})?""").matches(trimmedWeight)) {
        return WeightValidationResult(error = TARGET_WEIGHT_ERROR)
    }

    val value = trimmedWeight.toDoubleOrNull()
        ?: return WeightValidationResult(error = TARGET_WEIGHT_ERROR)

    if (value < 30.0 || value > 300.0) {
        return WeightValidationResult(error = TARGET_WEIGHT_ERROR)
    }

    val normalized = BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).toDouble()
    return WeightValidationResult(weightKg = normalized)
}

fun formatWeight(weightKg: Double): String {
    return String.format(Locale.US, "%.2f", weightKg)
}

fun formatSignedWeightChange(change: Double?): String {
    return change?.let { String.format(Locale.US, "%+.2f kg", it) } ?: "--"
}

fun rangeStartDate(latestDate: String?, range: WeightRange): String? {
    if (latestDate == null || range == WeightRange.ALL || !isValidIsoDate(latestDate)) return null
    val daysBack = when (range) {
        WeightRange.DAYS_7 -> 6
        WeightRange.DAYS_30 -> 29
        WeightRange.DAYS_90 -> 89
        WeightRange.ALL -> 0
    }
    return Calendar.getInstance().run {
        time = SimpleDateFormat(DATE_PATTERN, Locale.US).parse(latestDate) ?: return null
        add(Calendar.DAY_OF_YEAR, -daysBack)
        SimpleDateFormat(DATE_PATTERN, Locale.US).format(time)
    }
}

fun filterWeightPointsByRange(records: List<WeightPoint>, latestDate: String?, range: WeightRange): List<WeightPoint> {
    val startDate = rangeStartDate(latestDate, range) ?: return records
    return records.filter { it.date >= startDate }
}

fun filterWeightTrendPointsByRange(
    records: List<WeightTrendPointUiState>,
    latestDate: String?,
    range: WeightRange
): List<WeightTrendPointUiState> {
    val startDate = rangeStartDate(latestDate, range) ?: return records
    return records.filter { it.date >= startDate }
}

fun calculateTargetProgress(firstWeight: Double?, currentWeight: Double?, targetWeight: Double?): Float? {
    if (firstWeight == null || currentWeight == null || targetWeight == null) return null
    val total = firstWeight - targetWeight
    if (total <= 0.0) return null
    val completed = firstWeight - currentWeight
    return (completed / total).toFloat().coerceIn(0f, 1f)
}

fun calculateRemainingWeight(currentWeight: Double?, targetWeight: Double?): Double? {
    if (currentWeight == null || targetWeight == null) return null
    return (currentWeight - targetWeight).coerceAtLeast(0.0)
}

fun calculateWeightAxisRange(records: List<WeightPoint>): WeightAxisRange {
    if (records.isEmpty()) {
        return WeightAxisRange(60.0, 80.0)
    }

    val minWeight = records.minOf { it.weightKg }
    val maxWeight = records.maxOf { it.weightKg }
    val dataSpan = maxWeight - minWeight
    val padding = maxOf(1.0, dataSpan * 0.1)
    var min = floorToHalf(minWeight - padding)
    var max = ceilToHalf(maxWeight + padding)

    if (max - min < 2.0) {
        val center = (minWeight + maxWeight) / 2.0
        min = floorToHalf(center - 1.0)
        max = ceilToHalf(center + 1.0)
    }

    return WeightAxisRange(min, max)
}

private fun floorToHalf(value: Double): Double {
    return kotlin.math.floor(value * 2.0) / 2.0
}

private fun ceilToHalf(value: Double): Double {
    return kotlin.math.ceil(value * 2.0) / 2.0
}

data class YearMonth(
    val year: Int,
    val month: Int
)

private fun String.substringOrNull(startIndex: Int, endIndex: Int): String? {
    return if (length >= endIndex) substring(startIndex, endIndex) else null
}
