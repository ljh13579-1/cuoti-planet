package com.cuotixingqiu.app.ui.components

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 悬浮 Dock 的宽度计算测试（纯计算，跑在电脑上，不需要手机）。
 *
 * 把两条布局约定固定下来：
 *  1. 手机竖屏（< 600dp）保持现状：铺满可用宽度、左右各留 14dp；
 *  2. 更宽的窗口（手机横屏 / 平板）更舒展：比竖屏宽约 20%～30%，但绝不接近全屏宽度。
 * 以后调响应式参数时，先在这里暴露变化。
 */
class DockWidthTest {

    private val epsilon = 0.01f

    @Test
    fun 手机竖屏铺满可用宽度只留十四dp边距() {
        assertEquals(332f, dockWidthFor(360.dp, 4).value, epsilon)
        assertEquals(384f, dockWidthFor(412.dp, 4).value, epsilon)
        assertEquals(571f, dockWidthFor(599.dp, 4).value, epsilon)
    }

    @Test
    fun 平板横屏比手机竖屏宽百分之二十到三十() {
        val portrait = dockWidthFor(390.dp, 4).value      // 常见手机竖屏
        val landscape = dockWidthFor(1194.dp, 4).value    // 约 11 寸平板横屏
        assertTrue(
            "横屏应明显更宽：$landscape vs $portrait",
            landscape > portrait * 1.2f,
        )
        assertTrue(
            "但不超过 30% 参考上限：$landscape vs $portrait",
            landscape <= portrait * 1.3f,
        )
    }

    @Test
    fun 横屏不超过可用宽度的百分之六十二() {
        listOf(600, 720, 900, 1194, 1600, 2200).forEach { width ->
            val dock = dockWidthFor(width.dp, 4).value
            assertTrue(
                "窗口 $width dp 下 Dock 宽 $dock 不应接近全屏",
                dock <= width * 0.62f + epsilon,
            )
        }
    }

    @Test
    fun 窗口更宽Dock不减宽() {
        val widths = listOf(600, 700, 800, 900, 1000, 1100, 1200, 1400, 1800)
        val dockWidths = widths.map { dockWidthFor(it.dp, 4).value }
        dockWidths.zipWithNext().forEach { (a, b) ->
            assertTrue("宽度应随窗口宽度单调不减：$a -> $b", b >= a - epsilon)
        }
    }

    @Test
    fun 导航项数量参与宽度计算() {
        val three = dockWidthFor(1000.dp, 3).value
        val four = dockWidthFor(1000.dp, 4).value
        assertTrue("同宽窗口下项目多 Dock 更宽：$four vs $three", four > three)
        val delta = four - three
        assertTrue("多出的一项应接近单项目标宽度（约 103dp），实际 $delta", delta in 95f..110f)
    }
}
