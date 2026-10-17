package com.cuotixingqiu.app.ui.paper

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cuotixingqiu.app.data.db.NotebookEntity
import com.cuotixingqiu.app.data.db.WrongQuestionEntity
import com.cuotixingqiu.app.data.export.PaperExporter
import com.cuotixingqiu.app.data.repository.NotebookRepository
import com.cuotixingqiu.app.data.repository.WrongQuestionRepository
import com.cuotixingqiu.app.domain.ReviewStatus
import com.cuotixingqiu.app.domain.Subject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

/** 组卷的题目范围 */
enum class PaperScope(val displayName: String) {
    ALL("全部错题"),
    SELECTED("列表里选中的"),
    SUBJECT("按学科"),
    NOTEBOOK("按分类"),
    NEED_REVIEW("仅需要复习"),
    MASTERED("仅已掌握"),
}

/** 组卷设置 */
data class PaperSettings(
    val scope: PaperScope = PaperScope.ALL,
    val subject: String? = null,
    val notebookId: Long? = null,
    val title: String = "错题组卷",
    val includeImages: Boolean = true,
    val includeAnswers: Boolean = true,
)

/** 组卷页的界面状态 */
data class PaperUiState(
    val settings: PaperSettings = PaperSettings(),
    val subjects: List<String> = emptyList(),
    val notebooks: List<NotebookEntity> = emptyList(),
    val selectedCount: Int = 0,
    val candidateCount: Int = 0,
    val generating: Boolean = false,
    val message: String? = null,
    val generatedFile: File? = null,
    val generatedQuestionCount: Int = 0,
)

/**
 * 组卷导出（生成 PDF）的 ViewModel。
 *
 * 支持按“全部 / 列表里选中的 / 学科 / 分类 / 需要复习 / 已掌握”挑题，
 * 可以选择是否带图片、是否附答案与解析。
 */
class PaperViewModel(
    private val repository: WrongQuestionRepository,
    notebookRepository: NotebookRepository,
    private val paperExporter: PaperExporter,
    private val selectedIds: List<Long>,
) : ViewModel() {

    private val settingsFlow = MutableStateFlow(
        PaperSettings(
            scope = if (selectedIds.isEmpty()) PaperScope.ALL else PaperScope.SELECTED,
        ),
    )
    private val generatingFlow = MutableStateFlow(false)
    private val messageFlow = MutableStateFlow<String?>(null)
    private val resultFlow = MutableStateFlow<Pair<File, Int>?>(null)

    /** 所有错题：既要用于显示“将导出几道题”，也要在生成 PDF 时取用 */
    private val allQuestions = repository.observeAll().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList(),
    )

    val uiState: StateFlow<PaperUiState> = combine(
        combine(allQuestions, notebookRepository.observeAll(), settingsFlow) { questions, notebooks, settings ->
            Triple(questions, notebooks, settings)
        },
        combine(generatingFlow, messageFlow, resultFlow) { generating, message, result ->
            Triple(generating, message, result)
        },
    ) { first, second ->
        val (questions, notebooks, settings) = first
        val (generating, message, result) = second
        val candidates = pickQuestions(questions, settings)
        PaperUiState(
            settings = settings,
            subjects = questions.map { it.subject }.distinct()
                .sortedBy { subject ->
                    val index = Subject.displayOrder.indexOfFirst { it.displayName == subject }
                    if (index < 0) Int.MAX_VALUE else index
                },
            notebooks = notebooks,
            selectedCount = selectedIds.size,
            candidateCount = candidates.size,
            generating = generating,
            message = message,
            generatedFile = result?.first,
            generatedQuestionCount = result?.second ?: 0,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PaperUiState(),
    )

    // ---------------- 设置 ----------------

    fun setScope(scope: PaperScope) = settingsFlow.update { it.copy(scope = scope) }

    fun setSubject(subject: String?) = settingsFlow.update { it.copy(subject = subject) }

    fun setNotebook(notebookId: Long?) = settingsFlow.update { it.copy(notebookId = notebookId) }

    fun setTitle(title: String) = settingsFlow.update { it.copy(title = title) }

    fun setIncludeImages(value: Boolean) = settingsFlow.update { it.copy(includeImages = value) }

    fun setIncludeAnswers(value: Boolean) = settingsFlow.update { it.copy(includeAnswers = value) }

    // ---------------- 生成 PDF ----------------

    fun generate() {
        if (generatingFlow.value) return
        viewModelScope.launch {
            val settings = settingsFlow.value
            val questions = pickQuestions(allQuestions.value, settings)
            if (questions.isEmpty()) {
                messageFlow.value = "没有符合条件的错题，请换一个范围"
                return@launch
            }
            generatingFlow.value = true
            val file = paperExporter.exportPdf(
                questions = questions,
                options = PaperExporter.Options(
                    title = settings.title.ifBlank { "错题组卷" },
                    includeImages = settings.includeImages,
                    includeAnswers = settings.includeAnswers,
                ),
            )
            generatingFlow.value = false
            if (file == null) {
                messageFlow.value = "生成失败，请稍后再试"
            } else {
                resultFlow.value = file to questions.size
            }
        }
    }

    fun clearMessage() = messageFlow.update { null }

    /** 关闭“导出完成”弹窗，同时把结果文件删掉（避免私有目录越攒越多） */
    fun dismissResult(deleteFile: Boolean = false) {
        val file = resultFlow.value?.first
        resultFlow.value = null
        if (deleteFile && file != null) {
            runCatching { file.delete() }
        }
    }

    /** 按范围筛选题目 */
    private fun pickQuestions(
        questions: List<WrongQuestionEntity>,
        settings: PaperSettings,
    ): List<WrongQuestionEntity> = when (settings.scope) {
        PaperScope.ALL -> questions

        PaperScope.SELECTED -> questions.filter { it.id in selectedIds.toSet() }

        PaperScope.SUBJECT -> questions.filter { it.subject == settings.subject }

        PaperScope.NOTEBOOK -> questions.filter { it.notebookId == settings.notebookId }

        PaperScope.NEED_REVIEW -> questions.filter { it.status != ReviewStatus.MASTERED.name }

        PaperScope.MASTERED -> questions.filter { it.status == ReviewStatus.MASTERED.name }
    }
}
