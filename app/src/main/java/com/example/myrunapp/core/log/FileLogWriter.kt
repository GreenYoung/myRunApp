package com.example.myrunapp.core.log

import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FileLogWriter {
    private val lock = Any()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    private var logDir: File? = null

    fun init(cacheRoot: File): File {
        return synchronized(lock) {
            val targetDir = File(cacheRoot, "log").apply { mkdirs() }
            logDir = targetDir
            deleteOldLogs()
            targetDir
        }
    }

    fun write(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
        synchronized(lock) {
            val dir = logDir ?: return
            runCatching {
                val file = resolveWritableLogFile(dir)
                val stack = throwable?.let { "\n${Log.getStackTraceString(it)}" }.orEmpty()
                file.appendText("${timeFormat.format(Date())} [$tag] ${level.name} $message$stack\n")
            }
        }
    }

    private fun resolveWritableLogFile(dir: File): File {
        val date = dateFormat.format(Date())
        for (index in 0..9) {
            val suffix = if (index == 0) "" else "-$index"
            val candidate = File(dir, "myrun-$date$suffix.log")
            if (!candidate.exists() || candidate.length() < MAX_LOG_FILE_BYTES) {
                return candidate
            }
        }
        return File(dir, "myrun-$date-9.log")
    }

    private fun deleteOldLogs() {
        val dir = logDir ?: return
        val now = System.currentTimeMillis()
        dir.listFiles { file -> file.isFile && file.name.startsWith("myrun-") && file.name.endsWith(".log") }
            ?.forEach { file ->
                if (now - file.lastModified() > RETENTION_MS) {
                    file.delete()
                }
            }
    }

    private companion object {
        private const val MAX_LOG_FILE_BYTES = 2L * 1024L * 1024L
        private const val RETENTION_MS = 7L * 24L * 60L * 60L * 1000L
    }
}
