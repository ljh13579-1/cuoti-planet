package com.cuotixingqiu.app.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 时间工具：负责“今天”的计算和界面上的时间显示（使用手机所在时区）。
 *
 * 注意：项目最低支持 Android 8.0（API 26），因此可以直接使用 java.time。
 */
object TimeUtils {

    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    private fun zone(): ZoneId = ZoneId.systemDefault()

    /** 例如 2026-10-17 */
    fun formatDate(millis: Long): String =
        Instant.ofEpochMilli(millis).atZone(zone()).format(dateFormatter)

    /** 例如 2026-10-17 20:35 */
    fun formatDateTime(millis: Long): String =
        Instant.ofEpochMilli(millis).atZone(zone()).format(dateTimeFormatter)

    /** 今天 00:00:00.000 的时间戳 */
    fun startOfTodayMillis(): Long =
        LocalDate.now(zone()).atStartOfDay(zone()).toInstant().toEpochMilli()

    /** 今天 23:59:59.999 的时间戳 */
    fun endOfTodayMillis(): Long =
        LocalDate.now(zone()).plusDays(1).atStartOfDay(zone()).toInstant().toEpochMilli() - 1

    /** 取某个时间戳对应的“本地日期”（用来做“近 7 天复习趋势”统计） */
    fun localDateOf(millis: Long): LocalDate =
        Instant.ofEpochMilli(millis).atZone(zone()).toLocalDate()

    /**
     * 友好的相对时间描述，例如：
     * 今天 / 昨天 / 3 天前 / 5 天后
     */
    fun describeRelativeDay(millis: Long): String {
        val today = LocalDate.now(zone())
        val target = localDateOf(millis)
        val days = java.time.temporal.ChronoUnit.DAYS.between(today, target)
        return when {
            days == 0L -> "今天"
            days == -1L -> "昨天"
            days == 1L -> "明天"
            days < 0 -> "${-days} 天前"
            else -> "$days 天后"
        }
    }
}
