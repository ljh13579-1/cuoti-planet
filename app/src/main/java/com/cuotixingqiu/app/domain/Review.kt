package com.cuotixingqiu.app.domain

/**
 * 错题的复习状态。
 * 存进数据库时使用枚举名（例如 "NEEDS_REVIEW"），这样以后改中文名也不会影响已存的数据。
 */
enum class ReviewStatus(val displayName: String) {
    NEEDS_REVIEW("需要复习"),
    MASTERED("已掌握");

    companion object {
        fun fromName(value: String?): ReviewStatus =
            entries.firstOrNull { it.name == value } ?: NEEDS_REVIEW
    }
}

/** 复习时用户的三档选择 */
enum class ReviewResult(val displayName: String) {
    AGAIN("不会"),
    GOOD("基本掌握"),
    EASY("已掌握");

    companion object {
        fun fromName(value: String?): ReviewResult? =
            entries.firstOrNull { it.name == value }
    }
}

/** 一次复习之后，这道错题的最新安排 */
data class ReviewSchedule(
    val stage: Int,
    val status: ReviewStatus,
    val nextReviewAt: Long,
)
