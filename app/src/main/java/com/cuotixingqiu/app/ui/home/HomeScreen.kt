package com.cuotixingqiu.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cuotixingqiu.app.data.db.WrongQuestionEntity
import com.cuotixingqiu.app.ui.appViewModel
import com.cuotixingqiu.app.ui.components.CuotiCard
import com.cuotixingqiu.app.ui.components.CuotiVisual
import com.cuotixingqiu.app.ui.components.PlanetMark
import com.cuotixingqiu.app.ui.components.PrimaryActionButton
import com.cuotixingqiu.app.ui.components.SectionHeader
import com.cuotixingqiu.app.ui.components.SubjectDot
import com.cuotixingqiu.app.ui.components.ThinDivider
import com.cuotixingqiu.app.ui.components.pageSidePadding
import com.cuotixingqiu.app.ui.components.questionDisplayTitle
import com.cuotixingqiu.app.ui.components.rememberDockClearance
import com.cuotixingqiu.app.ui.components.subjectChartColor
import com.cuotixingqiu.app.util.TimeUtils

/**
 * 首页：品牌与日期 → 今日目标 → 深色学习统计卡 → 添加错题 → 今日复习任务 → 最近添加。
 *
 * 所有数字和任务都来自真实数据库 Flow；这里只改变视觉层级，
 * 不引入示例题目、示例姓名或写死的统计。
 */
