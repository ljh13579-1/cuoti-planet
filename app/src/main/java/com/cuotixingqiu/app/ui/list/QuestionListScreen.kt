package com.cuotixingqiu.app.ui.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cuotixingqiu.app.data.db.NotebookEntity
import com.cuotixingqiu.app.data.db.WrongQuestionEntity
import com.cuotixingqiu.app.domain.ReviewStatus
import com.cuotixingqiu.app.domain.Subject
import com.cuotixingqiu.app.ui.appViewModel
import com.cuotixingqiu.app.ui.components.ConfirmDialog
import com.cuotixingqiu.app.ui.components.EmptyState
import com.cuotixingqiu.app.ui.components.FilterChipRow
import com.cuotixingqiu.app.ui.components.QuestionCard
import com.cuotixingqiu.app.ui.components.ScreenScaffold
import com.cuotixingqiu.app.ui.paper.PaperRequest

/**
 * 错题列表页：搜索、按学科/知识点/状态/难度筛选、排序，
 * 长按进入多选后可以批量标记或批量删除。
 */
@Composable
fun QuestionListScreen(
    onOpenQuestion: (Long) -> Unit,
    onEditQuestion: (Long) -> Unit,
    onAddQuestion: () -> Unit,
    onExportPaper: () -> Unit,
) {
    val viewModel: QuestionListViewModel = appViewModel { container ->
        QuestionListViewModel(container.wrongQuestionRepository, container.notebookRepository)
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showFilterDialog by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<WrongQuestionEntity?>(null) }
    var showBatchDeleteDialog by remember { mutableStateOf(false) }

    val filtered = uiState.questions

    ScreenScaffold(
        title = if (uiState.selectionMode) "已选 ${uiState.selection.size} 道" else "错题列表",
        subtitle = if (uiState.selectionMode) {
            "可以批量标记或删除"
        } else {
            "共 ${uiState.totalCount} 道，当前显示 ${filtered.size} 道"
        },
        onBack = null,
        actions = {
            if (uiState.selectionMode) {
                IconButton(onClick = viewModel::clearSelection) {
                    Icon(Icons.Filled.Close, contentDescription = "取消多选")
                }
            } else {
                IconButton(onClick = onExportPaper) {
                    Icon(Icons.Filled.Share, contentDescription = "组卷导出")
                }
            }
        },
        bottomBar = {
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
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding),
        ) {
            // ---------- 搜索 ----------
            OutlinedTextField(
                value = uiState.filter.search,
                onValueChange = viewModel::setSearch,
                label = { Text("搜索题目、知识点、错因…") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (uiState.filter.search.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearch("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = "清空搜索")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )

            // ---------- 学科筛选 + 更多筛选 / 排序 ----------
            FilterChipRow(
                options = Subject.displayOrder,
                selected = Subject.displayOrder.firstOrNull { it.displayName == uiState.filter.subject },
                labelOf = { it.displayName },
                onSelect = { subject -> viewModel.setSubject(subject?.displayName) },
                leading = {
                    FilterChip(
                        selected = uiState.filter.hasExtraFilter,
                        onClick = { showFilterDialog = true },
                        label = { Text("更多筛选") },
                    )
                },
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = false,
                    onClick = { showSortDialog = true },
                    label = { Text("排序：${uiState.filter.sort.displayName}") },
                )
                if (uiState.filter.hasExtraFilter) {
                    TextButton(onClick = viewModel::resetExtraFilters) { Text("清除筛选") }
                }
            }
            HorizontalDivider()

            when {
                uiState.totalCount == 0 -> {
                    EmptyState(
                        icon = Icons.AutoMirrored.Filled.List,
                        title = "还没有错题",
                        message = "点击“添加错题”记录第一道题，App 会自动安排复习时间。",
                        actionLabel = "添加错题",
                        onAction = onAddQuestion,
                    )
                }

                filtered.isEmpty() -> {
                    EmptyState(
                        icon = Icons.Filled.Search,
                        title = "没有符合条件的错题",
                        message = "试着换一个搜索词，或者清除筛选条件。",
                        actionLabel = "清除筛选",
                        onAction = {
                            viewModel.resetExtraFilters()
                            viewModel.setSearch("")
                            viewModel.setSubject(null)
                        },
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(filtered, key = { it.id }) { question ->
                            QuestionListItem(
                                question = question,
                                notebookName = question.notebookId?.let { uiState.notebookNames[it] },
                                selectionMode = uiState.selectionMode,
                                selected = question.id in uiState.selection,
                                onClick = {
                                    if (uiState.selectionMode) {
                                        viewModel.toggleSelection(question.id)
                                    } else {
                                        onOpenQuestion(question.id)
                                    }
                                },
                                onLongClick = { viewModel.startSelection(question.id) },
                                onEdit = { onEditQuestion(question.id) },
                                onDelete = { deleteTarget = question },
                                onToggleStatus = {
                                    val target = if (question.status == ReviewStatus.MASTERED.name) {
                                        ReviewStatus.NEEDS_REVIEW
                                    } else {
                                        ReviewStatus.MASTERED
                                    }
                                    viewModel.setStatusForOne(question.id, target)
                                },
                            )
                        }
                        item { Spacer(modifier = Modifier.height(24.dp)) }
                    }
                }
            }
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
                viewModel.deleteSelected()
                showBatchDeleteDialog = false
            },
            onDismiss = { showBatchDeleteDialog = false },
        )
    }
}

/** 多选模式下底部的批量操作栏 */
@Composable
private fun SelectionBottomBar(
    onSelectAll: () -> Unit,
    onMarkMastered: () -> Unit,
    onMarkNeedsReview: () -> Unit,
    onExportPaper: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onSelectAll) { Text("全选") }
        TextButton(onClick = onMarkMastered) {
            Icon(Icons.Filled.Done, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.size(2.dp))
            Text("掌握")
        }
        TextButton(onClick = onMarkNeedsReview) {
            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.size(2.dp))
            Text("需复习")
        }
        TextButton(onClick = onExportPaper) {
            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.size(2.dp))
            Text("组卷")
        }
        TextButton(onClick = onDelete) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.size(2.dp))
            Text("删除", color = MaterialTheme.colorScheme.error)
        }
    }
}

/** 列表里的一项：卡片 + 右上角“更多”菜单 */
@Composable
private fun QuestionListItem(
    question: WrongQuestionEntity,
    notebookName: String?,
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

    QuestionCard(
        question = question,
        notebookName = notebookName,
        selectionMode = selectionMode,
        selected = selected,
        onClick = onClick,
        onLongClick = onLongClick,
        trailing = {
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "更多操作")
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
        },
    )
}

/** 更多筛选面板：错题本分类、状态、难度、知识点 */
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
