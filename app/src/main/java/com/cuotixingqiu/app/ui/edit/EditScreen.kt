package com.cuotixingqiu.app.ui.edit

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.cuotixingqiu.app.data.db.NotebookEntity
import com.cuotixingqiu.app.domain.Subject
import com.cuotixingqiu.app.ui.appViewModel
import com.cuotixingqiu.app.ui.components.CuotiTextField
import com.cuotixingqiu.app.ui.components.CuotiVisual
import com.cuotixingqiu.app.ui.components.DifficultyStars
import com.cuotixingqiu.app.ui.components.FilterChipRow
import com.cuotixingqiu.app.ui.components.ImageViewerDialog
import com.cuotixingqiu.app.ui.components.GlassActionButton
import com.cuotixingqiu.app.ui.components.ScreenScaffold
import com.cuotixingqiu.app.ui.components.SectionHeader
import com.cuotixingqiu.app.ui.components.withoutTopInset
import androidx.compose.ui.graphics.rememberGraphicsLayer
import com.cuotixingqiu.app.ui.components.glassButtonBackdropRecorder
import com.cuotixingqiu.app.ui.components.isGlassBackdropSupported
import com.cuotixingqiu.app.ui.components.rememberDockClearance
import com.cuotixingqiu.app.ui.rememberAppContainer
import com.cuotixingqiu.app.util.MediaUtils
import java.io.File

/** 一次最多选几张图片 */
private const val MAX_IMAGES = 9

/**
 * 添加 / 编辑错题页面。
 *
 * 支持：
 *  - 从相册选择（可以一次选多张，使用系统 Photo Picker，不需要任何权限）
 *  - 直接拍照（调用系统相机，也不需要相机权限）；拍照后以“临时文件里有没有
 *    照片”为准判定成败（兼容卓易通等会丢失结果标志的环境），没拍到会明确提示
 *  - 图片会自动压缩并保存到应用私有目录，删掉相册原图也不影响错题
 *
 * 表单约定：每个区域用小标题分开；输入框是浅灰填充的圆角样式；
 * 底部“保存”按钮始终可见，键盘弹出时也会自动上移，不会被挡住。
 */
