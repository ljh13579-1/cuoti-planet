package com.cuotixingqiu.app.data

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cuotixingqiu.app.data.backup.BackupManager
import com.cuotixingqiu.app.data.backup.ImportResult
import com.cuotixingqiu.app.data.db.AppDatabase
import com.cuotixingqiu.app.data.db.NotebookEntity
import com.cuotixingqiu.app.data.db.WrongQuestionEntity
import com.cuotixingqiu.app.data.image.ImageStore
import com.cuotixingqiu.app.domain.ReviewStatus
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.Base64

/**
 * 数据备份（导出 / 导入）的测试。
 *
 * 重点验证三件事：
 *  1. 导出再导入，内容（含分类、知识点、难度）不丢失；
 *  2. 导入是“合并模式”，不会覆盖或删除已有错题；
 *  3. 文件损坏、版本不认识时，导入失败但原有数据完好。
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class)
class BackupManagerTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var imageStore: ImageStore
    private lateinit var manager: BackupManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        imageStore = ImageStore(context)
        manager = BackupManager(
            context = context,
            db = db,
            questionDao = db.wrongQuestionDao(),
            notebookDao = db.notebookDao(),
            imageStore = imageStore,
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun question(
        title: String = "导数题",
        imageNames: List<String> = emptyList(),
        notebookId: Long? = null,
    ) = WrongQuestionEntity(
        subject = "数学",
        title = title,
        content = "求单调区间",
        knowledgePoints = listOf("导数", "单调性"),
        reason = "公式记错",
        solution = "先求导，再判断符号",
        summary = "注意定义域",
        difficulty = 3,
        imageNames = imageNames,
        notebookId = notebookId,
        createdAt = 1_700_000_000_000L,
        updatedAt = 1_700_000_000_000L,
    )

    @Test
    fun `导出后再导入内容不丢失`() = runTest {
        val notebookId = db.notebookDao().insert(NotebookEntity(name = "导数专题", createdAt = 1000L))
        db.wrongQuestionDao().insert(question(notebookId = notebookId))

        val json = manager.exportJson(includeImages = false)

        // 模拟“换了一台新手机”：先清空，再导入
        db.wrongQuestionDao().deleteAll()
        db.notebookDao().deleteAll()

        val result = manager.importJson(json)

        assertTrue("导入应该成功，实际是 $result", result is ImportResult.Success)
        val loaded = db.wrongQuestionDao().getAllOnce().single()
        assertEquals("导数题", loaded.title)
        assertEquals("数学", loaded.subject)
        assertEquals(listOf("导数", "单调性"), loaded.knowledgePoints)
        assertEquals(3, loaded.difficulty)
        assertEquals("先求导，再判断符号", loaded.solution)

        // 分类只保存了名字，导入时会自动重建
        val notebooks = db.notebookDao().getAllOnce()
        assertEquals(1, notebooks.size)
        assertEquals("导数专题", notebooks.single().name)
        assertEquals(notebooks.single().id, loaded.notebookId)
    }

    @Test
    fun `导入是合并模式不会覆盖已有错题`() = runTest {
        val originalId = db.wrongQuestionDao().insert(question(title = "手机上已有的题"))
        val json = manager.exportJson(includeImages = false)

        manager.importJson(json)
        manager.importJson(json)

        val all = db.wrongQuestionDao().getAllOnce()
        // 原有的 1 道 + 两次导入各生成 1 道新记录
        assertEquals(3, all.size)
        // 原有记录没有被覆盖或删除
        assertNotNull(all.firstOrNull { it.id == originalId })
        // 每次导入都是全新记录（合并模式），id 互不相同
        assertEquals(3, all.map { it.id }.distinct().size)
    }

    @Test
    fun `导入损坏的文件会失败且不影响原有数据`() = runTest {
        db.wrongQuestionDao().insert(question(title = "原有错题"))

        val result = manager.importJson("{ 这不是合法的 JSON }")

        assertTrue(result is ImportResult.Failure)
        assertEquals(1, db.wrongQuestionDao().getAllOnce().size)
    }

    @Test
    fun `导入更新版本的备份会失败并提示升级`() = runTest {
        val json = """
            {
              "app": "CuotiPlanet",
              "schemaVersion": 99,
              "questions": [ { "subject": "数学", "title": "来自未来的题" } ]
            }
        """.trimIndent()

        val result = manager.importJson(json)

        assertTrue(result is ImportResult.Failure)
        val reason = (result as ImportResult.Failure).reason
        assertTrue("提示里应该说明版本问题，实际是：$reason", reason.contains("版本"))
        assertEquals(0, db.wrongQuestionDao().getAllOnce().size)
    }

    @Test
    fun `没有错题的备份文件不会被导入`() = runTest {
        val json = """{ "app": "CuotiPlanet", "schemaVersion": 1, "questions": [] }"""

        val result = manager.importJson(json)

        assertTrue(result is ImportResult.Failure)
    }

    @Test
    fun `包含图片的备份可以恢复图片文件`() = runTest {
        val fileName = "IMG_test_backup.jpg"
        val bytes = byteArrayOf(10, 20, 30, 40, 50)
        val base64 = Base64.getEncoder().encodeToString(bytes)
        assertTrue(imageStore.saveBase64(fileName, base64))

        db.wrongQuestionDao().insert(question(title = "带图片的题", imageNames = listOf(fileName)))

        val json = manager.exportJson(includeImages = true)

        // 模拟图片文件丢失（例如换了手机）
        imageStore.delete(fileName)
        assertNull(imageStore.fileOrNull(fileName))

        val result = manager.importJson(json)

        assertTrue(result is ImportResult.Success)
        val restored = imageStore.fileOrNull(fileName)
        assertNotNull("导入后图片文件应该被写回来", restored)
        assertArrayEquals(bytes, restored!!.readBytes())
    }

    @Test
    fun `导入的错题状态与复习进度会保留`() = runTest {
        val entity = question().copy(
            status = ReviewStatus.MASTERED.name,
            reviewStage = 6,
            reviewCount = 4,
            lastReviewResult = "EASY",
            lastReviewedAt = 1_700_000_000_000L,
        )
        db.wrongQuestionDao().insert(entity)

        val json = manager.exportJson(includeImages = false)
        db.wrongQuestionDao().deleteAll()

        manager.importJson(json)

        val loaded = db.wrongQuestionDao().getAllOnce().single()
        assertEquals(ReviewStatus.MASTERED.name, loaded.status)
        assertEquals(6, loaded.reviewStage)
        assertEquals(4, loaded.reviewCount)
    }
}
