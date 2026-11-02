package com.cuotixingqiu.app.ui.list

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cuotixingqiu.app.data.db.NotebookEntity
import com.cuotixingqiu.app.data.db.WrongQuestionEntity
import com.cuotixingqiu.app.domain.ReviewStatus
import com.cuotixingqiu.app.domain.Subject
import com.cuotixingqiu.app.ui.adaptive.WindowWidthSize
import com.cuotixingqiu.app.ui.adaptive.rememberWindowWidthSize
import com.cuotixingqiu.app.ui.appViewModel
import com.cuotixingqiu.app.ui.components.ConfirmDialog
import com.cuotixingqiu.app.ui.components.CuotiSearchField
import com.cuotixingqiu.app.ui.components.EmptyState
import com.cuotixingqiu.app.ui.components.FilterChipRow
import com.cuotixingqiu.app.ui.components.FilterSlidersIcon
import com.cuotixingqiu.app.ui.components.KnowledgeTagRow
import com.cuotixingqiu.app.ui.components.ReviewStatusLabel
import com.cuotixingqiu.app.ui.components.SubjectDot
import com.cuotixingqiu.app.ui.components.SubjectFilterPill
import com.cuotixingqiu.app.ui.components.ThinDivider
import com.cuotixingqiu.app.ui.components.rememberDockClearance
import com.cuotixingqiu.app.ui.components.subjectChartColor
import com.cuotixingqiu.app.ui.detail.DetailScreen
import com.cuotixingqiu.app.ui.paper.PaperRequest
import com.cuotixingqiu.app.util.TimeUtils

/**
 * 错题列表页：标题 + 搜索 + 学科筛选 + 筛选排序 + 轻量纵向列表。
 *
 * 业务能力保持不变：
 *  - 搜索、学科 / 分类 / 状态 / 难度 / 知识点筛选、三种排序；
 *  - 长按进入多选，批量标记掌握、需复习、组卷和删除；
 *  - 大屏（≥840dp）左侧列表 + 右侧详情分栏。
 */
