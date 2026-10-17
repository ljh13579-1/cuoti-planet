package com.cuotixingqiu.app.ui.edit

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.cuotixingqiu.app.data.db.NotebookEntity
import com.cuotixingqiu.app.domain.Subject
import com.cuotixingqiu.app.ui.appViewModel
import com.cuotixingqiu.app.ui.components.DifficultyStars
import com.cuotixingqiu.app.ui.components.FilterChipRow
import com.cuotixingqiu.app.ui.components.ImageViewerDialog
import com.cuotixingqiu.app.ui.components.ScreenScaffold
import com.cuotixingqiu.app.ui.components.SectionTitle
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
 *  - 直接拍照（调用系统相机，也不需要相机权限）
 *  - 图片会自动压缩并保存到应用私有目录，删掉相册原图也不影响错题
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
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    var viewerFile by remember { mutableStateOf<File?>(null) }

    // 相册：系统 Photo Picker（老系统上没有相册选择器时会自动退回文件选择器）
    val pickImages = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(MAX_IMAGES),
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) viewModel.addImages(uris)
    }

    // 拍照：把照片写进应用私有的临时文件（通过 FileProvider 临时授权给相机）
    val takePicture = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { success ->
        val uri = cameraUri
        if (success && uri != null) {
            viewModel.addImages(listOf(uri))
        }
        cameraUri = null
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    ScreenScaffold(
        title = if (uiState.isNew) "添加错题" else "编辑错题",
        subtitle = "除学科外，其它内容都可以先不填",
        onBack = onBack,
        snackbarHostState = snackbarHostState,
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                Button(
                    onClick = { viewModel.save(onSaved) },
                    enabled = !uiState.saving && !uiState.loading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (uiState.saving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                    }
                    Text(if (uiState.isNew) "保存错题" else "保存修改")
                }
            }
        },
    ) { padding ->
        if (uiState.loading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@ScreenScaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // ---------- 学科（必选） ----------
            SectionTitle(text = "学科（必选）")
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
                SectionTitle(text = "错题本分类")
                NotebookDropdown(
                    notebooks = uiState.notebooks,
                    selectedId = uiState.notebookId,
                    onSelect = viewModel::setNotebook,
                )
            }

            // ---------- 难度 ----------
            SectionTitle(text = "难度（可选）")
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
            SectionTitle(text = "题目")
            OutlinedTextField(
                value = uiState.title,
                onValueChange = viewModel::setTitle,
                label = { Text("标题（可选，例如“导数求单调区间”）") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = uiState.content,
                onValueChange = viewModel::setContent,
                label = { Text("题目内容（可以直接写题干或做错的步骤）") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )

            // ---------- 知识点 ----------
            SectionTitle(text = "知识点（可选）")
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = uiState.knowledgePointDraft,
                    onValueChange = viewModel::setKnowledgePointDraft,
                    label = { Text("例如：导数") },
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
            SectionTitle(text = "题目图片（可选，最多 $MAX_IMAGES 张）")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(uiState.imageNames) { name ->
                    val file = container.imageStore.fileOrNull(name)
                    Box {
                        Card(
                            onClick = { file?.let { viewerFile = it } },
                            modifier = Modifier.size(110.dp),
                        ) {
                            if (file != null) {
                                AsyncImage(
                                    model = file,
                                    contentDescription = "错题图片",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(12.dp)),
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
                        IconButton(
                            onClick = { viewModel.removeImage(name) },
                            modifier = Modifier.align(Alignment.TopEnd),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "移除这张图片",
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
                item {
                    Card(
                        onClick = { showImageSourceDialog = true },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
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
                                Icon(Icons.Filled.Add, contentDescription = null)
                                Text(
                                    text = "加图片",
                                    style = MaterialTheme.typography.labelMedium,
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
            SectionTitle(text = "错因分析（可选）")
            OutlinedTextField(
                value = uiState.reason,
                onValueChange = viewModel::setReason,
                label = { Text("例如：看错条件、公式记错、思路没想全") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )

            SectionTitle(text = "解题思路（可选）")
            OutlinedTextField(
                value = uiState.solution,
                onValueChange = viewModel::setSolution,
                label = { Text("正确的做法是什么？关键步骤写清楚") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )

            SectionTitle(text = "我的总结（可选）")
            OutlinedTextField(
                value = uiState.summary,
                onValueChange = viewModel::setSummary,
                label = { Text("下次遇到同类题要注意什么？") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(12.dp))
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
                    val uri = runCatching { MediaUtils.createCameraUri(context) }.getOrNull()
                    if (uri == null) {
                        viewModel.addImages(emptyList())
                    } else {
                        cameraUri = uri
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

/** 知识点标签（点右边的 × 删除） */
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
            InputChip(
                selected = false,
                onClick = { onRemove(point) },
                label = { Text(point) },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Clear,
                        contentDescription = "删除知识点 $point",
                        modifier = Modifier.size(16.dp),
                    )
                },
            )
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
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            label = { Text("分类") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
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
