package com.cuotixingqiu.app.ui.paper

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.cuotixingqiu.app.ui.components.FilterChipRow
import com.cuotixingqiu.app.ui.components.ScreenScaffold
import com.cuotixingqiu.app.ui.components.SectionTitle
import com.cuotixingqiu.app.util.MediaUtils

/**
 * 组卷导出页：把错题排成一份 PDF 试卷，可以直接分享给同学、家长或打印出来做。
 *
 * 生成的过程完全在手机上完成，不联网、不依赖任何服务。
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
        title = "组卷导出（PDF）",
        subtitle = "在手机上生成试卷，可分享或打印",
        onBack = onBack,
        snackbarHostState = snackbarHostState,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SectionTitle(text = "选择范围")
                        Spacer(modifier = Modifier.height(8.dp))
                        FilterChipRow(
                            options = PaperScope.entries,
                            selected = uiState.settings.scope,
                            labelOf = { it.displayName },
                            onSelect = { scope -> scope?.let(viewModel::setScope) },
                            allLabel = "全部错题",
                        )
                        if (uiState.settings.scope == PaperScope.SELECTED) {
                            Spacer(modifier = Modifier.height(8.dp))
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
                            Spacer(modifier = Modifier.height(8.dp))
                            FilterChipRow(
                                options = uiState.subjects,
                                selected = uiState.settings.subject,
                                labelOf = { it },
                                onSelect = viewModel::setSubject,
                                allLabel = "全部学科",
                            )
                        }
                        if (uiState.settings.scope == PaperScope.NOTEBOOK && uiState.notebooks.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
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
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SectionTitle(text = "试卷设置")
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = uiState.settings.title,
                            onValueChange = viewModel::setTitle,
                            label = { Text("试卷标题") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("包含题目图片", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = "把题目截图一起排进试卷",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(
                                checked = uiState.settings.includeImages,
                                onCheckedChange = viewModel::setIncludeImages,
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("附答案与解析", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = "最后几页放错因、解题思路和总结",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(
                                checked = uiState.settings.includeAnswers,
                                onCheckedChange = viewModel::setIncludeAnswers,
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "将导出 ${uiState.candidateCount} 道题",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            item {
                Button(
                    onClick = viewModel::generate,
                    enabled = !uiState.generating && uiState.candidateCount > 0,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (uiState.generating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                    }
                    Text(if (uiState.generating) "正在生成…" else "生成 PDF")
                }
            }

            item {
                Text(
                    text = "生成的 PDF 会先保存在应用私有目录，可以在弹出的窗口里直接分享或打开；" +
                        "关闭窗口时会自动删除，避免占用手机空间。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
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