@Composable
fun QuestionListScreen(
    onOpenQuestion: (Long) -> Unit,
    onEditQuestion: (Long) -> Unit,
    onAddQuestion: () -> Unit,
    onExportPaper: () -> Unit,
    onStartReview: (Long) -> Unit,
    requestedSubject: String? = null,
    onRequestedSubjectApplied: () -> Unit = {},
) {
    val viewModel: QuestionListViewModel = appViewModel { container ->
        QuestionListViewModel(container.wrongQuestionRepository, container.notebookRepository)
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(requestedSubject) {
        val subject = requestedSubject ?: return@LaunchedEffect
        viewModel.setSubject(subject)
        onRequestedSubjectApplied()
    }

    var showFilterDialog by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<WrongQuestionEntity?>(null) }
    var showBatchDeleteDialog by remember { mutableStateOf(false) }

    val twoPane = rememberWindowWidthSize() == WindowWidthSize.EXPANDED
    var paneQuestionId by rememberSaveable { mutableStateOf<Long?>(null) }

    val filtered = uiState.questions
    val dockClearance = rememberDockClearance()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        if (twoPane) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
            ) {
                ListBody(
                    modifier = Modifier
                        .weight(0.44f)
                        .fillMaxHeight(),
                    viewModel = viewModel,
                    uiState = uiState,
                    filtered = filtered,
                    paneSelectedId = paneQuestionId,
                    bottomPadding = dockClearance + if (uiState.selectionMode) 88.dp else 20.dp,
                    onAddQuestion = onAddQuestion,
                    onOpenQuestion = { id -> paneQuestionId = id },
                    onEditQuestion = onEditQuestion,
                    onDeleteQuestion = { question -> deleteTarget = question },
                    onOpenFilterDialog = { showFilterDialog = true },
                    onOpenSortDialog = { showSortDialog = true },
                )
                VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Box(
                    modifier = Modifier
                        .weight(0.56f)
                        .fillMaxHeight(),
                ) {
                    val paneId = paneQuestionId
                    if (paneId == null) {
                        EmptyState(
                            icon = Icons.AutoMirrored.Filled.List,
                            title = "选一道错题查看详情",
                            message = "点左侧列表里的任意一道题，这里会显示完整内容、图片和复习操作。",
                            maxWidth = 320.dp,
                        )
                    } else {
                        DetailScreen(
                            questionId = paneId,
                            onEdit = { onEditQuestion(paneId) },
                            onStartReview = { onStartReview(paneId) },
                            onDeleted = { paneQuestionId = null },
                            statusBarInset = false,
                            reserveDockClearance = true,
                        )
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
            ) {
                ListBody(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxHeight()
                        .widthIn(max = 840.dp)
                        .fillMaxWidth(),
                    viewModel = viewModel,
                    uiState = uiState,
                    filtered = filtered,
                    paneSelectedId = null,
                    bottomPadding = dockClearance + if (uiState.selectionMode) 88.dp else 20.dp,
                    onAddQuestion = onAddQuestion,
                    onOpenQuestion = onOpenQuestion,
                    onEditQuestion = onEditQuestion,
                    onDeleteQuestion = { question -> deleteTarget = question },
                    onOpenFilterDialog = { showFilterDialog = true },
                    onOpenSortDialog = { showSortDialog = true },
                )
            }
        }

        if (uiState.selectionMode) {
            SelectionBottomBar(
                onSelectAll = { viewModel.selectAll(filtered.map { it.id }) },
                onMarkMastered = { viewModel.markSelected(ReviewStatus.MASTERED) },
                onMarkNeedsReview = { viewModel.markSelected(ReviewStatus.NEEDS_REVIEW) },
                onExportPaper = {
                    PaperRequest.questionIds = uiState.selection.toList()
                    onExportPaper()
                },
                onDelete = { showBatchDeleteDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(
                        start = 12.dp,
                        end = 12.dp,
                        bottom = dockClearance + 8.dp,
                    ),
            )
        }
    }

    if (showFilterDialog) {
        FilterDialog(
            filter = uiState.filter,
            notebooks = uiState.notebooks,
            knowledgePoints = uiState.knowledgePoints,
            onApply = { updated ->
                viewModel.setNotebook(updated.notebookId)
                viewModel.setStatus(updated.status)
                viewModel.setMinDifficulty(updated.minDifficulty)
                viewModel.setKnowledgePoint(updated.knowledgePoint)
                showFilterDialog = false
            },
            onDismiss = { showFilterDialog = false },
        )
    }

    if (showSortDialog) {
        SortDialog(
            current = uiState.filter.sort,
            onSelect = {
                viewModel.setSort(it)
                showSortDialog = false
            },
            onDismiss = { showSortDialog = false },
        )
    }

    deleteTarget?.let { question ->
        ConfirmDialog(
            title = "删除这道错题？",
            message = "删除后无法恢复，题目里的图片也会一起删除。",
            confirmLabel = "删除",
            destructive = true,
            onConfirm = {
                viewModel.deleteOne(question.id)
                if (paneQuestionId == question.id) paneQuestionId = null
                deleteTarget = null
            },
            onDismiss = { deleteTarget = null },
        )
    }

    if (showBatchDeleteDialog) {
        ConfirmDialog(
            title = "删除选中的 ${uiState.selection.size} 道错题？",
            message = "删除后无法恢复，这些题目的图片也会一起删除。",
            confirmLabel = "全部删除",
            destructive = true,
            onConfirm = {
                if (paneQuestionId != null && paneQuestionId!! in uiState.selection) {
                    paneQuestionId = null
                }
                viewModel.deleteSelected()
                showBatchDeleteDialog = false
            },
            onDismiss = { showBatchDeleteDialog = false },
        )
    }
}

