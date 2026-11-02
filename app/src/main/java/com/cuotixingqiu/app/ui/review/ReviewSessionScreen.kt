package com.cuotixingqiu.app.ui.review

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cuotixingqiu.app.data.db.WrongQuestionEntity
import com.cuotixingqiu.app.domain.ReviewResult
import com.cuotixingqiu.app.domain.ReviewScheduler
import com.cuotixingqiu.app.ui.appViewModel
import com.cuotixingqiu.app.ui.components.CuotiCard
import com.cuotixingqiu.app.ui.components.CuotiVisual
import com.cuotixingqiu.app.ui.components.DifficultyStars
import com.cuotixingqiu.app.ui.components.EmptyState
import com.cuotixingqiu.app.ui.components.ImageViewerDialog
import com.cuotixingqiu.app.ui.components.PrimaryActionButton
import com.cuotixingqiu.app.ui.components.QuestionImagesRow
import com.cuotixingqiu.app.ui.components.QuietSectionLabel
import com.cuotixingqiu.app.ui.components.ScreenScaffold
import com.cuotixingqiu.app.ui.components.SectionHeader
import com.cuotixingqiu.app.ui.components.SubjectDot
import com.cuotixingqiu.app.ui.components.ThinDivider
import com.cuotixingqiu.app.ui.components.questionDisplayTitle
import com.cuotixingqiu.app.ui.components.rememberDockClearance
import com.cuotixingqiu.app.ui.rememberAppContainer
import com.cuotixingqiu.app.util.TimeUtils
import java.io.File

/**
 * 复习会话页：一道一道地复习。
 *
 * 流程：看题目 → 想答案 → 点“显示答案”对照 → 选「不会 / 基本掌握 / 已掌握」→ 下一题。
 * 每次选择都会立刻写入数据库，并按规则算好下次复习时间（不会白复习）。
 * v1.3.8：视觉与其它页面统一，复习规则与数据流未改。
 */
