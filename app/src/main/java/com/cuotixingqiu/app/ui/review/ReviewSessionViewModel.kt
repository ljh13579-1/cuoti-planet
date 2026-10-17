package com.cuotixingqiu.app.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cuotixingqiu.app.data.db.WrongQuestionEntity
import com.cuotixingqiu.app.data.repository.WrongQuestionRepository
import com.cuotixingqiu.app.domain.ReviewResult
import com.cuotixingqiu.app.util.TimeUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 复习会话的界面状态 */
data class ReviewSessionUiState(
    val loading: Boolean = true,
    val questions: List<WrongQuestionEntity> = emptyList(),
    val index: Int = 0,
    val revealed: Boolean = false,
    val finished: Boolean = false,
    val againCount: Int = 0,
    val goodCount: Int = 0,
    val easyCount: Int = 0,
    val message: String? = null,
) {
    val current: WrongQuestionEntity? get() = questions.getOrNull(index)

    val answeredCount: Int get() = againCount + goodCount + easyCount
}

/**
 * 复习会话的 ViewModel：一道道地过题，每答一次就按规则安排下次复习时间。
 *
 * @param onlyQuestionId 不为空时只复习这一道（从详情页“立即复习”进来）；
 *                       为空时复习“今天到期的所有错题”。
 */
class ReviewSessionViewModel(
    private val repository: WrongQuestionRepository,
    private val onlyQuestionId: Long?,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReviewSessionUiState())
    val uiState: StateFlow<ReviewSessionUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val questions: List<WrongQuestionEntity> = if (onlyQuestionId != null) {
                listOfNotNull(repository.getById(onlyQuestionId))
            } else {
                // 只取一次快照：本次复习过程中列表不再变化，避免答完一题它就从列表里消失
                repository.observeDueList(TimeUtils.endOfTodayMillis()).first()
            }
            _uiState.update {
                it.copy(
                    loading = false,
                    questions = questions,
                    message = if (questions.isEmpty()) "现在没有需要复习的错题" else null,
                )
            }
        }
    }

    /** 显示答案与解析 */
    fun reveal() = _uiState.update { it.copy(revealed = true) }

    /**
     * 记录一次复习结果，然后自动进入下一题。
     * 结果会立刻写入数据库（包括下次复习时间）。
     */
    fun answer(result: ReviewResult) {
        val state = _uiState.value
        val question = state.current ?: return
        viewModelScope.launch {
            repository.applyReview(question, result)
            _uiState.update { current ->
                val answered = current.answeredCount + 1
                val nextIndex = current.index + 1
                current.copy(
                    index = nextIndex,
                    revealed = false,
                    finished = nextIndex >= current.questions.size,
                    againCount = current.againCount + if (result == ReviewResult.AGAIN) 1 else 0,
                    goodCount = current.goodCount + if (result == ReviewResult.GOOD) 1 else 0,
                    easyCount = current.easyCount + if (result == ReviewResult.EASY) 1 else 0,
                    message = if (answered == current.questions.size) null else current.message,
                )
            }
        }
    }

    fun clearMessage() = _uiState.update { it.copy(message = null) }
}
