package com.cuotixingqiu.app.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cuotixingqiu.app.data.db.WrongQuestionEntity
import com.cuotixingqiu.app.data.repository.NotebookRepository
import com.cuotixingqiu.app.data.repository.WrongQuestionRepository
import com.cuotixingqiu.app.domain.ReviewStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 错题详情页的数据 */
data class DetailUiState(
    val loaded: Boolean = false,
    val question: WrongQuestionEntity? = null,
    val notebookName: String? = null,
)

/**
 * 错题详情页的 ViewModel。
 * 数据来自数据库 Flow：在这里改状态（已掌握 / 需要复习）之后，页面上的按钮会立刻跟着变。
 */
class DetailViewModel(
    private val repository: WrongQuestionRepository,
    notebookRepository: NotebookRepository,
    private val questionId: Long,
) : ViewModel() {

    val uiState: StateFlow<DetailUiState> = combine(
        repository.observeById(questionId),
        notebookRepository.observeAll(),
    ) { question, notebooks ->
        DetailUiState(
            loaded = true,
            question = question,
            notebookName = question?.notebookId?.let { id ->
                notebooks.firstOrNull { it.id == id }?.name
            },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DetailUiState(),
    )

    /** 一次性的提示消息（例如“已标记为掌握”） */
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun clearMessage() {
        _message.value = null
    }

    /** 标记为已掌握（退出复习计划） */
    fun markMastered() {
        viewModelScope.launch {
            repository.setStatus(questionId, ReviewStatus.MASTERED)
            _message.value = "已标记为已掌握，不再安排复习"
        }
    }

    /** 重新加入复习计划（阶段归零，明天开始复习） */
    fun reopenReview() {
        viewModelScope.launch {
            repository.reopenForReview(questionId)
            _message.value = "已重新加入复习计划，明天需要复习"
        }
    }

    /** 删除这道错题（图片会一起删除） */
    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.delete(questionId)
            onDeleted()
        }
    }
}