@Composable
fun EditScreen(
    questionId: Long?,
    onBack: () -> Unit,
    onSaved: (Long) -> Unit,
) {
    val container = rememberAppContainer()
    val viewModel: EditViewModel = appViewModel { appContainer ->
        EditViewModel(
            repository = appContainer.wrongQuestionRepository,
            notebookRepository = appContainer.notebookRepository,
            imageStore = appContainer.imageStore,
            questionId = questionId,
        )
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showImageSourceDialog by remember { mutableStateOf(false) }
    var viewerFile by remember { mutableStateOf<File?>(null) }

    // 相册：系统 Photo Picker（老系统上没有相册选择器时会自动退回文件选择器）
    val pickImages = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(MAX_IMAGES),
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) viewModel.addImages(uris)
    }

    // 拍照：把照片写进应用私有的临时文件（通过 FileProvider 临时授权给相机）。
    // 成败不看“结果标志”，而是回到前台/收到结果时检查临时文件里有没有照片——
    // 鸿蒙卓易通等环境会丢失这个标志，所以两个时机都查一遍（重复调用是安全的）。
    val takePicture = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) {
        viewModel.checkPendingCapture()
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.checkPendingCapture()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    // 悬浮玻璃按钮不再占用一整条底栏：只保留按钮本身，内容从它身后滚过。
    val dockClearance = rememberDockClearance()
    val buttonBottomGap = dockClearance + 12.dp
    // 记录按钮身后那一块内容，让按钮能做真实背景采样（Android 12+）；低版本自动为空
    val buttonBackdropLayer = if (isGlassBackdropSupported()) rememberGraphicsLayer() else null
    val background = MaterialTheme.colorScheme.background

    ScreenScaffold(
        title = if (uiState.isNew) "添加错题" else "编辑错题",
        subtitle = "除学科外，其它内容都可以先不填",
        onBack = onBack,
        snackbarHostState = snackbarHostState,
        maxContentWidth = 680.dp,
        // 正文从顶栏下方穿过：Android 12+ 顶栏对身后的滚动内容做真实模糊
        contentBehindTopBar = true,
        // 键盘弹出时整体上移，保证输入区域和保存按钮始终可用
        modifier = Modifier.imePadding(),
    ) { padding ->
        if (uiState.loading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@ScreenScaffold
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                // 只去掉顶部一份：正文从屏幕顶端开始画，顶栏才能录到身后内容
                .padding(padding.withoutTopInset()),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (buttonBackdropLayer != null) {
                            Modifier.glassButtonBackdropRecorder(
                                layer = buttonBackdropLayer,
                                background = background,
                                horizontalInset = CuotiVisual.PageHorizontal,
                                bottomInset = buttonBottomGap,
                                buttonHeight = CuotiVisual.ButtonHeight,
                            )
                        } else {
                            Modifier
                        },
                    )
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = CuotiVisual.PageHorizontal),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // 顶部避让放进滚动内容里，滚动时才从顶栏后面穿过
                Spacer(modifier = Modifier.height(padding.calculateTopPadding() + 8.dp))

            // ---------- 学科（必选） ----------
            SectionHeader(title = "学科（必选）")
            FilterChipRow(
                options = Subject.displayOrder,
                selected = Subject.displayOrder.firstOrNull { it.displayName == uiState.subject },
                labelOf = { it.displayName },
                onSelect = { subject ->
                    viewModel.setSubject(subject?.displayName.orEmpty())
                },
                allLabel = "未选择",
            )

            // ---------- 错题本分类 ----------
            if (uiState.notebooks.isNotEmpty()) {
                SectionHeader(title = "错题本分类")
                NotebookDropdown(
                    notebooks = uiState.notebooks,
                    selectedId = uiState.notebookId,
                    onSelect = viewModel::setNotebook,
                )
            }

            // ---------- 难度 ----------
            SectionHeader(title = "难度（可选）")
            Row(verticalAlignment = Alignment.CenterVertically) {
                DifficultyStars(
                    difficulty = uiState.difficulty,
                    onSelect = viewModel::setDifficulty,
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = if (uiState.difficulty == 0) "点星星标记难度" else "${uiState.difficulty} 星",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // ---------- 题目 ----------
            SectionHeader(title = "题目")
            CuotiTextField(
                value = uiState.title,
                onValueChange = viewModel::setTitle,
                label = "标题（可选，例如“导数求单调区间”）",
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            CuotiTextField(
                value = uiState.content,
                onValueChange = viewModel::setContent,
                label = "题目内容（可以直接写题干或做错的步骤）",
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )

            // ---------- 知识点 ----------
            SectionHeader(title = "知识点（可选）")
            Row(verticalAlignment = Alignment.CenterVertically) {
                CuotiTextField(
                    value = uiState.knowledgePointDraft,
                    onValueChange = viewModel::setKnowledgePointDraft,
                    label = "例如：导数",
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = viewModel::addKnowledgePointsFromDraft) {
                    Icon(Icons.Filled.Add, contentDescription = "添加知识点")
                }
            }
            if (uiState.knowledgePoints.isNotEmpty()) {
                KnowledgePointChips(
                    points = uiState.knowledgePoints,
                    onRemove = viewModel::removeKnowledgePoint,
                )
            }

            // ---------- 图片 ----------
            SectionHeader(title = "题目图片（可选，最多 $MAX_IMAGES 张）")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(uiState.imageNames) { name ->
                    val file = container.imageStore.fileOrNull(name)
                    Box(modifier = Modifier.size(110.dp)) {
                        Card(
                            onClick = { file?.let { viewerFile = it } },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            ),
                            modifier = Modifier.size(110.dp),
                        ) {
                            if (file != null) {
                                AsyncImage(
                                    model = file,
                                    contentDescription = "错题图片",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text("图片缺失", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                        // 右上角“移除”：半透明圆底，点中区域够大，浅色/深色下都看得清
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.45f))
                                .clickable { viewModel.removeImage(name) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "移除这张图片",
                                tint = Color.White,
                                modifier = Modifier.size(17.dp),
                            )
                        }
                    }
                }
                item {
                    Card(
                        onClick = { showImageSourceDialog = true },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.size(110.dp),
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            if (uiState.importingImages) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            } else {
                                Icon(
                                    Icons.Filled.Add,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "加图片",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
            Text(
                text = "图片会自动压缩后存进 App 自己的目录，删掉相册里的原图也不影响错题。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // ---------- 错因 / 解法 / 总结 ----------
            SectionHeader(title = "错因分析（可选）")
            CuotiTextField(
                value = uiState.reason,
                onValueChange = viewModel::setReason,
                label = "例如：看错条件、公式记错、思路没想全",
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )

            SectionHeader(title = "解题思路（可选）")
            CuotiTextField(
                value = uiState.solution,
                onValueChange = viewModel::setSolution,
                label = "正确的做法是什么？关键步骤写清楚",
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )

            SectionHeader(title = "我的总结（可选）")
            CuotiTextField(
                value = uiState.summary,
                onValueChange = viewModel::setSummary,
                label = "下次遇到同类题要注意什么？",
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )

            // 给悬浮保存按钮 + Dock 留出空间，最后一条内容能完整滚出来
            Spacer(modifier = Modifier.height(buttonBottomGap + CuotiVisual.ButtonHeight + 18.dp))
            }

            // 悬浮玻璃保存按钮：不占底栏、不成条，胶囊形状与 Dock 一致
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(
                        start = CuotiVisual.PageHorizontal,
                        end = CuotiVisual.PageHorizontal,
                        bottom = buttonBottomGap,
                    ),
            ) {
                GlassActionButton(
                    text = if (uiState.isNew) "保存错题" else "保存修改",
                    onClick = { viewModel.save(onSaved) },
                    enabled = !uiState.loading && !uiState.importingImages,
                    loading = uiState.saving,
                    backgroundLayer = buttonBackdropLayer,
                    // 图层本身就是与按钮等大的一块切片，不需要再做屏幕位置换算
                    backdropPosition = Offset.Zero,
                )
            }
        }
    }

    if (showImageSourceDialog) {
        AlertDialog(
            onDismissRequest = { showImageSourceDialog = false },
            title = { Text("添加图片") },
            text = { Text("可以从相册选择，也可以现在拍一张。") },
            confirmButton = {
                TextButton(onClick = {
                    showImageSourceDialog = false
                    pickImages.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                }) { Text("从相册选择") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showImageSourceDialog = false
                    val file = runCatching { MediaUtils.createCameraFile(context) }.getOrNull()
                    val uri = file?.let {
                        runCatching { MediaUtils.fileProviderUri(context, it) }.getOrNull()
                    }
                    if (file == null || uri == null) {
                        file?.delete()
                        viewModel.showMessage("无法调用系统相机，请改用「从相册选择」")
                    } else {
                        viewModel.onCameraLaunched(file.absolutePath)
                        takePicture.launch(uri)
                    }
                }) { Text("拍照") }
            },
        )
    }

    viewerFile?.let { file ->
        ImageViewerDialog(file = file, onDismiss = { viewerFile = null })
    }
}

/** 知识点标签（点一下删除），与详情页的浅蓝标签是同一套观感 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun KnowledgePointChips(
    points: List<String>,
    onRemove: (String) -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        points.forEach { point ->
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable { onRemove(point) }
                    .padding(start = 10.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = point,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(modifier = Modifier.size(4.dp))
                Icon(
                    imageVector = Icons.Filled.Clear,
                    contentDescription = "删除知识点 $point",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

/** 错题本分类下拉框（选项不多，用下拉最省空间） */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotebookDropdown(
    notebooks: List<NotebookEntity>,
    selectedId: Long?,
    onSelect: (Long?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = notebooks.firstOrNull { it.id == selectedId }?.name ?: "未分类"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        CuotiTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            label = "分类",
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = { Text("未分类") },
                onClick = {
                    onSelect(null)
                    expanded = false
                },
            )
            notebooks.forEach { notebook ->
                DropdownMenuItem(
                    text = { Text(notebook.name) },
                    onClick = {
                        onSelect(notebook.id)
                        expanded = false
                    },
                )
            }
        }
    }
}
