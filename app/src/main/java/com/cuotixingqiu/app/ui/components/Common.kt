package com.cuotixingqiu.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.cuotixingqiu.app.data.db.WrongQuestionEntity
import com.cuotixingqiu.app.domain.ReviewStatus
import com.cuotixingqiu.app.domain.Subject
import com.cuotixingqiu.app.ui.theme.SubjectPalette
import com.cuotixingqiu.app.util.TimeUtils
import java.io.File

/**
 * 这一文件里放“多个页面都会用到”的通用小部件：
 * 页面骨架、空状态、确认对话框、筛选标签、难度星级、图片缩略图等。
 * 把这些抽出来，可以让各个页面代码更短、风格更统一。
 *
 * 视觉规范（改配色/圆角/字号时优先改这里，而不是逐个页面改）：
 *  - 卡片：白色（surfaceContainerLow），圆角 16dp，不用重边框和大阴影；
 *  - 输入框：浅灰填充、无下划线，圆角 12dp；
 *  - 筛选标签：未选中浅灰底，选中用品牌靛蓝；
 *  - 学科标签：每个学科一个柔和的独立颜色（浅色/深色自动适配）。
 */

// ---------------- 颜色小工具 ----------------

/** 当前是不是深色主题（用页面底色的明度判断，动态取色时同样有效） */
@Composable
internal fun isDarkScheme(): Boolean =
    MaterialTheme.colorScheme.background.luminance() < 0.5f

/** 学科的强调色（按学科顺序取，未知学科回退到“其他”色） */
fun subjectAccent(subjectName: String): Color {
    val index = Subject.displayOrder.indexOfFirst { it.displayName == subjectName }
    val safeIndex = if (index >= 0) index else SubjectPalette.lastIndex
    return SubjectPalette[safeIndex % SubjectPalette.size]
}

/** 图表里的学科颜色：深色主题下自动调亮，保证在深底上也能看清 */
@Composable
fun subjectChartColor(subjectName: String): Color {
    val accent = subjectAccent(subjectName)
    return if (isDarkScheme()) lerp(accent, Color.White, 0.35f) else accent
}

// ---------------- 页面骨架 ----------------

/**
 * 页面骨架：统一的标题栏 + 内容区。写新页面时只要套这一层就够了。
 *
 * 标题栏是自适应高度的自定义顶栏（不是固定 64dp 的 TopAppBar）：
 * 显示“标题 + 副标题”两行时不会挤，系统字体放大后也不会被裁掉。
 *
 * 自适应：在平板、手机横屏这类宽窗口上，顶栏和内容都会限制到
 * [maxContentWidth] 并水平居中，避免整页被横向拉宽（主从分栏等需要整宽的
 * 页面可以传 null 关掉这个限制）。
 *
 * @param maxContentWidth 顶栏与内容的最大宽度；null 表示铺满整宽
 * @param statusBarInset  是否自己避开状态栏。嵌入到别的页面里（例如大屏列表的详情栏）时传 false，
 *                        避免重复留出一条状态栏高度的空白
 * @param contentBehindTopBar 正文是否从顶栏下方穿过去（v1.3.5）。为 true 时：
 *                        Android 12+ 会把顶栏高度的一条页面内容录进离屏图层，顶栏对它做真实模糊，
 *                        并在底部柔和渐隐；Android 11 及以下只保留半透明 + 高光降级。
 *                        页面需要自己把顶部避让放进 LazyColumn 的 contentPadding，
 *                        并用 [withoutTopInset] 去掉内容容器上的 top padding。
 */
