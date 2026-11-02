package com.cuotixingqiu.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import kotlin.math.roundToInt

/**
 * 悬浮玻璃 Dock（iOS 26/27 风格底部导航）。
 *
 * 设计说明：
 *  - 一颗独立圆角胶囊，水平居中、悬浮在页面内容之上，不铺满整屏；
 *  - v1.3.3 起启用“真实背景采样”（见 [GlassBackdrop]）：页面内容被录制进 GraphicsLayer，
 *    Dock 采样身后的真实像素；Android 13+ 做 6dp 小半径模糊 + AGSL 边缘折射/调色，
 *    Android 12/12L 做真背景模糊（磨砂），Android 11 及以下自动退回纯材质模拟；
 *  - 纯材质部分用 Dock 专用的一次绘制（见 [dockGlassSurface]）：
 *    中性白/黑 scrim（只负责可读性）+ 左上局部镜面 + 很淡的顶部提亮
 *    + 单条 0.75dp 扫光边缘 + 柔和投影；不再使用内圈/外圈双描边，
 *    也不再用 `surfaceContainer*` 作为底材，让真实背景提供颜色；
 *    选中态由一条共享的青绿玻璃透镜（见 [dockSelectionSurface]）滑动标识；
 *  - 宽度按实际可用窗口宽度自适应（见 [dockWidthFor]）：手机竖屏铺满可用宽度、每项等宽；
 *    更宽的窗口把每个导航项的目标宽度按窗口宽度插值（84dp → 112dp），
 *    胶囊总宽 = 项数 × 单项目标宽度 + 内边距，并限制在可用宽度的 62% 以内——
 *    平板横屏更舒展，但不会接近全屏宽度或无限拉长；
 *  - 选中态同时用共享玻璃透镜 + 图标/文字颜色 + 字重区分，不只依赖颜色；
 *  - 每个目的地都显示图标 + 文字，顺序、标签、跳转目标与原底部导航完全一致。
 */

/** 悬浮 Dock 里的一项：路由、标签、图标（与原底部导航的 4 个标签一一对应） */
data class DockItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

/** 胶囊的最小高度（图标 + 文字 + 内边距；系统字体放大时允许变高） */
private val DockCapsuleMinHeight = 64.dp

/** 胶囊与屏幕底部的留白 */
private val DockMarginBelow = 10.dp

/** 列表最后一项与胶囊顶端之间额外留出的“呼吸”空隙 */
private val DockBreathingRoom = 8.dp

/** 胶囊与屏幕左右边缘的最小距离（手机竖屏铺满时的留白） */
private val DockSideMargin = 14.dp

/** 胶囊内、导航项左右的横向内边距（宽度计算与实际布局共用同一个值，避免两处对不上） */
private val DockContentPaddingHorizontal = 8.dp

/** 紧凑窗口的判定宽度：与全应用 WindowWidthSize.COMPACT 的断点一致（< 600dp 视为手机竖屏） */
private const val DockCompactMaxWidthDp = 600f

/** 窗口宽到这个值（约 11 寸平板横屏）后，单个导航项取到宽度上限，不再继续拉长 */
private const val DockWideFullWidthDp = 1200f

/** 非紧凑窗口下单个导航项的目标宽度：按窗口宽度在两者之间插值（约等于竖屏观感 → 平板舒展） */
private const val DockItemWidthWideMinDp = 84f
private const val DockItemWidthWideMaxDp = 112f

/** Dock 宽度上限：不超过可用宽度的这个比例，横屏可以舒展，但绝不接近全屏宽度 */
private const val DockMaxWidthFraction = 0.62f

/** Dock 的视觉总高（不含系统栏）：底部留白 + 胶囊 + 呼吸空隙 */
val DockVisualHeight: Dp = DockMarginBelow + DockCapsuleMinHeight + DockBreathingRoom

/**
 * 悬浮 Dock 的宽度规则（纯计算，便于单元测试；[availableWidth] 为扣掉系统栏后的实际可用窗口宽度）：
 *
 *  - 紧凑窗口（< 600dp，手机竖屏）：铺满可用宽度，左右各留 [DockSideMargin]——保持现状；
 *  - 更宽的窗口（手机横屏 / 平板）：每个导航项的目标宽度随窗口宽度在 84dp → 112dp 之间插值，
 *    胶囊宽度 = 项数 × 单项目标宽度 + 左右内边距，并限制在可用宽度的 62% 以内——
 *    屏幕越宽越舒展，但不会接近全屏宽度，也不会无限拉长。
 */
