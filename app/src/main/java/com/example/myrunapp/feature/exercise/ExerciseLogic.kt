package com.example.myrunapp.feature.exercise

import com.example.myrunapp.feature.exercise.data.ExerciseRecordEntity
import com.example.myrunapp.feature.run.RunTrackPointUiModel
import com.example.myrunapp.feature.run.data.RunSessionEntity
import com.example.myrunapp.feature.run.data.RunTrackPointEntity
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

private const val DATE_PATTERN = "yyyy-MM-dd"
private const val TIME_PATTERN = "HH:mm"
private const val MAX_INCLINE_CALORIE_BONUS = 1.25

fun todayExerciseDate(): String {
    return SimpleDateFormat(DATE_PATTERN, Locale.US).format(Calendar.getInstance().time)
}

fun currentExerciseTime(): String {
    return SimpleDateFormat(TIME_PATTERN, Locale.US).format(Calendar.getInstance().time)
}

fun formatDistance(value: Double): String {
    return String.format(Locale.US, "%.2f", value)
}

fun formatCalories(value: Int): String {
    return NumberFormat.getIntegerInstance(Locale.US).format(value)
}

fun formatDuration(totalSeconds: Long): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    return if (hours > 0) {
        if (minutes > 0) {
            String.format(Locale.US, "%dh%02dmin", hours, minutes)
        } else {
            String.format(Locale.US, "%dh", hours)
        }
    } else {
        String.format(Locale.US, "%dmin", minutes)
    }
}

fun formatPace(durationSeconds: Long, distanceKm: Double): String {
    if (distanceKm <= 0.0) return "--'--\"/km"
    val paceSeconds = (durationSeconds / distanceKm).roundToInt()
    val minutes = paceSeconds / 60
    val seconds = paceSeconds % 60
    return String.format(Locale.US, "%d'%02d\"/km", minutes, seconds)
}

fun formatExerciseDateTime(startTime: Long): String {
    return SimpleDateFormat(DATE_PATTERN, Locale.US).format(startTime)
}

fun formatExerciseInputDate(startTime: Long): String {
    return SimpleDateFormat(DATE_PATTERN, Locale.US).format(startTime)
}

fun exerciseTypeText(type: ExerciseType): String {
    return when (type) {
        ExerciseType.TREADMILL -> "跑步机"
        ExerciseType.OUTDOOR_RUNNING -> "户外"
    }
}

fun parseExerciseType(value: String): ExerciseType {
    return runCatching { ExerciseType.valueOf(value) }.getOrDefault(ExerciseType.TREADMILL)
}

fun validateExerciseInput(
    date: String,
    type: ExerciseType,
    distanceKm: String,
    hours: String,
    minutes: String,
    seconds: String,
    inclinePercent: String
): ExerciseValidationResult {
    val safeDate = date.trim().ifEmpty { todayExerciseDate() }
    val startTime = parseStartTime(safeDate)
        ?: return ExerciseValidationResult(error = "请输入合法运动日期")
    val distance = distanceKm.trim().toDoubleOrNull()
        ?: return ExerciseValidationResult(error = "请输入合法里程")
    if (distance <= 0.0 || distance > 500.0) {
        return ExerciseValidationResult(error = "里程需大于 0 且不超过 500 km")
    }

    val durationHours = hours.trim().ifEmpty { "0" }.toLongOrNull() ?: return ExerciseValidationResult(error = "请输入合法运动时长")
    val durationMinutes = minutes.trim().ifEmpty { "0" }.toLongOrNull() ?: return ExerciseValidationResult(error = "请输入合法运动时长")
    val durationSecondsInput = seconds.trim().ifEmpty { "0" }.toLongOrNull() ?: return ExerciseValidationResult(error = "请输入合法运动时长")
    if (durationHours < 0L || durationMinutes < 0L || durationMinutes > 59L || durationSecondsInput < 0L || durationSecondsInput > 59L) {
        return ExerciseValidationResult(error = "请输入合法运动时长")
    }
    val durationSeconds = durationHours * 3600L + durationMinutes * 60L + durationSecondsInput
    if (durationSeconds <= 0L) {
        return ExerciseValidationResult(error = "运动时长必须大于 0")
    }

    val incline = if (type == ExerciseType.TREADMILL) {
        inclinePercent.trim().ifEmpty { "0" }.toDoubleOrNull()
            ?: return ExerciseValidationResult(error = "请输入合法坡度")
    } else {
        0.0
    }
    if (incline < 0.0 || incline > 40.0) {
        return ExerciseValidationResult(error = "坡度需在 0～40% 之间")
    }

    return ExerciseValidationResult(
        startTime = startTime,
        durationSeconds = durationSeconds,
        distanceKm = distance,
        inclinePercent = incline
    )
}