@Composable
fun ScreenScaffold(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    /** 首页等主要页面用它显示更大的品牌标题 */
    largeTitle: Boolean = false,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    snackbarHostState: SnackbarHostState? = null,
    floatingActionButton: (@Composable () -> Unit)? = null,
    bottomBar: (@Composable () -> Unit)? = null,
    maxContentWidth: Dp? = 840.dp,
    statusBarInset: Boolean = true,
    contentBehindTopBar: Boolean = false,
    content: @Composable (PaddingValues) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val dark = isDarkScheme()
    // 只有“正文穿到顶栏下方”的滚动页才额外录一条顶栏高度的内容；其它页面零额外开销。
    // 平台模糊从 Android 12（API 31）开始可用，更低版本只显示静态玻璃色调。
    val blurSupported = remember { isGlassBackdropSupported() }
    val topBarBlurActive = contentBehindTopBar && blurSupported

    // 没有启用穿透的页面不创建采样图层，避免无意义的 GraphicsLayer / RenderNode 分配
    val topBarBackdropLayer = if (topBarBlurActive) rememberGraphicsLayer() else null
    var topBarHeightPx by remember { mutableIntStateOf(0) }
    val background = scheme.background

    Scaffold(
        modifier = modifier,
        // 外层 Scaffold 已经处理过系统栏，这里不必再留一次 padding
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { topBarHeightPx = it.height },
            ) {
                // 1. 真实背景模糊：只采样顶栏高度的一条页面内容（Android 12+）
                val backdropLayer = topBarBackdropLayer
                if (topBarBlurActive && backdropLayer != null) {
                    TopBarGlassBackdrop(
                        backgroundLayer = backdropLayer,
                        modifier = Modifier.matchParentSize(),
                    )
                }
                // 2. 玻璃色调 + 顶部高光：底部柔和渐隐，不画横向分隔线
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .liquidTopBarTint(
                            isDark = dark,
                            scheme = scheme,
                            hasBackdrop = topBarBlurActive,
                        ),
                )
                // 3. 标题与操作区：状态栏 Insets 仍然只在这里处理一次
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (statusBarInset) {
                                Modifier.windowInsetsPadding(WindowInsets.statusBars)
                            } else {
                                Modifier
                            },
                        ),
                ) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth()
                            .then(
                                if (maxContentWidth != null) {
                                    Modifier.widthIn(max = maxContentWidth)
                                } else {
                                    Modifier
                                },
                            )
                            .padding(
                                start = if (onBack != null) 4.dp else 20.dp,
                                end = 6.dp,
                                top = 8.dp,
                                bottom = 8.dp,
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (onBack != null) {
                            GlassBackButton(onClick = onBack)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = if (largeTitle) {
                                    MaterialTheme.typography.headlineSmall
                                } else {
                                    MaterialTheme.typography.titleLarge
                                },
                                color = if (largeTitle) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onBackground
                                },
                            )
                            if (subtitle != null) {
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, content = actions)
                    }
                }
            }
        },
        snackbarHost = {
            if (snackbarHostState != null) {
                SnackbarHost(
                    hostState = snackbarHostState,
                    // 没有自家底栏时，Scaffold 会把提示贴到屏幕最底部；这里垫到悬浮 Dock 上方。
                    // 有底栏时 Scaffold 本来就把它放在底栏之上，不需要重复垫高。
                    modifier = if (bottomBar == null) {
                        Modifier.padding(bottom = rememberDockClearance())
                    } else {
                        Modifier
                    },
                )
            }
        },
        floatingActionButton = { floatingActionButton?.invoke() },
        bottomBar = {
            // 有自家底栏的页面（编辑保存条、详情操作条、列表多选条）：
            // 让底栏浮在悬浮 Dock 上方，而不是被 Dock 盖住；
            // 这段空隙同时计入 Scaffold 的 innerPadding，页面内容避让也就自动包含它。
            val bar = bottomBar
            if (bar != null) {
                Box(modifier = Modifier.padding(bottom = rememberDockClearance())) {
                    bar()
                }
            }
        },
        content = { padding ->
            val backdropLayer = topBarBackdropLayer
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (topBarBlurActive && backdropLayer != null) {
                            Modifier.topBarBackdropRecorder(
                                layer = backdropLayer,
                                topBarHeightPx = topBarHeightPx,
                                background = background,
                            )
                        } else {
                            Modifier
                        },
                    ),
            ) {
                if (maxContentWidth == null) {
                    content(padding)
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = maxContentWidth)
                            .align(Alignment.TopCenter),
                    ) {
                        content(padding)
                    }
                }
            }
        },
    )
}

/**
 * 把正文顶部“顶栏高度”的一条录进离屏图层，供顶栏做真实背景模糊。
 *
 * 只录一条（不是整屏），并且先铺主题背景色，保证模糊采样到的是不透明页面像素；
 * 录制完成后正文照常绘制到屏幕。图层大小随顶栏高度变化，横竖屏 / 字体放大都会自动适配。
 */