@Composable
fun ReviewSessionScreen(
    onlyQuestionId: Long?,
    onBack: () -> Unit,
    onOpenQuestion: (Long) -> Unit,
) {
    val container = rememberAppContainer()
    val viewModel: ReviewSessionViewModel = appViewModel { appContainer ->
        ReviewSessionViewModel(appContainer.wrongQuestionRepository, onlyQuestionId)
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var viewerFile by remember { mutableStateOf<File?>(null) }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    val current = uiState.current
    val subtitle = when {
        uiState.loading -> null
        uiState.questions.isEmpty() -> null
        uiState.finished -> "本次复习完成"
        else -> "第 ${uiState.index + 1} / ${uiState.questions.size} 道"
    }

    ScreenScaffold(
        title = "复习",
        subtitle = subtitle,
        onBack = onBack,
        snackbarHostState = snackbarHostState,
        maxContentWidth = 680.dp,
        actions = {
            if (current != null && !uiState.finished) {
                IconButton(onClick = { onOpenQuestion(current.id) }) {
                    Icon(
                        Icons.Filled.Info,
                        contentDescription = "查看这道题的详情",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                uiState.loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                uiState.questions.isEmpty() -> {
                    EmptyState(
                        icon = Icons.Filled.Info,
                        title = "现在没有要复习的错题",
                        message = "所有到期的错题都复习完了。新错题会在第二天自动进入复习计划。",
                        actionLabel = "返回",
                        onAction = onBack,
                    )
                }

                uiState.finished -> {
                    SessionSummary(
                        uiState = uiState,
                        onFinish = onBack,
                    )
                }

                current != null -> {
                    ReviewQuestionContent(
                        question = current,
                        revealed = uiState.revealed,
                        progress = (uiState.index + 1).toFloat() / uiState.questions.size.toFloat(),
                        imageFilesOf = { names -> names.mapNotNull { container.imageStore.fileOrNull(it) } },
                        onReveal = viewModel::reveal,
                        onAnswer = viewModel::answer,
                        onImageClick = { file -> viewerFile = file },
                    )
                }
            }
        }
    }

    viewerFile?.let { file ->
        ImageViewerDialog(file = file, onDismiss = { viewerFile = null })
    }
}

/** 复习中的一题：题干 + （点开后的）答案与三个按钮 */
@Composable
private fun ReviewQuestionContent(
    question: WrongQuestionEntity,
    revealed: Boolean,
    progress: Float,
    imageFilesOf: (List<String>) -> List<File>,
    onReveal: () -> Unit,
    onAnswer: (ReviewResult) -> Unit,
    onImageClick: (File) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                start = CuotiVisual.PageHorizontal,
                end = CuotiVisual.PageHorizontal,
                top = 8.dp,
                bottom = 20.dp + rememberDockClearance(),
            ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // 进度：一条细圆角进度条，替代原来的粗条
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SubjectDot(subject = question.subject, size = 8.dp)
            Text(
                text = question.subject,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (question.difficulty > 0) {
                Spacer(modifier = Modifier.width(2.dp))
                DifficultyStars(difficulty = question.difficulty, starSize = 14.dp)
            }
        }
        if (question.knowledgePoints.isNotEmpty()) {
            Text(
                text = question.knowledgePoints.joinToString("、"),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Text(
            text = questionDisplayTitle(question),
            style = MaterialTheme.typography.headlineSmall.copy(fontSize = 24.sp),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        val images = imageFilesOf(question.imageNames)
        if (images.isNotEmpty()) {
            QuestionImagesRow(files = images, onClick = onImageClick)
        }

        if (question.content.isNotBlank()) {
            CuotiCard {
                Text(
                    text = question.content,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        if (!revealed) {
            PrimaryActionButton(text = "显示答案与解析", onClick = onReveal)
            Text(
                text = "先在心里（或纸上）做一遍，再对照答案，效果最好。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            AnswerBlock(title = "错因分析", text = question.reason)
            AnswerBlock(title = "解题思路", text = question.solution)
            AnswerBlock(title = "我的总结", text = question.summary)

            Spacer(modifier = Modifier.height(2.dp))
            ThinDivider()
            Spacer(modifier = Modifier.height(2.dp))

            SectionHeader(title = "这道题我记住得怎么样？")
            Text(
                text = nextIntervalHint(question),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AnswerButton(
                    text = "不会",
                    onClick = { onAnswer(ReviewResult.AGAIN) },
                    container = MaterialTheme.colorScheme.surfaceContainerLow,
                    content = MaterialTheme.colorScheme.onSurface,
                    border = true,
                    modifier = Modifier.weight(1f),
                )
                AnswerButton(
                    text = "基本掌握",
                    onClick = { onAnswer(ReviewResult.GOOD) },
                    container = MaterialTheme.colorScheme.secondaryContainer,
                    content = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.weight(1f),
                )
                AnswerButton(
                    text = "已掌握",
                    onClick = { onAnswer(ReviewResult.EASY) },
                    container = MaterialTheme.colorScheme.primary,
                    content = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.weight(1f),
                )
            }
            Text(
                text = "选「不会」明天再来；选「基本掌握」间隔会越拉越长；选「已掌握」就退出复习计划。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** 三档自评按钮：统一 54dp 高、16dp 圆角，靠底色区分轻重。 */
@Composable
private fun AnswerButton(
    text: String,
    onClick: () -> Unit,
    container: androidx.compose.ui.graphics.Color,
    content: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    border: Boolean = false,
) {
    val shape = RoundedCornerShape(CuotiVisual.ButtonCorner)
    if (border) {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.height(CuotiVisual.ButtonHeight),
            shape = shape,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant,
            ),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = content),
            contentPadding = PaddingValues(horizontal = 6.dp),
        ) {
            Text(text = text, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
    } else {
        Button(
            onClick = onClick,
            modifier = modifier.height(CuotiVisual.ButtonHeight),
            shape = shape,
            colors = ButtonDefaults.buttonColors(
                containerColor = container,
                contentColor = content,
            ),
            contentPadding = PaddingValues(horizontal = 6.dp),
        ) {
            Text(text = text, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
    }
}

@Composable
private fun AnswerBlock(title: String, text: String) {
    CuotiCard {
        QuietSectionLabel(text = title)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = text.ifBlank { "（这道题还没有填写$title）" },
            style = MaterialTheme.typography.bodyMedium,
            color = if (text.isBlank()) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
    }
}

/** 提示这次选择之后的下次复习时间，让规则变得可预期 */
private fun nextIntervalHint(question: WrongQuestionEntity): String {
    val now = System.currentTimeMillis()
    val again = ReviewScheduler.schedule(ReviewResult.AGAIN, question.reviewStage, now)
    val good = ReviewScheduler.schedule(ReviewResult.GOOD, question.reviewStage, now)
    return "如果选「不会」→ " + TimeUtils.describeRelativeDay(again.nextReviewAt) +
        "；选「基本掌握」→ " + TimeUtils.describeRelativeDay(good.nextReviewAt) + "再复习。"
}

/** 一轮复习结束后的小结 */
@Composable
private fun SessionSummary(
    uiState: ReviewSessionUiState,
    onFinish: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                start = CuotiVisual.PageHorizontal,
                end = CuotiVisual.PageHorizontal,
                top = 20.dp,
                bottom = 24.dp + rememberDockClearance(),
            ),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "本次复习完成",
            style = MaterialTheme.typography.headlineSmall.copy(fontSize = 26.sp),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "一共复习了 ${uiState.answeredCount} 道题",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(20.dp))
        CuotiCard {
            SummaryRow("不会", uiState.againCount)
            ThinDivider(modifier = Modifier.padding(vertical = 10.dp))
            SummaryRow("基本掌握", uiState.goodCount)
            ThinDivider(modifier = Modifier.padding(vertical = 10.dp))
            SummaryRow("已掌握", uiState.easyCount)
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "下次复习时间都已经自动安排好了，可以在「复习」页查看。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        PrimaryActionButton(text = "返回", onClick = onFinish)
        Spacer(modifier = Modifier.size(2.dp))
    }
}

@Composable
private fun SummaryRow(label: String, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = "$count 道",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