@Composable
private fun ListBody(
    modifier: Modifier,
    viewModel: QuestionListViewModel,
    uiState: QuestionListUiState,
    filtered: List<WrongQuestionEntity>,
    paneSelectedId: Long?,
    bottomPadding: androidx.compose.ui.unit.Dp,
    onAddQuestion: () -> Unit,
    onOpenQuestion: (Long) -> Unit,
    onEditQuestion: (Long) -> Unit,
    onDeleteQuestion: (WrongQuestionEntity) -> Unit,
    onOpenFilterDialog: () -> Unit,
    onOpenSortDialog: () -> Unit,
) {
    Column(modifier = modifier.fillMaxSize()) {
        ListHeader(
            selectionMode = uiState.selectionMode,
            selectionCount = uiState.selection.size,
            totalCount = uiState.totalCount,
            onAddQuestion = onAddQuestion,
            onCancelSelection = viewModel::clearSelection,
        )

        CuotiSearchField(
            value = uiState.filter.search,
            onValueChange = viewModel::setSearch,
            placeholder = "搜索题目、知识点",
            modifier = Modifier.padding(horizontal = 20.dp),
        )

        Spacer(modifier = Modifier.height(14.dp))
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 20.dp),
        ) {
            item {
                SubjectFilterPill(
                    label = "全部学科",
                    selected = uiState.filter.subject == null,
                    onClick = { viewModel.setSubject(null) },
                )
            }
            items(Subject.displayOrder) { subject ->
                SubjectFilterPill(
                    label = subject.displayName,
                    selected = uiState.filter.subject == subject.displayName,
                    onClick = { viewModel.setSubject(subject.displayName) },
                )
            }
        }

        FilterSortRow(
            filter = uiState.filter,
            onOpenFilterDialog = onOpenFilterDialog,
            onOpenSortDialog = onOpenSortDialog,
        )

        ThinDivider()

        when {
            uiState.totalCount == 0 -> EmptyState(
                icon = Icons.AutoMirrored.Filled.List,
                title = "还没有错题",
                message = "添加第一道错题后，这里会按学科、状态和知识点帮你整理好。",
                actionLabel = "添加第一道错题",
                onAction = onAddQuestion,
                maxWidth = 420.dp,
                modifier = Modifier.weight(1f),
            )

            filtered.isEmpty() -> EmptyState(
                icon = Icons.Filled.Search,
                title = "没有符合条件的错题",
                message = when {
                    uiState.filter.search.isNotBlank() ->
                        "没有找到与“${uiState.filter.search.trim()}”相关的错题，换个关键词再试试。"
                    uiState.filter.subject != null ->
                        "「${uiState.filter.subject}」下暂时没有符合条件的错题，可以放宽筛选条件。"
                    else -> "当前筛选条件下没有错题，可以放宽筛选条件再试试。"
                },
                actionLabel = "清除搜索和筛选",
                onAction = {
                    viewModel.resetExtraFilters()
                    viewModel.setSearch("")
                    viewModel.setSubject(null)
                },
                maxWidth = 420.dp,
                modifier = Modifier.weight(1f),
            )

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    top = 0.dp,
                    bottom = bottomPadding,
                ),
            ) {
                items(filtered, key = { it.id }) { question ->
                    QuestionListRow(
                        question = question,
                        selectionMode = uiState.selectionMode,
                        selected = if (uiState.selectionMode) {
                            question.id in uiState.selection
                        } else {
                            question.id == paneSelectedId
                        },
                        onClick = {
                            if (uiState.selectionMode) {
                                viewModel.toggleSelection(question.id)
                            } else {
                                onOpenQuestion(question.id)
                            }
                        },
                        onLongClick = { viewModel.startSelection(question.id) },
                        onEdit = { onEditQuestion(question.id) },
                        onDelete = { onDeleteQuestion(question) },
                        onToggleStatus = {
                            val target = if (question.status == ReviewStatus.MASTERED.name) {
                                ReviewStatus.NEEDS_REVIEW
                            } else {
                                ReviewStatus.MASTERED
                            }
                            viewModel.setStatusForOne(question.id, target)
                        },
                    )
                    ThinDivider(
                        modifier = Modifier.padding(
                            start = if (uiState.selectionMode) 34.dp else 0.dp,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun ListHeader(
    selectionMode: Boolean,
    selectionCount: Int,
    totalCount: Int,
    onAddQuestion: () -> Unit,
    onCancelSelection: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (selectionMode) "已选 $selectionCount 道" else "错题",
            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 32.sp),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.weight(1f))
        if (selectionMode) {
            TextButton(onClick = onCancelSelection) {
                Text("取消", style = MaterialTheme.typography.labelLarge)
            }
        } else {
            Text(
                text = "共 $totalCount 题",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable(onClick = onAddQuestion),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "添加错题",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(19.dp),
                )
            }
        }
    }
}

