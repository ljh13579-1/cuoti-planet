package com.cuotixingqiu.app.data

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cuotixingqiu.app.data.image.ImageStore
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File
import java.util.Base64

/** 图片仓库测试：图片内容能存能读、文件名安全、能删除 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class)
class ImageStoreTest {

    private lateinit var context: Context
    private lateinit var imageStore: ImageStore

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        imageStore = ImageStore(context)
    }

    @Test
    fun `图片可以保存并读回`() = runTest {
        val name = "IMG_roundtrip.jpg"
        val bytes = byteArrayOf(9, 8, 7, 6)
        val base64 = Base64.getEncoder().encodeToString(bytes)

        assertTrue(imageStore.saveBase64(name, base64))

        val file = imageStore.fileOrNull(name)
        assertNotNull(file)
        assertArrayEquals(bytes, file!!.readBytes())
        assertEquals(base64, imageStore.toBase64(name))
    }

    @Test
    fun `文件名里的路径会被过滤掉`() = runTest {
        val base64 = Base64.getEncoder().encodeToString(byteArrayOf(1, 2, 3))

        assertTrue(imageStore.saveBase64("../../evil.jpg", base64))

        // 只保留文件名本身，不会写到上级目录
        assertNotNull(imageStore.fileOrNull("evil.jpg"))
        val outside = File(context.filesDir.parentFile, "evil.jpg")
        assertFalse("不应该写到应用目录外面", outside.exists())
    }

    @Test
    fun `删除单个图片与清空图片`() = runTest {
        val base64 = Base64.getEncoder().encodeToString(byteArrayOf(5, 5, 5))
        imageStore.saveBase64("IMG_a.jpg", base64)
        imageStore.saveBase64("IMG_b.jpg", base64)

        imageStore.delete("IMG_a.jpg")
        assertNull(imageStore.fileOrNull("IMG_a.jpg"))
        assertNotNull(imageStore.fileOrNull("IMG_b.jpg"))

        imageStore.deleteAll()
        assertNull(imageStore.fileOrNull("IMG_b.jpg"))
    }

    @Test
    fun `读取不存在的图片会返回null而不是崩溃`() = runTest {
        assertNull(imageStore.toBase64("IMG_not_exist.jpg"))
        assertNull(imageStore.fileOrNull("IMG_not_exist.jpg"))
    }
}
