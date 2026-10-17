package com.cuotixingqiu.app.ui.profile

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cuotixingqiu.app.BuildConfig
import com.cuotixingqiu.app.data.settings.ThemeMode
import com.cuotixingqiu.app.ui.appViewModel
import com.cuotixingqiu.app.ui.components.ConfirmDialog
import com.cuotixingqiu.app.ui.components.FilterChipRow
import com.cuotixingqiu.app.ui.components.ScreenScaffold
import com.cuotixingqiu.app.ui.components.SectionTitle

/**
 * “我的”页面：外观设置、数据管理（导入/导出/清空）、应用信息。
 * 所有开关和数据操作都是真实的，点了立刻生效。
 */
@Composable
fun ProfileScreen(
    onOpenStats: () -> Unit,
    onOpenNotebooks: () -> Unit,
    onOpenPaper: () -> Unit,
) {
    val viewModel: ProfileViewModel = appViewModel { container ->
        ProfileViewModel(
            context = container.applicationContext,
            settingsRepository = container.settingsRepository,
            repository = container.wrongQuestionRepository,
            notebookRepository = container.notebookRepository,
            backupManager = container.backupManager,
            imageStore = container.imageStore,
        )
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var showExportDialog by remember { mutableStateOf(false) }
    var exportIncludeImages by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    // 导出 / 导入用的系统文件选择器（都不需要存储权限）
    val createDocument = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) viewModel.exportJson(uri, exportIncludeImages)
    }

    val openDocument = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) viewModel.importJson(uri)
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    ScreenScaffold(
        title = "我的",
        snackbarHostState = snackbarHostState,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ---------------- 外观 ----------------
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SectionTitle(text = "外观")
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("主题模式", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(6.dp))
                        FilterChipRow(
                            options = ThemeMode.entries,
                            selected = uiState.themeMode,
                            labelOf = { it.displayName },
                            onSelect = { mode -> mode?.let(viewModel::setThemeMode) },
                            allLabel = "跟随系统",
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("跟随壁纸取色", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                        "根据手机壁纸自动生成配色"
                                    } else {
                                        "需要 Android 12 及以上系统"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(
                                checked = uiState.dynamicColor,
                                onCheckedChange = viewModel::setDynamicColor,
                                enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                            )
                        }
                    }
                }
            }

            // ---------------- 数据管理 ----------------
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        SectionTitle(
                            text = "数据管理",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        )
                        SettingRow(
                            icon = Icons.Filled.Share,
                            title = "导出备份（JSON）",
                            subtitle = "把错题保存成一个文件，可以随时导入回来",
                            onClick = { showExportDialog = true },
                        )
                        SettingRow(
                            icon = Icons.Filled.Refresh,
                            title = "导入备份（JSON）",
                            subtitle = "合并模式：不会覆盖手机上已有的错题",
                            onClick = {
                                openDocument.launch(arrayOf("application/json", "text/plain", "*/*"))
                            },
                        )
                        SettingRow(
                            icon = Icons.AutoMirrored.Filled.List,
                            title = "组卷导出（PDF）",
                            subtitle = "按范围生成一份可以打印的试卷",
                            onClick = onOpenPaper,
                        )
                        SettingRow(
                            icon = Icons.Filled.Star,
                            title = "统计图表",
                            subtitle = "学科分布、复习趋势、难度分布",
                            onClick = onOpenStats,
                        )
                        SettingRow(
                            icon = Icons.AutoMirrored.Filled.List,
                            title = "错题本分类",
                            subtitle = "按教材或专题给错题分组",
                            onClick = onOpenNotebooks,
                        )
                        HorizontalDivider()
                        SettingRow(
                            icon = Icons.Filled.Delete,
                            title = "清空所有数据",
                            subtitle = "删除全部错题、分类和图片（不可恢复）",
                            titleColor = MaterialTheme.colorScheme.error,
                            onClick = { showClearDialog = true },
                        )
                    }
                }
            }

            // ---------------- 应用信息 ----------------
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SectionTitle(text = "应用信息")
                        Spacer(modifier = Modifier.height(10.dp))
                        InfoLine("应用名称", "错题星球")
                        InfoLine("版本", "${BuildConfig.VERSION_NAME}（${BuildConfig.VERSION_CODE}）")
                        InfoLine("错题数量", "${uiState.totalCount} 道")
                        InfoLine("图片数量", "${uiState.imageCount} 张")
                        InfoLine("分类数量", "${uiState.notebookCount} 个")
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "本应用完全离线：没有申请网络权限，也不会收集或上传任何数据。" +
                                "所有内容只保存在这台手机上，卸载应用会一起删除，" +
                                "所以建议定期用“导出备份”保存一份 JSON 文件。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    if (uiState.busy) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("正在处理，请稍候…", style = MaterialTheme.typography.bodySmall)
                    } else {
                        Icon(
                            Icons.Filled.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(
                            text = "让每一道错题都有价值",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }

    // ---------------- 导出选项 ----------------
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("导出备份") },
            text = {
                Column {
                    Text("是否把图片一起导出？")
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("包含图片", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = "文件会大很多，但导入后图片不会丢",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = exportIncludeImages,
                            onCheckedChange = { exportIncludeImages = it },
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showExportDialog = false
                    createDocument.launch(viewModel.suggestedBackupFileName())
                }) { Text("选择保存位置") }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) { Text("取消") }
            },
        )
    }

    // ---------------- 清空数据（两次确认） ----------------
    if (showClearDialog) {
        ConfirmDialog(
            title = "清空所有数据？",
            message = "将会删除 ${uiState.totalCount} 道错题、${uiState.notebookCount} 个分类" +
                "和全部图片。这个操作无法撤销。",
            confirmLabel = "继续",
            destructive = true,
            onConfirm = {
                showClearDialog = false
                showClearConfirmDialog = true
            },
            onDismiss = { showClearDialog = false },
        )
    }

    if (showClearConfirmDialog) {
        ConfirmDialog(
            title = "真的要清空吗？",
            message = "最后一次确认：删除后数据无法恢复。" +
                "如果这些错题还有用，请先导出备份。",
            confirmLabel = "确认清空",
            destructive = true,
            onConfirm = {
                showClearConfirmDialog = false
                viewModel.clearAllData()
            },
            onDismiss = { showClearConfirmDialog = false },
        )
    }

    // ---------------- 导入结果 ----------------
    val resultTitle = uiState.resultTitle
    val resultMessage = uiState.resultMessage
    if (resultTitle != null && resultMessage != null) {
        AlertDialog(
            onDismissRequest = viewModel::clearResult,
            title = { Text(resultTitle) },
            text = { Text(resultMessage) },
            confirmButton = {
                TextButton(onClick = viewModel::clearResult) { Text("知道了") }
            },
        )
    }
}

/** 一行设置项 */
@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    titleColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp),
        )
        Spacer(modifier = Modifier.size(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, color = titleColor)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.4f),
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(0.6f))
    }
}
