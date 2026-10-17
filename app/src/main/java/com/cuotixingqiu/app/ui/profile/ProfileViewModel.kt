package com.cuotixingqiu.app.ui.profile

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cuotixingqiu.app.data.backup.BackupManager
import com.cuotixingqiu.app.data.backup.ImportResult
import com.cuotixingqiu.app.data.image.ImageStore
import com.cuotixingqiu.app.data.repository.NotebookRepository
import com.cuotixingqiu.app.data.repository.WrongQuestionRepository
import com.cuotixingqiu.app.data.settings.SettingsRepository
import com.cuotixingqiu.app.data.settings.ThemeMode
import com.cuotixingqiu.app.util.MediaUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

/** “我的”页面的状态 */
data class ProfileUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = false,
    val totalCount: Int = 0,
    val imageCount: Int = 0,
    val notebookCount: Int = 0,
    val busy: Boolean = false,
    val message: String? = null,
    /** 导入结束后要给用户看的结果（标题 + 详细说明） */
    val resultTitle: String? = null,
    val resultMessage: String? = null,
)

/**
 * “我的”页面：主题设置、数据导入导出、清空数据、应用信息。
 *
 * 所有操作都在本地完成：导出是写一个 JSON 文件，导入是读一个 JSON 文件，
 * 不会上传任何数据（应用也没有申请网络权限）。
 */
class ProfileViewModel(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val repository: WrongQuestionRepository,
    private val notebookRepository: NotebookRepository,
    private val backupManager: BackupManager,
    private val imageStore: ImageStore,
) : ViewModel() {

    private val busyFlow = MutableStateFlow(false)
    private val messageFlow = MutableStateFlow<String?>(null)
    private val resultFlow = MutableStateFlow<Pair<String, String>?>(null)

    val uiState: StateFlow<ProfileUiState> = combine(
        combine(
            settingsRepository.themeMode,
            settingsRepository.dynamicColor,
            repository.observeAll(),
        ) { themeMode, dynamicColor, questions ->
            Triple(themeMode, dynamicColor, questions)
        },
        combine(
            notebookRepository.observeAll(),
            busyFlow,
            messageFlow,
            resultFlow,
        ) { notebooks, busy, message, result ->
            Quadruple(notebooks.size, busy, message, result)
        },
    ) { first, second ->
        ProfileUiState(
            themeMode = first.first,
            dynamicColor = first.second,
            totalCount = first.third.size,
            imageCount = first.third.sumOf { it.imageNames.size },
            notebookCount = second.notebookCount,
            busy = second.busy,
            message = second.message,
            resultTitle = second.result?.first,
            resultMessage = second.result?.second,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProfileUiState(),
    )

    // ---------------- 主题 ----------------

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setDynamicColor(enabled) }
    }

    // ---------------- 导出 / 导入 ----------------

    /** 导出 JSON 到用户选择的文件（includeImages = 是否把图片打包成 Base64） */
    fun exportJson(uri: Uri, includeImages: Boolean) {
        viewModelScope.launch {
            busyFlow.value = true
            try {
                val text = backupManager.exportJson(includeImages)
                val ok = MediaUtils.writeTextToUri(context, uri, text)
                messageFlow.value = if (ok) {
                    "备份已导出（${if (includeImages) "包含图片" else "仅文字，文件更小"}）"
                } else {
                    "导出失败：无法写入这个位置，请换一个文件夹试试"
                }
            } catch (error: Exception) {
                messageFlow.value = "导出失败：${error.message ?: "未知错误"}"
            } finally {
                busyFlow.value = false
            }
        }
    }

    /** 从用户选择的 JSON 文件导入（合并模式，不会覆盖已有错题） */
    fun importJson(uri: Uri) {
        viewModelScope.launch {
            busyFlow.value = true
            try {
                val text = MediaUtils.readTextFromUri(context, uri)
                if (text == null) {
                    resultFlow.value = "导入失败" to "读不到这个文件，请确认文件还能正常打开。"
                    return@launch
                }
                when (val result = backupManager.importJson(text)) {
                    is ImportResult.Success -> {
                        resultFlow.value = "导入完成" to buildString {
                            append("成功导入 ${result.questionCount} 道错题")
                            if (result.notebookCount > 0) append("、${result.notebookCount} 个分类")
                            if (result.imageCount > 0) append("、${result.imageCount} 张图片")
                            append("。原有数据没有被覆盖。")
                            if (result.skippedImageCount > 0) {
                                append("\n有 ${result.skippedImageCount} 张图片在备份文件里没有内容，已跳过。")
                            }
                        }
                    }

                    is ImportResult.Failure -> {
                        resultFlow.value = "导入失败（原有数据未受影响）" to result.reason
                    }
                }
            } catch (error: Exception) {
                resultFlow.value = "导入失败（原有数据未受影响）" to (error.message ?: "未知错误")
            } finally {
                busyFlow.value = false
            }
        }
    }

    /** 建议的导出文件名 */
    fun suggestedBackupFileName(): String = "错题星球备份_${java.time.LocalDate.now()}.json"

    /** 组卷导出的 PDF 目录（用于在界面上提示文件位置） */
    fun exportDir(): File = File(context.filesDir, "exports")

    // ---------------- 清空数据 ----------------

    fun clearAllData() {
        viewModelScope.launch {
            busyFlow.value = true
            try {
                repository.deleteAll()
                notebookRepository.deleteAllNotebooks()
                messageFlow.value = "已清空所有错题、分类和图片"
            } catch (error: Exception) {
                messageFlow.value = "清空失败：${error.message ?: "未知错误"}"
            } finally {
                busyFlow.value = false
            }
        }
    }

    fun clearMessage() {
        messageFlow.update { null }
    }

    fun clearResult() {
        resultFlow.update { null }
    }

    fun imageStoreForUi(): ImageStore = imageStore

    private data class Quadruple(
        val notebookCount: Int,
        val busy: Boolean,
        val message: String?,
        val result: Pair<String, String>?,
    )
}
