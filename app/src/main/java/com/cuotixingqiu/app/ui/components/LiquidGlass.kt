package com.cuotixingqiu.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Liquid Glass 共用材质与控件（v1.3.6）。
 *
 * 这一文件是 Dock、首页“添加错题”按钮、二级页返回键、页面顶栏的共同材质来源：
 *  - [dockGlassSurface]：Dock 专用的清透玻璃（中性 scrim + 局部镜面 + 单圈扫光边），
 *    不再使用主题色底材和双圈描边；[dockSelectionSurface] 是它配套的选中透镜；
 *  - [liquidGlassSurface]：小尺寸控件的玻璃本体（半透明渐变底 + 顶部反光 + 镜面光斑
 *    + 底部内暗层 + 内侧细环 + 外圈发丝边），圆角 FAB、圆形返回键继续复用它；
 *  - [liquidTopBarTint]：顶栏的色调与顶部高光，底部向正文柔和渐隐，不画横向硬线；
 *  - [TopBarGlassBackdrop]：顶栏真实背景模糊（Android 12+）采样层，只录顶栏高度一条内容；
 *  - [GlassBackButton]：48dp 触摸区 + 40dp 圆形静态玻璃（在内容录制层内部，不做自采样）；
 *  - [GlassExtendedFab]：保留 M3 ExtendedFAB 的尺寸/布局/点击，把实心容器换成玻璃。
 *
 * 性能约定：所有渐变与路径都在 drawWithCache 里按“尺寸 + 主题 + 按下状态”缓存；
 * 顶栏模糊只发生在顶栏区域内；没有逐帧 shader 编译，也没有持续动画。
 */

/** 玻璃表面的通用材质（只负责绘制，不参与布局） */
internal fun Modifier.liquidGlassSurface(
    isDark: Boolean,
    scheme: ColorScheme,
    hasGlassBackdrop: Boolean = false,
    /** null = 胶囊（圆角取高度一半）；圆角 FAB / 圆形按钮传入实际圆角 */
    cornerRadius: Dp? = null,
    pressed: Boolean = false,
): Modifier = drawWithCache {
    val hairline = 1.dp.toPx()
    val half = hairline / 2f
    val radiusPx = (cornerRadius?.toPx() ?: (size.height / 2f))
        .coerceIn(0f, size.minDimension / 2f)
    val corner = CornerRadius(radiusPx)

    // 按下时只提高一点点亮度/高光，不做夸张的放大或变色
    val pressLift = if (pressed) 0.06f else 0f

    // 1. 底色：上亮下暗的玻璃“厚度”，全部从主题色派生，深浅主题与动态取色自动适配
    val baseBrush = Brush.verticalGradient(
        0f to if (isDark) {
            scheme.surfaceContainerHigh.copy(
                alpha = (if (hasGlassBackdrop) 0.62f else 0.72f) + pressLift,
            )
        } else {
            Color.White.copy(
                alpha = (if (hasGlassBackdrop) 0.50f else 0.60f) + pressLift,
            )
        },
        1f to if (isDark) {
            scheme.surfaceContainerLow.copy(alpha = if (hasGlassBackdrop) 0.70f else 0.78f)
        } else {
            scheme.surfaceContainerHighest.copy(alpha = if (hasGlassBackdrop) 0.62f else 0.72f)
        },
    )
    // 2. 顶部反光带（只占上半部分）
    val sheenBrush = Brush.verticalGradient(
        0f to Color.White.copy(alpha = (if (isDark) 0.10f else 0.40f) + pressLift),
        1f to Color.Transparent,
        endY = size.height * 0.42f,
    )
    // 3. 左上镜面光斑
    val specularBrush = Brush.radialGradient(
        0f to Color.White.copy(alpha = (if (isDark) 0.05f else 0.12f) + pressLift * 0.5f),
        1f to Color.Transparent,
        center = Offset(size.width * 0.30f, -size.height * 0.10f),
        radius = size.width * 0.55f,
    )
    // 4. 底部内侧暗层
    val bottomShadeBrush = Brush.verticalGradient(
        0f to Color.Transparent,
        1f to Color.Black.copy(alpha = if (isDark) 0.22f else 0.04f),
        startY = size.height * 0.68f,
    )
    // 5. 内侧折射细环
    val ringInset = 2.dp.toPx().coerceAtMost(radiusPx)
    val ringBrush = Brush.verticalGradient(
        0f to Color.White.copy(alpha = if (isDark) 0.16f else 0.70f),
        1f to Color.White.copy(alpha = if (isDark) 0.06f else 0.28f),
    )
    // 6. 外圈发丝边
    val hairlineBrush = if (isDark) {
        Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.18f), Color.White.copy(alpha = 0.05f)),
        )
    } else {
        Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.90f), Color.Black.copy(alpha = 0.06f)),
        )
    }

    onDrawBehind {
        drawRoundRect(brush = baseBrush, cornerRadius = corner)
        drawRoundRect(brush = sheenBrush, cornerRadius = corner)
        drawRoundRect(brush = specularBrush, cornerRadius = corner)
        drawRoundRect(brush = bottomShadeBrush, cornerRadius = corner)
        // 折射细环：整条线收进边缘 2dp（圆角很小时自动收窄）
        drawRoundRect(
            brush = ringBrush,
            topLeft = Offset(ringInset, ringInset),
            size = Size(
                (size.width - ringInset * 2f).coerceAtLeast(0f),
                (size.height - ringInset * 2f).coerceAtLeast(0f),
            ),
            cornerRadius = CornerRadius((radiusPx - ringInset).coerceAtLeast(0f)),
            style = Stroke(width = hairline),
        )
        // 发丝边：收进半个线宽，整条线都落在裁剪范围内
        drawRoundRect(
            brush = hairlineBrush,
            topLeft = Offset(half, half),
            size = Size(
                (size.width - hairline).coerceAtLeast(0f),
                (size.height - hairline).coerceAtLeast(0f),
            ),
            cornerRadius = CornerRadius((radiusPx - half).coerceAtLeast(0f)),
            style = Stroke(width = hairline),
        )
    }
}

