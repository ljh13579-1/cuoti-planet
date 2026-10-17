package com.cuotixingqiu.app.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cuotixingqiu.app.data.db.WrongQuestionEntity
import com.cuotixingqiu.app.data.repository.NotebookRepository
import com.cuotixingqiu.app.data.repository.WrongQuestionRepository
import com.cuotixingqiu.app.domain.ReviewStatus
import com.cuotixingqiu.app.util.TimeUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.util.concurrent.TimeUnit

/** 复习页的数据 */
data class ReviewUiState(
    val dueCount: Int = 0,
    val dueQuestions: List<WrongQuestionEntity> = emptyList(),
    val reviewedTodayCount: Int = 0,
    val totalCount: Int = 0,
    val masteredCount: Int = 0,
    val upcoming: List<WrongQuestionEntity> = emptyList(),
    val notebookNames: Map<Long, String> = emptyMap(),
) {
    val masteredRate: Float
        get() = if (totalCount == 0) 0f else masteredCount.toFloat() / totalCount.toFloat()
}

/**
 * 复习页的 ViewModel：
 * 显示“今天要复习什么”“接下来几天有什么安排”，以及几个简单的统计数字。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReviewViewModel(
    private val repository: WrongQuestionRepository,
    notebookRepository: NotebookRepository,
) : ViewModel() {

    private val refreshTick = MutableStateFlow(0)

    val uiState: StateFlow<ReviewUiState> = refreshTick
        .flatMapLatest { buildFlow(notebookRepository) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ReviewUiState(),
        )

    /** 重新计算“今天”（处理跨天的情况） */
    fun refresh() {
        refreshTick.value = refreshTick.value + 1
    }

    private fun buildFlow(notebookRepository: NotebookRepository): Flow<ReviewUiState> {
        val dayStart = TimeUtils.startOfTodayMillis()
        val dueUntil = TimeUtils.endOfTodayMillis()
        val weekLater = dueUntil + TimeUnit.DAYS.toMillis(7)

        val numbers = combine(
            repository.observeDueCount(dueUntil),
            repository.observeReviewedCountSince(dayStart),
            repository.observeTotalCount(),
            repository.observeCountByStatus(ReviewStatus.MASTERED),
        ) { due, reviewedToday, total, mastered -> Numbers(due, reviewedToday, total, mastered) }

        val lists = combine(
            repository.observeDueList(dueUntil),
            repository.observeUpcoming(from = dueUntil, to = weekLater, limit = 10),
            notebookRepository.observeAll(),
        ) { dueList, upcoming, notebooks -> Lists(dueList, upcoming, notebooks.associate { it.id to it.name }) }

        return combine(numbers, lists) { number, list ->
            ReviewUiState(
                dueCount = number.due,
                dueQuestions = list.dueList,
                reviewedTodayCount = number.reviewedToday,
                totalCount = number.total,
                masteredCount = number.mastered,
                upcoming = list.upcoming,
                notebookNames = list.notebookNames,
            )
        }
    }

    private data class Numbers(
        val due: Int,
        val reviewedToday: Int,
        val total: Int,
        val mastered: Int,
    )

    private data class Lists(
        val dueList: List<WrongQuestionEntity>,
        val upcoming: List<WrongQuestionEntity>,
        val notebookNames: Map<Long, String>,
    )
}
