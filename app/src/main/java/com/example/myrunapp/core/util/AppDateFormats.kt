package com.example.myrunapp.core.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * 进程内缓存的日期格式化工具。
 *
 * SimpleDateFormat 不是线程安全的，因此每个 pattern/locale 组合用 ThreadLocal 缓存：
 * 既避免按调用、按记录反复创建对象，也不会在多线程（UI/IO/日志线程）之间串用同一个实例。
 */
object AppDateFormats {
    const val ISO_DATE = "yyyy-MM-dd"
    const val ISO_MONTH = "yyyy-MM"
    const val TIME_OF_DAY = "HH:mm"

    private class CachedFormatter(
        private val pattern: String,
        private val locale: Locale,
        private val lenient: Boolean
    ) : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat {
            return SimpleDateFormat(pattern, locale).apply { isLenient = lenient }
        }
    }

    private val formatters = ConcurrentHashMap<String, CachedFormatter>()

    private fun formatter(pattern: String, locale: Locale, lenient: Boolean): SimpleDateFormat {
        val key = "$pattern|${locale.toLanguageTag()}|$lenient"
        return formatters.getOrPut(key) { CachedFormatter(pattern, locale, lenient) }.get()
            ?: error("formatter unavailable for pattern $pattern")
    }

    fun format(pattern: String, timeMillis: Long, locale: Locale = Locale.US): String {
        return formatter(pattern, locale, lenient = true).format(Date(timeMillis))
    }

    fun formatIsoDate(timeMillis: Long): String = format(ISO_DATE, timeMillis)

    fun formatIsoMonth(timeMillis: Long): String = format(ISO_MONTH, timeMillis)

    fun formatTimeOfDay(timeMillis: Long): String = format(TIME_OF_DAY, timeMillis)

    /** 严格解析 yyyy-MM-dd，格式非法时返回 null。 */
    fun parseIsoDateStrict(value: String): Long? {
        return runCatching { formatter(ISO_DATE, Locale.US, lenient = false).parse(value)?.time }
            .getOrNull()
    }
}
