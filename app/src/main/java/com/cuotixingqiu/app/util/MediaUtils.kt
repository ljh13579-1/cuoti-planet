package com.cuotixingqiu.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.cuotixingqiu.app.BuildConfig
import java.io.File

/**
 * 和“文件 / URI”有关的小工具，集中放在这里：
 *  - 拍照时创建临时文件并生成给系统相机用的 URI；
 *  - 导出/导入 JSON 时读写用户选择的文件；
 *  - 导出 PDF 后分享或打开。
 *
 * 这些操作都不需要额外权限：
 * 拍照和分享用的是 FileProvider 临时授权，导入导出用的是系统文件选择器给的授权。
 */
object MediaUtils {

    /** 文件提供者的 authority（必须和 AndroidManifest.xml 里一致） */
    private const val FILE_PROVIDER_SUFFIX = ".fileprovider"

    /** 创建一个“交给系统相机写照片”的临时文件 URI */
    fun createCameraUri(context: Context): Uri {
        val dir = File(context.cacheDir, "camera").apply { mkdirs() }
        val file = File.createTempFile("capture_", ".jpg", dir)
        return fileProviderUri(context, file)
    }

    /** 把应用私有文件转成可以临时授权给其它应用的 URI */
    fun fileProviderUri(context: Context, file: File): Uri =
        FileProvider.getUriForFile(
            context,
            BuildConfig.APPLICATION_ID + FILE_PROVIDER_SUFFIX,
            file,
        )

    /** 读取用户从文件选择器里选中的文本文件（导入 JSON 用）；失败返回 null */
    fun readTextFromUri(context: Context, uri: Uri): String? = runCatching {
        context.contentResolver.openInputStream(uri)?.use { input ->
            input.bufferedReader().readText()
        }
    }.getOrNull()

    /** 把文本写入用户选中的文件（导出 JSON 用）；成功返回 true */
    fun writeTextToUri(context: Context, uri: Uri, text: String): Boolean = runCatching {
        context.contentResolver.openOutputStream(uri)?.use { output ->
            output.write(text.toByteArray())
        }
        true
    }.getOrDefault(false)

    /** 分享一个文件（弹出系统分享面板） */
    fun shareFile(context: Context, file: File, mimeType: String, chooserTitle: String = "分享") {
        val uri = runCatching { fileProviderUri(context, file) }.getOrNull() ?: return
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(Intent.createChooser(intent, chooserTitle)) }
    }

    /** 用其它应用打开一个文件（例如用阅读器打开导出的 PDF）；成功返回 true */
    fun openFile(context: Context, file: File, mimeType: String): Boolean {
        val uri = runCatching { fileProviderUri(context, file) }.getOrNull() ?: return false
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching {
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    /** 把字节数转成“1.2 MB”这样的可读文字 */
    fun formatFileSize(bytes: Long): String = when {
        bytes >= 1024 * 1024 -> String.format("%.1f MB", bytes / 1024.0 / 1024.0)
        bytes >= 1024 -> String.format("%.1f KB", bytes / 1024.0)
        else -> "$bytes B"
    }
}
