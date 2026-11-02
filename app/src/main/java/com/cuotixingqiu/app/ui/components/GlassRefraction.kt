package com.cuotixingqiu.app.ui.components

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.RenderEffect as ComposeRenderEffect
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

/**
 * Liquid Glass 背景采样层——真实背景，不是“自己糊自己”。
 *
 * 页面内容（NavHost）在 CuotiApp 里被录制进一个 [GraphicsLayer]（同一帧、GPU 渲染节点级别的重录，
 * 没有截屏、没有 CPU 读回、没有轮询）；Dock / “添加错题”按钮内的背景采样节点按组件的屏幕位置
 * 把该图层平移绘制，采到的就是组件身后的真实像素。之后的“玻璃后处理”按系统能力分两档：
 *
 *  - **Android 13+（API 33）：小半径模糊 + 逐像素折射**。先用 [Modifier.blur] 做一层 6dp 的
 *    轻微模糊，再把 AGSL Shader（android.graphics.RuntimeShader）挂在 RenderEffect 上：
 *    用“圆角矩形有符号距离场（SDF）”计算每个像素离边缘的距离，在边缘带内沿（向内）法线方向
 *    做轻微采样位移，模拟厚玻璃边缘的光线折射；shader 同时做很轻的饱和度和对比度提升，
 *    让背景颜色真正参与材质，而不是被 scrim 洗灰。
 *  - **Android 12 / 12L（API 31–32）：真背景模糊（磨砂玻璃）**。平台从 API 31 起才有 RenderEffect，
 *    但没有 AGSL（RuntimeShader 是 API 33 才有），因此用 [Modifier.blur] 把采到的真实背景糊掉，
 *    得到 iOS 风格的磨砂玻璃；它不是“折射位移”，但背景是真实采样的，不是纯色模拟。
 *  - **Android 11 及以下（API < 31）**：没有可用的 GPU 后处理，不录制、不绘制采样层（零额外开销），
 *    玻璃组件保持静态半透明材质观感。
 *
 * 降级链：折射 Shader 创建失败（个别设备）→ 自动退回磨砂模糊；模糊也不可用/异常 → 整层不绘制。
 * 性能约定：Shader 只编译一次（remember，绝不逐帧重建）；uniforms 只在尺寸变化时刷新；
 * 折射/模糊都只发生在玻璃组件范围内；没有任何持续动画或逐帧刷新。
 */

/** 背景采样 + 磨砂模糊可用的最低系统版本（Android 12 / API 31：RenderEffect） */
internal fun isGlassBackdropSupported(): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/** 逐像素折射可用的最低系统版本（Android 13 / API 33：RuntimeShader / AGSL） */
internal fun isGlassRefractionSupported(): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

/** 折射带宽度：从胶囊边缘向内多少距离内发生位移（越大边缘越“厚”） */
private val RefractionEdgeWidth = 16.dp

/** 边缘最外处的最大向内位移（越大扭曲越明显；保持克制，不影响内容辨识） */
private val RefractionMaxShift = 4.5.dp

/** 边缘轻微聚光量（0~1，只加一点点，模拟光在玻璃边缘聚集） */
private const val RefractionEdgeLighten = 0.03f

/** 背景轻微提饱和/提对比，让玻璃不是“灰蒙蒙的一层”，而是能带出环境颜色 */
private const val RefractionSaturation = 1.08f
private const val RefractionContrast = 1.02f

/** 磨砂玻璃的模糊半径（Android 12/12L；比旧版轻一点，保留更多背景层次） */
private val GlassBlurRadius = 12.dp

/** Android 13+ 的“清透玻璃”模糊半径：小半径模糊 + 边缘折射，避免中央完全清晰 */
private val RefractionBlurRadius = 6.dp

/**
 * AGSL 源码：
 *  - rrectSdf：圆角矩形有符号距离（内部为负、外部为正）；
 *  - 边缘带强度 t：最外侧约 1，向内平滑衰减到 0（平方衰减）；
 *  - 位移方向：SDF 数值梯度的反方向（指向胶囊内部），数值梯度天然平滑，
 *    圆角处不会出现方向突变或矩形硬边；
 *  - 采样位置钳制在节点范围内；中央区域不做位移，只保留外层小半径模糊和调色。
 */