fun estimateExerciseCalories(
    weightKg: Double,
    type: ExerciseType,
    durationSeconds: Long,
    distanceKm: Double,
    inclinePercent: Double
): Int {
    if (durationSeconds <= 0L || distanceKm <= 0.0) return 0
    val hours = durationSeconds / 3600.0
    val speedKmh = distanceKm / hours
    val distanceCoefficient = calorieDistanceCoefficient(speedKmh)
    val inclineBonus = if (type == ExerciseType.TREADMILL) {
        (1.0 + inclinePercent.coerceAtLeast(0.0) * 0.0125).coerceAtMost(MAX_INCLINE_CALORIE_BONUS)
    } else {
        1.0
    }
    return (weightKg * distanceKm * distanceCoefficient * inclineBonus).roundToInt().coerceAtLeast(0)
}

private fun calorieDistanceCoefficient(speedKmh: Double): Double {
    return when {
        speedKmh < 4.0 -> 0.55
        speedKmh < 6.0 -> 0.70
        speedKmh < 8.0 -> 0.90
        speedKmh < 10.0 -> 0.98
        speedKmh < 12.0 -> 1.02
        else -> 1.06
    }
}

fun buildExerciseSummary(
    records: List<ExerciseRecordEntity>,
    weeklyGoal: Double = 40.0,
    monthlyGoal: Double = 100.0
): ExerciseSummaryUiState {
    val now = Calendar.getInstance()
    val todayStart = startOfDay(now).timeInMillis
    val tomorrowStart = Calendar.getInstance().apply {
        timeInMillis = todayStart
        add(Calendar.DAY_OF_YEAR, 1)
    }.timeInMillis
    val weekStart = startOfWeek(now).timeInMillis
    val monthStart = startOfMonth(now).timeInMillis

    val todayRecords = records.filter { it.startTime in todayStart until tomorrowStart }
    val weeklyRecords = records.filter { it.startTime >= weekStart }
    val monthlyRecords = records.filter { it.startTime >= monthStart }
    val dailyGoal = 8.0
    val todayDistance = todayRecords.sumOf { it.distanceKm }
    val weeklyDistance = weeklyRecords.sumOf { it.distanceKm }
    val monthlyDistance = monthlyRecords.sumOf { it.distanceKm }

    return ExerciseSummaryUiState(
        todayDistanceKm = todayDistance,
        dailyGoalKm = dailyGoal,
        dailyProgress = progressOf(todayDistance, dailyGoal),
        weeklyDistanceKm = weeklyDistance,
        weeklyGoalKm = weeklyGoal,
        weeklyProgress = progressOf(weeklyDistance, weeklyGoal),
        weeklyRemainingKm = (weeklyGoal - weeklyDistance).coerceAtLeast(0.0),
        weeklyGoalCompleted = weeklyGoal > 0.0 && weeklyDistance >= weeklyGoal,
        monthlyDistanceKm = monthlyDistance,
        monthlyGoalKm = monthlyGoal,
        monthlyProgress = progressOf(monthlyDistance, monthlyGoal),
        monthlyProgressPercent = progressPercentOf(monthlyDistance, monthlyGoal),
        monthlyGoalCompleted = monthlyGoal > 0.0 && monthlyDistance >= monthlyGoal,
        totalDistanceKm = records.sumOf { it.distanceKm },
        checkInDays = records.map { dateKey(it.startTime) }.toSet().size,
        streakDays = calculateExerciseStreakDays(records, now),
        todayCaloriesKcal = todayRecords.sumOf { it.caloriesKcal },
        weeklyCaloriesKcal = weeklyRecords.sumOf { it.caloriesKcal },
        monthlyCaloriesKcal = monthlyRecords.sumOf { it.caloriesKcal },
        checkedInToday = todayRecords.isNotEmpty()
    )
}

