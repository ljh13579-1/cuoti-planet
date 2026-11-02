package com.cuotixingqiu.app.ui

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cuotixingqiu.app.data.db.AppDatabase
import com.cuotixingqiu.app.data.db.WrongQuestionEntity
import com.cuotixingqiu.app.data.export.PaperExporter
import com.cuotixingqiu.app.data.image.ImageStore
import com.cuotixingqiu.app.data.repository.NotebookRepository
import com.cuotixingqiu.app.data.repository.WrongQuestionRepository
import com.cuotixingqiu.app.domain.ReviewResult
import com.cuotixingqiu.app.domain.ReviewStatus
import com.cuotixingqiu.app.ui.home.HomeViewModel
import com.cuotixingqiu.app.ui.list.QuestionListViewModel
import com.cuotixingqiu.app.ui.list.QuestionSort
import com.cuotixingqiu.app.ui.paper.PaperScope
import com.cuotixingqiu.app.ui.paper.PaperViewModel
import com.cuotixingqiu.app.ui.review.ReviewSessionViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

/**
 * 页面逻辑（ViewModel）的测试。
 *
 * 界面本身需要在手机上才能看到，但“页面背后的逻辑”——搜索筛选、复习流程、
 * 组卷范围、首页统计——都可以在电脑上验证，这部分测试就是干这个的。
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class)
class ViewModelLogicTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var repository: WrongQuestionRepository
    private lateinit var notebookRepository: NotebookRepository

    private val day = TimeUnit.DAYS.toMillis(1)
    private val now = System.currentTimeMillis()

    @Before
    fun setUp() {
        // ViewModel 内部使用 Dispatchers.Main，测试里要替换成测试调度器
        Dispatchers.setMain(UnconfinedTestDispatcher())
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = WrongQuestionRepository(db, db.wrongQuestionDao(), ImageStore(context))
        notebookRepository =
            NotebookRepository(db, db.notebookDao(), db.wrongQuestionDao())
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    /**
     * 往数据库里放一道错题。
     * 测试里直接写 DAO，这样可以精确控制状态和复习时间（不走“新增自动排复习”的逻辑）。
     */
    private fun addQuestion(
        subject: String = "数学",
        title: String = "",
        content: String = "",
        knowledgePoints: List<String> = emptyList(),
        dueNow: Boolean = false,
        status: String = ReviewStatus.NEEDS_REVIEW.name,
        createdAt: Long = now,
    ): Long = kotlinx.coroutines.runBlocking {
        db.wrongQuestionDao().insert(
            WrongQuestionEntity(
                subject = subject,
                title = title,
                content = content,
                knowledgePoints = knowledgePoints,
                status = status,
                nextReviewAt = if (dueNow) now - day else now + day,
                createdAt = createdAt,
                updatedAt = createdAt,
            ),
        )
    }

    @Test
    fun `列表页可以按学科和搜索词筛选`() = runTest {
        addQuestion(subject = "数学", title = "导数求单调区间", knowledgePoints = listOf("导数"))
        addQuestion(subject = "物理", title = "洛伦兹力方向")
        addQuestion(subject = "数学", title = "数列求和")

        val viewModel = QuestionListViewModel(repository, notebookRepository)

        val all = viewModel.uiState.first { it.questions.size == 3 && it.totalCount == 3 }
        assertEquals(3, all.questions.size)

        viewModel.setSubject("数学")
        val mathOnly =
            viewModel.uiState.first { it.filter.subject == "数学" && it.questions.size == 2 }
        assertTrue(mathOnly.questions.all { it.subject == "数学" })

        viewModel.setSubject(null)
        viewModel.setSearch("洛伦兹")
        val searched = viewModel.uiState
            .first { it.filter.search == "洛伦兹" && it.questions.size == 1 }
        assertEquals("洛伦兹力方向", searched.questions.first().title)

        viewModel.setSearch("")
        viewModel.setKnowledgePoint("导数")
        val byTag = viewModel.uiState.first { state ->
            state.filter.knowledgePoint == "导数" &&
                state.questions.size == 1 &&
                state.questions.all { "导数" in it.knowledgePoints }
        }
        assertEquals("导数求单调区间", byTag.questions.first().title)
    }

    @Test
    fun `列表页排序与批量标记`() = runTest {
        val first = addQuestion(title = "先添加的", createdAt = now - 60_000)
        val second = addQuestion(title = "后添加的", createdAt = now)

        val viewModel = QuestionListViewModel(repository, notebookRepository)
        val newestFirst = viewModel.uiState.first { it.questions.size == 2 }
        // 默认“最新优先”（后添加的排在前面）
        assertEquals(listOf(second, first), newestFirst.questions.map { it.id })

        viewModel.setSort(QuestionSort.OLDEST)
        // 改动排序条件时，combine 可能会先吐出一个“新排序 + 旧列表”的瞬时状态，
        // 所以这里等到数据库按“最早优先”重新查询出正确顺序之后再断言。
        val oldestFirst = viewModel.uiState.first { state ->
            state.filter.sort == QuestionSort.OLDEST &&
                state.questions.map { it.id } == listOf(first, second)
        }
        assertEquals(listOf(first, second), oldestFirst.questions.map { it.id })

        // 批量标记为已掌握（用 first{} 等待数据库把写入落盘后再检查）
        viewModel.selectAll(listOf(first, second))
        viewModel.markSelected(ReviewStatus.MASTERED)
        assertEquals(2, repository.observeCountByStatus(ReviewStatus.MASTERED).first { it == 2 })

        // 批量删除
        viewModel.selectAll(listOf(first, second))
        viewModel.deleteSelected()
        val remaining = viewModel.uiState.first { it.questions.isEmpty() && it.totalCount == 0 }
        assertEquals(0, remaining.totalCount)
    }

    @Test
    fun `复习会话按顺序出题并在答完后结束`() = runTest {
        addQuestion(title = "第一道", dueNow = true)
        addQuestion(title = "第二道", dueNow = true)
        // 已经掌握的题目不应该出现在复习列表里
        addQuestion(title = "已掌握", dueNow = true, status = ReviewStatus.MASTERED.name)

        val viewModel = ReviewSessionViewModel(repository, onlyQuestionId = null)
        val loaded = viewModel.uiState.first { !it.loading }

        assertEquals(2, loaded.questions.size)
        assertNotNull(loaded.current)

        viewModel.answer(ReviewResult.AGAIN)
        val second = viewModel.uiState.first { it.index == 1 }
        assertEquals(1, second.againCount)
        assertTrue(second.revealed.not())

        viewModel.answer(ReviewResult.EASY)
        val finished = viewModel.uiState.first { it.finished }
        assertEquals(2, finished.answeredCount)
        assertEquals(1, finished.easyCount)

        // 数据库里确实记录了复习结果：
        // 原有 1 道“已掌握” + 刚把“第二道”答成“已掌握” = 2 道
        assertEquals(2, repository.observeCountByStatus(ReviewStatus.MASTERED).first { it == 2 })
    }

    @Test
    fun `只复习指定的一道题`() = runTest {
        addQuestion(title = "指定题", dueNow = true)
        addQuestion(title = "别的题", dueNow = true)
        val targetId = repository.observeAll().first().first { it.title == "指定题" }.id

        val viewModel = ReviewSessionViewModel(repository, onlyQuestionId = targetId)
        val loaded = viewModel.uiState.first { !it.loading }

        assertEquals(1, loaded.questions.size)
        assertEquals("指定题", loaded.questions.first().title)
    }

    @Test
    fun `首页统计显示总数与今日新增`() = runTest {
        addQuestion(title = "今天的题")
        addQuestion(title = "另一道题")

        val viewModel = HomeViewModel(repository, notebookRepository)
        val state = viewModel.uiState.first { it.totalCount == 2 }

        assertEquals(2, state.totalCount)
        assertEquals(2, state.todayAddedCount)
        assertEquals(0, state.dueCount)
        assertEquals(2, state.subjectCounts.sumOf { it.count })
    }

    @Test
    fun `组卷按范围计算题目数量`() = runTest {
        addQuestion(subject = "数学", title = "数学题")
        addQuestion(subject = "物理", title = "物理题", status = ReviewStatus.MASTERED.name)
        repository.observeAll().first()
        val exporter = PaperExporter(context, ImageStore(context))

        val viewModel = PaperViewModel(
            repository = repository,
            notebookRepository = notebookRepository,
            paperExporter = exporter,
            selectedIds = emptyList(),
        )

        val all = viewModel.uiState.first { it.candidateCount == 2 }
        assertEquals(PaperScope.ALL, all.settings.scope)

        viewModel.setScope(PaperScope.MASTERED)
        val mastered = viewModel.uiState.first { it.settings.scope == PaperScope.MASTERED }
        assertEquals(1, mastered.candidateCount)

        viewModel.setScope(PaperScope.NEED_REVIEW)
        val needsReview = viewModel.uiState.first { it.settings.scope == PaperScope.NEED_REVIEW }
        assertEquals(1, needsReview.candidateCount)

        viewModel.setScope(PaperScope.SUBJECT)
        viewModel.setSubject("物理")
        val physics = viewModel.uiState.first { it.settings.subject == "物理" }
        assertEquals(1, physics.candidateCount)
    }
}
