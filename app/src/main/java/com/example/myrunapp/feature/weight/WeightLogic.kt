package com.example.myrunapp.feature.weight

import com.example.myrunapp.feature.exercise.data.ExerciseRecordEntity
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private const val DATE_PATTERN = "yyyy-MM-dd"
private const val WEIGHT_ERROR = "请输入 30.00～300.00 kg 之间的体重"
private const val TARGET_WEIGHT_ERROR = "请输入 30.00～300.00 kg 之间的目标体重"
private const val HEIGHT_ERROR = "请输入 100.0～250.0 cm 之间的身高"

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

fun validateWeightSettingsInput(targetWeight: String, heightCm: String): WeightSettingsValidationResult {
    val targetValidation = validateTargetWeightInput(targetWeight)
    if (!targetValidation.isValid) {
        return WeightSettingsValidationResult(error = targetValidation.error)
    }

    val trimmedHeight = heightCm.trim()
    val height = if (trimmedHeight.isEmpty()) {
        null
    } else {
        if (!Regex("""\d+(\.\d{1,2})?""").matches(trimmedHeight)) {
            return WeightSettingsValidationResult(error = HEIGHT_ERROR)
        }
        val value = trimmedHeight.toDoubleOrNull()
            ?: return WeightSettingsValidationResult(error = HEIGHT_ERROR)
        if (value < 100.0 || value > 250.0) {
            return WeightSettingsValidationResult(error = HEIGHT_ERROR)
        }
        BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).toDouble()
    }

    return WeightSettingsValidationResult(
        targetWeightKg = targetValidation.weightKg,
        heightCm = height
    )
}

fun formatWeight(weightKg: Double): String {
    return String.format(Locale.US, "%.2f", weightKg)
}

fun formatHeightCm(heightCm: Double): String {
    return String.format(Locale.US, "%.1f", heightCm)
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

fun calculateBmiUiState(latestWeight: Double?, heightCm: Double?): WeightBmiUiState {
    if (latestWeight == null || heightCm == null || heightCm <= 0.0) {
        return WeightBmiUiState(heightCm = heightCm)
    }
    val heightM = heightCm / 100.0
    val bmi = BigDecimal.valueOf(latestWeight / (heightM * heightM))
        .setScale(1, RoundingMode.HALF_UP)
        .toDouble()
    val category = when {
        bmi < 18.5 -> "偏低"
        bmi < 24.0 -> "正常"
        bmi < 28.0 -> "偏高"
        else -> "肥胖"
    }
    return WeightBmiUiState(heightCm = heightCm, bmi = bmi, category = category)
}

fun buildWeightChangeSpeed(
    records: List<WeightPoint>,
    latestDate: String?,
    range: WeightRange
): WeightChangeSpeedUiState {
    val filtered = filterWeightPointsByRange(records, latestDate, range).sortedBy { it.date }
    val label = range.speedLabel
    if (filtered.size < 2) {
        return WeightChangeSpeedUiState(range = range, label = label, description = "数据不足")
    }

    val first = filtered.first()
    val last = filtered.last()
    val days = daysBetween(first.date, last.date).coerceAtLeast(1)
    val weeklyChange = (last.weightKg - first.weightKg) / days * 7.0
    val direction = when {
        weeklyChange < 0.0 -> "↓"
        weeklyChange > 0.0 -> "↑"
        else -> ""
    }
    return WeightChangeSpeedUiState(
        range = range,
        weeklyChangeKg = BigDecimal.valueOf(weeklyChange).setScale(2, RoundingMode.HALF_UP).toDouble(),
        label = label,
        description = "$direction ${formatWeight(kotlin.math.abs(weeklyChange))} kg/周".trim()
    )
}

fun buildWeightExerciseCorrelation(
    weightRecords: List<WeightPoint>,
    exerciseRecords: List<ExerciseRecordEntity>,
    latestDate: String?
): WeightExerciseCorrelationUiState {
    val change7 = weightChangeInRange(weightRecords, latestDate, WeightRange.DAYS_7)
    val change30 = weightChangeInRange(weightRecords, latestDate, WeightRange.DAYS_30)
    val start7 = rangeStartDate(latestDate, WeightRange.DAYS_7)
    val start30 = rangeStartDate(latestDate, WeightRange.DAYS_30)
    val distance7 = exerciseRecords.distanceFrom(start7)
    val distance30 = exerciseRecords.distanceFrom(start30)
    val calories7 = exerciseRecords
        .filterByIsoStart(start7)
        .sumOf { it.caloriesKcal }
    val insight = when {
        weightRecords.size < 2 -> "体重数据不足"
        distance7 <= 0.0 && distance30 <= 0.0 -> "暂无运动数据关联"
        change7 != null && change7 < 0.0 -> "最近运动后体重呈下降趋势"
        change7 != null && change7 > 0.0 -> "有运动记录，但体重仍在上升"
        change30 != null && change30 < 0.0 -> "近30天体重整体下降"
        else -> "运动和体重变化较平稳"
    }

    return WeightExerciseCorrelationUiState(
        sevenDayDistanceKm = distance7,
        sevenDayCaloriesKcal = calories7,
        sevenDayWeightChangeKg = change7,
        thirtyDayDistanceKm = distance30,
        thirtyDayWeightChangeKg = change30,
        insight = insight
    )
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

private val WeightRange.speedLabel: String
    get() = when (this) {
        WeightRange.DAYS_7 -> "近7天"
        WeightRange.DAYS_30 -> "近30天"
        WeightRange.DAYS_90 -> "近90天"
        WeightRange.ALL -> "全部"
    }

private fun weightChangeInRange(records: List<WeightPoint>, latestDate: String?, range: WeightRange): Double? {
    val filtered = filterWeightPointsByRange(records, latestDate, range).sortedBy { it.date }
    return if (filtered.size >= 2) filtered.last().weightKg - filtered.first().weightKg else null
}

private fun daysBetween(startDate: String, endDate: String): Int {
    return try {
        val parser = SimpleDateFormat(DATE_PATTERN, Locale.US).apply { isLenient = false }
        val start = parser.parse(startDate)?.time ?: return 1
        val end = parser.parse(endDate)?.time ?: return 1
        (((end - start) / (24L * 60L * 60L * 1000L)).toInt()).coerceAtLeast(1)
    } catch (_: ParseException) {
        1
    }
}

private fun ExerciseRecordEntity.isoStartDate(): String {
    return SimpleDateFormat(DATE_PATTERN, Locale.US).format(Date(startTime))
}

private fun List<ExerciseRecordEntity>.filterByIsoStart(startDate: String?): List<ExerciseRecordEntity> {
    if (startDate == null) return this
    return filter { it.isoStartDate() >= startDate }
}

private fun List<ExerciseRecordEntity>.distanceFrom(startDate: String?): Double {
    return filterByIsoStart(startDate).sumOf { it.distanceKm }
}

data class YearMonth(
    val year: Int,
    val month: Int
)

private fun String.substringOrNull(startIndex: Int, endIndex: Int): String? {
    return if (length >= endIndex) substring(startIndex, endIndex) else null
}
