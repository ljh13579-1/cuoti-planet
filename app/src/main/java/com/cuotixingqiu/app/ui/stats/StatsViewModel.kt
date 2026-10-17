package com.cuotixingqiu.app.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cuotixingqiu.app.data.db.NotebookCount
import com.cuotixingqiu.app.data.db.SubjectCount
import com.cuotixingqiu.app.data.repository.NotebookRepository
import com.cuotixingqiu.app.data.repository.WrongQuestionRepository
import com.cuotixingqiu.app.domain.ReviewStatus
import com.cuotixingqiu.app.util.TimeUtils
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/** 统计页的数据 */
data class StatsUiState(
    val totalCount: Int = 0,
    val masteredCount: Int = 0,
    val needsReviewCount: Int = 0,
    val dueTodayCount: Int = 0,
    val subjectCounts: List<SubjectCount> = emptyList(),
    val notebookCounts: List<NotebookCount> = emptyList(),
    val notebookNames: Map<Long, String> = emptyMap(),
    /** 难度 1..5 各有几道（索引 0 不使用） */
    val difficultyCounts: List<Int> = List(6) { 0 },
    /** 复习阶段 0..6 各有几道 */
    val stageCounts: List<Int> = List(7) { 0 },
    /** 近 7 天每天的复习次数（从 6 天前到今天） */
    val weeklyReviewCounts: List<Int> = List(7) { 0 },
    /** 近 7 天的日期标签（例如 10-11） */
    val weeklyLabels: List<String> = emptyList(),
)

/**
 * 统计页的 ViewModel。
 *
 * 图表数据全部在本地算好（不联网、不上传），使用“近 7 天复习次数”“学科分布”
 * “难度分布”“复习阶段分布”这四种最常见的视角。
 */
class StatsViewModel(
    repository: WrongQuestionRepository,
    notebookRepository: NotebookRepository,
) : ViewModel() {

    private val dayMillis = TimeUnit.DAYS.toMillis(1)
    private val today = TimeUtils.startOfTodayMillis()
    private val weekStart = today - 6 * dayMillis
    private val weekLabels: List<String> = (0..6).map { offset ->
        val date: LocalDate = TimeUtils.localDateOf(weekStart + offset * dayMillis)
        "%02d-%02d".format(date.monthValue, date.dayOfMonth)
    }

    private val allQuestions = repository.observeAll()

    val uiState: StateFlow<StatsUiState> = combine(
        combine(
            repository.observeTotalCount(),
            repository.observeCountByStatus(ReviewStatus.MASTERED),
            repository.observeDueCount(TimeUtils.endOfTodayMillis()),
            repository.observeSubjectCounts(),
        ) { total, mastered, dueToday, subjects ->
            Numbers(total, mastered, dueToday, subjects)
        },
        combine(
            allQuestions,
            repository.observeReviewTimestampsSince(weekStart),
            notebookRepository.observeAll(),
            repository.observeNotebookCounts(),
        ) { questions, reviewTimestamps, notebooks, notebookCounts ->
            Lists(
                difficulty = List(6) { level ->
                    if (level == 0) 0 else questions.count { it.difficulty == level }
                },
                stages = List(7) { stage -> questions.count { it.reviewStage == stage } },
                weekly = (0..6).map { offset ->
                    val dayStart = weekStart + offset * dayMillis
                    reviewTimestamps.count { it >= dayStart && it < dayStart + dayMillis }
                },
                notebookNames = notebooks.associate { it.id to it.name },
                notebookCounts = notebookCounts,
            )
        },
    ) { numbers, lists ->
        StatsUiState(
            totalCount = numbers.total,
            masteredCount = numbers.mastered,
            needsReviewCount = numbers.total - numbers.mastered,
            dueTodayCount = numbers.dueToday,
            subjectCounts = numbers.subjects,
            notebookCounts = lists.notebookCounts,
            notebookNames = lists.notebookNames,
            difficultyCounts = lists.difficulty,
            stageCounts = lists.stages,
            weeklyReviewCounts = lists.weekly,
            weeklyLabels = weekLabels,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StatsUiState(weeklyLabels = weekLabels),
    )

    private data class Numbers(
        val total: Int,
        val mastered: Int,
        val dueToday: Int,
        val subjects: List<SubjectCount>,
    )

    private data class Lists(
        val difficulty: List<Int>,
        val stages: List<Int>,
        val weekly: List<Int>,
        val notebookNames: Map<Long, String>,
        val notebookCounts: List<NotebookCount>,
    )
}

/** 把“学科分布”变成画饼图用的数据：名字 + 数量 */
fun StatsUiState.subjectChartItems(): List<Pair<String, Int>> =
    subjectCounts.map { it.subject to it.count }

/** 复习阶段的中文说明 */
fun stageLabel(stage: Int): String = when (stage) {
    0 -> "新题"
    1 -> "第 1 轮"
    2 -> "第 2 轮"
    3 -> "第 3 轮"
    4 -> "第 4 轮"
    5 -> "第 5 轮"
    else -> "已熟练"
}

/** 方便界面显示：某一天的复习次数 */
val StatsUiState.weeklyTotal: Int
    get() = weeklyReviewCounts.sum()

/** 供界面显示用：某个学科的占比文字 */
fun StatsUiState.percentText(count: Int): String =
    if (totalCount == 0) "0%" else "${(count * 100 / totalCount)}%"
