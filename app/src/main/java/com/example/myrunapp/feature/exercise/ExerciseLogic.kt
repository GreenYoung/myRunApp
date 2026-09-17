package com.example.myrunapp.feature.exercise

import com.example.myrunapp.feature.exercise.data.ExerciseRecordEntity
import com.example.myrunapp.feature.run.RunTrackPointUiModel
import com.example.myrunapp.feature.run.data.RunSessionEntity
import com.example.myrunapp.feature.run.data.RunTrackPointEntity
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.LinkedHashMap
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

fun exerciseDateWithCurrentClockTime(date: String): Long? {
    val dateMillis = parseStartTime(date.trim().ifEmpty { todayExerciseDate() }) ?: return null
    val now = Calendar.getInstance()
    return Calendar.getInstance().apply {
        timeInMillis = dateMillis
        set(Calendar.HOUR_OF_DAY, now.get(Calendar.HOUR_OF_DAY))
        set(Calendar.MINUTE, now.get(Calendar.MINUTE))
        set(Calendar.SECOND, now.get(Calendar.SECOND))
        set(Calendar.MILLISECOND, now.get(Calendar.MILLISECOND))
    }.timeInMillis
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
    return formatPaceSeconds(paceSeconds.toLong())
}

fun formatPaceSeconds(paceSeconds: Long?): String {
    if (paceSeconds == null || paceSeconds <= 0L) return "--'--\"/km"
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
        trend = buildExerciseTrend(records, selectedStatsRange),
        personalBest = buildExercisePersonalBest(records),
        consistency = buildExerciseConsistency(records),
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

fun buildExerciseTrend(
    records: List<ExerciseRecordEntity>,
    range: ExerciseStatsRange
): ExerciseTrendUiState {
    val now = Calendar.getInstance()
    val buckets = buildTrendBuckets(records, range, now)
    records.forEach { record ->
        val bucketKey = trendBucketKey(record.startTime, range)
        buckets[bucketKey]?.records?.add(record)
    }

    return ExerciseTrendUiState(
        range = range,
        points = buckets.values.map { bucket ->
            val totalDistance = bucket.records.sumOf { it.distanceKm }
            val totalDuration = bucket.records.sumOf { it.durationSeconds }
            ExerciseTrendPointUiState(
                label = bucket.label,
                distanceKm = totalDistance,
                durationSeconds = totalDuration,
                averagePaceSecondsPerKm = averagePaceSeconds(totalDuration, totalDistance)
            )
        }
    )
}

fun buildExercisePersonalBest(records: List<ExerciseRecordEntity>): ExercisePersonalBestUiState {
    val validDistanceRecords = records.filter { it.distanceKm > 0.0 && it.durationSeconds > 0L }
    val longestDistance = validDistanceRecords.maxByOrNull { it.distanceKm }
    val fastestOneKm = validDistanceRecords
        .filter { it.distanceKm >= 1.0 }
        .minByOrNull { it.durationSeconds / it.distanceKm }
    val fastestFiveKm = validDistanceRecords
        .filter { it.distanceKm >= 5.0 }
        .minByOrNull { it.durationSeconds / it.distanceKm }
    val highestCalories = records.maxByOrNull { it.caloriesKcal }

    return ExercisePersonalBestUiState(
        longestDistance = longestDistance?.let {
            ExercisePersonalBestItemUiState(
                value = "${formatDistance(it.distanceKm)} km",
                dateText = formatExerciseDateTime(it.startTime)
            )
        } ?: ExercisePersonalBestItemUiState(value = "0.00 km"),
        fastestOneKm = fastestOneKm?.let {
            ExercisePersonalBestItemUiState(
                value = formatPace(it.durationSeconds, it.distanceKm),
                dateText = formatExerciseDateTime(it.startTime),
                subtitle = "基于单次平均配速"
            )
        } ?: ExercisePersonalBestItemUiState(value = "--", subtitle = "基于单次平均配速"),
        fastestFiveKm = fastestFiveKm?.let {
            ExercisePersonalBestItemUiState(
                value = formatPace(it.durationSeconds, it.distanceKm),
                dateText = formatExerciseDateTime(it.startTime),
                subtitle = "基于单次平均配速"
            )
        } ?: ExercisePersonalBestItemUiState(value = "--", subtitle = "基于单次平均配速"),
        highestCalories = highestCalories?.let {
            ExercisePersonalBestItemUiState(
                value = "${formatCalories(it.caloriesKcal)} kcal",
                dateText = formatExerciseDateTime(it.startTime)
            )
        } ?: ExercisePersonalBestItemUiState(value = "0 kcal")
    )
}

fun buildExerciseConsistency(records: List<ExerciseRecordEntity>): ExerciseConsistencyUiState {
    val now = Calendar.getInstance()
    val latestRecord = records.maxByOrNull { it.startTime }
    val monthStart = startOfMonth(now).timeInMillis
    val nowMillis = now.timeInMillis
    val monthlyActiveDays = records
        .filter { it.startTime in monthStart..nowMillis }
        .map { dateKey(it.startTime) }
        .toSet()
        .size

    return ExerciseConsistencyUiState(
        streakDays = calculateExerciseStreakDays(records, now),
        monthlyActiveDays = monthlyActiveDays,
        latestCheckInDateText = latestRecord?.let { formatExerciseDateTime(it.startTime) } ?: "--"
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

fun buildExerciseStatsDetail(
    records: List<ExerciseRecordEntity>,
    period: ExerciseStatsPeriod,
    periodOffset: Int = 0
): ExerciseStatsDetailUiState {
    val now = Calendar.getInstance()
    val nowMillis = now.timeInMillis
    val safeOffset = periodOffset.coerceAtMost(0)
    val periodRange = buildStatsPeriodRange(period, safeOffset, now)
    val startMillis = periodRange.first
    val endMillis = periodRange.second.coerceAtMost(nowMillis)
    val periodRecords = records.filter {
        it.startTime >= startMillis && it.startTime <= endMillis
    }
    val totalDistance = periodRecords.sumOf { it.distanceKm }
    val totalDurationSeconds = periodRecords.sumOf { it.durationSeconds }
    val recordCount = periodRecords.size
    val fastestRecord = periodRecords
        .filter { it.distanceKm > 0.0 && it.durationSeconds > 0L }
        .maxByOrNull { it.distanceKm / (it.durationSeconds / 3600.0) }
    val fastestSpeedKmh = fastestRecord?.let {
        it.distanceKm / (it.durationSeconds / 3600.0)
    } ?: 0.0

    return ExerciseStatsDetailUiState(
        period = period,
        periodOffset = safeOffset,
        title = period.statsTitle,
        rangeText = buildStatsRangeText(startMillis, endMillis),
        canGoNext = safeOffset < 0,
        canGoPrevious = true,
        totalDistanceKm = totalDistance,
        totalDurationSeconds = totalDurationSeconds,
        recordCount = recordCount,
        checkInDays = periodRecords.map { dateKey(it.startTime) }.toSet().size,
        averageDistanceKm = if (recordCount > 0) totalDistance / recordCount else 0.0,
        averagePaceText = formatPace(totalDurationSeconds, totalDistance),
        fastestSpeedKmh = fastestSpeedKmh,
        longestDistanceKm = periodRecords.maxOfOrNull { it.distanceKm } ?: 0.0,
        highestCaloriesKcal = periodRecords.maxOfOrNull { it.caloriesKcal } ?: 0
    )
}

private fun buildStatsPeriodRange(
    period: ExerciseStatsPeriod,
    offset: Int,
    now: Calendar
): Pair<Long, Long> {
    val start = when (period) {
        ExerciseStatsPeriod.WEEK -> startOfWeek(now).apply { add(Calendar.WEEK_OF_YEAR, offset) }
        ExerciseStatsPeriod.MONTH -> startOfMonth(now).apply { add(Calendar.MONTH, offset) }
        ExerciseStatsPeriod.YEAR -> startOfYear(now).apply { add(Calendar.YEAR, offset) }
    }
    val nextStart = Calendar.getInstance().apply {
        timeInMillis = start.timeInMillis
        when (period) {
            ExerciseStatsPeriod.WEEK -> add(Calendar.WEEK_OF_YEAR, 1)
            ExerciseStatsPeriod.MONTH -> add(Calendar.MONTH, 1)
            ExerciseStatsPeriod.YEAR -> add(Calendar.YEAR, 1)
        }
    }
    return start.timeInMillis to (nextStart.timeInMillis - 1L)
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

private data class ExerciseTrendBucket(
    val key: String,
    val label: String,
    val records: MutableList<ExerciseRecordEntity> = mutableListOf()
)

private fun buildTrendBuckets(
    records: List<ExerciseRecordEntity>,
    range: ExerciseStatsRange,
    now: Calendar
): LinkedHashMap<String, ExerciseTrendBucket> {
    val buckets = LinkedHashMap<String, ExerciseTrendBucket>()
    when (range) {
        ExerciseStatsRange.WEEK -> {
            val cursor = startOfWeek(now)
            repeat(7) {
                val key = dateKey(cursor.timeInMillis)
                buckets[key] = ExerciseTrendBucket(key = key, label = weekDayLabel(cursor))
                cursor.add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        ExerciseStatsRange.MONTH -> {
            val cursor = startOfMonth(now)
            val maxDay = now.get(Calendar.DAY_OF_MONTH)
            repeat(maxDay) {
                val key = dateKey(cursor.timeInMillis)
                buckets[key] = ExerciseTrendBucket(
                    key = key,
                    label = cursor.get(Calendar.DAY_OF_MONTH).toString()
                )
                cursor.add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        ExerciseStatsRange.YEAR -> {
            val cursor = startOfYear(now)
            repeat(12) {
                val key = monthKey(cursor.timeInMillis)
                buckets[key] = ExerciseTrendBucket(
                    key = key,
                    label = "${cursor.get(Calendar.MONTH) + 1}月"
                )
                cursor.add(Calendar.MONTH, 1)
            }
        }
        ExerciseStatsRange.ALL -> {
            records.sortedBy { it.startTime }.forEach { record ->
                val calendar = Calendar.getInstance().apply { timeInMillis = record.startTime }
                val key = monthKey(record.startTime)
                buckets.getOrPut(key) {
                    ExerciseTrendBucket(
                        key = key,
                        label = "${calendar.get(Calendar.YEAR)}-${String.format(Locale.US, "%02d", calendar.get(Calendar.MONTH) + 1)}"
                    )
                }
            }
        }
    }
    return buckets
}

private fun trendBucketKey(timeMillis: Long, range: ExerciseStatsRange): String {
    return when (range) {
        ExerciseStatsRange.WEEK,
        ExerciseStatsRange.MONTH -> dateKey(timeMillis)
        ExerciseStatsRange.YEAR,
        ExerciseStatsRange.ALL -> monthKey(timeMillis)
    }
}

private fun averagePaceSeconds(durationSeconds: Long, distanceKm: Double): Long? {
    if (durationSeconds <= 0L || distanceKm <= 0.0) return null
    return (durationSeconds / distanceKm).roundToInt().toLong()
}

private fun weekDayLabel(calendar: Calendar): String {
    return when (calendar.get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY -> "一"
        Calendar.TUESDAY -> "二"
        Calendar.WEDNESDAY -> "三"
        Calendar.THURSDAY -> "四"
        Calendar.FRIDAY -> "五"
        Calendar.SATURDAY -> "六"
        else -> "日"
    }
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

private fun monthKey(timeMillis: Long): String {
    return SimpleDateFormat("yyyy-MM", Locale.US).format(timeMillis)
}

private val ExerciseStatsPeriod.statsTitle: String
    get() = when (this) {
        ExerciseStatsPeriod.WEEK -> "周统计"
        ExerciseStatsPeriod.MONTH -> "月统计"
        ExerciseStatsPeriod.YEAR -> "年统计"
    }

private fun buildStatsRangeText(startMillis: Long, endMillis: Long): String {
    val formatter = SimpleDateFormat(DATE_PATTERN, Locale.US)
    return "${formatter.format(Date(startMillis))} 至 ${formatter.format(Date(endMillis))}"
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