/**
 * Dock 专用 Liquid Glass 材质（v1.3.6）。
 *
 * 与 [liquidGlassSurface] 的区别：
 *  - 不再用 `surfaceContainer*` 作为底材，改成中性白/黑 scrim，让背景采样层提供颜色；
 *  - 去掉 2dp 内缩的内圈，只保留一条 0.75dp 的扫光边缘；
 *  - 高光从“全宽亮带”改成左上局部镜面 + 很淡的顶部提亮；
 *  - 底部暗层大幅减薄，避免深色模式变成一整块深灰板。
 *
 * 有真实背景采样时（Android 12+）scrim 很薄，让模糊/折射透出来；
 * 没有采样时（Android 11 及以下）用同一套中性材质，只把 scrim 加厚到可读水平。
 */
internal fun Modifier.dockGlassSurface(
    isDark: Boolean,
    hasGlassBackdrop: Boolean,
): Modifier = drawWithCache {
    val rimWidth = 0.75.dp.toPx()
    val halfRim = rimWidth / 2f
    val radiusPx = (size.height / 2f).coerceAtMost(size.minDimension / 2f)
    val corner = CornerRadius(radiusPx)

    // 1. 中性 scrim：只负责对比度，不负责颜色。颜色交给身后真实背景。
    val baseBrush = Brush.verticalGradient(
        0f to if (isDark) {
            Color.Black.copy(alpha = if (hasGlassBackdrop) 0.30f else 0.60f)
        } else {
            Color.White.copy(alpha = if (hasGlassBackdrop) 0.24f else 0.56f)
        },
        1f to if (isDark) {
            Color.Black.copy(alpha = if (hasGlassBackdrop) 0.22f else 0.50f)
        } else {
            Color.White.copy(alpha = if (hasGlassBackdrop) 0.16f else 0.44f)
        },
    )
    // 2. 顶部很淡的提亮，避免底板顶部完全平掉
    val topLift = Brush.verticalGradient(
        0f to Color.White.copy(alpha = if (isDark) 0.03f else 0.06f),
        1f to Color.Transparent,
        endY = size.height * 0.28f,
    )
    // 3. 左上局部镜面（不是横贯整条的亮带）
    val mainSpecular = Brush.radialGradient(
        0f to Color.White.copy(alpha = if (isDark) 0.06f else 0.14f),
        1f to Color.Transparent,
        center = Offset(size.width * 0.28f, -size.height * 0.10f),
        radius = size.width * 0.62f,
    )
    // 4. 右上非常淡的第二光斑，让高光不是单点
    val softSpecular = Brush.radialGradient(
        0f to Color.White.copy(alpha = if (isDark) 0.025f else 0.05f),
        1f to Color.Transparent,
        center = Offset(size.width * 0.68f, -size.height * 0.26f),
        radius = size.width * 0.52f,
    )
    // 5. 底部内侧厚度暗层：比旧版轻很多，只留一点体积感
    val bottomShade = Brush.verticalGradient(
        0f to Color.Transparent,
        1f to Color.Black.copy(alpha = if (isDark) 0.10f else 0.015f),
        startY = size.height * 0.75f,
    )
    // 6. 单条扫光边缘：左上亮、右下暗，替代旧版的“内圈 + 外圈”双描边。
    //    0 弧度在 3 点钟方向，0.625 约在左上 10:30 方向。
    val rimBrush = Brush.sweepGradient(
        0f to Color.White.copy(alpha = if (isDark) 0.06f else 0.10f),
        0.10f to Color.White.copy(alpha = if (isDark) 0.04f else 0.08f),
        0.20f to Color.Black.copy(alpha = if (isDark) 0.18f else 0.08f),
        0.40f to Color.White.copy(alpha = if (isDark) 0.08f else 0.14f),
        0.50f to Color.White.copy(alpha = if (isDark) 0.10f else 0.18f),
        0.625f to Color.White.copy(alpha = if (isDark) 0.28f else 0.55f),
        0.75f to Color.White.copy(alpha = if (isDark) 0.16f else 0.35f),
        0.90f to Color.White.copy(alpha = if (isDark) 0.06f else 0.12f),
        1f to Color.White.copy(alpha = if (isDark) 0.06f else 0.10f),
        center = Offset(size.width / 2f, size.height / 2f),
    )

    onDrawBehind {
        drawRoundRect(brush = baseBrush, cornerRadius = corner)
        drawRoundRect(brush = topLift, cornerRadius = corner)
        drawRoundRect(brush = mainSpecular, cornerRadius = corner)
        drawRoundRect(brush = softSpecular, cornerRadius = corner)
        drawRoundRect(brush = bottomShade, cornerRadius = corner)
        drawRoundRect(
            brush = rimBrush,
            topLeft = Offset(halfRim, halfRim),
            size = Size(
                (size.width - rimWidth).coerceAtLeast(0f),
                (size.height - rimWidth).coerceAtLeast(0f),
            ),
            cornerRadius = CornerRadius((radiusPx - halfRim).coerceAtLeast(0f)),
            style = Stroke(width = rimWidth),
        )
    }
}

