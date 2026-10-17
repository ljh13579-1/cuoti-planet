package com.cuotixingqiu.app.data.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Base64
import java.util.UUID

/**
 * 图片仓库：负责把用户从相册/相机选来的图片“复制一份”到应用私有目录
 * （files/images/），数据库里只保存文件名。
 *
 * 这样做的好处：
 *  - 不需要任何存储权限；
 *  - 用户以后删除相册里的原图，错题里的图片仍然在；
 *  - 导入/导出时可以按文件名找到图片。
 *
 * 图片会先压缩：最长边压到 1600 像素以内、JPEG 质量 85，
 * 既保证手机上看清楚题目，又不会占用太多空间。
 */
class ImageStore(private val context: Context) {

    private val imagesDir: File
        get() = File(context.filesDir, IMAGES_DIR_NAME).apply { if (!exists()) mkdirs() }

    /** 根据文件名拿到真实文件（界面显示图片时使用） */
    fun fileFor(fileName: String): File = File(imagesDir, fileName)

    /** 返回文件（不存在则返回 null），供界面加载 */
    fun fileOrNull(fileName: String): File? = fileFor(fileName).takeIf { it.exists() }

    /** 从 Uri（相册或相机临时文件）导入一张图片，返回保存后的文件名；失败返回 null */
    suspend fun importFromUri(uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val bitmap = decodeScaledBitmap(uri) ?: return@runCatching null
            val fileName = newFileName()
            FileOutputStream(fileFor(fileName)).use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
            }
            bitmap.recycle()
            fileName
        }.getOrNull()
    }

    /** 读取图片内容为 Base64 文本（导出备份时使用） */
    suspend fun toBase64(fileName: String): String? = withContext(Dispatchers.IO) {
        val file = fileFor(fileName)
        if (!file.exists()) return@withContext null
        runCatching { Base64.getEncoder().encodeToString(file.readBytes()) }.getOrNull()
    }

    /** 把 Base64 文本写回图片文件（导入备份时使用） */
    suspend fun saveBase64(fileName: String, base64: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val bytes = Base64.getDecoder().decode(base64)
            FileOutputStream(fileFor(safeFileName(fileName))).use { it.write(bytes) }
            true
        }.getOrDefault(false)
    }

    fun delete(fileName: String) {
        runCatching { fileFor(safeFileName(fileName)).delete() }
    }

    fun deleteAll() {
        runCatching { imagesDir.listFiles()?.forEach { it.delete() } }
    }

    // ---------------- 内部工具 ----------------

    private fun newFileName(): String =
        "IMG_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.jpg"

    /** 只保留文件名本身，防止备份文件里出现 ../ 之类的路径 */
    private fun safeFileName(fileName: String): String =
        fileName.substringAfterLast('/').substringAfterLast('\\')

    /** 按“最长边不超过 MAX_DIMENSION”的规则压缩解码，并处理相机照片的旋转信息 */
    private fun decodeScaledBitmap(uri: Uri): Bitmap? {
        val resolver = context.contentResolver

        // 第一步：只读取尺寸（不真正解码图片，速度快、不占内存）
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, bounds)
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        // 第二步：按比例采样解码
        val options = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight)
        }
        var bitmap = resolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, options)
        } ?: return null

        // 第三步：处理 EXIF 旋转（否则竖着拍的照片可能会躺着显示）
        val rotation = resolver.openInputStream(uri)?.use { input ->
            when (
                ExifInterface(input).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                )
            ) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } ?: 0f

        if (rotation != 0f) {
            val matrix = Matrix().apply { postRotate(rotation) }
            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            if (rotated != bitmap) bitmap.recycle()
            bitmap = rotated
        }
        return bitmap
    }

    private fun calculateInSampleSize(width: Int, height: Int): Int {
        var sampleSize = 1
        var longestSide = maxOf(width, height)
        while (longestSide / 2 >= MAX_DIMENSION) {
            longestSide /= 2
            sampleSize *= 2
        }
        return sampleSize
    }

    private companion object {
        const val IMAGES_DIR_NAME = "images"
        const val MAX_DIMENSION = 1600
        const val JPEG_QUALITY = 85
    }
}