private const val AGSL_SOURCE = """
uniform shader input;
uniform float2 uSize;
uniform float uEdge;
uniform float uShift;
uniform float uLighten;
uniform float uRadius;
uniform float uSaturation;
uniform float uContrast;

// 点 p 到半径 r 的圆角矩形（半宽高 halfSize）的有符号距离：内部为负、外部为正
float rrectSdf(float2 p, float2 halfSize, float r) {
    float2 q = abs(p) - (halfSize - float2(r, r));
    float2 outside = max(q, float2(0.0, 0.0));
    return length(outside) + min(max(q.x, q.y), 0.0) - r;
}

half4 main(float2 coord) {
    float2 halfSize = uSize * 0.5;
    // uRadius <= 0 表示胶囊（高度一半）；圆角按钮 / FAB 传入实际圆角
    float r = uRadius > 0.0 ? min(uRadius, halfSize.y) : halfSize.y;
    float2 p = coord - halfSize;
    float d = rrectSdf(p, halfSize, r);   // <= 0 表示在胶囊内部

    // 边缘带强度：最外侧约 1，向内平滑衰减到 0
    float t = clamp(1.0 + d / uEdge, 0.0, 1.0);
    float2 samplePos = coord;
    if (t > 0.0) {
        t = t * t;

        // 数值梯度求 SDF 法线方向（指向外侧），取反得到“向内”方向
        float e = 1.0;
        float gx = rrectSdf(p + float2(e, 0.0), halfSize, r) - rrectSdf(p - float2(e, 0.0), halfSize, r);
        float gy = rrectSdf(p + float2(0.0, e), halfSize, r) - rrectSdf(p - float2(0.0, e), halfSize, r);
        float2 grad = float2(gx, gy);
        float gl = length(grad);
        float2 inDir = float2(0.0, 0.0);
        if (gl > 0.0001) {
            inDir = -grad / gl;
        }

        // 边缘向内轻微采样位移
        samplePos = coord + inDir * (uShift * t);
        samplePos = clamp(samplePos, float2(0.5, 0.5), uSize - float2(0.5, 0.5));
    }

    // 轻微提饱和 + 提对比：让真实背景的颜色和层次透出来，而不是被 scrim 洗灰
    half4 c = input.eval(samplePos);
    float3 rgb = float3(c.rgb);
    float luma = dot(rgb, float3(0.2126, 0.7152, 0.0722));
    rgb = mix(float3(luma), rgb, uSaturation);
    rgb = (rgb - 0.5) * uContrast + 0.5;

    // 极淡的边缘聚光；主要亮边交给上层的单圈扫光
    float lift = t * uLighten;
    rgb = clamp(rgb + float3(lift), 0.0, 1.0);
    return half4(half(rgb.r), half(rgb.g), half(rgb.b), c.a);
}
"""

/** 背景采样层的状态：折射（Shader + RenderEffect）或磨砂（二者都为空，走 [Modifier.blur]） */
internal class GlassBackdropState(
    /** 折射 Shader；磨砂模式（Android 12/12L）为 null */
    val shader: RuntimeShader? = null,
    /** 折射 RenderEffect；磨砂模式为 null */
    val refractionEffect: ComposeRenderEffect? = null,
) {
    /** true = 逐像素折射；false = 真背景模糊（磨砂玻璃） */
    val isRefraction: Boolean get() = refractionEffect != null
}

/**
 * 创建背景采样状态；低于 Android 12 时返回 null（不采样，保持原材质）。
 * 用 remember 保证整个组合生命周期只创建一次，绝不逐帧重建；
 * 外层 runCatching 兜底：即使某些设备创建失败，也只是回退，不会崩溃。
 */
@Composable
internal fun rememberGlassBackdropState(): GlassBackdropState? {
    if (!isGlassBackdropSupported()) return null
    return remember { runCatching { createGlassBackdropState() }.getOrNull() }
}

