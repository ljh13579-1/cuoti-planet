package com.cuotixingqiu.app.ui.profile

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cuotixingqiu.app.BuildConfig
import com.cuotixingqiu.app.data.settings.ThemeMode
import com.cuotixingqiu.app.ui.appViewModel
import com.cuotixingqiu.app.ui.components.ConfirmDialog
import com.cuotixingqiu.app.ui.components.CuotiCard
import com.cuotixingqiu.app.ui.components.CuotiVisual
import com.cuotixingqiu.app.ui.components.FilterChipRow
import com.cuotixingqiu.app.ui.components.SectionHeader
import com.cuotixingqiu.app.ui.components.WeekReviewChart
import com.cuotixingqiu.app.ui.components.pageSidePadding
import com.cuotixingqiu.app.ui.components.rememberDockClearance

/**
 * “我的”页面：
 * 真实身份占位（项目没有姓名/头像字段）→ 累计/掌握/连续复习 → 近 7 天学习记录 → 管理入口。
 *
 * 入口对应现有能力：外观与主题、错题本分类、JSON 备份/导入、PDF 组卷、清空数据。
 * 不展示示例姓名、示例提醒时间或示例回收站。
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
    var showAppearanceDialog by remember { mutableStateOf(false) }
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

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        val side = pageSidePadding(maxWidth)
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(
                start = side,
                end = side,
                top = 16.dp,
                bottom = rememberDockClearance() + 26.dp,
            ),
        ) {
            item {
                IdentityHeader(uiState = uiState)
            }

            item {
                Spacer(modifier = Modifier.height(22.dp))
                ProfileStatsCard(uiState = uiState)
            }

            item {
                Spacer(modifier = Modifier.height(CuotiVisual.SectionGap))
                SectionHeader(title = "学习记录")
                Spacer(modifier = Modifier.height(12.dp))
                CuotiCard(
                    modifier = Modifier
                        .clip(RoundedCornerShape(CuotiVisual.CardCorner))
                        .clickable(
                            onClickLabel = "查看学习统计",
                            onClick = onOpenStats,
                        ),
                    contentPadding = PaddingValues(18.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "近 7 天共复习 ${uiState.last7Total} 题",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "${uiState.subjectCount} 个学科",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    if (uiState.last7Days.isEmpty()) {
                        Text(
                            text = "还没有复习记录",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 18.dp),
                        )
                    } else {
                        WeekReviewChart(days = uiState.last7Days)
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(CuotiVisual.SectionGap))
                SectionHeader(title = "管理")
                Spacer(modifier = Modifier.height(12.dp))
                CuotiCard(contentPadding = PaddingValues(vertical = 2.dp)) {
                    ManagementRow(
                        icon = Icons.Filled.Star,
                        title = "外观与主题",
                        value = themeValue(uiState),
                        onClick = { showAppearanceDialog = true },
                    )
                    ManagementDivider()
                    ManagementRow(
                        icon = Icons.AutoMirrored.Filled.List,
                        title = "错题本分类",
                        value = if (uiState.notebookCount == 0) "未分类" else "${uiState.notebookCount} 个分类",
                        onClick = onOpenNotebooks,
                    )
                    ManagementDivider()
                    ManagementRow(
                        icon = Icons.Filled.Refresh,
                        title = "数据备份",
                        value = "JSON",
                        onClick = { showExportDialog = true },
                    )
                    ManagementDivider()
                    ManagementRow(
                        icon = Icons.Filled.Share,
                        title = "导出错题本",
                        value = "PDF",
                        onClick = onOpenPaper,
                    )
                    ManagementDivider()
                    ManagementRow(
                        icon = Icons.Filled.Delete,
                        title = "清空数据",
                        value = "不可恢复",
                        onClick = { showClearDialog = true },
                        destructive = true,
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(22.dp))
                if (uiState.busy) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(15.dp),
                            strokeWidth = 2.dp,
                        )
                        Spacer(modifier = Modifier.width(7.dp))
                        Text(
                            text = "正在处理，请稍候…",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
                Text(
                    text = "错题星球 ${BuildConfig.VERSION_NAME} · 所有数据仅保存在本机",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "完全离线，不收集也不上传任何内容",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = rememberDockClearance() + 8.dp,
                ),
        )
    }

    if (showAppearanceDialog) {
        AlertDialog(
            onDismissRequest = { showAppearanceDialog = false },
            title = { Text("外观与主题") },
            text = {
                Column {
                    FilterChipRow(
                        options = ThemeMode.entries,
                        selected = uiState.themeMode,
                        labelOf = { it.displayName },
                        onSelect = { mode -> mode?.let(viewModel::setThemeMode) },
                        allLabel = null,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "跟随壁纸取色",
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    if (uiState.dynamicColor) {
                                        "已开启：配色跟随手机壁纸"
                                    } else {
                                        "关闭中：使用默认蓝紫色"
                                    }
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
            },
            confirmButton = {
                TextButton(onClick = { showAppearanceDialog = false }) { Text("完成") }
            },
        )
    }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("数据备份") },
            text = {
                Column {
                    Text(
                        text = "把错题保存成一个 JSON 文件，换手机或重装应用后可以再导入回来。",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(modifier = Modifier.height(14.dp))
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
                    Spacer(modifier = Modifier.height(4.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(2.dp))
                    TextButton(
                        onClick = {
                            showExportDialog = false
                            openDocument.launch(arrayOf("application/json", "text/plain", "*/*"))
                        },
                        contentPadding = PaddingValues(horizontal = 0.dp),
                    ) {
                        Text("从文件导入备份（合并，不覆盖已有错题）")
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

    if (showClearDialog) {
        ConfirmDialog(
            title = "清空所有数据？",
            message = "将会删除 ${uiState.totalCount} 道错题、${uiState.notebookCount} 个分类和全部图片。这个操作无法撤销。",
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

@Composable
private fun IdentityHeader(uiState: ProfileUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "星",
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 25.sp),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "同学",
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 23.sp),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = if (uiState.totalCount == 0) {
                    "今天开始记录第一道错题"
                } else {
                    "已加入 ${uiState.joinedDays} 天 · 累计 ${uiState.totalCount} 道错题"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ProfileStatsCard(uiState: ProfileUiState) {
    CuotiCard(contentPadding = PaddingValues(vertical = 17.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProfileStat(
                value = uiState.totalCount.toString(),
                label = "错题累计",
                modifier = Modifier.weight(1f),
            )
            ProfileStatDivider()
            ProfileStat(
                value = uiState.masteredCount.toString(),
                label = "已掌握",
                modifier = Modifier.weight(1f),
            )
            ProfileStatDivider()
            ProfileStat(
                value = "${uiState.studyStreak}天",
                label = "连续复习",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ProfileStat(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall.copy(fontSize = 24.sp),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

@Composable
private fun ProfileStatDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(32.dp)
            .background(MaterialTheme.colorScheme.outlineVariant),
    )
}

@Composable
private fun ManagementRow(
    icon: ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit,
    destructive: Boolean = false,
) {
    val titleColor = if (destructive) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val iconColor = if (destructive) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
            color = titleColor,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun ManagementDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 48.dp),
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

private fun themeValue(state: ProfileUiState): String =
    if (state.dynamicColor) "壁纸取色" else state.themeMode.displayName