@Composable
fun HomeScreen(
    onAddQuestion: () -> Unit,
    onOpenQuestion: (Long) -> Unit,
    onStartReview: () -> Unit,
    onOpenReview: () -> Unit,
    onOpenQuestions: () -> Unit,
    onOpenSubject: (String) -> Unit,
    onOpenStats: () -> Unit,
) {
    val viewModel: HomeViewModel = appViewModel { container ->
        HomeViewModel(container.wrongQuestionRepository, container.notebookRepository)
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.refresh() }

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
                top = 8.dp,
                bottom = rememberDockClearance() + 26.dp,
            ),
        ) {
            item { HomeHeader() }

            item {
                Spacer(modifier = Modifier.height(22.dp))
                HeroTitle(
                    dueCount = uiState.dueCount,
                    totalCount = uiState.totalCount,
                )
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
                TodayStudyCard(
                    uiState = uiState,
                    onOpenStats = onOpenStats,
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                PrimaryActionButton(
                    text = "添加错题",
                    onClick = onAddQuestion,
                    leadingIcon = Icons.Filled.Add,
                )
            }

            item {
                Spacer(modifier = Modifier.height(CuotiVisual.SectionGap))
                SectionHeader(
                    title = "今日复习任务",
                    trailing = {
                        if (uiState.dueCount > 0) {
                            TextButton(onClick = onStartReview) {
                                Text(
                                    text = "开始复习",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        } else {
                            Text(
                                text = "已完成",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                )
                Spacer(modifier = Modifier.height(12.dp))
                TodayTaskCard(
                    uiState = uiState,
                    onOpenQuestion = onOpenQuestion,
                    onOpenReview = onOpenReview,
                )
            }

            item {
                Spacer(modifier = Modifier.height(CuotiVisual.SectionGap))
                SectionHeader(
                    title = "最近添加",
                    trailing = {
                        TextButton(onClick = onOpenQuestions) {
                            Text(
                                text = "查看全部",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                )
                Spacer(modifier = Modifier.height(12.dp))
                RecentQuestionsCard(
                    questions = uiState.recentQuestions,
                    onOpenQuestion = onOpenQuestion,
                    onAddQuestion = onAddQuestion,
                )
            }
        }
    }
}

@Composable
private fun HomeHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlanetMark()
        Spacer(modifier = Modifier.width(9.dp))
        Text(
            text = "错题星球",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = TimeUtils.formatMonthDayWeekday(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HeroTitle(dueCount: Int, totalCount: Int) {
    Column {
        if (totalCount == 0) {
            Text(
                text = "今天先记下",
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 25.sp),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "第一道错题",
                    style = MaterialTheme.typography.displaySmall.copy(fontSize = 34.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        } else if (dueCount == 0) {
            Text(
                text = "今天没有",
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 25.sp),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "待复习错题",
                style = MaterialTheme.typography.displaySmall.copy(fontSize = 34.sp),
                color = MaterialTheme.colorScheme.onSurface,
            )
        } else {
            Text(
                text = "今天再清掉",
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 25.sp),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = dueCount.toString(),
                    style = MaterialTheme.typography.displaySmall.copy(fontSize = 38.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "道错题",
                    style = MaterialTheme.typography.displaySmall.copy(fontSize = 30.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun TodayStudyCard(
    uiState: HomeUiState,
    onOpenStats: () -> Unit,
) {
    val totalTasks = uiState.reviewedTodayCount + uiState.dueCount
    val progress = if (totalTasks == 0) 0f else {
        uiState.reviewedTodayCount.toFloat() / totalTasks.toFloat()
    }
    val cardColor = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) {
        Color(0xFF181B22)
    } else {
        Color(0xFF17191D)
    }
    val onCard = Color.White
    val muted = Color.White.copy(alpha = 0.58f)
    val accent = Color(0xFF767BFF)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .clickable(
                onClickLabel = "查看学习统计",
                onClick = onOpenStats,
            ),
        shape = RoundedCornerShape(26.dp),
        color = cardColor,
        contentColor = onCard,
        shadowElevation = 0.dp,
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 19.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "今日学习",
                    style = MaterialTheme.typography.labelLarge,
                    color = muted,
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = if (totalTasks == 0) {
                        "今天还没有任务"
                    } else {
                        "已复习 ${uiState.reviewedTodayCount} / $totalTasks"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.72f),
                )
            }

            Spacer(modifier = Modifier.height(15.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f)),
            ) {
                if (progress > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress.coerceIn(0f, 1f))
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(accent),
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                HeroMetric(
                    value = uiState.totalCount.toString(),
                    label = "错题总数",
                    modifier = Modifier.weight(1f),
                    muted = muted,
                )
                HeroMetricDivider()
                HeroMetric(
                    value = uiState.dueCount.toString(),
                    label = "待复习",
                    modifier = Modifier.weight(1f),
                    muted = muted,
                    valueColor = if (uiState.dueCount > 0) accent else onCard,
                )
                HeroMetricDivider()
                HeroMetric(
                    value = "${uiState.studyStreak}天",
                    label = "连续学习",
                    modifier = Modifier.weight(1f),
                    muted = muted,
                )
            }
        }
    }
}

@Composable
private fun HeroMetric(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    muted: Color,
    valueColor: Color = Color.White,
) {
    Column(modifier = modifier) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall.copy(fontSize = 25.sp),
            fontWeight = FontWeight.Bold,
            color = valueColor,
            maxLines = 1,
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = muted,
            maxLines = 1,
        )
    }
}

@Composable
private fun HeroMetricDivider() {
    Box(
        modifier = Modifier
            .padding(horizontal = 10.dp)
            .width(1.dp)
            .height(34.dp)
            .background(Color.White.copy(alpha = 0.12f)),
    )
}

@Composable
private fun TodayTaskCard(
    uiState: HomeUiState,
    onOpenQuestion: (Long) -> Unit,
    onOpenReview: () -> Unit,
) {
    CuotiCard(contentPadding = PaddingValues(vertical = 3.dp)) {
        if (uiState.dueQuestions.isEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenReview)
                    .padding(horizontal = 16.dp, vertical = 19.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(17.dp),
                    )
                }
                Spacer(modifier = Modifier.width(11.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "今天的复习任务已完成",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "休息一下，或者继续添加新的错题。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            uiState.dueQuestions.forEachIndexed { index, question ->
                TodayTaskRow(
                    question = question,
                    onClick = { onOpenQuestion(question.id) },
                )
                if (index != uiState.dueQuestions.lastIndex) {
                    ThinDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun TodayTaskRow(
    question: WrongQuestionEntity,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SubjectDot(subject = question.subject, size = 9.dp)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = question.knowledgePoints.firstOrNull() ?: questionDisplayTitle(question),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${question.subject} · 待复习",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun RecentQuestionsCard(
    questions: List<WrongQuestionEntity>,
    onOpenQuestion: (Long) -> Unit,
    onAddQuestion: () -> Unit,
) {
    CuotiCard(contentPadding = PaddingValues(vertical = 3.dp)) {
        if (questions.isEmpty()) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp)) {
                Text(
                    text = "还没有最近添加的错题",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "添加一道错题，它会出现在这里。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable(onClick = onAddQuestion),
                )
            }
        } else {
            questions.forEachIndexed { index, question ->
                RecentQuestionRow(
                    question = question,
                    onClick = { onOpenQuestion(question.id) },
                )
                if (index != questions.lastIndex) {
                    ThinDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }
}

@Composable
private fun RecentQuestionRow(
    question: WrongQuestionEntity,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SubjectDot(subject = question.subject, size = 7.dp)
            Spacer(modifier = Modifier.width(7.dp))
            Text(
                text = question.subject,
                style = MaterialTheme.typography.labelMedium,
                color = subjectChartColor(question.subject),
            )
            Text(
                text = " · ${TimeUtils.formatMonthDay(question.createdAt)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = questionPreview(question),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun questionPreview(question: WrongQuestionEntity): String {
    question.content.lineSequence().firstOrNull { it.isNotBlank() }?.trim()?.let { return it }
    if (question.title.isNotBlank()) return question.title.trim()
    return "未填写题目内容"
}
