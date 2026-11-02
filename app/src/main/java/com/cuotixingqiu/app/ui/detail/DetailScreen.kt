package com.cuotixingqiu.app.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cuotixingqiu.app.data.db.WrongQuestionEntity
import com.cuotixingqiu.app.domain.ReviewStatus
import com.cuotixingqiu.app.ui.appViewModel
import com.cuotixingqiu.app.ui.components.ConfirmDialog
import com.cuotixingqiu.app.ui.components.CuotiCard
import com.cuotixingqiu.app.ui.components.GlassBackButton
import com.cuotixingqiu.app.ui.components.ImageViewerDialog
import com.cuotixingqiu.app.ui.components.KnowledgeTag
import com.cuotixingqiu.app.ui.components.PrimaryActionButton
import com.cuotixingqiu.app.ui.components.QuestionImagesRow
import com.cuotixingqiu.app.ui.components.QuietSectionLabel
import com.cuotixingqiu.app.ui.components.ReviewStatusLabel
import com.cuotixingqiu.app.ui.components.SecondaryActionButton
import com.cuotixingqiu.app.ui.components.SubjectDot
import com.cuotixingqiu.app.ui.components.ThinDivider
import com.cuotixingqiu.app.ui.components.pageSidePadding
import com.cuotixingqiu.app.ui.components.rememberDockClearance
import com.cuotixingqiu.app.ui.components.subjectChartColor
import com.cuotixingqiu.app.ui.rememberAppContainer
import com.cuotixingqiu.app.util.TimeUtils
import java.io.File

/**
 * 错题详情（二级页面）：
 * 玻璃返回键 + 标题 + 更多菜单；真实学科/日期/复习次数；知识点；题目；错因；解析；
 * 底部“编辑 / 加入今日复习”。
 *
 * @param reserveDockClearance 二级页面本身不显示 Dock，传 false；
 *                             大屏列表-详情分栏仍由外层 Dock 覆盖，传 true。
 */
@Composable
fun DetailScreen(
    questionId: Long,
    onBack: (() -> Unit)? = null,
    onEdit: () -> Unit,
    onStartReview: () -> Unit,
    onDeleted: (() -> Unit)? = null,
    statusBarInset: Boolean = true,
    reserveDockClearance: Boolean = true,
) {
    val container = rememberAppContainer()
    val viewModel: DetailViewModel = appViewModel(key = "detail-$questionId") { appContainer ->
        DetailViewModel(
            repository = appContainer.wrongQuestionRepository,
            notebookRepository = appContainer.notebookRepository,
            questionId = questionId,
        )
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var viewerFile by remember { mutableStateOf<File?>(null) }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    val question = uiState.question
    val mastered = question?.status == ReviewStatus.MASTERED.name

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val side = pageSidePadding(maxWidth)
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (statusBarInset) Modifier.statusBarsPadding() else Modifier),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(horizontal = (side - 4.dp).coerceAtLeast(4.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (onBack != null) {
                            GlassBackButton(
                                onClick = onBack,
                                modifier = Modifier.align(Alignment.CenterStart),
                            )
                        }
                        Text(
                            text = "错题详情",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.align(Alignment.Center),
                        )
                        Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .clickable(enabled = question != null) { showMoreMenu = true },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.MoreVert,
                                    contentDescription = "更多操作",
                                    tint = if (question != null) {
                                        MaterialTheme.colorScheme.onSurface
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                    },
                                    modifier = Modifier.size(21.dp),
                                )
                            }
                            DropdownMenu(
                                expanded = showMoreMenu,
                                onDismissRequest = { showMoreMenu = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text("编辑这道错题") },
                                    leadingIcon = {
                                        Icon(Icons.Filled.Edit, contentDescription = null)
                                    },
                                    onClick = {
                                        showMoreMenu = false
                                        onEdit()
                                    },
                                )
                                if (mastered) {
                                    DropdownMenuItem(
                                        text = { Text("重新加入复习计划") },
                                        onClick = {
                                            showMoreMenu = false
                                            viewModel.reopenReview()
                                        },
                                    )
                                } else {
                                    DropdownMenuItem(
                                        text = { Text("标记为已掌握") },
                                        onClick = {
                                            showMoreMenu = false
                                            viewModel.markMastered()
                                        },
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("删除", color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Filled.Delete,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                        )
                                    },
                                    onClick = {
                                        showMoreMenu = false
                                        showDeleteDialog = true
                                    },
                                )
                            }
                        }
                    }
                }

                if (!uiState.loaded) {
                    Box(modifier = Modifier.fillMaxSize())
                } else if (question == null) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = "这道错题已经不存在了（可能刚被删除）",
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(24.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(
                            start = side,
                            end = side,
                            top = 10.dp,
                            bottom = 24.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(22.dp),
                    ) {
                        item {
                            DetailMetaLine(
                                question = question,
                            )
                        }

                        if (question.knowledgePoints.isNotEmpty()) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                                ) {
                                    question.knowledgePoints.forEach { tag ->
                                        KnowledgeTag(text = tag)
                                    }
                                }
                            }
                        }

                        item {
                            QuestionContentCard(
                                question = question,
                                imageFiles = question.imageNames.mapNotNull {
                                    container.imageStore.fileOrNull(it)
                                },
                                onImageClick = { file -> viewerFile = file },
                            )
                        }

                        if (question.reason.isNotBlank()) {
                            item {
                                PlainTextSection(
                                    title = "错误原因",
                                    text = question.reason,
                                )
                            }
                        }

                        if (question.solution.isNotBlank()) {
                            item {
                                SolutionSection(text = question.solution)
                            }
                        }

                        if (question.summary.isNotBlank()) {
                            item {
                                PlainTextSection(
                                    title = "我的总结",
                                    text = question.summary,
                                )
                            }
                        }

                        item {
                            ReviewFooter(
                                question = question,
                                notebookName = uiState.notebookName,
                            )
                        }
                    }

                    DetailBottomBar(
                        onEdit = onEdit,
                        onStartReview = onStartReview,
                        reserveDockClearance = reserveDockClearance,
                        contentSide = side,
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(
                    bottom = if (reserveDockClearance) {
                        rememberDockClearance() + 100.dp
                    } else {
                        110.dp
                    },
                ),
        )
    }

    if (showDeleteDialog) {
        ConfirmDialog(
            title = "删除这道错题？",
            message = "删除后无法恢复，题目里的图片也会一起删除。如果只是想暂时不看，可以改成“已掌握”。",
            confirmLabel = "删除",
            destructive = true,
            onConfirm = {
                showDeleteDialog = false
                viewModel.delete(
                    onDeleted = {
                        val callback = onDeleted ?: onBack
                        callback?.invoke()
                    },
                )
            },
            onDismiss = { showDeleteDialog = false },
        )
    }

    viewerFile?.let { file ->
        ImageViewerDialog(file = file, onDismiss = { viewerFile = null })
    }
}

