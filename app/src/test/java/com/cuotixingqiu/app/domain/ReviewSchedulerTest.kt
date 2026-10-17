package com.cuotixingqiu.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * 复习规则的单元测试（纯逻辑测试，运行在电脑上，不需要手机）。
 *
 * 图标规则：
 *  - 新题：次日复习
 *  - 不会：重置到阶段 0，1 天后复习
 *  - 基本掌握：间隔依次 2 / 4 / 7 / 15 / 30 天（封顶 30 天）
 *  - 已掌握：退出复习计划
 */
class ReviewSchedulerTest {

    private val now = 1_700_000_000_000L // 固定一个时间点，保证测试结果稳定
    private val day = TimeUnit.DAYS.toMillis(1)

    @Test
    fun `新错题的下次复习时间是次日`() {
        assertEquals(now + day, ReviewScheduler.initialNextReviewAt(now))
    }

    @Test
    fun `答不会时阶段归零并安排明天复习`() {
        val schedule = ReviewScheduler.schedule(ReviewResult.AGAIN, currentStage = 3, now = now)

        assertEquals(0, schedule.stage)
        assertEquals(ReviewStatus.NEEDS_REVIEW, schedule.status)
        assertEquals(now + day, schedule.nextReviewAt)
    }

    @Test
    fun `连续答基本掌握时间隔依次拉长并在30天封顶`() {
        val expectedDays = listOf(2, 4, 7, 15, 30, 30, 30)
        var stage = 0

        expectedDays.forEach { expected ->
            val schedule = ReviewScheduler.schedule(ReviewResult.GOOD, stage, now)
            assertEquals("间隔应为 $expected 天", expected * day, schedule.nextReviewAt - now)
            assertEquals(ReviewStatus.NEEDS_REVIEW, schedule.status)
            stage = schedule.stage
        }
    }

    @Test
    fun `答已掌握时状态变为已掌握`() {
        val schedule = ReviewScheduler.schedule(ReviewResult.EASY, currentStage = 2, now = now)

        assertEquals(ReviewStatus.MASTERED, schedule.status)
        assertEquals(30 * day, schedule.nextReviewAt - now)
    }
}
