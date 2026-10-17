package com.cuotixingqiu.app.domain

import java.util.concurrent.TimeUnit

/**
 * 复习规则（第一版：简单、可解释，不依赖任何 AI）。
 *
 * 规则说明：
 *  1. 新添加的错题：默认“需要复习”，次日进入待复习列表（阶段 0）。
 *  2. 复习时选择「不会」：阶段归零，1 天后再次复习。
 *  3. 选择「基本掌握」：阶段 +1，下次间隔依次拉长为 2、4、7、15、30 天（封顶 30 天）。
 *  4. 选择「已掌握」：状态变为“已掌握”，退出待复习列表；
 *     以后如果发现其实没掌握，可以在详情页手动改回“需要复习”。
 *
 * 这个类是纯逻辑（不依赖 Android），因此可以用单元测试完整验证。
 */
object ReviewScheduler {

    /** 复习间隔天数。索引 0 是“重置后”的 1 天，之后依次拉长，最后固定 30 天。 */
    val INTERVAL_DAYS = intArrayOf(1, 2, 4, 7, 15, 30)

    private val ONE_DAY_MS = TimeUnit.DAYS.toMillis(1)

    /** 新错题的下次复习时间：创建时间 + 1 天 */
    fun initialNextReviewAt(createdAt: Long): Long = createdAt + ONE_DAY_MS

    /**
     * 计算一次复习之后的新安排。
     *
     * @param result       用户的选择（不会 / 基本掌握 / 已掌握）
     * @param currentStage 当前阶段（新题是 0）
     * @param now          本次复习时间
     */
    fun schedule(result: ReviewResult, currentStage: Int, now: Long): ReviewSchedule =
        when (result) {
            ReviewResult.AGAIN -> ReviewSchedule(
                stage = 0,
                status = ReviewStatus.NEEDS_REVIEW,
                nextReviewAt = now + ONE_DAY_MS,
            )

            ReviewResult.GOOD -> {
                val newStage = (currentStage + 1).coerceAtMost(INTERVAL_DAYS.size) // 1..6
                val days = INTERVAL_DAYS[newStage.coerceAtMost(INTERVAL_DAYS.size - 1)]
                ReviewSchedule(
                    stage = newStage,
                    status = ReviewStatus.NEEDS_REVIEW,
                    nextReviewAt = now + days * ONE_DAY_MS,
                )
            }

            ReviewResult.EASY -> ReviewSchedule(
                stage = INTERVAL_DAYS.size, // 6：代表已掌握（不再排复习）
                status = ReviewStatus.MASTERED,
                nextReviewAt = now + INTERVAL_DAYS.last() * ONE_DAY_MS,
            )
        }
}
