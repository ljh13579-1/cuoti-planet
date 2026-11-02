package com.cuotixingqiu.app.ui.edit

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cuotixingqiu.app.data.db.NotebookEntity
import com.cuotixingqiu.app.data.db.WrongQuestionEntity
import com.cuotixingqiu.app.data.image.ImageStore
import com.cuotixingqiu.app.data.repository.NotebookRepository
import com.cuotixingqiu.app.data.repository.WrongQuestionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

/** 添加 / 编辑错题页面上的所有内容 */
data class EditUiState(
    val loading: Boolean = false,
    val isNew: Boolean = true,
    val subject: String = "",
    val notebookId: Long? = null,
    val title: String = "",
    val content: String = "",
    val reason: String = "",
    val solution: String = "",
    val summary: String = "",
    val knowledgePoints: List<String> = emptyList(),
    val knowledgePointDraft: String = "",
    val difficulty: Int = 0,
    val imageNames: List<String> = emptyList(),
    val notebooks: List<NotebookEntity> = emptyList(),
    val saving: Boolean = false,
    val importingImages: Boolean = false,
    val message: String? = null,
)

/**
 * 添加 / 编辑错题的 ViewModel。
 *
 * 设计原则（按需求）：除了“学科”需要选择之外，其它内容都可以不填；
 * 图片可以从相册选，也可以直接拍照。
 */