private fun Modifier.topBarBackdropRecorder(
    layer: GraphicsLayer,
    topBarHeightPx: Int,
    background: Color,
): Modifier = drawWithContent {
    val width = size.width.toInt()
    val height = topBarHeightPx.coerceIn(0, size.height.toInt())
    if (width > 0 && height > 0) {
        // 必须用 DrawScope 内的 GraphicsLayer.record(...) 扩展：它会把当前绘制上下文
        // 临时指到图层画布，这样 drawContent() 才真正录进图层（成员重载不会切换上下文）。
        layer.record(IntSize(width, height)) {
            drawRect(color = background)
            this@drawWithContent.drawContent()
        }
    }
    this@drawWithContent.drawContent()
}

/**
 * 去掉 Scaffold 内边距里的顶部一份（正文要穿到顶栏下面时使用）。
 * 左右、底部保持原值，所以底栏、Dock 的避让不会丢。
 */
fun PaddingValues.withoutTopInset(): PaddingValues = object : PaddingValues {
    override fun calculateLeftPadding(layoutDirection: LayoutDirection): Dp =
        this@withoutTopInset.calculateLeftPadding(layoutDirection)

    override fun calculateTopPadding(): Dp = 0.dp

    override fun calculateRightPadding(layoutDirection: LayoutDirection): Dp =
        this@withoutTopInset.calculateRightPadding(layoutDirection)

    override fun calculateBottomPadding(): Dp =
        this@withoutTopInset.calculateBottomPadding()
}

/** 难度星级。
 * onSelect 不为空时是“可以点”的（用于编辑页设置难度；点已经选中的星星表示取消，重新变回 0）。
 */
@Composable
fun DifficultyStars(
    difficulty: Int,
    modifier: Modifier = Modifier,
    starSize: Dp = 18.dp,
    onSelect: ((Int) -> Unit)? = null,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        for (index in 1..MAX_DIFFICULTY) {
            val filled = index <= difficulty
            val starModifier = Modifier.size(starSize)
            val tint = if (filled) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
            }
            if (onSelect == null) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = tint,
                    modifier = starModifier,
                )
            } else {
                IconButton(
                    onClick = { onSelect(if (difficulty == index) 0 else index) },
                    modifier = Modifier.size(starSize + 12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = "${index} 星难度",
                        tint = tint,
                        modifier = starModifier,
                    )
                }
            }
        }
    }
}

// ---------------- 输入与筛选 ----------------

/**
 * 统一风格的单行/多行输入框：浅灰填充、无下划线、圆角 12dp。
 * 比 OutlinedTextField 的边框轻，写长文本时也保持干净。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CuotiTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    singleLine: Boolean = false,
    minLines: Int = 1,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    leadingIcon: ImageVector? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        readOnly = readOnly,
        singleLine = singleLine,
        minLines = minLines,
        label = label?.let { { Text(text = it) } },
        placeholder = placeholder?.let { { Text(text = it) } },
        leadingIcon = leadingIcon?.let {
            {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
            }
        },
        trailingIcon = trailingIcon,
        shape = RoundedCornerShape(12.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            cursorColor = MaterialTheme.colorScheme.primary,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
            focusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            focusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unfocusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
        ),
    )
}

/**
 * 统一样式的筛选标签。
 * 未选中：浅灰底 + 次级文字；选中：品牌靛蓝 + 白字。
 */
@Composable
fun CuotiFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text = label, maxLines = 1) },
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        leadingIcon = leadingIcon?.let {
            {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            }
        },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
        ),
        border = null,
    )
}

/**
 * 一行横向排列的筛选标签（第一个通常是“全部”）。
 *
 * @param allLabel 最前面“全部”标签的文字。传 null 表示不需要这个标签
 *                 （例如“主题模式”这种本身就是完整选项列表的场景）。
 */
@Composable
fun <T> FilterChipRow(
    options: List<T>,
    selected: T?,
    labelOf: (T) -> String,
    onSelect: (T?) -> Unit,
    modifier: Modifier = Modifier,
    allLabel: String? = "全部",
    leading: (@Composable () -> Unit)? = null,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp),
    ) {
        if (leading != null) {
            item { leading() }
        }
        if (allLabel != null) {
            item {
                CuotiFilterChip(
                    selected = selected == null,
                    onClick = { onSelect(null) },
                    label = allLabel,
                )
            }
        }
        items(options) { option ->
            CuotiFilterChip(
                selected = selected == option,
                onClick = { onSelect(option) },
                label = labelOf(option),
            )
        }
    }
}

// ---------------- 空状态与对话框 ----------------

