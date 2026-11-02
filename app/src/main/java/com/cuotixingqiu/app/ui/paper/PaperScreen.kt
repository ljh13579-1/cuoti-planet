package com.cuotixingqiu.app.ui.paper

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cuotixingqiu.app.ui.appViewModel
import com.cuotixingqiu.app.ui.components.CuotiCard
import com.cuotixingqiu.app.ui.components.CuotiTextField
import com.cuotixingqiu.app.ui.components.CuotiVisual
import com.cuotixingqiu.app.ui.components.FilterChipRow
import com.cuotixingqiu.app.ui.components.PrimaryActionButton
import com.cuotixingqiu.app.ui.components.ScreenScaffold
import com.cuotixingqiu.app.ui.components.SectionHeader
import com.cuotixingqiu.app.ui.components.ThinDivider
import com.cuotixingqiu.app.ui.components.rememberDockClearance
import com.cuotixingqiu.app.ui.components.withoutTopInset
import com.cuotixingqiu.app.util.MediaUtils

/**
 * 组卷导出页：把错题排成一份 PDF 试卷，可以直接分享给同学、家长或打印出来做。
 *
 * 生成的过程完全在手机上完成，不联网、不依赖任何服务。
 * v1.3.8：视觉与其它页面统一（暖灰底 + 白卡 + 品牌蓝紫），逻辑未改。
 */
@Composable
fun PaperScreen(
    onBack: () -> Unit,
    onOpenQuestion: (Long) -> Unit,
) {
    val viewModel: PaperViewModel = appViewModel { container ->
        PaperViewModel(
            repository = container.wrongQuestionRepository,
            notebookRepository = container.notebookRepository,
            paperExporter = container.paperExporter,
            selectedIds = PaperRequest.consume(),
        )
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    ScreenScaffold(
        title = "组卷导出",
        subtitle = "在手机上生成 PDF 试卷，可分享或打印",
        onBack = onBack,
        snackbarHostState = snackbarHostState,
        maxContentWidth = 680.dp,
        contentBehindTopBar = true,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding.withoutTopInset()),
            contentPadding = PaddingValues(
                start = CuotiVisual.PageHorizontal,
                end = CuotiVisual.PageHorizontal,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = rememberDockClearance() + 26.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { SectionHeader(title = "选择范围") }
            item {
                CuotiCard {
                    FilterChipRow(
                        options = PaperScope.entries,
                        selected = uiState.settings.scope,
                        labelOf = { it.displayName },
                        onSelect = { scope -> scope?.let(viewModel::setScope) },
                        allLabel = "全部错题",
                    )
                    if (uiState.settings.scope == PaperScope.SELECTED) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (uiState.selectedCount > 0) {
                                "已从列表页选中 ${uiState.selectedCount} 道题"
                            } else {
                                "还没有选中题目，请回到列表页长按选择，或者换一个范围"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (uiState.settings.scope == PaperScope.SUBJECT && uiState.subjects.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        ThinDivider()
                        Spacer(modifier = Modifier.height(10.dp))
                        FilterChipRow(
                            options = uiState.subjects,
                            selected = uiState.settings.subject,
                            labelOf = { it },
                            onSelect = viewModel::setSubject,
                            allLabel = "全部学科",
                        )
                    }
                    if (uiState.settings.scope == PaperScope.NOTEBOOK && uiState.notebooks.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        ThinDivider()
                        Spacer(modifier = Modifier.height(10.dp))
                        FilterChipRow(
                            options = uiState.notebooks,
                            selected = uiState.notebooks.firstOrNull {
                                it.id == uiState.settings.notebookId
                            },
                            labelOf = { it.name },
                            onSelect = { notebook -> viewModel.setNotebook(notebook?.id) },
                            allLabel = "全部分类",
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(6.dp)) }
            item { SectionHeader(title = "试卷设置") }
            item {
                CuotiCard {
                    CuotiTextField(
                        value = uiState.settings.title,
                        onValueChange = viewModel::setTitle,
                        label = "试卷标题",
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    SettingSwitchRow(
                        title = "包含题目图片",
                        message = "把题目截图一起排进试卷",
                        checked = uiState.settings.includeImages,
                        onCheckedChange = viewModel::setIncludeImages,
                    )
                    SettingSwitchRow(
                        title = "附答案与解析",
                        message = "最后几页放错因、解题思路和总结",
                        checked = uiState.settings.includeAnswers,
                        onCheckedChange = viewModel::setIncludeAnswers,
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "将导出",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "${uiState.candidateCount} 道题",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            item {
                PrimaryActionButton(
                    text = if (uiState.generating) "正在生成…" else "生成 PDF",
                    onClick = viewModel::generate,
                    enabled = uiState.candidateCount > 0,
                    loading = uiState.generating,
                )
            }

            item {
                Text(
                    text = "生成的 PDF 会先保存在应用私有目录，可以在弹出的窗口里直接分享或打开；" +
                        "关闭窗口时会自动删除，避免占用手机空间。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    val file = uiState.generatedFile
    if (file != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissResult(deleteFile = true) },
            title = { Text("PDF 已生成") },
            text = {
                Column {
                    Text("题目数量：${uiState.generatedQuestionCount} 道")
                    Text("文件名：${file.name}")
                    Text("文件大小：${MediaUtils.formatFileSize(file.length())}")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "可以点“分享”发到微信/QQ，或者点“打开”用系统阅读器查看并打印。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    MediaUtils.shareFile(context, file, "application/pdf", "分享试卷")
                    viewModel.dismissResult(deleteFile = false)
                }) { Text("分享") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        val opened = MediaUtils.openFile(context, file, "application/pdf")
                        if (opened) viewModel.dismissResult(deleteFile = false)
                    }) { Text("打开") }
                    TextButton(onClick = { viewModel.dismissResult(deleteFile = true) }) {
                        Text("关闭")
                    }
                }
            },
        )
    }
}

/** 设置行：标题 + 说明 + 右侧开关 */
@Composable
private fun SettingSwitchRow(
    title: String,
    message: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
