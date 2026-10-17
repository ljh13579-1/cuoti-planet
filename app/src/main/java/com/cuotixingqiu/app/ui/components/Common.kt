package com.cuotixingqiu.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.cuotixingqiu.app.data.db.WrongQuestionEntity
import com.cuotixingqiu.app.domain.ReviewStatus
import com.cuotixingqiu.app.util.TimeUtils
import java.io.File

/**
 * 这一文件里放“多个页面都会用到”的通用小部件：
 * 页面骨架、空状态、确认对话框、筛选标签、难度星级、图片缩略图等。
 * 把这些抽出来，可以让各个页面代码更短、风格更统一。
 */

/** 页面骨架：统一的标题栏 + 内容区。写新页面时只要套这一层就够了。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenScaffold(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    snackbarHostState: SnackbarHostState? = null,
    floatingActionButton: (@Composable () -> Unit)? = null,
    bottomBar: (@Composable () -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        // 外层 Scaffold 已经处理过系统栏，这里不必再留一次 padding
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleLarge,
                        )
                        if (subtitle != null) {
                            Text(
                                text = subtitle,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回",
                            )
                        }
                    }
                },
                actions = actions,
            )
        },
        snackbarHost = {
            if (snackbarHostState != null) {
                SnackbarHost(hostState = snackbarHostState)
            }
        },
        floatingActionButton = { floatingActionButton?.invoke() },
        bottomBar = { bottomBar?.invoke() },
        content = content,
    )
}

/** 小标题（列表里的分组标题） */
@Composable
fun SectionTitle(
    text: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        trailing?.invoke()
    }
}

/** 学科小标签 */
@Composable
fun SubjectBadge(subject: String, modifier: Modifier = Modifier) {
    SmallBadge(
        text = subject,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    )
}

/** 通用的小圆角标签 */
@Composable
fun SmallBadge(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Surface(
        modifier = modifier,
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(6.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

/**
 * 难度星级。
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
            if (onSelect == null) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = if (filled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                    },
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
                        tint = if (filled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                        },
                        modifier = starModifier,
                    )
                }
            }
        }
    }
}

/** 空状态：没有数据时显示，给用户一个明确的下一步动作 */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
            modifier = Modifier.size(64.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(20.dp))
            Button(onClick = onAction) { Text(text = actionLabel) }
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

/** 一行横向排列的筛选标签（第一个是“全部”） */
@Composable
fun <T> FilterChipRow(
    options: List<T>,
    selected: T?,
    labelOf: (T) -> String,
    onSelect: (T?) -> Unit,
    modifier: Modifier = Modifier,
    allLabel: String = "全部",
    leading: (@Composable () -> Unit)? = null,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 2.dp),
    ) {
        if (leading != null) {
            item { leading() }
        }
        item {
            FilterChip(
                selected = selected == null,
                onClick = { onSelect(null) },
                label = { Text(allLabel) },
            )
        }
        items(options) { option ->
            FilterChip(
                selected = selected == option,
                onClick = { onSelect(option) },
                label = { Text(labelOf(option), maxLines = 1) },
            )
        }
    }
}

/** “标签 + 内容”的一行文字（详情页使用） */
@Composable
fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(72.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
    }
}

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
                        .width(thumbHeight)
                        .clip(RoundedCornerShape(12.dp)),
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