internal fun dockWidthFor(availableWidth: Dp, itemCount: Int): Dp {
    if (availableWidth < DockCompactMaxWidthDp.dp) {
        return (availableWidth.value - DockSideMargin.value * 2f).coerceAtLeast(0f).dp
    }
    val fraction = ((availableWidth.value - DockCompactMaxWidthDp) /
        (DockWideFullWidthDp - DockCompactMaxWidthDp)).coerceIn(0f, 1f)
    val itemWidth = DockItemWidthWideMinDp + (DockItemWidthWideMaxDp - DockItemWidthWideMinDp) * fraction
    val wanted = itemWidth * itemCount + DockContentPaddingHorizontal.value * 2f
    return minOf(wanted, availableWidth.value * DockMaxWidthFraction).dp
}

/**
 * 悬浮 Dock 需要的内容避让值 = 系统栏高度 + [DockVisualHeight]。
 *
 * 页面把它加到可滚动内容的底部留白里，最后一条内容就能滚到 Dock 上方；
 * 不做成“整页统一垫高”，避免只为了 Dock 把所有页面都挤窄。
 */
@Composable
fun rememberDockClearance(): Dp =
    WindowInsets.systemBars.asPaddingValues().calculateBottomPadding() + DockVisualHeight

@Composable
fun FloatingGlassDock(
    items: List<DockItem>,
    selectedRoute: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    backgroundLayer: GraphicsLayer? = null,
    pageBackdrop: LayerBackdrop? = null,
) {
    val scheme = MaterialTheme.colorScheme
    // 与项目其它地方一致：用背景亮度判断明暗主题
    val isDark = scheme.background.luminance() < 0.5f

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            // 避开手势条 / 三按键导航栏，以及横屏时的侧边手势区
            .windowInsetsPadding(
                WindowInsets.systemBars.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
            )
            .padding(bottom = DockMarginBelow),
        contentAlignment = Alignment.Center,
    ) {
        // 用“实际可用窗口宽度”（已扣掉系统栏）计算宽度，而不是按设备型号写死
        val dockWidth = dockWidthFor(maxWidth, items.size)
        // Android 13+：页面背景 + Dock 底板 + 选中项组合背景，使用 AndroidLiquidGlass 的 lens。
        // Android 12/12L 及以下返回 null，继续走项目原有的模糊 / 静态材质。
        val liquidBackdrops = rememberLiquidDockBackdrops(pageBackdrop)
        val useLiquidGlass = liquidBackdrops != null
        // 旧路径的背景采样状态：只在没有进入 Android 13+ 透镜路径时创建，避免重复 shader。
        val backdropState = if (!useLiquidGlass && backgroundLayer != null) {
            rememberGlassBackdropState()
        } else {
            null
        }
        // Dock 在根布局里的位置：背景采样图层要按这个位置平移才能与身后像素对位
        var dockPositionInRoot by remember { mutableStateOf(Offset.Unspecified) }

        Box(
            modifier = Modifier
                .width(dockWidth)
                // 更轻盈的悬浮阴影：只留一层很淡的环境晕，不参与“边框感”
                .shadow(
                    elevation = 8.dp,
                    shape = CircleShape,
                    clip = false,
                    ambientColor = Color.Black.copy(alpha = 0.03f),
                    spotColor = Color.Black.copy(alpha = 0.05f),
                )
                .clip(CircleShape)
                .onGloballyPositioned { dockPositionInRoot = it.positionInRoot() },
        ) {
            // 背景采样 + 玻璃后处理层：与胶囊完全重合，画在材质下方；只在页面内容变化时才有绘制
            val layer = backgroundLayer
            if (!useLiquidGlass && layer != null && backdropState != null) {
                GlassBackdrop(
                    backgroundLayer = layer,
                    state = backdropState,
                    dockPositionInRoot = dockPositionInRoot,
                    modifier = Modifier.matchParentSize(),
                )
            }

            // Android 13+：底板由库绘制，并导出成 dockBackdrop 供选中透镜组合采样。
            // 这个子层在 Row 之前绘制，所以选中透镜同一帧就能读到当前底板。
            val liquid = liquidBackdrops
            if (liquid != null && pageBackdrop != null) {
                LiquidDockBackground(
                    pageBackdrop = pageBackdrop,
                    exportedBackdrop = liquid.dock,
                    isDark = isDark,
                    modifier = Modifier.matchParentSize(),
                )
            }

            val selectedIndex = items.indexOfFirst { it.route == selectedRoute }
            // 每个导航项在 Row 里的实际位置：选中透镜用它来滑动，避免手算 RTL / 字体放大后的偏差
            val itemBounds = remember(items.size) {
                mutableStateListOf<Rect?>().apply { repeat(items.size) { add(null) } }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        // Android 13+ 的底板、高光和阴影已由 drawBackdrop 绘制；
                        // 旧路径才需要项目自己的单描边静态材质。
                        if (!useLiquidGlass) {
                            Modifier.dockGlassSurface(
                                isDark = isDark,
                                hasGlassBackdrop = backdropState != null,
                            )
                        } else {
                            Modifier
                        },
                    )
                    .padding(horizontal = DockContentPaddingHorizontal, vertical = 6.dp),
            ) {
                // 选中透镜画在材质之上、图标文字之下；它是装饰层，不参与点击和语义
                DockSelectionLens(
                    bounds = itemBounds.getOrNull(selectedIndex),
                    isDark = isDark,
                    scheme = scheme,
                    liquidBackdrop = liquidBackdrops?.selection,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    items.forEachIndexed { index, item ->
                        DockItemView(
                            item = item,
                            selected = index == selectedIndex,
                            isDark = isDark,
                            onBoundsChanged = { rect ->
                                if (itemBounds[index] != rect) itemBounds[index] = rect
                            },
                            onClick = { onSelect(item.route) },
                        )
                    }
                }
            }
        }
    }
}

