package com.example.myrunapp.core.log

import android.util.Log
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

/**
 * Writes logs on a single background thread with a buffered writer, so callers
 * (GPS callbacks, Compose effects, ViewModels) never block on file I/O.
 */
class FileLogWriter {
    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "file-log-writer").apply { isDaemon = true }
    }
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    @Volatile
    private var logDir: File? = null

    private var writer: BufferedWriter? = null
    private var currentDate: String? = null
    private var writtenBytes = 0L
    private var linesSinceFlush = 0

    fun init(cacheRoot: File): File {
        val targetDir = File(cacheRoot, "log").apply { mkdirs() }
        logDir = targetDir
        executor.execute { runCatching { deleteOldLogs(targetDir) } }
        return targetDir
    }

    fun write(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
        val dir = logDir ?: return
        executor.execute {
            runCatching {
                val stack = throwable?.let { "\n${Log.getStackTraceString(it)}" }.orEmpty()
                val line = "${timeFormat.format(Date())} [$tag] ${level.name} $message$stack\n"
                writeLine(dir, line, level)
            }
        }
    }

    private fun writeLine(dir: File, line: String, level: LogLevel) {
        val today = dateFormat.format(Date())
        if (writer == null || currentDate != today || writtenBytes >= MAX_LOG_FILE_BYTES) {
            rollFile(dir, today)
        }
        val target = writer ?: return
        target.write(line)
        writtenBytes += line.toByteArray(Charsets.UTF_8).size
        linesSinceFlush += 1
        if (level == LogLevel.W || level == LogLevel.E || linesSinceFlush >= FLUSH_EVERY_LINES) {
            target.flush()
            linesSinceFlush = 0
        }
    }

    private fun rollFile(dir: File, date: String) {
        closeWriter()
        val file = resolveWritableLogFile(dir, date)
        currentDate = date
        writtenBytes = file.length()
        writer = runCatching { BufferedWriter(FileWriter(file, true)) }.getOrNull()
    }

    private fun closeWriter() {
        runCatching { writer?.flush() }
        runCatching { writer?.close() }
        writer = null
        currentDate = null
        linesSinceFlush = 0
    }

    private fun resolveWritableLogFile(dir: File, date: String): File {
        for (index in 0..9) {
            val suffix = if (index == 0) "" else "-$index"
            val candidate = File(dir, "myrun-$date$suffix.log")
            if (!candidate.exists() || candidate.length() < MAX_LOG_FILE_BYTES) {
                return candidate
            }
        }
        return File(dir, "myrun-$date-9.log")
    }

    private fun deleteOldLogs(dir: File) {
        val now = System.currentTimeMillis()
        val today = dateFormat.format(Date(now))
        dir.listFiles { file -> file.isFile && file.name.startsWith("myrun-") && file.name.endsWith(".log") }
            ?.forEach { file ->
                val isToday = file.name.startsWith("myrun-$today")
                if (!isToday && now - file.lastModified() > RETENTION_MS) {
                    file.delete()
                }
            }
    }

    private companion object {
        private const val MAX_LOG_FILE_BYTES = 2L * 1024L * 1024L
        private const val RETENTION_MS = 7L * 24L * 60L * 60L * 1000L
        private const val FLUSH_EVERY_LINES = 10
    }
}
