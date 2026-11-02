package com.cuotixingqiu.app.ui.notebook

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cuotixingqiu.app.data.db.NotebookEntity
import com.cuotixingqiu.app.data.repository.NotebookRepository
import com.cuotixingqiu.app.data.repository.WrongQuestionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 分类管理页的数据 */
data class NotebookUiState(
    val notebooks: List<NotebookEntity> = emptyList(),
    /** 每个分类里有几道错题 */
    val counts: Map<Long?, Int> = emptyMap(),
    val message: String? = null,
) {
    val unclassifiedCount: Int get() = counts[null] ?: 0

    fun countOf(notebookId: Long): Int = counts[notebookId] ?: 0
}

/**
 * 错题本分类管理：可以新建、改名、删除分类。
 * 删除分类只会把错题变回“未分类”，不会删除错题本身。
 */
class NotebookViewModel(
    private val notebookRepository: NotebookRepository,
    questionRepository: WrongQuestionRepository,
) : ViewModel() {

    private val messageFlow = MutableStateFlow<String?>(null)

    val uiState: StateFlow<NotebookUiState> = combine(
        notebookRepository.observeAll(),
        questionRepository.observeNotebookCounts(),
        messageFlow,
    ) { notebooks, counts, message ->
        NotebookUiState(
            notebooks = notebooks,
            counts = counts.associate { it.notebookId to it.count },
            message = message,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = NotebookUiState(),
    )

    /** 新建分类；重名时给出提示而不是崩溃 */
    fun create(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            messageFlow.value = "分类名称不能为空"
            return
        }
        viewModelScope.launch {
            runCatching { notebookRepository.create(trimmed) }
                .onSuccess { messageFlow.value = "已新建分类「$trimmed」" }
                .onFailure { messageFlow.value = "新建失败：已经有同名分类了" }
        }
    }

    fun rename(id: Long, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) {
            messageFlow.value = "分类名称不能为空"
            return
        }
        viewModelScope.launch {
            runCatching { notebookRepository.rename(id, trimmed) }
                .onSuccess { messageFlow.value = "已改名为「$trimmed」" }
                .onFailure { messageFlow.value = "改名失败：已经有同名分类了" }
        }
    }

    /** 删除分类（分类里的错题会回到“未分类”） */
    fun delete(id: Long) {
        viewModelScope.launch {
            notebookRepository.delete(id)
            messageFlow.value = "分类已删除，里面的错题已回到“未分类”"
        }
    }

    fun clearMessage() {
        messageFlow.update { null }
    }
}
