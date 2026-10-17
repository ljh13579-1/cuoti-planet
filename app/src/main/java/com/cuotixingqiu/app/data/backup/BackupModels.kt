package com.cuotixingqiu.app.data.backup

import kotlinx.serialization.Serializable

/**
 * 导出文件的“数据格式”定义（用 kotlinx.serialization 自动转成 JSON）。
 *
 * 设计原则：
 *  - 文件里带 schemaVersion（格式版本号），将来格式升级时可以识别旧文件；
 *  - 图片是可选的：导出时可以选择“包含图片”（Base64 内嵌）或“仅文字”；
 *  - 读取时忽略不认识的字段，方便以后加字段。
 *
 * 注意：这里故意不导出数据库自增 id，
 * 导入时全部生成新 id（合并模式），绝不会覆盖手机上已有的错题。
 */
@Serializable
data class BackupFile(
    val app: String = APP_NAME,
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val exportedAt: Long = 0L,
    val notebooks: List<BackupNotebook> = emptyList(),
    val questions: List<BackupQuestion> = emptyList(),
) {
    companion object {
        const val APP_NAME = "CuotiPlanet"
        const val CURRENT_SCHEMA_VERSION = 1
    }
}

/** 备份里的分类（只存名字） */
@Serializable
data class BackupNotebook(
    val name: String = "",
)

/** 备份里的一道错题 */
@Serializable
data class BackupQuestion(
    val subject: String = "",
    val notebookName: String? = null,
    val title: String = "",
    val content: String = "",
    val knowledgePoints: List<String> = emptyList(),
    val reason: String = "",
    val solution: String = "",
    val summary: String = "",
    val difficulty: Int = 0,
    val status: String = "",
    val reviewStage: Int = 0,
    val lastReviewResult: String? = null,
    val lastReviewedAt: Long? = null,
    val reviewCount: Int = 0,
    val nextReviewAt: Long? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val images: List<BackupImage> = emptyList(),
)

/** 备份里的一张图片：文件名 + （可选的）Base64 内容 */
@Serializable
data class BackupImage(
    val fileName: String = "",
    val base64: String? = null,
)

/** 导入结果 */
sealed interface ImportResult {

    data class Success(
        val questionCount: Int,
        val notebookCount: Int,
        val imageCount: Int,
        val skippedImageCount: Int,
    ) : ImportResult

    data class Failure(val reason: String) : ImportResult
}