/**
 * Dock 选中透镜（v1.3.6）：一块低透明度青绿玻璃，而不是实心色块。
 *
 * 透镜由 Dock 统一绘制并滑动，不再由每个导航项各自铺满一个单元格；
 * 这样选中态和 Dock 主体共享同一套“低透明度染色 + 同款边缘光”的语言。
 */
internal fun Modifier.dockSelectionSurface(
    isDark: Boolean,
    scheme: ColorScheme,
): Modifier = drawWithCache {
    val tint = scheme.secondary
    val rimWidth = 0.5.dp.toPx()
    val halfRim = rimWidth / 2f
    val radiusPx = (size.height / 2f).coerceAtMost(size.minDimension / 2f)
    val corner = CornerRadius(radiusPx)

    val baseBrush = Brush.verticalGradient(
        0f to tint.copy(alpha = if (isDark) 0.20f else 0.14f),
        1f to tint.copy(alpha = if (isDark) 0.12f else 0.08f),
    )
    val topGlow = Brush.radialGradient(
        0f to Color.White.copy(alpha = if (isDark) 0.05f else 0.10f),
        1f to Color.Transparent,
        center = Offset(size.width * 0.30f, -size.height * 0.15f),
        radius = size.width * 0.72f,
    )
    val bottomShade = Brush.verticalGradient(
        0f to Color.Transparent,
        1f to Color.Black.copy(alpha = if (isDark) 0.08f else 0.02f),
        startY = size.height * 0.70f,
    )
    val rimBrush = Brush.sweepGradient(
        0f to Color.White.copy(alpha = if (isDark) 0.03f else 0.06f),
        0.125f to Color.Black.copy(alpha = if (isDark) 0.10f else 0.04f),
        0.50f to Color.White.copy(alpha = if (isDark) 0.06f else 0.10f),
        0.625f to Color.White.copy(alpha = if (isDark) 0.18f else 0.26f),
        0.75f to Color.White.copy(alpha = if (isDark) 0.12f else 0.18f),
        1f to Color.White.copy(alpha = if (isDark) 0.03f else 0.06f),
        center = Offset(size.width / 2f, size.height / 2f),
    )

    onDrawBehind {
        drawRoundRect(brush = baseBrush, cornerRadius = corner)
        drawRoundRect(brush = topGlow, cornerRadius = corner)
        drawRoundRect(brush = bottomShade, cornerRadius = corner)
        drawRoundRect(
            brush = rimBrush,
            topLeft = Offset(halfRim, halfRim),
            size = Size(
                (size.width - rimWidth).coerceAtLeast(0f),
                (size.height - rimWidth).coerceAtLeast(0f),
            ),
            cornerRadius = CornerRadius((radiusPx - halfRim).coerceAtLeast(0f)),
            style = Stroke(width = rimWidth),
        )
    }
}

