package com.cuotixingqiu.app.ui.edit

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cuotixingqiu.app.data.db.AppDatabase
import com.cuotixingqiu.app.data.image.ImageStore
import com.cuotixingqiu.app.data.repository.NotebookRepository
import com.cuotixingqiu.app.data.repository.WrongQuestionRepository
import com.cuotixingqiu.app.util.MediaUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File

/**
 * 拍照兜底逻辑的测试：
 *
 * 有些系统（如鸿蒙卓易通）会丢失“拍照成功”的结果标志，但照片其实已经写进了
 * 临时文件。这里的逻辑不看标志、只看文件，保证照片不丢、失败也有明确提示。
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class)
class CameraCaptureRecoveryTest {

    /** 和页面里完全一致的提示文案（改文案时这里要同步改） */
    private val noPhotoMessage = "拍照没有返回照片，请重试，或改用「从相册选择」"

    private lateinit var context: Context
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        // ViewModel 内部使用 Dispatchers.Main，测试里要替换成测试调度器
        Dispatchers.setMain(UnconfinedTestDispatcher())
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    private fun newViewModel(): EditViewModel {
        val repository = WrongQuestionRepository(db, db.wrongQuestionDao(), ImageStore(context))
        val notebookRepository =
            NotebookRepository(db, db.notebookDao(), db.wrongQuestionDao())
        return EditViewModel(
            repository = repository,
            notebookRepository = notebookRepository,
            imageStore = ImageStore(context),
            questionId = null,
        )
    }

    private fun captureFile(name: String, bytes: ByteArray?): File {
        val dir = File(context.cacheDir, "camera").apply { mkdirs() }
        return File(dir, name).apply {
            if (bytes != null) writeBytes(bytes)
        }
    }

    @Test
    fun `临时文件是空的会明确提示并清理文件`() = runTest {
        val viewModel = newViewModel()
        val empty = captureFile("empty_capture.jpg", byteArrayOf())
        assertTrue(empty.exists())

        viewModel.onCameraLaunched(empty.absolutePath)
        viewModel.checkPendingCapture()

        assertEquals(noPhotoMessage, viewModel.uiState.value.message)
        assertFalse("空的临时文件应该被清理", empty.exists())
    }

    @Test
    fun `文件不存在时同样给提示且重复检查是安全的`() = runTest {
        val viewModel = newViewModel()
        val ghost = File(File(context.cacheDir, "camera"), "ghost.jpg")

        viewModel.onCameraLaunched(ghost.absolutePath)
        viewModel.checkPendingCapture()
        assertEquals(noPhotoMessage, viewModel.uiState.value.message)

        // 待处理状态已经被消费，再检查一次不应该重复提示
        viewModel.clearMessage()
        viewModel.checkPendingCapture()
        assertNull(viewModel.uiState.value.message)
    }

    @Test
    fun `文件里有照片时会走导入流程并清理源文件`() = runTest {
        val viewModel = newViewModel()
        val captured = captureFile("captured.jpg", byteArrayOf(1, 2, 3, 4, 5))

        viewModel.onCameraLaunched(captured.absolutePath)
        viewModel.checkPendingCapture()

        // 有内容的文件不能被当成“没有照片”
        assertNotEquals(noPhotoMessage, viewModel.uiState.value.message)

        // 导入在后台线程压缩，等它把临时文件消费掉（成功或失败都会清理）
        val deadline = System.currentTimeMillis() + 10_000L
        while (captured.exists() && System.currentTimeMillis() < deadline) {
            Thread.sleep(20)
        }
        assertFalse("源临时文件应该在导入后被清理", captured.exists())
    }

    @Test
    fun `创建拍照文件时会清理旧的空文件和过期残留`() = runTest {
        val staleEmpty = captureFile("stale_empty.jpg", byteArrayOf())
        val staleOld = captureFile("stale_old.jpg", byteArrayOf(1, 2, 3)).apply {
            setLastModified(System.currentTimeMillis() - 2 * 24 * 60 * 60 * 1000L)
        }
        val keep = captureFile("keep_recent.jpg", byteArrayOf(4, 5, 6))

        val created = MediaUtils.createCameraFile(context)

        assertTrue(created.exists())
        assertEquals(
            File(context.cacheDir, "camera").absolutePath,
            created.parentFile?.absolutePath,
        )
        assertFalse("0 字节的旧文件应该被清理", staleEmpty.exists())
        assertFalse("超过一天的残留应该被清理", staleOld.exists())
        assertTrue("有内容的近期文件应该保留", keep.exists())
    }
}
