package com.cuotixingqiu.app.data

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cuotixingqiu.app.data.db.AppDatabase
import com.cuotixingqiu.app.data.db.WrongQuestionEntity
import com.cuotixingqiu.app.data.image.ImageStore
import com.cuotixingqiu.app.data.repository.WrongQuestionRepository
import com.cuotixingqiu.app.domain.ReviewResult
import com.cuotixingqiu.app.domain.ReviewScheduler
import com.cuotixingqiu.app.domain.ReviewStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.Base64
import java.util.concurrent.TimeUnit

/**
 * 仓库层（错题仓库）的测试：验证“新增自动排复习”“删除连图片一起删”
 * “重新加入复习计划”“清空数据”等规则，全部在电脑上跑，不需要手机。
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class)
class WrongQuestionRepositoryTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var imageStore: ImageStore
    private lateinit var repository: WrongQuestionRepository

    private val day = TimeUnit.DAYS.toMillis(1)

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        imageStore = ImageStore(context)
        repository = WrongQuestionRepository(db, db.wrongQuestionDao(), imageStore)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun question(title: String = "错题", imageNames: List<String> = emptyList()) =
        WrongQuestionEntity(
            subject = "数学",
            title = title,
            imageNames = imageNames,
            createdAt = 0L,
            updatedAt = 0L,
        )

    /** 往私有目录写一个假图片，返回文件名 */
    private suspend fun fakeImage(name: String): String {
        val base64 = Base64.getEncoder().encodeToString(byteArrayOf(1, 2, 3))
        assertTrue(imageStore.saveBase64(name, base64))
        return name
    }

    @Test
    fun `新增错题会自动补齐时间并安排次日复习`() = runTest {
        val before = System.currentTimeMillis()

        val id = repository.add(question())
        val saved = repository.getById(id)

        assertNotNull(saved)
        assertEquals(ReviewStatus.NEEDS_REVIEW.name, saved!!.status)
        assertTrue("创建时间应该被自动补上", saved.createdAt >= before)
        assertNotNull(saved.nextReviewAt)
        // 首次复习时间 = 创建时间 + 1 天
        assertEquals(
            ReviewScheduler.initialNextReviewAt(saved.createdAt),
            saved.nextReviewAt,
        )
    }

    @Test
    fun `删除错题时图片文件会一起删除`() = runTest {
        val name = fakeImage("IMG_delete_me.jpg")
        val id = repository.add(question(imageNames = listOf(name)))
        assertNotNull(imageStore.fileOrNull(name))

        repository.delete(id)

        assertNull(repository.getById(id))
        assertNull("删除错题后不应残留图片", imageStore.fileOrNull(name))
    }

    @Test
    fun `批量删除错题时图片也会一起删除`() = runTest {
        val name1 = fakeImage("IMG_batch_1.jpg")
        val name2 = fakeImage("IMG_batch_2.jpg")
        val id1 = repository.add(question(title = "题一", imageNames = listOf(name1)))
        val id2 = repository.add(question(title = "题二", imageNames = listOf(name2)))
        val id3 = repository.add(question(title = "题三"))

        repository.deleteByIds(listOf(id1, id2))

        assertNull(imageStore.fileOrNull(name1))
        assertNull(imageStore.fileOrNull(name2))
        assertNotNull(repository.getById(id3))
    }

    @Test
    fun `重新加入复习计划会把阶段归零并安排到明天`() = runTest {
        val id = repository.add(question())
        // 先复习到“已掌握”
        repository.applyReview(repository.getById(id)!!, ReviewResult.EASY)
        assertEquals(ReviewStatus.MASTERED.name, repository.getById(id)!!.status)

        repository.reopenForReview(id)

        val reopened = repository.getById(id)!!
        assertEquals(ReviewStatus.NEEDS_REVIEW.name, reopened.status)
        assertEquals(0, reopened.reviewStage)
        assertTrue(reopened.nextReviewAt!! in (System.currentTimeMillis() + day - 60_000)..(System.currentTimeMillis() + day + 60_000))
    }

    @Test
    fun `清空数据会连图片一起清空`() = runTest {
        val name = fakeImage("IMG_clear_all.jpg")
        repository.add(question(imageNames = listOf(name)))
        repository.add(question(title = "第二题"))

        repository.deleteAll()

        assertEquals(0, repository.observeAll().first().size)
        assertNull(imageStore.fileOrNull(name))
    }

    @Test
    fun `复习结果会改变待复习数量`() = runTest {
        val id = repository.add(question())
        val saved = repository.getById(id)!!

        // 刚新增时还没到复习时间，所以待复习为 0
        assertEquals(0, repository.observeDueCount(System.currentTimeMillis()).first())

        // 到了明天就该复习了
        assertEquals(1, repository.observeDueCount(saved.nextReviewAt!! + 1).first())

        // 答“已掌握”之后就不再出现在待复习列表里
        repository.applyReview(repository.getById(id)!!, ReviewResult.EASY)
        assertEquals(0, repository.observeDueCount(saved.nextReviewAt!! + day).first())
        assertEquals(1, repository.observeCountByStatus(ReviewStatus.MASTERED).first())
    }

    @Test
    fun `批量标记状态`() = runTest {
        val id1 = repository.add(question(title = "题一"))
        val id2 = repository.add(question(title = "题二"))

        repository.setStatusForIds(listOf(id1, id2), ReviewStatus.MASTERED)

        assertEquals(2, repository.observeCountByStatus(ReviewStatus.MASTERED).first())
        assertFalse(repository.observeAll().first().any { it.status == ReviewStatus.NEEDS_REVIEW.name })
    }
}