fun buildExerciseDetail(
    records: List<ExerciseRecordEntity>,
    runSessions: List<RunSessionEntity> = emptyList(),
    runTrackPoints: List<RunTrackPointEntity> = emptyList(),
    selectedStatsRange: ExerciseStatsRange = ExerciseStatsRange.WEEK
): ExerciseDetailUiState {
    val summary = buildExerciseSummary(records)
    val sessionsByExerciseRecord = runSessions.associateBy { it.exerciseRecordId }
    val pointsBySession = runTrackPoints.groupBy { it.sessionId }
    return ExerciseDetailUiState(
        totalDistanceKm = summary.totalDistanceKm,
        checkInDays = summary.checkInDays,
        weeklyDistanceKm = summary.weeklyDistanceKm,
        monthlyDistanceKm = summary.monthlyDistanceKm,
        totalCaloriesKcal = records.sumOf { it.caloriesKcal },
        monthlyCaloriesKcal = summary.monthlyCaloriesKcal,
        selectedStatsRange = selectedStatsRange,
        rangeStats = buildExerciseRangeStats(records, selectedStatsRange),
        records = records.sortedByDescending { it.startTime }.map {
            val type = parseExerciseType(it.type)
            val session = sessionsByExerciseRecord[it.id]
            val trackPoints = session?.let { runSession ->
                pointsBySession[runSession.id].orEmpty().map { point ->
                    point.toRunTrackPointUiModel()
                }
            }.orEmpty()
            ExerciseRecordUiModel(
                id = it.id,
                type = type,
                startTime = it.startTime,
                durationSeconds = it.durationSeconds,
                distanceKm = it.distanceKm,
                typeText = exerciseTypeText(type),
                dateTimeText = formatExerciseDateTime(it.startTime),
                distanceText = "${formatDistance(it.distanceKm)} km",
                durationText = formatDuration(it.durationSeconds),
                paceText = formatPace(it.durationSeconds, it.distanceKm),
                caloriesText = "${formatCalories(it.caloriesKcal)} kcal",
                runSessionId = session?.id,
                trackPoints = trackPoints
            )
        }
    )
}

fun ExerciseRecordEntity.toExerciseRecordUiModel(
    runSession: RunSessionEntity? = null,
    trackPoints: List<RunTrackPointEntity> = emptyList()
): ExerciseRecordUiModel {
    val type = parseExerciseType(type)
    return ExerciseRecordUiModel(
        id = id,
        type = type,
        startTime = startTime,
        durationSeconds = durationSeconds,
        distanceKm = distanceKm,
        typeText = exerciseTypeText(type),
        dateTimeText = formatExerciseDateTime(startTime),
        distanceText = "${formatDistance(distanceKm)} km",
        durationText = formatDuration(durationSeconds),
        paceText = formatPace(durationSeconds, distanceKm),
        caloriesText = "${formatCalories(caloriesKcal)} kcal",
        runSessionId = runSession?.id,
        trackPoints = trackPoints.map { it.toRunTrackPointUiModel() }
    )
}

