package com.cuotixingqiu.app.data.backup

import android.content.Context
import androidx.room.withTransaction
import com.cuotixingqiu.app.data.db.AppDatabase
import com.cuotixingqiu.app.data.db.NotebookDao
import com.cuotixingqiu.app.data.db.NotebookEntity
import com.cuotixingqiu.app.data.db.WrongQuestionDao
import com.cuotixingqiu.app.data.db.WrongQuestionEntity
import com.cuotixingqiu.app.data.image.ImageStore
import com.cuotixingqiu.app.domain.ReviewStatus
import kotlinx.serialization.json.Json
import java.io.File

/**
 * 备份管理器：负责“导出为 JSON”和“从 JSON 导入”。
 *
 * 导入的三条安全规则（都是为了让数据不丢）：
 *  1. 先完整解析和校验，全部通过才开始写数据库；
 *  2. 写数据库使用一个事务：任何一条出错，全部回滚；
 *  3. 导入是“合并”模式：所有内容都会生成新纪录，不覆盖也不删除已有错题。
 */
class BackupManager(
    private val context: Context,
    private val db: AppDatabase,
    private val questionDao: WrongQuestionDao,
    private val notebookDao: NotebookDao,
    private val imageStore: ImageStore,
) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // ---------------- 导出 ----------------

    /**
     * 生成备份 JSON 文本。
     *
     * @param includeImages 是否把图片以 Base64 内嵌进 JSON（文件会大很多，但恢复时图片不丢）
     */
    suspend fun exportJson(includeImages: Boolean): String {
        val questions = questionDao.getAllOnce()
        val notebooks = notebookDao.getAllOnce()
        val notebookNames = notebooks.associate { it.id to it.name }

        val backupQuestions = questions.map { question ->
            val images = question.imageNames.mapNotNull { fileName ->
                val base64 = if (includeImages) imageStore.toBase64(fileName) else null
                if (includeImages && base64 == null) null else BackupImage(fileName, base64)
            }
            question.toBackup(notebookName = question.notebookId?.let { notebookNames[it] }, images = images)
        }

        val backup = BackupFile(
            exportedAt = System.currentTimeMillis(),
            notebooks = notebooks.map { BackupNotebook(name = it.name) },
            questions = backupQuestions,
        )
        return json.encodeToString(BackupFile.serializer(), backup)
    }

    // ---------------- 导入 ----------------

    suspend fun importJson(text: String): ImportResult {
        // 第 1 步：解析 JSON
        val backup = runCatching { json.decodeFromString(BackupFile.serializer(), text) }.getOrElse { error ->
            return ImportResult.Failure("文件不是本应用导出的 JSON 备份（解析失败）：${error.message ?: "未知错误"}")
        }

        // 第 2 步：校验格式版本
        if (backup.schemaVersion < 1) {
            return ImportResult.Failure("备份文件的版本号无效（${backup.schemaVersion}）")
        }
        if (backup.schemaVersion > BackupFile.CURRENT_SCHEMA_VERSION) {
            return ImportResult.Failure(
                "备份文件来自更新版本的应用（格式版本 ${backup.schemaVersion}，" +
                    "当前应用支持 ${BackupFile.CURRENT_SCHEMA_VERSION}），请先升级应用再导入"
            )
        }
        if (backup.questions.isEmpty()) {
            return ImportResult.Failure("备份文件里没有错题，已取消导入")
        }

        // 第 3 步：逐条校验内容
        val validationError = validate(backup)
        if (validationError != null) {
            return ImportResult.Failure(validationError)
        }

        // 第 4 步：先把图片写到私有目录（记下写了哪些，失败时删掉）
        val writtenImageNames = mutableListOf<String>()
        var skippedImages = 0
        val questionImageNames = mutableMapOf<Int, List<String>>()

        backup.questions.forEachIndexed { index, question ->
            val savedNames = mutableListOf<String>()
            question.images.forEach { image ->
                val base64 = image.base64
                if (base64.isNullOrBlank()) {
                    skippedImages++ // 仅文字备份：图片内容缺失，跳过
                } else {
                    val safeName = sanitizeFileName(image.fileName.ifBlank { "image_$index.jpg" })
                    if (imageStore.saveBase64(safeName, base64)) {
                        savedNames += safeName
                        writtenImageNames += safeName
                    } else {
                        skippedImages++
                    }
                }
            }
            questionImageNames[index] = savedNames
        }

        // 第 5 步：写数据库（一个事务，失败就回滚，并清掉刚写入的图片）
        return try {
            var createdNotebooks = 0
            db.withTransaction {
                // 分类：按名字复用已有的，缺失的才新建
                val existingNotebooks = notebookDao.getAllOnce().associateBy { it.name }
                val notebookIdByName = existingNotebooks.mapValues { it.value.id }.toMutableMap()

                backup.notebooks.forEach { backupNotebook ->
                    val name = backupNotebook.name.trim()
                    if (name.isEmpty() || notebookIdByName.containsKey(name)) return@forEach
                    val newId = notebookDao.insert(
                        NotebookEntity(name = name, createdAt = System.currentTimeMillis()),
                    )
                    notebookIdByName[name] = newId
                    createdNotebooks++
                }

                val entities = backup.questions.mapIndexed { index, question ->
                    question.toEntity(
                        notebookId = question.notebookName?.trim()
                            ?.takeIf { it.isNotEmpty() }
                            ?.let { notebookIdByName[it] },
                        imageNames = questionImageNames[index] ?: emptyList(),
                    )
                }
                questionDao.insertAll(entities)
            }

            ImportResult.Success(
                questionCount = backup.questions.size,
                notebookCount = createdNotebooks,
                imageCount = writtenImageNames.size,
                skippedImageCount = skippedImages,
            )
        } catch (error: Exception) {
            // 数据库失败：把刚才写进去的图片删掉，保持和导入前一致
            writtenImageNames.forEach { imageStore.delete(it) }
            ImportResult.Failure("写入数据库失败，已回滚（原有数据未受影响）：${error.message ?: "未知错误"}")
        }
    }

    // ---------------- 内部校验与转换 ----------------

    /** 返回错误说明；null 表示校验通过 */
    private fun validate(backup: BackupFile): String? {
        val maxTextLength = 20_000

        backup.questions.forEachIndexed { index, question ->
            val position = "第 ${index + 1} 条错题"
            val texts = listOf(
                question.title to "标题",
                question.content to "内容",
                question.reason to "错因",
                question.solution to "解题思路",
                question.summary to "总结",
            )
            texts.forEach { (text, name) ->
                if (text.length > maxTextLength) {
                    return "$position 的$name 太长（超过 $maxTextLength 个字符），无法导入"
                }
            }
            if (question.knowledgePoints.size > 100) {
                return "$position 的知识点标签太多（超过 100 个），无法导入"
            }
            if (question.images.size > 30) {
                return "$position 的图片太多（超过 30 张），无法导入"
            }
            if (question.createdAt < 0 || question.updatedAt < 0) {
                return "$position 的时间字段无效"
            }
        }

        backup.notebooks.forEachIndexed { index, notebook ->
            if (notebook.name.length > 50) {
                return "第 ${index + 1} 个分类的名称太长（超过 50 个字符），无法导入"
            }
        }
        return null
    }

    private fun sanitizeFileName(name: String): String {
        val base = name.substringAfterLast('/').substringAfterLast('\\')
        val cleaned = base.replace(Regex("[^A-Za-z0-9._-]"), "_")
        return if (cleaned.isBlank() || cleaned == "." || cleaned == "..") {
            "image_${System.currentTimeMillis()}.jpg"
        } else {
            cleaned
        }
    }
}

