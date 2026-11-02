package com.cuotixingqiu.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle

/** 一键关闭 Android 13+ Dock 透镜，出问题时无需拆依赖即可回到项目原有路径。 */
private const val DockLiquidGlassEnabled = true

/**
 * Android 13+ Dock 的 Liquid Glass 图层。
 *
 * 只在 API 33+ 创建：
 *  - [dock]：由 Dock 背景层导出，只包含底板材质，不包含图标、文字和选中透镜；
 *  - [selection]：页面背景 + Dock 底板，供选中透镜同时采样两层。
 *
 * 不要把选中透镜本身录进 [dock]，否则会形成图层循环，严重时会让 RenderThread 崩溃。
 */
@Stable
internal class LiquidDockBackdrops(
    val dock: LayerBackdrop,
    val selection: Backdrop,
)

@Composable
internal fun rememberLiquidDockBackdrops(
    pageBackdrop: LayerBackdrop?,
): LiquidDockBackdrops? {
    // Android 12/12L 只有 RenderEffect 模糊，没有 RuntimeShader。
    // 低于 Android 13 继续走现有 GlassBackdrop / 静态材质，不创建这里的额外图层。
    if (!DockLiquidGlassEnabled || !isGlassRefractionSupported() || pageBackdrop == null) return null

    val dockBackdrop = rememberLayerBackdrop()
    val selectionBackdrop = rememberCombinedBackdrop(pageBackdrop, dockBackdrop)
    return remember(dockBackdrop, selectionBackdrop) {
        LiquidDockBackdrops(dock = dockBackdrop, selection = selectionBackdrop)
    }
}

/**
 * Android 13+ Dock 底板。
 *
 * 与官方示例的 24dp/24dp 相比，这里主动收小折射参数，避免 64dp 高的胶囊里
 * 图标和文字被过度拉伸；模糊也保持 6dp，和 v1.3.6 已有的清晰度一致。
 * 高光只画一套，外层阴影继续交给 FloatingGlassDock 现有的一层 8dp 阴影，
 * 因此这里 shadow = null，避免“双层边 + 双层影”。
 */
@Composable
internal fun LiquidDockBackground(
    pageBackdrop: LayerBackdrop,
    exportedBackdrop: LayerBackdrop,
    isDark: Boolean,
    modifier: Modifier = Modifier,
) {
    val surface = if (isDark) {
        Color.Black.copy(alpha = 0.22f)
    } else {
        Color.White.copy(alpha = 0.28f)
    }

    Box(
        modifier = modifier.drawBackdrop(
            backdrop = pageBackdrop,
            shape = { CircleShape },
            effects = {
                // 与旧 shader 的 1.08 / 1.02 对齐，不用 vibrancy() 的固定 1.5。
                colorControls(saturation = 1.08f, contrast = 1.02f)
                blur(6.dp.toPx())
                lens(
                    refractionHeight = 16.dp.toPx(),
                    refractionAmount = 18.dp.toPx(),
                )
            },
            highlight = {
                Highlight(style = HighlightStyle.Ambient(intensity = 0.30f))
            },
            // 外层 Box 已经有一层很淡的悬浮阴影；这里不再叠第二层。
            shadow = null,
            exportedBackdrop = exportedBackdrop,
            onDrawSurface = {
                // 中性 scrim 只负责文字对比度，颜色交给真实背景和折射。
                drawRect(surface)
            },
        ),
    )
}

/**
 * Android 13+ 选中透镜。
 *
 * 关键点是 [backdrop] 必须是“页面 + Dock 底板”的组合背景：
 *  - 只采样页面：会像在胶囊上挖了一个洞；
 *  - 只加 tint：仍然是一块青绿色色块；
 *  - 组合背景 + lens：选中项才会像 Dock 内部的玻璃透镜。
 *
 * 图标和文字不进入透镜采样，保持清晰；如果后续真机对比后希望它们也轻微放大，
 * 再按官方示例增加透明复制 Row，不在第一版引入额外的语义和图层成本。
 */
@Composable
internal fun LiquidDockSelection(
    backdrop: Backdrop,
    scheme: ColorScheme,
    isDark: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { CircleShape },
            effects = {
                lens(
                    refractionHeight = 10.dp.toPx(),
                    refractionAmount = 14.dp.toPx(),
                    chromaticAberration = true,
                )
            },
            highlight = {
                // 默认高光整层收一点，避免在浅色主题里压过底板。
                Highlight.Default.copy(alpha = 0.72f)
            },
            shadow = null,
            onDrawSurface = {
                // 只留一层很淡的品牌色，选中状态主要靠图标/文字颜色和透镜。
                drawRect(
                    scheme.secondary.copy(alpha = if (isDark) 0.12f else 0.09f),
                )
            },
        ),
    )
}