@Composable
private fun FilterSortRow(
    filter: QuestionFilter,
    onOpenFilterDialog: () -> Unit,
    onOpenSortDialog: () -> Unit,
) {
    val extraCount = listOf(
        filter.notebookId != null,
        filter.status != null,
        filter.minDifficulty > 0,
        filter.knowledgePoint != null,
    ).count { it }
    val filterLabel = when {
        filter.status == ReviewStatus.NEEDS_REVIEW -> "筛选 · 未掌握"
        filter.status == ReviewStatus.MASTERED -> "筛选 · 已掌握"
        extraCount > 0 -> "筛选 · $extraCount 项"
        else -> "筛选"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onOpenFilterDialog)
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilterSlidersIcon(
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(7.dp))
            Text(
                text = filterLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onOpenSortDialog)
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = filter.sort.displayName,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun QuestionListRow(
    question: WrongQuestionEntity,
    selectionMode: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleStatus: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val mastered = question.status == ReviewStatus.MASTERED.name
    val rowBackground = if (selected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.34f)
    } else {
        Color.Transparent
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowBackground)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(vertical = 14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (selectionMode) {
                Checkbox(
                    checked = selected,
                    onCheckedChange = { onClick() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary,
                    ),
                    modifier = Modifier.size(24.dp),
                )
                Spacer(modifier = Modifier.width(10.dp))
            } else {
                SubjectDot(subject = question.subject, size = 8.dp)
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = question.subject,
                style = MaterialTheme.typography.labelMedium,
                color = subjectChartColor(question.subject),
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = TimeUtils.formatMonthDay(question.createdAt),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!selectionMode) {
                Spacer(modifier = Modifier.width(2.dp))
                Box {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .clickable { menuExpanded = true },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "更多操作",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("查看 / 复习") },
                            onClick = {
                                menuExpanded = false
                                onClick()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("编辑") },
                            onClick = {
                                menuExpanded = false
                                onEdit()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(if (mastered) "标记为需要复习" else "标记为已掌握") },
                            onClick = {
                                menuExpanded = false
                                onToggleStatus()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("删除", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            },
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(7.dp))
        Text(
            text = question.content
                .ifBlank { question.title }
                .ifBlank { "未填写题目内容" },
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 15.sp,
                lineHeight = 25.sp,
            ),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )

        if (question.knowledgePoints.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            KnowledgeTagRow(
                tags = question.knowledgePoints,
                maxTags = 3,
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        ReviewStatusLabel(mastered = mastered)
    }
}

@Composable
private fun SelectionBottomBar(
    onSelectAll: () -> Unit,
    onMarkMastered: () -> Unit,
    onMarkNeedsReview: () -> Unit,
    onExportPaper: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant,
        ),
        shadowElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SelectionAction(
                icon = Icons.Filled.Done,
                label = "全选",
                onClick = onSelectAll,
                modifier = Modifier.weight(1f),
            )
            SelectionAction(
                icon = Icons.Filled.Check,
                label = "掌握",
                onClick = onMarkMastered,
                modifier = Modifier.weight(1f),
            )
            SelectionAction(
                icon = Icons.Filled.Refresh,
                label = "需复习",
                onClick = onMarkNeedsReview,
                modifier = Modifier.weight(1f),
            )
            SelectionAction(
                icon = Icons.Filled.Share,
                label = "组卷",
                onClick = onExportPaper,
                modifier = Modifier.weight(1f),
            )
            SelectionAction(
                icon = Icons.Filled.Delete,
                label = "删除",
                onClick = onDelete,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SelectionAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            maxLines = 1,
        )
    }
}

/** 更多筛选面板：错题本分类、状态、难度、知识点。 */
@Composable
private fun FilterDialog(
    filter: QuestionFilter,
    notebooks: List<NotebookEntity>,
    knowledgePoints: List<String>,
    onApply: (QuestionFilter) -> Unit,
    onDismiss: () -> Unit,
) {
    var local by remember { mutableStateOf(filter) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("更多筛选") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (notebooks.isNotEmpty()) {
                    Text("错题本分类", style = MaterialTheme.typography.labelLarge)
                    FilterChipRow(
                        options = notebooks,
                        selected = notebooks.firstOrNull { it.id == local.notebookId },
                        labelOf = { it.name },
                        onSelect = { notebook -> local = local.copy(notebookId = notebook?.id) },
                        allLabel = "全部分类",
                    )
                }

                Text("复习状态", style = MaterialTheme.typography.labelLarge)
                FilterChipRow(
                    options = listOf(ReviewStatus.NEEDS_REVIEW, ReviewStatus.MASTERED),
                    selected = local.status,
                    labelOf = { it.displayName },
                    onSelect = { status -> local = local.copy(status = status) },
                )

                Text("难度不低于", style = MaterialTheme.typography.labelLarge)
                FilterChipRow(
                    options = listOf(1, 2, 3, 4, 5),
                    selected = local.minDifficulty.takeIf { it > 0 },
                    labelOf = { "$it ★" },
                    onSelect = { value -> local = local.copy(minDifficulty = value ?: 0) },
                    allLabel = "不限",
                )

                Text("知识点", style = MaterialTheme.typography.labelLarge)
                if (knowledgePoints.isEmpty()) {
                    Text(
                        text = "还没有知识点标签",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    FilterChipRow(
                        options = knowledgePoints,
                        selected = local.knowledgePoint,
                        labelOf = { it },
                        onSelect = { point -> local = local.copy(knowledgePoint = point) },
                        allLabel = "全部知识点",
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = { onApply(local) }) { Text("应用") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

/** 排序方式选择 */
@Composable
private fun SortDialog(
    current: QuestionSort,
    onSelect: (QuestionSort) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("排序方式") },
        text = {
            Column {
                QuestionSort.entries.forEach { sort ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = sort == current,
                            onClick = { onSelect(sort) },
                        )
                        Text(text = sort.displayName)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } },
    )
}
