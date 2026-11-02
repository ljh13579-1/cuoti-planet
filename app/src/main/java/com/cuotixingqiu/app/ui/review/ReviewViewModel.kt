package com.cuotixingqiu.app.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cuotixingqiu.app.data.db.WrongQuestionEntity
import com.cuotixingqiu.app.data.repository.NotebookRepository
import com.cuotixingqiu.app.data.repository.WrongQuestionRepository
import com.cuotixingqiu.app.domain.ReviewStatus
import com.cuotixingqiu.app.domain.Subject
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

/** 某个学科的掌握进度（真实数据按学科聚合） */
data class SubjectProgress(
    val subject: String,
    val mastered: Int,
    val total: Int,
) {
    val rate: Float get() = if (total == 0) 0f else mastered.toFloat() / total.toFloat()
}

/** 复习页的数据 */
data class ReviewUiState(
    val dueCount: Int = 0,
    val dueQuestions: List<WrongQuestionEntity> = emptyList(),
    val reviewedTodayCount: Int = 0,
    val totalCount: Int = 0,
    val masteredCount: Int = 0,
    val upcoming: List<WrongQuestionEntity> = emptyList(),
    val subjectProgress: List<SubjectProgress> = emptyList(),
    val notebookNames: Map<Long, String> = emptyMap(),
) {
    val masteredRate: Float
        get() = if (totalCount == 0) 0f else masteredCount.toFloat() / totalCount.toFloat()

    val todayTaskTotal: Int
        get() = reviewedTodayCount + dueCount
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
            repository.observeUpcoming(from = dueUntil, to = weekLater, limit = 100),
            notebookRepository.observeAll(),
            repository.observeAll(),
        ) { dueList, upcoming, notebooks, allQuestions ->
            Lists(
                dueList = dueList,
                upcoming = upcoming,
                notebookNames = notebooks.associate { it.id to it.name },
                subjectProgress = allQuestions.toSubjectProgress(),
            )
        }

        return combine(numbers, lists) { number, list ->
            ReviewUiState(
                dueCount = number.due,
                dueQuestions = list.dueList,
                reviewedTodayCount = number.reviewedToday,
                totalCount = number.total,
                masteredCount = number.mastered,
                upcoming = list.upcoming,
                subjectProgress = list.subjectProgress,
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
        val subjectProgress: List<SubjectProgress>,
    )
}

private fun List<WrongQuestionEntity>.toSubjectProgress(): List<SubjectProgress> {
    val grouped = groupBy { it.subject }
    return grouped.map { (subject, questions) ->
        SubjectProgress(
            subject = subject,
            mastered = questions.count { it.status == ReviewStatus.MASTERED.name },
            total = questions.size,
        )
    }.sortedBy { item ->
        val index = Subject.displayOrder.indexOfFirst { it.displayName == item.subject }
        if (index < 0) Int.MAX_VALUE else index
    }.take(5)
}