/**
 * 顶栏色调：与 Dock 同一套“上亮下暗 + 顶部高光”的玻璃语言。
 *
 * 不画横向分隔线：颜色在顶栏下半段平滑过渡到全透明，正文从下方滚过时不会出现色带或硬边。
 * [hasBackdrop] 为 true（Android 12+ 真背景模糊已启用）时整体更透一点，让模糊看得见；
 * 不支持模糊时稍微提高不透明度，保证标题和按钮仍然清晰。
 */
internal fun Modifier.liquidTopBarTint(
    isDark: Boolean,
    scheme: ColorScheme,
    hasBackdrop: Boolean,
): Modifier = drawWithCache {
    val topColor = if (isDark) scheme.surfaceContainerHigh else Color.White
    val lowerColor = if (isDark) scheme.surfaceContainer else scheme.surfaceContainerLow
    val topAlpha = when {
        // 有真实模糊时把色调让薄一些，让模糊后的正文纹理真正透出来；
        // 没有模糊的降级页才用更厚的色调保证标题可读。
        hasBackdrop && isDark -> 0.58f
        hasBackdrop -> 0.55f
        isDark -> 0.90f
        else -> 0.84f
    }
    // 多段渐变把“不透明 → 透明”摊开，避免单段折线在视觉上形成一条浅色带
    val tint = Brush.verticalGradient(
        0f to topColor.copy(alpha = topAlpha),
        0.42f to lowerColor.copy(alpha = topAlpha * 0.94f),
        (if (hasBackdrop) 0.72f else 0.80f) to lowerColor.copy(alpha = topAlpha * 0.56f),
        1f to lowerColor.copy(alpha = 0f),
    )
    val sheen = Brush.verticalGradient(
        0f to Color.White.copy(
            alpha = when {
                hasBackdrop && isDark -> 0.035f
                hasBackdrop -> 0.10f
                isDark -> 0.06f
                else -> 0.22f
            },
        ),
        1f to Color.Transparent,
        endY = size.height * 0.45f,
    )
    onDrawBehind {
        drawRect(brush = tint)
        drawRect(brush = sheen)
    }
}

/**
 * 顶栏背后的真实页面内容（薄薄一条，只录顶栏高度）。
 *
 * 采样源由 [ScreenScaffold] 的内容层录制。Scaffold 的 body 与 topBar 都放在 (0,0)，
 * 所以采样层直接按 0 偏移绘制即可；这样即使 `onGloballyPositioned` 还没回调，
 * 第一帧也不会因为原点未就绪而整层跳过。
 *
 * 绘制顺序：blur 在外，采样和 DstIn 渐隐在内。blur 自己会创建离屏层，DstIn 直接作用在
 * 这一层上，不再额外套一层 `graphicsLayer(Offscreen)`；少一层嵌套，底边也真正渐隐。
 * 只处理顶栏这一条，不重复捕获整屏，也不影响正文文字清晰度。
 */