@RequiresApi(Build.VERSION_CODES.S)
private fun createGlassBackdropState(): GlassBackdropState {
    if (isGlassRefractionSupported()) {
        // Android 13+：优先逐像素折射；个别设备 Shader 编译失败时退回同版本的磨砂模糊
        val refraction = runCatching { createRefractionState() }.getOrNull()
        if (refraction != null) return refraction
    }
    // Android 12/12L（Android 13+ 上折射失败也走这里）：真背景模糊（磨砂玻璃）
    return GlassBackdropState()
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun createRefractionState(): GlassBackdropState {
    val shader = RuntimeShader(AGSL_SOURCE)
    // 先给一组安全初值：即使某一帧早于首次 onSizeChanged 绘制，也不会出现除零 / NaN
    shader.setFloatUniform("uSize", 1f, 1f)
    shader.setFloatUniform("uEdge", 1f)
    shader.setFloatUniform("uShift", 0f)
    shader.setFloatUniform("uLighten", 0f)
    shader.setFloatUniform("uRadius", -1f)
    shader.setFloatUniform("uSaturation", RefractionSaturation)
    shader.setFloatUniform("uContrast", RefractionContrast)
    val effect = RenderEffect.createRuntimeShaderEffect(shader, "input")
    return GlassBackdropState(shader = shader, refractionEffect = effect.asComposeRenderEffect())
}

/**
 * Dock / 玻璃按钮内的“背景采样 + 玻璃后处理”子节点（与玻璃轮廓完全同尺寸、画在材质下方）：
 *  1. 把录制的页面图层按 Dock 的屏幕位置平移，画进本节点区域（即 Dock 身后像素）；
 *  2. Android 13+ 先做 6dp 小半径模糊，再走 AGSL 边缘折射/调色；Android 12/12L 走 [Modifier.blur] 真背景模糊。
 * 只在 [dockPositionInRoot] 有效（已完成布局）时绘制，首帧也不会画错位置。
 */
@Composable
internal fun GlassBackdrop(
    backgroundLayer: GraphicsLayer,
    state: GlassBackdropState,
    dockPositionInRoot: Offset,
    modifier: Modifier = Modifier,
    /** null = 胶囊（圆角取高度一半）；圆角 FAB 传入实际圆角 */
    cornerRadius: Dp? = null,
    /** 磨砂模式（Android 12/12L）的模糊半径；折射模式下使用更小的 RefractionBlurRadius */
    blurRadius: Dp = GlassBlurRadius,
) {
    val density = LocalDensity.current
    val edgePx = with(density) { RefractionEdgeWidth.toPx() }
    val shiftPx = with(density) { RefractionMaxShift.toPx() }
    val radiusPx = cornerRadius?.let { with(density) { it.toPx() } } ?: -1f
    val shader = state.shader

    // uniforms 只在尺寸/参数变化时刷新，不逐帧设置；磨砂模式下是空操作
    val refreshUniforms: (IntSize) -> Unit = remember(shader, edgePx, shiftPx, radiusPx) {
        { size ->
            if (shader != null) {
                shader.setFloatUniform("uSize", size.width.toFloat(), size.height.toFloat())
                shader.setFloatUniform("uEdge", edgePx)
                shader.setFloatUniform("uShift", shiftPx)
                shader.setFloatUniform("uLighten", RefractionEdgeLighten)
                shader.setFloatUniform("uRadius", radiusPx)
                shader.setFloatUniform("uSaturation", RefractionSaturation)
                shader.setFloatUniform("uContrast", RefractionContrast)
            }
        }
    }

    val effectModifier = if (state.refractionEffect != null) {
        // Android 13+：先做 6dp 小半径背景模糊，再让 AGSL 折射/调色。
        // 外层 shader 的输入就是内层模糊后的画面，中央不会完全清晰。
        Modifier
            .graphicsLayer { renderEffect = state.refractionEffect }
            .blur(RefractionBlurRadius, BlurredEdgeTreatment.Unbounded)
    } else {
        // Android 12/12L：真背景模糊（磨砂玻璃）。
        // Unbounded：不让模糊在边缘衰减成透明，整个玻璃区域内的磨砂强度一致
        Modifier.blur(blurRadius, BlurredEdgeTreatment.Unbounded)
    }

    Box(
        modifier = modifier
            .then(effectModifier)
            .onSizeChanged(refreshUniforms)
            .drawWithContent {
                if (dockPositionInRoot.isSpecified) {
                    // 录制图层覆盖整个窗口、左上角为 (0,0)，平移 -Dock 位置即可对位
                    translate(-dockPositionInRoot.x, -dockPositionInRoot.y) {
                        drawLayer(backgroundLayer)
                    }
                }
            },
    ) {
        // 只负责绘制，没有子内容
    }
}
