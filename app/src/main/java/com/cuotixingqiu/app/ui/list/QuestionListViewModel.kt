package com.cuotixingqiu.app.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cuotixingqiu.app.data.db.NotebookEntity
import com.cuotixingqiu.app.data.db.WrongQuestionEntity
import com.cuotixingqiu.app.data.repository.NotebookRepository
import com.cuotixingqiu.app.data.repository.WrongQuestionRepository
import com.cuotixingqiu.app.domain.ReviewStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 列表排序方式 */
enum class QuestionSort(val displayName: String, val daoValue: Int) {
    NEWEST("最新优先", 0),
    OLDEST("最早优先", 1),
    DUE_FIRST("待复习优先", 2),
}

/** 列表页的筛选条件 */
data class QuestionFilter(
    val subject: String? = null,
    val notebookId: Long? = null,
    val status: ReviewStatus? = null,
    val minDifficulty: Int = 0,
    val knowledgePoint: String? = null,
    val search: String = "",
    val sort: QuestionSort = QuestionSort.NEWEST,
) {
    /** 除了搜索和排序之外，是否还设置了“更多筛选” */
    val hasExtraFilter: Boolean
        get() = status != null || minDifficulty > 0 || knowledgePoint != null || notebookId != null
}

/** 列表页的界面状态 */
data class QuestionListUiState(
    val questions: List<WrongQuestionEntity> = emptyList(),
    val filter: QuestionFilter = QuestionFilter(),
    val selection: Set<Long> = emptySet(),
    val notebooks: List<NotebookEntity> = emptyList(),
    val notebookNames: Map<Long, String> = emptyMap(),
    val knowledgePoints: List<String> = emptyList(),
    val totalCount: Int = 0,
) {
    val selectionMode: Boolean get() = selection.isNotEmpty()
}

/**
 * 错题列表页的 ViewModel：负责搜索、筛选、排序和多选批量操作。
 *
 * 筛选条件一变，就重新订阅数据库查询（flatMapLatest），因此界面永远是“最新数据”。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class QuestionListViewModel(
    private val repository: WrongQuestionRepository,
    private val notebookRepository: NotebookRepository,
) : ViewModel() {

    private val filterState = MutableStateFlow(QuestionFilter())
    private val selectionState = MutableStateFlow<Set<Long>>(emptySet())

    private val questionsFlow: Flow<List<WrongQuestionEntity>> = filterState.flatMapLatest { filter ->
        repository.observeFiltered(
            subject = filter.subject,
            notebookId = filter.notebookId,
            status = filter.status?.name,
            minDifficulty = filter.minDifficulty,
            search = filter.search,
            sort = filter.sort.daoValue,
        ).map { list ->
            // 知识点筛选在内存里做：数据量对手机来说很小，这样 SQL 更简单
            val point = filter.knowledgePoint
            if (point == null) list else list.filter { point in it.knowledgePoints }
        }
    }

    /** 所有出现过的知识点（给筛选面板用） */
    private val knowledgePointsFlow: Flow<List<String>> = repository.observeAll().map { list ->
        list.flatMap { it.knowledgePoints }.distinct().sorted()
    }

    val uiState: StateFlow<QuestionListUiState> = combine(
        combine(
            questionsFlow,
            filterState,
            selectionState,
            notebookRepository.observeAll(),
        ) { questions, filter, selection, notebooks -> Quadruple(questions, filter, selection, notebooks) },
        repository.observeTotalCount(),
        knowledgePointsFlow,
    ) { base, totalCount, knowledgePoints ->
        QuestionListUiState(
            questions = base.questions,
            filter = base.filter,
            selection = base.selection,
            notebooks = base.notebooks,
            notebookNames = base.notebooks.associate { it.id to it.name },
            knowledgePoints = knowledgePoints,
            totalCount = totalCount,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = QuestionListUiState(),
    )

    // ---------------- 筛选 ----------------

    fun setSearch(text: String) = filterState.update { it.copy(search = text) }

    fun setSubject(subject: String?) = filterState.update { it.copy(subject = subject) }

    fun setNotebook(notebookId: Long?) = filterState.update { it.copy(notebookId = notebookId) }

    fun setStatus(status: ReviewStatus?) = filterState.update { it.copy(status = status) }

    fun setMinDifficulty(value: Int) = filterState.update { it.copy(minDifficulty = value) }

    fun setKnowledgePoint(point: String?) = filterState.update { it.copy(knowledgePoint = point) }

    fun setSort(sort: QuestionSort) = filterState.update { it.copy(sort = sort) }

    /** 重置“更多筛选”，保留搜索词和排序 */
    fun resetExtraFilters() = filterState.update {
        it.copy(
            notebookId = null,
            status = null,
            minDifficulty = 0,
            knowledgePoint = null,
        )
    }

    // ---------------- 多选与批量操作 ----------------

    fun startSelection(id: Long) = selectionState.update { it + id }

    fun toggleSelection(id: Long) = selectionState.update { current ->
        if (id in current) current - id else current + id
    }

    fun selectAll(ids: List<Long>) = selectionState.update { ids.toSet() }

    fun clearSelection() = selectionState.update { emptySet() }

    /** 批量标记状态 */
    fun markSelected(status: ReviewStatus) {
        val ids = selectionState.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.setStatusForIds(ids, status)
            clearSelection()
        }
    }

    /** 批量删除（界面层必须先弹确认框） */
    fun deleteSelected() {
        val ids = selectionState.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.deleteByIds(ids)
            clearSelection()
        }
    }

    /** 删除单条 */
    fun deleteOne(id: Long) {
        viewModelScope.launch { repository.delete(id) }
    }

    /** 单条改状态 */
    fun setStatusForOne(id: Long, status: ReviewStatus) {
        viewModelScope.launch { repository.setStatusForIds(listOf(id), status) }
    }

    private data class Quadruple(
        val questions: List<WrongQuestionEntity>,
        val filter: QuestionFilter,
        val selection: Set<Long>,
        val notebooks: List<NotebookEntity>,
    )
}
