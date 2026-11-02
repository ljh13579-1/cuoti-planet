package com.cuotixingqiu.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** 首页/我的页使用的真实日期统计工具测试（不依赖 Android）。 */
class TimeUtilsTest {

    private val zone: ZoneId = ZoneId.systemDefault()

    private fun millisOf(date: LocalDate, hour: Int = 12): Long =
        date.atTime(hour, 0).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun recentDailyReviewCounts_returnsSevenDaysOldestFirst() {
        val today = LocalDate.of(2026, 10, 31)
        val timestamps = listOf(
            millisOf(today),
            millisOf(today),
            millisOf(today.minusDays(2)),
            millisOf(today.minusDays(6)),
        )

        val days = TimeUtils.recentDailyReviewCounts(timestamps, days = 7, today = today)

        assertEquals(7, days.size)
        assertEquals(today.minusDays(6), days.first().date)
        assertEquals(today, days.last().date)
        assertEquals(1, days.first().count)
        assertEquals(0, days[1].count)
        assertEquals(1, days[4].count)
        assertEquals(2, days.last().count)
    }

    @Test
    fun calculateStudyStreak_countsTodayAndPreviousDays() {
        val today = LocalDate.of(2026, 10, 31)
        val timestamps = listOf(
            millisOf(today),
            millisOf(today.minusDays(1)),
            millisOf(today.minusDays(2)),
            millisOf(today.minusDays(5)),
        )

        assertEquals(3, TimeUtils.calculateStudyStreak(timestamps, today = today))
    }

    @Test
    fun calculateStudyStreak_allowsCurrentStreakEndingYesterday() {
        val today = LocalDate.of(2026, 10, 31)
        val timestamps = listOf(
            millisOf(today.minusDays(1)),
            millisOf(today.minusDays(2)),
        )

        assertEquals(2, TimeUtils.calculateStudyStreak(timestamps, today = today))
    }

    @Test
    fun calculateStudyStreak_returnsZeroWhenLastStudyWasTooLongAgo() {
        val today = LocalDate.of(2026, 10, 31)
        val timestamps = listOf(millisOf(today.minusDays(3)))

        assertEquals(0, TimeUtils.calculateStudyStreak(timestamps, today = today))
    }

    @Test
    fun joinedDays_countsFirstDayAsOne() {
        val today = LocalDate.of(2026, 10, 31)
        val first = millisOf(today.minusDays(4))

        assertEquals(5, TimeUtils.joinedDays(first, today = today))
        assertEquals(0, TimeUtils.joinedDays(null, today = today))
    }

    @Test
    fun formatDateAndDateTime_useExpectedPattern() {
        val millis = LocalDateTime.of(2026, 10, 17, 20, 35)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()

        assertEquals("2026-10-17", TimeUtils.formatDate(millis))
        assertEquals("2026-10-17 20:35", TimeUtils.formatDateTime(millis))
    }

    @Test
    fun todayRange_coversNowAndUsesLocalDay() {
        val now = System.currentTimeMillis()
        val start = TimeUtils.startOfTodayMillis()
        val end = TimeUtils.endOfTodayMillis()

        assertTrue(start <= now)
        assertTrue(now <= end)
        assertEquals(LocalDate.now(zone), TimeUtils.localDateOf(start))
        assertEquals(TimeUtils.localDateOf(start), TimeUtils.localDateOf(end))
        assertEquals(start, TimeUtils.startOfDayMillis(LocalDate.now(zone)))
    }

    @Test
    fun weekdayNarrow_usesChineseSingleCharacter() {
        assertEquals("六", TimeUtils.weekdayNarrow(LocalDate.of(2026, 10, 31)))
        assertEquals("日", TimeUtils.weekdayNarrow(LocalDate.of(2026, 11, 1)))
    }
}
