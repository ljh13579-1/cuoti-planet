package com.cuotixingqiu.app.ui

import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cuotixingqiu.app.ui.navigation.Routes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * 悬浮 Dock 的导航契约测试。
 *
 * 底部导航从实心 NavigationBar 改成悬浮玻璃 Dock 后，4 个目的地的数量、顺序、标签
 * 必须和改造前完全一致（不多、不少、不换位置），这个文件把它固定下来，
 * 以后有人改 Dock 样式时不会顺手把导航项改坏。
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class DockTabsTest {

    @Test
    fun 底部导航保持四个目的地() {
        assertEquals(4, cuotiTabs.size)
        assertEquals(4, cuotiTabs.map { it.route }.distinct().size)
    }

    @Test
    fun 路由顺序与改造前一致() {
        assertEquals(
            listOf(Routes.HOME, Routes.QUESTIONS, Routes.REVIEW, Routes.PROFILE),
            cuotiTabs.map { it.route },
        )
    }

    @Test
    fun 标签文字与改造前一致() {
        assertEquals(
            listOf("首页", "错题", "复习", "我的"),
            cuotiTabs.map { it.label },
        )
    }

    @Test
    fun 每一项都有图标且标签非空() {
        cuotiTabs.forEach { item ->
            assertTrue("标签不能为空：${item.route}", item.label.isNotBlank())
            assertTrue("每一项都要有可用图标：${item.route}", item.icon.defaultWidth > 0.dp)
        }
    }
}
