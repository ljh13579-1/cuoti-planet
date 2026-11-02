package com.cuotixingqiu.app.data

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cuotixingqiu.app.data.db.AppDatabase
import com.cuotixingqiu.app.data.db.NotebookEntity
import com.cuotixingqiu.app.data.db.WrongQuestionDao
import com.cuotixingqiu.app.data.db.WrongQuestionEntity
import com.cuotixingqiu.app.domain.ReviewStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * 数据库（Room）测试。
 *
 * 使用 Robolectric：真实运行 SQLite 数据库，但跑在电脑上，不需要手机。
 * 测试里用“内存数据库”，结束即销毁，不会污染真实数据。
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class)
class WrongQuestionDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: WrongQuestionDao

    private val now = 1_700_000_000_000L
    private val day = 24L * 3600 * 1000

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.wrongQuestionDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun question(
        subject: String = "数学",
        title: String = "",
        content: String = "",
        knowledgePoints: List<String> = emptyList(),
        status: String = ReviewStatus.NEEDS_REVIEW.name,
        nextReviewAt: Long? = now,
        createdAt: Long = now,
        difficulty: Int = 0,
    ) = WrongQuestionEntity(
        subject = subject,
        title = title,
        content = content,
        knowledgePoints = knowledgePoints,
        status = status,
        nextReviewAt = nextReviewAt,
        createdAt = createdAt,
        updatedAt = createdAt,
        difficulty = difficulty,
    )

    @Test
    fun `新增与按id查询`() = runTest {
        val id = dao.insert(question(title = "导数求单调区间"))

        val loaded = dao.getById(id)
        assertEquals("导数求单调区间", loaded?.title)
    }

    @Test
    fun `知识点列表可以正确存库和读回`() = runTest {
        val id = dao.insert(question(knowledgePoints = listOf("导数", "单调性")))

        val loaded = dao.getById(id)
        assertEquals(listOf("导数", "单调性"), loaded?.knowledgePoints)
    }

    @Test
    fun `搜索可以匹配标题内容和知识点`() = runTest {
        dao.insert(question(title = "导数求单调区间"))
        dao.insert(question(title = "别的题", content = "洛伦兹力方向"))
        dao.insert(question(title = "第三题", knowledgePoints = listOf("动量守恒")))

        val byTitle = dao.observeFiltered(null, null, null, 0, "导数", 0).first()
        assertEquals(1, byTitle.size)

        val byContent = dao.observeFiltered(null, null, null, 0, "洛伦兹", 0).first()
        assertEquals(1, byContent.size)

        val byTag = dao.observeFiltered(null, null, null, 0, "动量", 0).first()
        assertEquals(1, byTag.size)
    }

    @Test
    fun `按学科和难度筛选`() = runTest {
        dao.insert(question(subject = "数学", difficulty = 2))
        dao.insert(question(subject = "数学", difficulty = 5))
        dao.insert(question(subject = "物理", difficulty = 5))

        val mathOnly = dao.observeFiltered("数学", null, null, 0, "", 0).first()
        assertEquals(2, mathOnly.size)

        val hardMath = dao.observeFiltered("数学", null, null, 4, "", 0).first()
        assertEquals(1, hardMath.size)

        val hardAny = dao.observeFiltered(null, null, null, 5, "", 0).first()
        assertEquals(2, hardAny.size)
    }

    @Test
    fun `待复习列表排除已掌握和未到期的题`() = runTest {
        dao.insert(question(title = "已到期待复习", nextReviewAt = now - day))
        dao.insert(question(title = "未来才复习", nextReviewAt = now + day))
        dao.insert(
            question(
                title = "已掌握",
                status = ReviewStatus.MASTERED.name,
                nextReviewAt = now - day,
            )
        )

        val due = dao.observeDueList(ReviewStatus.MASTERED.name, now).first()
        assertEquals(1, due.size)
        assertEquals("已到期待复习", due.first().title)
    }

    @Test
    fun `统计查询数量正确`() = runTest {
        dao.insert(question(subject = "数学", createdAt = now))
        dao.insert(question(subject = "数学", createdAt = now - 10 * day))
        dao.insert(question(subject = "物理", createdAt = now - 10 * day))

        assertEquals(3, dao.observeTotalCount().first())

        val subjectCounts = dao.observeSubjectCounts().first()
        assertEquals(2, subjectCounts.first { it.subject == "数学" }.count)
        assertEquals(1, subjectCounts.first { it.subject == "物理" }.count)

        assertEquals(3, dao.observeDueCount(ReviewStatus.MASTERED.name, now + day).first())
    }

    @Test
    fun `记录复习结果会累加次数并更新状态`() = runTest {
        val id = dao.insert(question())

        dao.applyReviewResult(
            id = id,
            status = ReviewStatus.MASTERED.name,
            stage = 6,
            lastResult = "EASY",
            nextReviewAt = now + 30 * day,
            now = now + day,
        )

        val loaded = dao.getById(id)
        assertEquals(ReviewStatus.MASTERED.name, loaded?.status)
        assertEquals(6, loaded?.reviewStage)
        assertEquals(1, loaded?.reviewCount)
        assertEquals("EASY", loaded?.lastReviewResult)
    }

    @Test
    fun `批量删除与批量标记`() = runTest {
        val id1 = dao.insert(question(title = "第一题"))
        val id2 = dao.insert(question(title = "第二题"))
        val id3 = dao.insert(question(title = "第三题"))

        dao.updateStatusByIds(listOf(id1, id2), ReviewStatus.MASTERED.name, now)
        assertEquals(2, dao.observeCountByStatus(ReviewStatus.MASTERED.name).first())

        dao.deleteByIds(listOf(id1, id2))
        assertEquals(1, dao.observeTotalCount().first())
        assertNull(dao.getById(id1))
        assertTrue(dao.getById(id3) != null)
    }

    @Test
    fun `删除分类时错题回到未分类`() = runTest {
        val notebookId = db.notebookDao().insert(NotebookEntity(name = "导数专题", createdAt = now))
        val questionId = dao.insert(question()).let { id ->
            dao.update(dao.getById(id)!!.copy(notebookId = notebookId))
            id
        }

        assertEquals(notebookId, dao.getById(questionId)?.notebookId)

        dao.clearNotebookReferences(notebookId)
        assertNull(dao.getById(questionId)?.notebookId)
        assertEquals(1, dao.getAllOnce().size)
    }

    @Test
    fun `未来的复习安排按时间排序且只取范围内`() = runTest {
        dao.insert(question(title = "明天", nextReviewAt = now + day))
        dao.insert(question(title = "10 天后", nextReviewAt = now + 10 * day))
        dao.insert(
            question(
                title = "已掌握",
                status = ReviewStatus.MASTERED.name,
                nextReviewAt = now + day,
            )
        )

        val upcoming = dao
            .observeUpcoming(ReviewStatus.MASTERED.name, from = now, to = now + 7 * day, limit = 10)
            .first()

        assertEquals(1, upcoming.size)
        assertEquals("明天", upcoming.first().title)
    }

    @Test
    fun `重新加入复习计划会重置阶段和下次复习时间`() = runTest {
        val id = dao.insert(
            question(status = ReviewStatus.MASTERED.name, nextReviewAt = now + 30 * day)
        )

        dao.reopenForReview(
            id = id,
            status = ReviewStatus.NEEDS_REVIEW.name,
            stage = 0,
            nextReviewAt = now + day,
            now = now,
        )

        val loaded = dao.getById(id)
        assertEquals(ReviewStatus.NEEDS_REVIEW.name, loaded?.status)
        assertEquals(0, loaded?.reviewStage)
        assertEquals(now + day, loaded?.nextReviewAt)
    }
}
