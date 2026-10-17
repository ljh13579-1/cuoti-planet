package com.cuotixingqiu.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 时间工具测试（运行在电脑上） */
class TimeUtilsTest {

    @Test
    fun `今天零点在今天结束之前`() {
        val start = TimeUtils.startOfTodayMillis()
        val end = TimeUtils.endOfTodayMillis()
        val now = System.currentTimeMillis()

        assertTrue("今天零点应该早于现在", start <= now)
        assertTrue("今天结束应该晚于现在", end >= now)
        assertTrue("今天的时间长度应为一天左右", end - start > 23L * 3600 * 1000)
    }

    @Test
    fun `相对日期描述正确`() {
        val day = 24L * 3600 * 1000
        val now = System.currentTimeMillis()

        assertEquals("今天", TimeUtils.describeRelativeDay(now))
        assertEquals("昨天", TimeUtils.describeRelativeDay(now - day))
        assertEquals("2 天前", TimeUtils.describeRelativeDay(now - 2 * day))
    }
}
