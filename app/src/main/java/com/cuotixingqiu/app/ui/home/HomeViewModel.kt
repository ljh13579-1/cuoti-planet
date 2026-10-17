package com.cuotixingqiu.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cuotixingqiu.app.data.db.SubjectCount
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

/** 首页需要的全部数据 */
data class HomeUiState(
    val totalCount: Int = 0,
    val todayAddedCount: Int = 0,
    val dueCount: Int = 0,
    val masteredCount: Int = 0,
    val reviewedTodayCount: Int = 0,
    val subjectCounts: List<SubjectCount> = emptyList(),
    val recentQuestions: List<WrongQuestionEntity> = emptyList(),
    val dueQuestions: List<WrongQuestionEntity> = emptyList(),
    val notebookNames: Map<Long, String> = emptyMap(),
)

/**
 * 首页的 ViewModel。
 *
 * 注意：所有数据都来自数据库的 Flow（“自动刷新”），
 * 所以新增、删除、复习之后，首页数字会立刻变化，不需要手动刷新。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val repository: WrongQuestionRepository,
    notebookRepository: NotebookRepository,
) : ViewModel() {

    /** 每次进入首页就 +1，用来重新计算“今天”（防止跨天后数字不更新） */
    private val refreshTick = MutableStateFlow(0)

    val uiState: StateFlow<HomeUiState> = refreshTick
        .flatMapLatest { buildStateFlow(notebookRepository) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState(),
        )

    fun refresh() {
        refreshTick.value = refreshTick.value + 1
    }

    private fun buildStateFlow(notebookRepository: NotebookRepository): Flow<HomeUiState> {
        val dayStart = TimeUtils.startOfTodayMillis()
        val dueUntil = TimeUtils.endOfTodayMillis()

        val numbers = combine(
            repository.observeTotalCount(),
            repository.observeCountCreatedSince(dayStart),
            repository.observeDueCount(dueUntil),
            repository.observeReviewedCountSince(dayStart),
            repository.observeCountByStatus(ReviewStatus.MASTERED),
        ) { total, today, due, reviewedToday, mastered ->
            NumberSnapshot(total, today, due, reviewedToday, mastered)
        }

        val lists = combine(
            repository.observeSubjectCounts(),
            repository.observeRecent(RECENT_LIMIT),
            repository.observeDueList(dueUntil),
            notebookRepository.observeAll(),
        ) { subjects, recent, dueList, notebooks ->
            ListSnapshot(subjects, recent, dueList, notebooks.associate { it.id to it.name })
        }

        return combine(numbers, lists) { number, list ->
            HomeUiState(
                totalCount = number.total,
                todayAddedCount = number.today,
                dueCount = number.due,
                masteredCount = number.mastered,
                reviewedTodayCount = number.reviewedToday,
                subjectCounts = list.subjects,
                recentQuestions = list.recent,
                dueQuestions = list.dueList.take(DUE_PREVIEW_LIMIT),
                notebookNames = list.notebookNames,
            )
        }
    }

    private data class NumberSnapshot(
        val total: Int,
        val today: Int,
        val due: Int,
        val reviewedToday: Int,
        val mastered: Int,
    )

    private data class ListSnapshot(
        val subjects: List<SubjectCount>,
        val recent: List<WrongQuestionEntity>,
        val dueList: List<WrongQuestionEntity>,
        val notebookNames: Map<Long, String>,
    )

    private companion object {
        const val RECENT_LIMIT = 5
        const val DUE_PREVIEW_LIMIT = 4
    }
}
