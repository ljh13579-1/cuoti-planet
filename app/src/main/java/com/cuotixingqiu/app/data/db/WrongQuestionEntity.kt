package com.cuotixingqiu.app.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.cuotixingqiu.app.domain.ReviewStatus

/**
 * 一道错题（数据库表名 wrong_questions）。
 *
 * 设计说明：
 *  - 除“学科”外，其它文本内容都允许是空字符串：App 不强制用户填满所有内容。
 *  - 知识点、图片文件名是“字符串列表”，由 Converters 自动转成 JSON 文本存取。
 *  - 复习相关的字段：status（状态）、reviewStage（阶段）、nextReviewAt（下次复习时间）等。
 */
@Entity(
    tableName = "wrong_questions",
    indices = [
        Index("subject"),
        Index("notebookId"),
        Index("status"),
        Index("nextReviewAt"),
        Index("createdAt"),
    ],
)
data class WrongQuestionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    /** 学科（存中文名，例如“数学”） */
    val subject: String,

    /** 所属错题本分类（null 表示未分类） */
    val notebookId: Long? = null,

    /** 题目标题（可空） */
    val title: String = "",

    /** 题目内容（可空） */
    val content: String = "",

    /** 知识点标签，例如 ["导数", "单调性"] */
    val knowledgePoints: List<String> = emptyList(),

    /** 错题原因（可空） */
    val reason: String = "",

    /** 正确解题思路（可空） */
    val solution: String = "",

    /** 个人总结（可空） */
    val summary: String = "",

    /** 图片文件名列表（图片本体保存在应用私有目录 files/images/ 下） */
    val imageNames: List<String> = emptyList(),

    /** 难度星级：0 = 未标注，1..5 = 一到五星 */
    val difficulty: Int = 0,

    /** 复习状态：NEEDS_REVIEW / MASTERED（存枚举名） */
    val status: String = ReviewStatus.NEEDS_REVIEW.name,

    /** 复习阶段：0 = 新题，数字越大表示记得越牢 */
    val reviewStage: Int = 0,

    /** 上次复习的结果：AGAIN / GOOD / EASY（null = 还没复习过） */
    val lastReviewResult: String? = null,

    /** 上次复习的时间 */
    val lastReviewedAt: Long? = null,

    /** 累计复习次数 */
    val reviewCount: Int = 0,

    /** 下次建议复习时间（null = 已掌握，不参与复习计划） */
    val nextReviewAt: Long? = null,

    /** 创建时间 */
    val createdAt: Long,

    /** 最后修改时间 */
    val updatedAt: Long,
)