/**
 * 空状态：没有数据时显示，给用户一个明确的下一步动作。
 *
 * 用法要点：
 *  - [title] / [message] 要写真实语境（“还没有错题”和“没有符合条件的错题”是两回事）；
 *  - 主要按钮最多一个；可以再给一个次要的文字按钮（例如“先看看复习规则”）；
 *  - [maxWidth] 限制文字与按钮的宽度，大屏上不会把一句话拉成一行字。
 */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    secondaryActionLabel: String? = null,
    onSecondaryAction: (() -> Unit)? = null,
    maxWidth: Dp = 380.dp,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            // 空状态是上下居中的：底部再让出悬浮 Dock 的高度，
            // 避免窗口很矮（比如横屏）时按钮沉到 Dock 后面
            .padding(
                start = 32.dp,
                end = 32.dp,
                top = 24.dp,
                bottom = 24.dp + rememberDockClearance(),
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Column(
            modifier = Modifier.widthIn(max = maxWidth),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(34.dp),
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(22.dp))
                Button(onClick = onAction) { Text(text = actionLabel) }
            }
            if (secondaryActionLabel != null && onSecondaryAction != null) {
                Spacer(modifier = Modifier.height(2.dp))
                TextButton(onClick = onSecondaryAction) { Text(text = secondaryActionLabel) }
            }
        }
    }
}

/** 二次确认对话框（删除、清空数据等危险操作都要先弹它） */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmLabel: String = "确定",
    dismissLabel: String = "取消",
    destructive: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = { Text(text = message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = confirmLabel,
                    fontWeight = FontWeight.SemiBold,
                    color = if (destructive) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(text = dismissLabel) }
        },
    )
}

// ---------------- 图片 ----------------

/** 详情/复习页显示错题图片：横向可滑动，点一下放大看 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionImagesRow(
    files: List<File>,
    onClick: (File) -> Unit,
    modifier: Modifier = Modifier,
    thumbHeight: Dp = 130.dp,
) {
    if (files.isEmpty()) return
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(files) { file ->
            Card(
                onClick = { onClick(file) },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
                modifier = Modifier.height(thumbHeight),
            ) {
                AsyncImage(
                    model = file,
                    contentDescription = "错题图片",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .height(thumbHeight)
                        .width(thumbHeight),
                )
            }
        }
    }
}

/** 点开图片后的大图查看（点任意位置关闭） */
@Composable
fun ImageViewerDialog(file: File, onDismiss: () -> Unit) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.92f))
                .padding(12.dp),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = file,
                contentDescription = "错题图片（大图）",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                Text(text = "关闭", color = Color.White)
            }
        }
    }
}

// ---------------- 下面是一些“纯函数”小工具 ----------------

const val MAX_DIFFICULTY = 5

/** 列表里显示的标题：没填标题时用内容的第一行代替 */
fun questionDisplayTitle(question: WrongQuestionEntity): String {
    if (question.title.isNotBlank()) return question.title.trim()
    val firstLine = question.content.lineSequence().firstOrNull { it.isNotBlank() }?.trim()
    if (!firstLine.isNullOrEmpty()) {
        return if (firstLine.length > 30) firstLine.take(30) + "…" else firstLine
    }
    return "未命名错题"
}

/** 难度的文字形式 */
fun difficultyLabel(difficulty: Int): String =
    if (difficulty <= 0) "未标注" else "★".repeat(difficulty)

/**
 * 复习状态的文字说明。
 * @return 文字 + 是否已逾期（逾期显示成醒目的颜色）
 */
fun reviewStatusInfo(question: WrongQuestionEntity, now: Long = System.currentTimeMillis()): Pair<String, Boolean> {
    if (question.status == ReviewStatus.MASTERED.name) return "已掌握" to false
    val next = question.nextReviewAt
    if (next == null) return "待安排复习" to false
    val overdue = next < now
    val text = if (overdue) "待复习 · 已到期" else "下次复习 · " + TimeUtils.describeRelativeDay(next)
    return text to overdue
}

/** “2 天前复习过”之类的说明 */
fun lastReviewLabel(question: WrongQuestionEntity): String {
    val at = question.lastReviewedAt ?: return "还没有复习记录"
    val result = when (question.lastReviewResult) {
        "AGAIN" -> "不会"
        "GOOD" -> "基本掌握"
        "EASY" -> "已掌握"
        else -> "已复习"
    }
    return "最近一次：$result（${TimeUtils.describeRelativeDay(at)}，共复习 ${question.reviewCount} 次）"
}
