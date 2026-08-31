package com.example.myrunapp.core.log

import android.content.Context
import android.util.Log
import com.example.myrunapp.BuildConfig

object AppLogger {
    private val fileLogWriter = FileLogWriter()

    fun init(context: Context) {
        if (!BuildConfig.ENABLE_FILE_LOG) return
        fileLogWriter.init(context.applicationContext.cacheDir)
        i(LogTags.APP, "file logging initialized dir=${context.applicationContext.cacheDir.absolutePath}/logs")
    }

    fun d(tag: String, message: String) {
        Log.d(tag, message)
        write(LogLevel.D, tag, message, null)
    }

    fun i(tag: String, message: String) {
        Log.i(tag, message)
        write(LogLevel.I, tag, message, null)
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        Log.w(tag, message, throwable)
        write(LogLevel.W, tag, message, throwable)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        Log.e(tag, message, throwable)
        write(LogLevel.E, tag, message, throwable)
    }

    private fun write(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
        if (!BuildConfig.ENABLE_FILE_LOG) return
        fileLogWriter.write(level, tag, message, throwable)
    }
}