// ---------------- 实体与备份对象之间的转换 ----------------

private fun WrongQuestionEntity.toBackup(
    notebookName: String?,
    images: List<BackupImage>,
): BackupQuestion = BackupQuestion(
    subject = subject,
    notebookName = notebookName,
    title = title,
    content = content,
    knowledgePoints = knowledgePoints,
    reason = reason,
    solution = solution,
    summary = summary,
    difficulty = difficulty,
    status = status,
    reviewStage = reviewStage,
    lastReviewResult = lastReviewResult,
    lastReviewedAt = lastReviewedAt,
    reviewCount = reviewCount,
    nextReviewAt = nextReviewAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
    images = images,
)

private fun BackupQuestion.toEntity(
    notebookId: Long?,
    imageNames: List<String>,
): WrongQuestionEntity {
    val now = System.currentTimeMillis()
    val createdAt = if (createdAt in 1..now) createdAt else now
    return WrongQuestionEntity(
        id = 0L, // 合并模式：永远生成新 id
        subject = com.cuotixingqiu.app.domain.Subject.fromDisplayName(subject).displayName,
        notebookId = notebookId,
        title = title,
        content = content,
        knowledgePoints = knowledgePoints.map { it.trim() }.filter { it.isNotEmpty() },
        reason = reason,
        solution = solution,
        summary = summary,
        imageNames = imageNames,
        difficulty = difficulty.coerceIn(0, 5),
        // 只接受两种合法的状态，其它一律按“需要复习”处理
        status = if (status == ReviewStatus.MASTERED.name) ReviewStatus.MASTERED.name
        else ReviewStatus.NEEDS_REVIEW.name,
        reviewStage = reviewStage.coerceIn(0, 6),
        lastReviewResult = lastReviewResult?.takeIf { it in listOf("AGAIN", "GOOD", "EASY") },
        lastReviewedAt = lastReviewedAt?.takeIf { it > 0 },
        reviewCount = reviewCount.coerceAtLeast(0),
        nextReviewAt = nextReviewAt?.takeIf { it > 0 },
        createdAt = createdAt,
        updatedAt = if (updatedAt in 1..now) updatedAt else createdAt,
    )
}

/** 备份文件建议的扩展名（仅仅是为了用户好认） */
fun backupFileName(timestamp: Long): String {
    val date = java.time.LocalDate.now()
    return "错题星球备份_$date.json"
}

/** 供界面提示使用：文件是否存在（本类内部其实用不到，留作扩展点） */
internal fun File.existsSafe(): Boolean = runCatching { exists() }.getOrDefault(false)