fun buildExerciseRangeStats(
    records: List<ExerciseRecordEntity>,
    range: ExerciseStatsRange
): ExerciseRangeStatsUiState {
    val now = Calendar.getInstance()
    val nowMillis = now.timeInMillis
    val startMillis = when (range) {
        ExerciseStatsRange.WEEK -> startOfWeek(now).timeInMillis
        ExerciseStatsRange.MONTH -> startOfMonth(now).timeInMillis
        ExerciseStatsRange.YEAR -> startOfYear(now).timeInMillis
        ExerciseStatsRange.ALL -> Long.MIN_VALUE
    }
    val rangeRecords = records.filter {
        it.startTime >= startMillis && it.startTime <= nowMillis
    }
    val totalDistance = rangeRecords.sumOf { it.distanceKm }
    val totalDurationSeconds = rangeRecords.sumOf { it.durationSeconds }

    return ExerciseRangeStatsUiState(
        range = range,
        totalDistanceKm = totalDistance,
        totalCaloriesKcal = rangeRecords.sumOf { it.caloriesKcal },
        checkInDays = rangeRecords.map { dateKey(it.startTime) }.toSet().size,
        averagePaceText = formatPace(totalDurationSeconds, totalDistance),
        longestDistanceKm = rangeRecords.maxOfOrNull { it.distanceKm } ?: 0.0
    )
}

private fun progressOf(value: Double, goal: Double): Float {
    if (goal <= 0.0) return 0f
    return (value / goal).toFloat().coerceIn(0f, 1f)
}

private fun progressPercentOf(value: Double, goal: Double): Int {
    if (goal <= 0.0) return 0
    return ((value / goal) * 100.0).roundToInt().coerceIn(0, 100)
}

private fun calculateExerciseStreakDays(
    records: List<ExerciseRecordEntity>,
    now: Calendar = Calendar.getInstance()
): Int {
    val recordDates = records.map { dateKey(it.startTime) }.toSet()
    if (recordDates.isEmpty()) return 0

    val cursor = startOfDay(now)
    val todayKey = dateKey(cursor.timeInMillis)
    if (todayKey !in recordDates) {
        cursor.add(Calendar.DAY_OF_YEAR, -1)
    }

    var streak = 0
    while (dateKey(cursor.timeInMillis) in recordDates) {
        streak += 1
        cursor.add(Calendar.DAY_OF_YEAR, -1)
    }
    return streak
}

private fun parseStartTime(date: String): Long? {
    return runCatching {
        SimpleDateFormat(DATE_PATTERN, Locale.US).apply {
            isLenient = false
        }.parse(date)?.time
    }.getOrNull()
}

private fun startOfDay(calendar: Calendar): Calendar {
    return Calendar.getInstance().apply {
        timeInMillis = calendar.timeInMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
}

private fun startOfWeek(calendar: Calendar): Calendar {
    return startOfDay(calendar).apply {
        firstDayOfWeek = Calendar.MONDAY
        set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
    }
}

private fun startOfMonth(calendar: Calendar): Calendar {
    return startOfDay(calendar).apply {
        set(Calendar.DAY_OF_MONTH, 1)
    }
}

private fun startOfYear(calendar: Calendar): Calendar {
    return startOfDay(calendar).apply {
        set(Calendar.DAY_OF_YEAR, 1)
    }
}

private fun dateKey(timeMillis: Long): String {
    return SimpleDateFormat(DATE_PATTERN, Locale.US).format(timeMillis)
}

private fun RunTrackPointEntity.toRunTrackPointUiModel(): RunTrackPointUiModel {
    return RunTrackPointUiModel(
        latitude = latitude,
        longitude = longitude,
        coordinateSystem = coordinateSystem,
        accuracyMeters = accuracyMeters,
        altitudeMeters = altitudeMeters,
        speedMetersPerSecond = speedMetersPerSecond,
        recordedAt = recordedAt
    )
}