/** Dock 里的单个目的地：图标在上、文字在下；选中态由共享的青绿玻璃透镜标识 */
@Composable
private fun RowScope.DockItemView(
    item: DockItem,
    selected: Boolean,
    isDark: Boolean,
    onBoundsChanged: (Rect) -> Unit,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    // 选中时用更明确的青绿色内容色，未选中时用偏深的中性文字色；
    // 不再给每个 item 画实心胶囊，避免选中态看起来像贴上去的色块。
    val contentColor by animateColorAsState(
        targetValue = if (selected) {
            scheme.secondary
        } else {
            scheme.onSurface.copy(alpha = if (isDark) 0.78f else 0.74f)
        },
        animationSpec = tween(durationMillis = 180),
        label = "dockItemContent",
    )

    Column(
        modifier = Modifier
            // 每项等宽：竖屏下与原底部导航的均分方式一致，横屏下一起平分更宽的胶囊
            .weight(1f)
            .heightIn(min = 52.dp)
            .onGloballyPositioned { onBoundsChanged(it.boundsInParent()) }
            .clip(CircleShape)
            .selectable(
                selected = selected,
                role = Role.Tab,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = item.icon,
            // 文字标签就在下面，图标只是装饰，避免读屏重复朗读
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(22.dp),
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = item.label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * 一块在导航项之间滑动的青绿玻璃透镜。
 *
 * 位置和尺寸来自每个 item 的真实 bounds，所以 RTL、平板宽度、系统字体放大都不需要单独换算；
 * 透镜只负责视觉，点击和选中语义仍然在 [DockItemView] 的 `selectable` 上。
 */
@Composable
private fun DockSelectionLens(
    bounds: Rect?,
    isDark: Boolean,
    scheme: ColorScheme,
    liquidBackdrop: Backdrop?,
) {
    if (bounds == null) return

    val density = LocalDensity.current
    val insetX = with(density) { 4.dp.toPx() }
    val insetY = with(density) { 3.dp.toPx() }
    val targetWidth = (bounds.width - insetX * 2f).coerceAtLeast(0f)
    val targetHeight = (bounds.height - insetY * 2f).coerceAtLeast(0f)
    if (targetWidth <= 0f || targetHeight <= 0f) return

    val lensSpring = spring<Float>(dampingRatio = 0.86f, stiffness = 520f)
    val left by animateFloatAsState(
        targetValue = bounds.left + insetX,
        animationSpec = lensSpring,
        label = "dockLensLeft",
    )
    val top by animateFloatAsState(
        targetValue = bounds.top + insetY,
        animationSpec = lensSpring,
        label = "dockLensTop",
    )
    val width by animateFloatAsState(
        targetValue = targetWidth,
        animationSpec = lensSpring,
        label = "dockLensWidth",
    )
    val height by animateFloatAsState(
        targetValue = targetHeight,
        animationSpec = lensSpring,
        label = "dockLensHeight",
    )

    Box(
        modifier = Modifier
            .offset { IntOffset(left.roundToInt(), top.roundToInt()) }
            .size(
                width = with(density) { width.toDp() },
                height = with(density) { height.toDp() },
            )
            .clip(CircleShape)
            .then(
                if (liquidBackdrop != null) {
                    Modifier
                } else {
                    Modifier.dockSelectionSurface(isDark = isDark, scheme = scheme)
                },
            ),
    ) {
        if (liquidBackdrop != null) {
            LiquidDockSelection(
                backdrop = liquidBackdrop,
                scheme = scheme,
                isDark = isDark,
                modifier = Modifier.matchParentSize(),
            )
        }
    }
}
