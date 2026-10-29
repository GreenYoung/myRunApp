package com.example.myrunapp.feature.run

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import com.example.myrunapp.core.log.AppLogger
import com.example.myrunapp.core.log.LogTags

private const val PREFS_NAME = "run_tracking_prefs"
private const val KEY_BATTERY_PROMPTED = "battery_optimization_prompted"

internal fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return powerManager.isIgnoringBatteryOptimizations(context.packageName)
}

/**
 * 只在首次（未提示过）且尚未加入白名单时提示，避免每次开始跑步都打扰用户。
 */
internal fun shouldPromptBatteryOptimization(context: Context): Boolean {
    if (isIgnoringBatteryOptimizations(context)) return false
    return !context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getBoolean(KEY_BATTERY_PROMPTED, false)
}

internal fun markBatteryOptimizationPrompted(context: Context) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putBoolean(KEY_BATTERY_PROMPTED, true)
        .apply()
}

internal fun openBatteryOptimizationSettings(context: Context) {
    markBatteryOptimizationPrompted(context)
    val packageUri = Uri.parse("package:${context.packageName}")
    val direct = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, packageUri)
    val opened = runCatching { context.startActivity(direct) }.isSuccess
    AppLogger.i(LogTags.RUN, "battery optimization settings opened direct=$opened")
    if (opened) return
    val fallback = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
    runCatching { context.startActivity(fallback) }
        .onFailure { AppLogger.w(LogTags.RUN, "battery optimization settings unavailable error=${it.message}") }
}
