package com.cuotixingqiu.app.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/** 近 7 天柱状图 / 学习记录使用的一天 */
data class DailyReviewCount(
    val date: LocalDate,
    val count: Int,
)

/**
 * 时间工具：负责“今天”的计算和界面上的时间显示（使用手机所在时区）。
 *
 * 注意：项目最低支持 Android 8.0（API 26），因此可以直接使用 java.time。
 */
object TimeUtils {

    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    private val monthDayFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("M月d日")

    private val weekdayFull = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
    private val weekdayNarrowText = "日一二三四五六"

    private fun zone(): ZoneId = ZoneId.systemDefault()

    /** 例如 2026-10-17 */
    fun formatDate(millis: Long): String =
        Instant.ofEpochMilli(millis).atZone(zone()).format(dateFormatter)

    /** 例如 2026-10-17 20:35 */
    fun formatDateTime(millis: Long): String =
        Instant.ofEpochMilli(millis).atZone(zone()).format(dateTimeFormatter)

    /** 例如 10月29日 */
    fun formatMonthDay(millis: Long = System.currentTimeMillis()): String =
        Instant.ofEpochMilli(millis).atZone(zone()).format(monthDayFormatter)

    /** 例如 10月29日 周四 */
    fun formatMonthDayWeekday(millis: Long = System.currentTimeMillis()): String {
        val date = Instant.ofEpochMilli(millis).atZone(zone()).toLocalDate()
        return "${date.monthValue}月${date.dayOfMonth}日 ${weekdayFull[date.dayOfWeek.value - 1]}"
    }

    /** 例如 周四 · 10月29日（复习页顶部） */
    fun formatWeekdayMonthDay(millis: Long = System.currentTimeMillis()): String {
        val date = Instant.ofEpochMilli(millis).atZone(zone()).toLocalDate()
        return "${weekdayFull[date.dayOfWeek.value - 1]} · ${date.monthValue}月${date.dayOfMonth}日"
    }

    /** 周几，例如 周一 */
    fun formatWeekday(millis: Long): String {
        val date = localDateOf(millis)
        return weekdayFull[date.dayOfWeek.value - 1]
    }

    /** 柱状图底部的单字星期：日、一、二、三、四、五、六 */
    fun weekdayNarrow(millis: Long): String = weekdayNarrow(localDateOf(millis))

    /** 柱状图底部的单字星期：日、一、二、三、四、五、六 */
    fun weekdayNarrow(date: LocalDate): String {
        val index = date.dayOfWeek.value % 7
        return weekdayNarrowText.substring(index, index + 1)
    }

    /** 今天 00:00:00.000 的时间戳 */
    fun startOfTodayMillis(): Long =
        LocalDate.now(zone()).atStartOfDay(zone()).toInstant().toEpochMilli()

    /** 今天 23:59:59.999 的时间戳 */
    fun endOfTodayMillis(): Long =
        LocalDate.now(zone()).plusDays(1).atStartOfDay(zone()).toInstant().toEpochMilli() - 1

    /** 某一天的开始时间戳 */
    fun startOfDayMillis(date: LocalDate): Long =
        date.atStartOfDay(zone()).toInstant().toEpochMilli()

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
        val days = ChronoUnit.DAYS.between(today, target)
        return when {
            days == 0L -> "今天"
            days == -1L -> "昨天"
            days == 1L -> "明天"
            days < 0 -> "${-days} 天前"
            else -> "$days 天后"
        }
    }

    /**
     * 近 [days] 天每天“有复习记录的错题数”，按日期从早到晚返回。
     * 数据库只保存每道题的最后一次复习时间，因此这里统计的是“当天复习过的题目数”，
     * 不是复习动作次数；对首页/我的页的轻量趋势来说已经足够，且完全来自真实数据。
     */
    fun recentDailyReviewCounts(
        timestamps: List<Long>,
        days: Int = 7,
        today: LocalDate = LocalDate.now(zone()),
    ): List<DailyReviewCount> {
        val counts = timestamps
            .map(::localDateOf)
            .groupingBy { it }
            .eachCount()
        return (days - 1 downTo 0).map { offset ->
            val date = today.minusDays(offset.toLong())
            DailyReviewCount(date = date, count = counts[date] ?: 0)
        }
    }

    /**
     * 连续学习天数：从今天（今天还没复习则从昨天）向前数，连续有复习记录的天数。
     * 这里使用每道题的最后复习时间；没有记录的日期会中断连续天数。
     */
    fun calculateStudyStreak(
        timestamps: List<Long>,
        today: LocalDate = LocalDate.now(zone()),
    ): Int {
        if (timestamps.isEmpty()) return 0
        val studyDays = timestamps.map(::localDateOf).toSet()
        var cursor = when {
            today in studyDays -> today
            today.minusDays(1) in studyDays -> today.minusDays(1)
            else -> return 0
        }
        var streak = 0
        while (cursor in studyDays) {
            streak++
            cursor = cursor.minusDays(1)
        }
        return streak
    }

    /** 从第一条错题开始算“已加入 N 天”；没有题目时返回 0 */
    fun joinedDays(firstCreatedAt: Long?, today: LocalDate = LocalDate.now(zone())): Int {
        if (firstCreatedAt == null) return 0
        val first = localDateOf(firstCreatedAt)
        return (ChronoUnit.DAYS.between(first, today) + 1).toInt().coerceAtLeast(1)
    }
}