@Composable
private fun DetailMetaLine(question: WrongQuestionEntity) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SubjectDot(subject = question.subject, size = 8.dp)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = question.subject,
            style = MaterialTheme.typography.labelLarge,
            color = subjectChartColor(question.subject),
        )
        Text(
            text = " · ${TimeUtils.formatMonthDay(question.createdAt)}收集 · 已复习${question.reviewCount}次",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun QuestionContentCard(
    question: WrongQuestionEntity,
    imageFiles: List<File>,
    onImageClick: (File) -> Unit,
) {
    CuotiCard(contentPadding = PaddingValues(20.dp)) {
        QuietSectionLabel(text = "题目")
        Spacer(modifier = Modifier.height(10.dp))
        val title = question.title.trim()
        val content = question.content.trim()
        if (title.isNotEmpty()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 16.sp,
                    lineHeight = 23.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (content.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
        if (content.isNotEmpty()) {
            Text(
                text = content,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 16.sp,
                    lineHeight = 28.sp,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
        } else if (title.isEmpty()) {
            Text(
                text = "未填写题目内容",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 16.sp,
                    lineHeight = 28.sp,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (imageFiles.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            QuestionImagesRow(
                files = imageFiles,
                onClick = onImageClick,
                thumbHeight = 118.dp,
            )
        }
    }
}

@Composable
private fun PlainTextSection(title: String, text: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        QuietSectionLabel(text = title)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 15.sp,
                lineHeight = 26.sp,
            ),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f),
        )
    }
}

@Composable
private fun SolutionSection(text: String) {
    val lines = remember(text) {
        text.lines().map { it.trimEnd() }.filter { it.isNotBlank() }
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        QuietSectionLabel(text = "正确解析")
        Spacer(modifier = Modifier.height(8.dp))
        if (lines.size <= 1) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 15.sp,
                    lineHeight = 26.sp,
                ),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f),
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                lines.forEach { line ->
                    val trimmed = line.trimStart()
                    val isAnswer = trimmed.startsWith("答案") || trimmed.startsWith("答：")
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 15.sp,
                            lineHeight = 25.sp,
                            fontWeight = if (isAnswer) FontWeight.SemiBold else FontWeight.Normal,
                        ),
                        color = if (isAnswer) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ReviewFooter(question: WrongQuestionEntity, notebookName: String?) {
    val mastered = question.status == ReviewStatus.MASTERED.name
    Column(modifier = Modifier.fillMaxWidth()) {
        ThinDivider()
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ReviewStatusLabel(mastered = mastered)
            Spacer(modifier = Modifier.weight(1f))
            if (notebookName != null) {
                Text(
                    text = notebookName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        val next = question.nextReviewAt
        Text(
            text = if (mastered) {
                "已掌握，退出复习计划；可以从右上角重新加入"
            } else if (next != null) {
                "下次复习：${TimeUtils.describeRelativeDay(next)} · 累计复习 ${question.reviewCount} 次"
            } else {
                "等待安排下次复习 · 累计复习 ${question.reviewCount} 次"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DetailBottomBar(
    onEdit: () -> Unit,
    onStartReview: () -> Unit,
    reserveDockClearance: Boolean,
    contentSide: androidx.compose.ui.unit.Dp,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = if (reserveDockClearance) {
                Modifier.padding(bottom = rememberDockClearance())
            } else {
                Modifier.navigationBarsPadding()
            },
        ) {
            ThinDivider()
            Box(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = contentSide, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SecondaryActionButton(
                        text = "编辑",
                        onClick = onEdit,
                        modifier = Modifier.weight(0.38f),
                    )
                    PrimaryActionButton(
                        text = "加入今日复习",
                        onClick = onStartReview,
                        modifier = Modifier.weight(0.62f),
                    )
                }
            }
        }
    }
}