class EditViewModel(
    private val repository: WrongQuestionRepository,
    private val notebookRepository: NotebookRepository,
    private val imageStore: ImageStore,
    private val questionId: Long?,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditUiState(isNew = questionId == null))
    val uiState: StateFlow<EditUiState> = _uiState.asStateFlow()

    /** 编辑模式下的原始数据（保存时以它为基础，避免丢掉复习进度等字段） */
    private var loaded: WrongQuestionEntity? = null

    init {
        viewModelScope.launch {
            val notebooks = notebookRepository.getAll()
            _uiState.update { it.copy(notebooks = notebooks) }

            if (questionId == null) return@launch

            _uiState.update { it.copy(loading = true) }
            val entity = repository.getById(questionId)
            if (entity == null) {
                _uiState.update { it.copy(loading = false, message = "这道错题不存在，可能已被删除") }
                return@launch
            }
            loaded = entity
            _uiState.update {
                it.copy(
                    loading = false,
                    isNew = false,
                    subject = entity.subject,
                    notebookId = entity.notebookId,
                    title = entity.title,
                    content = entity.content,
                    reason = entity.reason,
                    solution = entity.solution,
                    summary = entity.summary,
                    knowledgePoints = entity.knowledgePoints,
                    difficulty = entity.difficulty,
                    imageNames = entity.imageNames,
                )
            }
        }
    }

    // ---------------- 表单修改 ----------------

    fun setSubject(subject: String) = _uiState.update { it.copy(subject = subject) }

    fun setNotebook(notebookId: Long?) = _uiState.update { it.copy(notebookId = notebookId) }

    fun setTitle(value: String) = _uiState.update { it.copy(title = value) }

    fun setContent(value: String) = _uiState.update { it.copy(content = value) }

    fun setReason(value: String) = _uiState.update { it.copy(reason = value) }

    fun setSolution(value: String) = _uiState.update { it.copy(solution = value) }

    fun setSummary(value: String) = _uiState.update { it.copy(summary = value) }

    fun setDifficulty(value: Int) = _uiState.update { it.copy(difficulty = value.coerceIn(0, 5)) }

    fun setKnowledgePointDraft(value: String) =
        _uiState.update { it.copy(knowledgePointDraft = value) }

    /** 把输入框里的文字加成知识点标签（支持用空格、逗号、顿号一次输入多个） */
    fun addKnowledgePointsFromDraft() {
        val draft = _uiState.value.knowledgePointDraft
        if (draft.isBlank()) return
        _uiState.update { state ->
            val newOnes = splitKnowledgePoints(draft)
                .filter { it.isNotBlank() && !state.knowledgePoints.contains(it) }
            state.copy(
                knowledgePoints = state.knowledgePoints + newOnes,
                knowledgePointDraft = "",
            )
        }
    }

    fun removeKnowledgePoint(point: String) = _uiState.update { state ->
        state.copy(knowledgePoints = state.knowledgePoints.filterNot { it == point })
    }

    // ---------------- 图片 ----------------

    /** 从相册/相机导入图片：压缩后保存在应用私有目录 */
    fun addImages(uris: List<Uri>) {
        if (uris.isEmpty()) return
        if (_uiState.value.importingImages) {
            _uiState.update { it.copy(message = "图片还在处理中，请稍候再试") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(importingImages = true) }
            val names = mutableListOf<String>()
            var failed = 0
            uris.forEach { uri ->
                val name = imageStore.importFromUri(uri)
                if (name != null) names += name else failed++
                // 拍照用的临时文件导入完就删掉（相册选来的 content:// 文件绝不动）
                if (uri.scheme == "file") {
                    runCatching { uri.path?.let { File(it).delete() } }
                }
            }
            _uiState.update { state ->
                state.copy(
                    imageNames = state.imageNames + names,
                    importingImages = false,
                    message = if (failed > 0) "有 $failed 张图片读取失败" else null,
                )
            }
        }
    }

    // ---------------- 拍照兜底 ----------------

    /** 等待相机写入照片的临时文件路径（在内存里，页面重建不会丢） */
    private var pendingCapturePath: String? = null

    /** 打开相机前，先记下相机要把照片写进哪个临时文件 */
    fun onCameraLaunched(filePath: String) {
        pendingCapturePath = filePath
    }

    /** 通用的小提示（相机相关的错误等） */
    fun showMessage(text: String) = _uiState.update { it.copy(message = text) }

    /**
     * 拍照返回后的兜底检查（结果回调、页面回到前台都会调用，可重复调用）：
     *
     * 不看“拍照成功”的标志，只看相机有没有真的把照片写进临时文件——
     * 有些系统（如鸿蒙卓易通）会丢失这个标志，但照片其实已经写好了。
     * 判断完清空待处理状态；文件为空就删除并给出明确提示，不再静默丢弃。
     */
    fun checkPendingCapture() {
        val path = pendingCapturePath ?: return
        pendingCapturePath = null
        val file = File(path)
        if (file.exists() && file.length() > 0L) {
            addImages(listOf(Uri.fromFile(file)))
        } else {
            runCatching { file.delete() }
            _uiState.update {
                it.copy(message = "拍照没有返回照片，请重试，或改用「从相册选择」")
            }
        }
    }

    /** 从表单里移除一张图片（真正的文件会在保存时删除） */
    fun removeImage(fileName: String) = _uiState.update { state ->
        state.copy(imageNames = state.imageNames.filterNot { it == fileName })
    }

    // ---------------- 保存 ----------------

    fun save(onSaved: (Long) -> Unit) {
        val state = _uiState.value
        if (state.saving) return

        // 图片还在导入时先别保存，否则刚拍的照片会来不及写进这道错题
        if (state.importingImages) {
            _uiState.update { it.copy(message = "图片还在处理中，请稍候再保存") }
            return
        }

        if (state.subject.isBlank()) {
            _uiState.update { it.copy(message = "请先选择学科") }
            return
        }
        val knowledgePoints = (
            state.knowledgePoints + splitKnowledgePoints(state.knowledgePointDraft)
            ).filter { it.isNotBlank() }.distinct()

        if (state.title.isBlank() && state.content.isBlank() && knowledgePoints.isEmpty()) {
            _uiState.update { it.copy(message = "请至少填写题目内容或标题") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(saving = true) }
            try {
                val original = loaded
                val savedId = if (original == null) {
                    repository.add(
                        WrongQuestionEntity(
                            subject = state.subject,
                            notebookId = state.notebookId,
                            title = state.title.trim(),
                            content = state.content.trim(),
                            knowledgePoints = knowledgePoints,
                            reason = state.reason.trim(),
                            solution = state.solution.trim(),
                            summary = state.summary.trim(),
                            imageNames = state.imageNames,
                            difficulty = state.difficulty,
                            createdAt = 0L,
                            updatedAt = 0L,
                        ),
                    )
                } else {
                    repository.update(
                        original.copy(
                            subject = state.subject,
                            notebookId = state.notebookId,
                            title = state.title.trim(),
                            content = state.content.trim(),
                            knowledgePoints = knowledgePoints,
                            reason = state.reason.trim(),
                            solution = state.solution.trim(),
                            summary = state.summary.trim(),
                            imageNames = state.imageNames,
                            difficulty = state.difficulty,
                        ),
                    )
                    // 保存成功后，把用户移除的图片文件真正删掉
                    (original.imageNames - state.imageNames.toSet()).forEach { imageStore.delete(it) }
                    original.id
                }
                _uiState.update { it.copy(saving = false) }
                onSaved(savedId)
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        saving = false,
                        message = "保存失败：${error.message ?: "未知错误"}",
                    )
                }
            }
        }
    }

    fun clearMessage() = _uiState.update { it.copy(message = null) }

    /** 把“导数 单调性、极值”这样的输入拆成多个标签 */
    private fun splitKnowledgePoints(text: String): List<String> =
        text.split(' ', '\n', ',', '，', '、', ';', '；', '/')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .take(20)
}
