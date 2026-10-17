package com.cuotixingqiu.app.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cuotixingqiu.app.data.db.WrongQuestionEntity
import com.cuotixingqiu.app.domain.ReviewStatus
import com.cuotixingqiu.app.ui.appViewModel
import com.cuotixingqiu.app.ui.components.ConfirmDialog
import com.cuotixingqiu.app.ui.components.DifficultyStars
import com.cuotixingqiu.app.ui.components.ImageViewerDialog
import com.cuotixingqiu.app.ui.components.InfoRow
import com.cuotixingqiu.app.ui.components.QuestionImagesRow
import com.cuotixingqiu.app.ui.components.ScreenScaffold
import com.cuotixingqiu.app.ui.components.SectionTitle
import com.cuotixingqiu.app.ui.components.SmallBadge
import com.cuotixingqiu.app.ui.components.SubjectBadge
import com.cuotixingqiu.app.ui.components.difficultyLabel
import com.cuotixingqiu.app.ui.components.lastReviewLabel
import com.cuotixingqiu.app.ui.components.questionDisplayTitle
import com.cuotixingqiu.app.ui.components.reviewStatusInfo
import com.cuotixingqiu.app.ui.rememberAppContainer
import com.cuotixingqiu.app.util.TimeUtils
import java.io.File

/**
 * 错题详情页：完整显示一道错题的图片和文字，
 * 并且可以直接改状态、进入复习、编辑或删除。
 */
@Composable
fun DetailScreen(
    questionId: Long,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onStartReview: () -> Unit,
) {
    val container = rememberAppContainer()
    val viewModel: DetailViewModel = appViewModel { appContainer ->
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
    var viewerFile by remember { mutableStateOf<File?>(null) }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    val question = uiState.question

    ScreenScaffold(
        title = "错题详情",
        onBack = onBack,
        snackbarHostState = snackbarHostState,
        actions = {
            IconButton(onClick = onEdit, enabled = question != null) {
                Icon(Icons.Filled.Edit, contentDescription = "编辑")
            }
            IconButton(onClick = { showDeleteDialog = true }, enabled = question != null) {
                Icon(Icons.Filled.Delete, contentDescription = "删除")
            }
        },
        bottomBar = {
            if (question != null) {
                DetailBottomBar(
                    question = question,
                    onMarkMastered = viewModel::markMastered,
                    onReopen = viewModel::reopenReview,
                    onStartReview = onStartReview,
                )
            }
        },
    ) { padding ->
        if (!uiState.loaded) {
            Box(modifier = Modifier.fillMaxSize().padding(padding))
            return@ScreenScaffold
        }
        if (question == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                Text(
                    text = "这道错题已经不存在了（可能刚被删除）",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@ScreenScaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column {
                    Text(
                        text = questionDisplayTitle(question),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SubjectBadge(subject = question.subject)
                        if (uiState.notebookName != null) {
                            SmallBadge(
                                text = uiState.notebookName!!,
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            )
                        }
                        if (question.difficulty > 0) {
                            DifficultyStars(difficulty = question.difficulty, starSize = 16.dp)
                        }
                    }
                }
            }

            val imageFiles = question.imageNames.mapNotNull { container.imageStore.fileOrNull(it) }
            if (imageFiles.isNotEmpty()) {
                item {
                    QuestionImagesRow(
                        files = imageFiles,
                        onClick = { file -> viewerFile = file },
                    )
                }
            }

            item { MetaCard(question = question, notebookName = uiState.notebookName) }

            textSection(title = "题目内容", text = question.content)
            if (question.knowledgePoints.isNotEmpty()) {
                item {
                    Column {
                        SectionTitle(text = "知识点")
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = question.knowledgePoints.joinToString("、"),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
            textSection(title = "错因分析", text = question.reason)
            textSection(title = "解题思路", text = question.solution)
            textSection(title = "我的总结", text = question.summary)

            item { Spacer(modifier = Modifier.height(4.dp)) }
        }
    }

    if (showDeleteDialog) {
        ConfirmDialog(
            title = "删除这道错题？",
            message = "删除后无法恢复，题目里的图片也会一起删除。如果只是想暂时不看，可以改成“已掌握”。",
            confirmLabel = "删除",
            destructive = true,
            onConfirm = {
                showDeleteDialog = false
                viewModel.delete(onDeleted = onBack)
            },
            onDismiss = { showDeleteDialog = false },
        )
    }

    viewerFile?.let { file ->
        ImageViewerDialog(file = file, onDismiss = { viewerFile = null })
    }
}

/** 详情页底部按钮：标记已掌握 / 重新加入复习 / 立即复习 */
@Composable
private fun DetailBottomBar(
    question: WrongQuestionEntity,
    onMarkMastered: () -> Unit,
    onReopen: () -> Unit,
    onStartReview: () -> Unit,
) {
    val mastered = question.status == ReviewStatus.MASTERED.name
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (mastered) {
            OutlinedButton(onClick = onReopen, modifier = Modifier.weight(1f)) {
                Text("重新复习")
            }
        } else {
            OutlinedButton(onClick = onMarkMastered, modifier = Modifier.weight(1f)) {
                Text("已掌握")
            }
        }
        Button(onClick = onStartReview, modifier = Modifier.weight(1f)) {
            Text("立即复习")
        }
    }
}

/** 复习信息卡片：状态、阶段、次数、下次复习时间 */
@Composable
private fun MetaCard(question: WrongQuestionEntity, notebookName: String?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SectionTitle(text = "复习情况")
            InfoRow(label = "状态", value = reviewStatusInfo(question).first)
            InfoRow(label = "掌握阶段", value = "第 ${question.reviewStage} 阶段")
            InfoRow(label = "复习记录", value = lastReviewLabel(question))
            InfoRow(label = "难度", value = difficultyLabel(question.difficulty))
            val next = question.nextReviewAt
            if (next != null && question.status != ReviewStatus.MASTERED.name) {
                InfoRow(
                    label = "下次复习",
                    value = TimeUtils.formatDate(next) + "（" + TimeUtils.describeRelativeDay(next) + "）",
                )
            }
            if (notebookName != null) {
                InfoRow(label = "所属分类", value = notebookName)
            }
            HorizontalDivider()
            InfoRow(label = "创建时间", value = TimeUtils.formatDateTime(question.createdAt))
            InfoRow(label = "修改时间", value = TimeUtils.formatDateTime(question.updatedAt))
        }
    }
}

/** 一段带标题的正文；没有内容时不显示，界面会更干净 */
private fun androidx.compose.foundation.lazy.LazyListScope.textSection(title: String, text: String) {
    if (text.isBlank()) return
    item {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                SectionTitle(text = title)
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = text, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
