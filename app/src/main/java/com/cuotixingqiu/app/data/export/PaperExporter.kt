package com.cuotixingqiu.app.data.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.cuotixingqiu.app.data.db.WrongQuestionEntity
import com.cuotixingqiu.app.data.image.ImageStore
import com.cuotixingqiu.app.util.TimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * 「组卷导出」：把选中的错题排版成一份 PDF 试卷。
 *
 * 使用系统自带的 PdfDocument，不需要任何第三方库，也完全离线。
 * PDF 页面尺寸用 A4（595 x 842 点，1 点 = 1/72 英寸）。
 *
 * @param includeImages  是否把题目截图一起放进试卷
 * @param includeAnswers 是否在最后附上「答案与解析」（错因 / 解题思路 / 总结）
 */
class PaperExporter(
    private val context: Context,
    private val imageStore: ImageStore,
) {

    data class Options(
        val title: String = "错题组卷",
        val includeImages: Boolean = true,
        val includeAnswers: Boolean = false,
    )

    /** 生成 PDF，返回文件；失败返回 null */
    suspend fun exportPdf(
        questions: List<WrongQuestionEntity>,
        options: Options,
    ): File? = withContext(Dispatchers.IO) {
        if (questions.isEmpty()) return@withContext null

        val document = PdfDocument()
        return@withContext runCatching {
            val writer = PaperWriter(document)

            // ---------- 第一部分：题目 ----------
            writer.drawText(
                text = options.title,
                paint = writer.titlePaint,
                spacingAfter = 4f,
            )
            writer.drawText(
                text = "共 ${questions.size} 题 · 生成时间 ${TimeUtils.formatDateTime(System.currentTimeMillis())}",
                paint = writer.hintPaint,
                spacingAfter = 14f,
            )

            questions.forEachIndexed { index, question ->
                writer.startNewBlock(40f)
                writer.drawText(
                    text = "${index + 1}. " + questionTitle(question),
                    paint = writer.headingPaint,
                    spacingAfter = 4f,
                )
                writer.drawText(
                    text = questionMetaLine(question),
                    paint = writer.hintPaint,
                    spacingAfter = 6f,
                )
                writer.drawText(question.content.ifBlank { "（未填写题目内容）" }, writer.bodyPaint)

                if (options.includeImages && question.imageNames.isNotEmpty()) {
                    question.imageNames.forEach { name ->
                        val bitmap = decodeForPdf(name)
                        if (bitmap != null) {
                            writer.drawImage(bitmap)
                            bitmap.recycle()
                        }
                    }
                }
                writer.drawText("", writer.bodyPaint, spacingAfter = 10f)
            }

            // ---------- 第二部分：答案与解析 ----------
            if (options.includeAnswers) {
                writer.newPage()
                writer.drawText("答案与解析", writer.titlePaint, spacingAfter = 12f)
                questions.forEachIndexed { index, question ->
                    writer.startNewBlock(40f)
                    writer.drawText(
                        text = "${index + 1}. " + questionTitle(question),
                        paint = writer.headingPaint,
                        spacingAfter = 4f,
                    )
                    question.content.ifBlank { "" }
                        .takeIf { it.isNotBlank() }
                        ?.let { writer.drawText("题目：$it", writer.bodyPaint) }
                    writer.drawText(
                        "错因：" + question.reason.ifBlank { "（未填写）" },
                        writer.bodyPaint,
                    )
                    writer.drawText(
                        "解题思路：" + question.solution.ifBlank { "（未填写）" },
                        writer.bodyPaint,
                    )
                    writer.drawText(
                        "总结：" + question.summary.ifBlank { "（未填写）" },
                        writer.bodyPaint,
                        spacingAfter = 10f,
                    )
                }
            }

            writer.finish()

            val file = outputFile()
            FileOutputStream(file).use { output -> document.writeTo(output) }
            file
        }.getOrNull().also {
            runCatching { document.close() }
        }
    }

    // ---------------- 内部实现 ----------------

    private fun outputFile(): File {
        val dir = File(context.filesDir, EXPORT_DIR_NAME).apply { mkdirs() }
        val fileName = "错题组卷_${TimeUtils.formatDate(System.currentTimeMillis())}_" +
            "${System.currentTimeMillis() % 100000}.pdf"
        return File(dir, fileName)
    }

    private fun questionTitle(question: WrongQuestionEntity): String =
        question.title.ifBlank {
            question.content.lineSequence().firstOrNull()?.take(24).orEmpty().ifBlank { "未命名错题" }
        }

    private fun questionMetaLine(question: WrongQuestionEntity): String {
        val parts = mutableListOf(question.subject)
        if (question.knowledgePoints.isNotEmpty()) {
            parts += question.knowledgePoints.joinToString("、")
        }
        if (question.difficulty > 0) {
            parts += "难度 " + "★".repeat(question.difficulty)
        }
        return parts.joinToString("  |  ")
    }

    /** 把图片解码成适合放进 PDF 的位图（先缩小，避免内存过大） */
    private fun decodeForPdf(fileName: String): Bitmap? {
        val file = imageStore.fileOrNull(fileName) ?: return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sample = 1
        var longest = maxOf(bounds.outWidth, bounds.outHeight)
        while (longest / 2 >= 1200) {
            longest /= 2
            sample *= 2
        }
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeFile(file.absolutePath, options)
    }

    /**
     * 负责“一页一页写内容”的小工具。
     * 文字会自动换行；内容写到底部时会自动另起一页。
     */
    private class PaperWriter(
        private val document: PdfDocument,
    ) {

        private val pageWidth = 595
        private val pageHeight = 842
        private val margin = 42f
        private val contentWidth: Float = pageWidth - margin * 2

        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val headingPaint = Paint().apply {
            color = Color.BLACK
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val bodyPaint = Paint().apply {
            color = Color.BLACK
            textSize = 11f
            isAntiAlias = true
        }
        val hintPaint = Paint().apply {
            color = Color.rgb(110, 110, 110)
            textSize = 9.5f
            isAntiAlias = true
        }

        private var page: PdfDocument.Page? = null
        private var pageNumber = 0
        private var y: Float = margin

        init {
            newPage()
        }

        fun newPage() {
            page?.let { document.finishPage(it) }
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, ++pageNumber).create()
            page = document.startPage(pageInfo)
            y = margin
        }

        /** 如果当前页剩余空间不够，就换页 */
        fun startNewBlock(needed: Float) {
            if (y + needed > pageHeight - margin) newPage()
        }

        fun drawText(text: String, paint: Paint, spacingAfter: Float = 5f) {
            val canvas = page?.canvas ?: return
            val lineHeight = paint.textSize * 1.45f
            wrapText(text, paint, contentWidth).forEach { line ->
                if (y + lineHeight > pageHeight - margin) newPage()
                val target = page?.canvas ?: return
                target.drawText(line, margin, y + paint.textSize, paint)
                y += lineHeight
            }
            y += spacingAfter
        }

        fun drawImage(bitmap: Bitmap) {
            val maxHeight = 300f
            val scale = minOf(contentWidth / bitmap.width, maxHeight / bitmap.height, 1.6f)
            val drawWidth = bitmap.width * scale
            val drawHeight = bitmap.height * scale
            if (y + drawHeight + 8f > pageHeight - margin) newPage()
            val canvas: Canvas = page?.canvas ?: return
            canvas.drawBitmap(bitmap, margin, y, null)
            y += drawHeight + 8f
        }

        fun finish() {
            page?.let { document.finishPage(it) }
            page = null
        }

        /** 按“最长不超过 maxWidth”把一段文字切成若干行（支持中文和换行） */
        private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
            val lines = mutableListOf<String>()
            text.split('\n').forEach { paragraph ->
                if (paragraph.isEmpty()) {
                    lines += ""
                    return@forEach
                }
                val current = StringBuilder()
                paragraph.forEach { char ->
                    val candidate = current.toString() + char
                    if (current.isNotEmpty() && paint.measureText(candidate) > maxWidth) {
                        lines += current.toString()
                        current.setLength(0)
                    }
                    current.append(char)
                }
                if (current.isNotEmpty()) lines += current.toString()
            }
            return lines
        }
    }

    /** 导出文件的目录名（放在应用私有目录 files/exports 下） */
    private companion object {
        const val EXPORT_DIR_NAME = "exports"
    }
}
