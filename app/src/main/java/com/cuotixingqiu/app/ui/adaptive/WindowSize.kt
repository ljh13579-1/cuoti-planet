package com.cuotixingqiu.app.ui.adaptive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration

/**
 * 窗口宽度的三档分类（参考 Material 的断点建议），用于自适应布局：
 *
 *  - [COMPACT]：手机竖屏等窄窗口（< 600dp）——单列布局；
 *  - [MEDIUM]：手机横屏、小平板（600dp–839dp）——卡片可以排成两列；
 *  - [EXPANDED]：平板横向、大屏设备（≥ 840dp）——可以用「列表 + 详情」主从分栏。
 *
 * 判断依据是「当前窗口的实际可用宽度」（[LocalConfiguration.screenWidthDp]），
 * 分屏、折叠屏、横竖屏切换都会自动重新计算，不依赖设备型号。
 */
enum class WindowWidthSize { COMPACT, MEDIUM, EXPANDED }

/**
 * 根据宽度（单位 dp）算出档位。
 * 抽成纯函数是为了能写单元测试（界面本身要上手机才能看，但这个规则可以在电脑上验证）。
 */
fun windowWidthSizeOf(widthDp: Int): WindowWidthSize = when {
    widthDp < 600 -> WindowWidthSize.COMPACT
    widthDp < 840 -> WindowWidthSize.MEDIUM
    else -> WindowWidthSize.EXPANDED
}

/** 当前窗口宽度的档位（窗口尺寸变化时自动刷新） */
@Composable
fun rememberWindowWidthSize(): WindowWidthSize {
    val widthDp = LocalConfiguration.current.screenWidthDp
    return remember(widthDp) { windowWidthSizeOf(widthDp) }
}