@Composable
internal fun TopBarGlassBackdrop(
    backgroundLayer: GraphicsLayer,
    modifier: Modifier = Modifier,
) {
    if (!isGlassBackdropSupported()) return
    Box(
        modifier = modifier
            // 顶栏是整宽矩形：Rectangle 让模糊在边界内取样（Clamp），
            // 避免 Unbounded 在顶栏底边采到透明黑而出现一圈发灰的边。
            .blur(20.dp, BlurredEdgeTreatment.Rectangle)
            .drawWithCache {
                // 渐隐画刷按尺寸缓存，滚动时不每帧重新分配
                val fade = Brush.verticalGradient(
                    0f to Color.Black,
                    0.56f to Color.Black,
                    0.82f to Color.Black.copy(alpha = 0.45f),
                    1f to Color.Transparent,
                )
                onDrawWithContent {
                    // 内容层和顶栏都在 Scaffold 的 (0,0)，直接对齐绘制
                    drawLayer(backgroundLayer)
                    // 底部逐渐透明：顶栏与正文之间是柔和过渡，没有横向硬边界
                    drawRect(brush = fade, blendMode = BlendMode.DstIn)
                }
            },
    )
}

/**
 * 二级页返回键：48dp 触摸区，40dp 可见圆形玻璃，箭头居中。
 *
 * 返回键位于页面内容录制层内部，不能直接采样身后内容（否则会引用到自己），
 * 因此复用 Dock 的静态玻璃材质：半透明底、顶部高光、内侧细环、发丝边和轻阴影；
 * 圆本身已经坐在顶栏的模糊层上，观感与 Dock 属于同一套玻璃语言。
 */
@Composable
fun GlassBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val isDark = isDarkScheme()
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = tween(durationMillis = 110),
        label = "glassBackScale",
    )

    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .shadow(
                    elevation = 3.dp,
                    shape = CircleShape,
                    clip = false,
                    ambientColor = Color.Black.copy(alpha = 0.05f),
                    spotColor = Color.Black.copy(alpha = 0.10f),
                )
                .clip(CircleShape)
                .liquidGlassSurface(
                    isDark = isDark,
                    scheme = scheme,
                    hasGlassBackdrop = false,
                    pressed = pressed,
                ),
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "返回",
            tint = scheme.onSurface,
            modifier = Modifier.size(22.dp),
        )
    }
}

/**
 * 首页 / 错题页的“添加错题”玻璃按钮。
 *
 * 外层尺寸、位置、文案、图标与点击逻辑保持原 M3 ExtendedFAB 不变：
 * 容器透明、elevation 归零，玻璃画在它后面；悬浮位置仍由调用方的 Modifier 决定。
 * Android 13+ 做边缘折射，Android 12/12L 做真实背景模糊，更低版本退回静态玻璃；
 * 按下时只缩到 0.96 并稍微提亮，不夸张。
 */
@Composable
fun GlassExtendedFab(
    onClick: () -> Unit,
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    backgroundLayer: GraphicsLayer? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val isDark = isDarkScheme()
    val shape = RoundedCornerShape(16.dp)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = tween(durationMillis = 120),
        label = "glassFabScale",
    )
    val backdropState = if (backgroundLayer != null) rememberGlassBackdropState() else null
    var positionInRoot by remember { mutableStateOf(Offset.Unspecified) }

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = 9.dp,
                shape = shape,
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.05f),
                spotColor = Color.Black.copy(alpha = 0.10f),
            )
            .clip(shape)
            .onGloballyPositioned { positionInRoot = it.positionInRoot() },
    ) {
        val layer = backgroundLayer
        if (layer != null && backdropState != null) {
            GlassBackdrop(
                backgroundLayer = layer,
                state = backdropState,
                dockPositionInRoot = positionInRoot,
                cornerRadius = 16.dp,
                modifier = Modifier.matchParentSize(),
            )
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .liquidGlassSurface(
                    isDark = isDark,
                    scheme = scheme,
                    hasGlassBackdrop = backdropState != null,
                    cornerRadius = 16.dp,
                    pressed = pressed,
                ),
        )
        ExtendedFloatingActionButton(
            onClick = onClick,
            icon = {
                Icon(imageVector = icon, contentDescription = null)
            },
            text = { Text(text = text) },
            shape = shape,
            containerColor = Color.Transparent,
            contentColor = scheme.primary,
            elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
            interactionSource = interaction,
        )
    }
}
