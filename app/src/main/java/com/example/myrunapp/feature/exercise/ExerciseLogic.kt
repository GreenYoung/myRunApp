package com.example.myrunapp.feature.exercise

import com.example.myrunapp.core.util.AppDateFormats
import com.example.myrunapp.feature.exercise.data.ExerciseRecordEntity
import com.example.myrunapp.feature.run.RunTrackPointUiModel
import com.example.myrunapp.feature.run.data.RunSessionEntity
import com.example.myrunapp.feature.run.data.RunTrackPointEntity
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

private const val MAX_INCLINE_CALORIE_BONUS = 1.25

fun todayExerciseDate(): String {
    return AppDateFormats.formatIsoDate(System.currentTimeMillis())
}

fun currentExerciseTime(): String {
    return AppDateFormats.formatTimeOfDay(System.currentTimeMillis())
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

fun exerciseDateTimeWithOriginalClockTime(date: String, originalStartTime: Long): Long? {
    val dateMillis = parseStartTime(date.trim().ifEmpty { todayExerciseDate() }) ?: return null
    val original = Calendar.getInstance().apply { timeInMillis = originalStartTime }
    return Calendar.getInstance().apply {
        timeInMillis = dateMillis
        set(Calendar.HOUR_OF_DAY, original.get(Calendar.HOUR_OF_DAY))
        set(Calendar.MINUTE, original.get(Calendar.MINUTE))
        set(Calendar.SECOND, original.get(Calendar.SECOND))
        set(Calendar.MILLISECOND, original.get(Calendar.MILLISECOND))
    }.timeInMillis
}

private fun parseStartTime(date: String): Long? {
    return AppDateFormats.parseIsoDateStrict(date)
}

fun formatDistance(value: Double): String {
    return String.format(Locale.US, "%.2f", value)
}

fun formatCalories(value: Int): String {
    return NumberFormat.getIntegerInstance(Locale.US).format(value)
}

fun formatCaloriesCompact(value: Int): String {
    return if (value >= 10_000) {
        String.format(Locale.US, "%.1fk", value / 1000.0)
    } else {
        formatCalories(value)
    }
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

fun formatExerciseDurationDetail(totalSeconds: Long): String {
    if (totalSeconds <= 0L) return "0 秒"
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    val parts = mutableListOf<String>()
    if (hours > 0) parts += "$hours 小时"
    if (minutes > 0) parts += "$minutes 分"
    if (seconds > 0 || parts.isEmpty()) parts += "$seconds 秒"
    return parts.joinToString(" ")
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
    return AppDateFormats.formatIsoDate(startTime)
}

fun formatExerciseInputDate(startTime: Long): String {
    return AppDateFormats.formatIsoDate(startTime)
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
    if (startTime > startOfDay(Calendar.getInstance()).timeInMillis) {
        return ExerciseValidationResult(error = "运动日期不能晚于今天")
    }
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
        inclinePercent = inclinePercent,
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

internal fun RunTrackPointEntity.toRunTrackPointUiModel(): RunTrackPointUiModel {
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
