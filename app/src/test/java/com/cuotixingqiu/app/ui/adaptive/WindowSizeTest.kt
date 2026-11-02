package com.cuotixingqiu.app.ui.adaptive

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 窗口宽度分档的测试。
 *
 * 界面本身要上手机或平板才能看到，但“多宽算宽屏、什么时候切两栏”这个规则
 * 是纯计算，可以在电脑上直接验证，避免改断点时把手机竖屏误判成宽屏。
 */
class WindowSizeTest {

    @Test
    fun 手机竖屏算紧凑宽度() {
        assertEquals(WindowWidthSize.COMPACT, windowWidthSizeOf(320))
        assertEquals(WindowWidthSize.COMPACT, windowWidthSizeOf(360))
        assertEquals(WindowWidthSize.COMPACT, windowWidthSizeOf(411))
        assertEquals(WindowWidthSize.COMPACT, windowWidthSizeOf(599))
    }

    @Test
    fun 中等宽度从600dp开始() {
        assertEquals(WindowWidthSize.MEDIUM, windowWidthSizeOf(600))
        assertEquals(WindowWidthSize.MEDIUM, windowWidthSizeOf(720))
        assertEquals(WindowWidthSize.MEDIUM, windowWidthSizeOf(839))
    }

    @Test
    fun 展开宽度从840dp开始() {
        assertEquals(WindowWidthSize.EXPANDED, windowWidthSizeOf(840))
        assertEquals(WindowWidthSize.EXPANDED, windowWidthSizeOf(1024))
        assertEquals(WindowWidthSize.EXPANDED, windowWidthSizeOf(1280))
    }
}
