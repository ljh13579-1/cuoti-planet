package com.cuotixingqiu.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cuotixingqiu.app.data.db.SubjectCount
import com.cuotixingqiu.app.domain.Subject
import com.cuotixingqiu.app.ui.appViewModel
import com.cuotixingqiu.app.ui.components.EmptyState
import com.cuotixingqiu.app.ui.components.QuestionCard
import com.cuotixingqiu.app.ui.components.ScreenScaffold
import com.cuotixingqiu.app.ui.components.SectionTitle

/**
 * 首页：先看整体情况（总题数、今日新增、待复习、学科分布），
 * 再看“需要复习”和“最近添加”，并且随时可以从右下角添加错题。
 */
@Composable
fun HomeScreen(
    onAddQuestion: () -> Unit,
    onOpenQuestion: (Long) -> Unit,
    onStartReview: () -> Unit,
    onOpenQuestions: () -> Unit,
    onOpenStats: () -> Unit,
) {
    val viewModel: HomeViewModel = appViewModel { container ->
        HomeViewModel(container.wrongQuestionRepository, container.notebookRepository)
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 每次进入首页重新算一次“今天”，跨天使用也不会显示旧数字
    LaunchedEffect(Unit) { viewModel.refresh() }

    ScreenScaffold(title = "错题星球", subtitle = "让每一道错题都有价值") { padding ->
        if (uiState.totalCount == 0) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(padding),
            ) {
                EmptyState(
                    icon = Icons.AutoMirrored.Filled.List,
                    title = "还没有错题",
                    message = "把你做错的题拍照或写下来，App 会自动安排复习时间，帮你真正记住它。",
                    actionLabel = "添加第一道错题",
                    onAction = onAddQuestion,
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
            item { OverviewCard(uiState) }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onStartReview,
                        enabled = uiState.dueCount > 0,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(if (uiState.dueCount > 0) "开始复习（${uiState.dueCount}）" else "今天没有要复习的")
                    }
                    OutlinedButton(onClick = onOpenStats) { Text("统计图表") }
                }
            }

            if (uiState.subjectCounts.isNotEmpty()) {
                item { SectionTitle(text = "学科分布") }
                item { SubjectDistributionCard(counts = uiState.subjectCounts) }
            }

            item {
                SectionTitle(
                    text = "需要复习",
                    trailing = {
                        if (uiState.dueQuestions.isNotEmpty()) {
                            TextButton(onClick = onStartReview) { Text("去复习") }
                        }
                    },
                )
            }
            if (uiState.dueQuestions.isEmpty()) {
                item { HintCard(text = "今天没有需要复习的错题，休息一下或者添加新错题吧。") }
            } else {
                items(uiState.dueQuestions, key = { it.id }) { question ->
                    QuestionCard(
                        question = question,
                        onClick = { onOpenQuestion(question.id) },
                        notebookName = question.notebookId?.let { uiState.notebookNames[it] },
                    )
                }
            }

            item {
                SectionTitle(
                    text = "最近添加",
                    trailing = { TextButton(onClick = onOpenQuestions) { Text("查看全部") } },
                )
            }
            items(uiState.recentQuestions, key = { it.id }) { question ->
                QuestionCard(
                    question = question,
                    onClick = { onOpenQuestion(question.id) },
                    notebookName = question.notebookId?.let { uiState.notebookNames[it] },
                )
            }
        }
    }
}

/** 顶部四个数字的概览卡片 */
@Composable
private fun OverviewCard(uiState: HomeUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                StatCell("错题总数", uiState.totalCount.toString(), Modifier.weight(1f))
                StatCell("今日新增", uiState.todayAddedCount.toString(), Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(14.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                StatCell("今日待复习", uiState.dueCount.toString(), Modifier.weight(1f))
                StatCell("已掌握", uiState.masteredCount.toString(), Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "今天已经复习了 ${uiState.reviewedTodayCount} 道错题",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StatCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** 学科分布：每个学科一行，用进度条表示占比 */
@Composable
private fun SubjectDistributionCard(counts: List<SubjectCount>) {
    val ordered = remember(counts) {
        counts.sortedBy { count ->
            val index = Subject.displayOrder.indexOfFirst { it.displayName == count.subject }
            if (index < 0) Int.MAX_VALUE else index
        }
    }
    val max = ordered.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ordered.forEach { item ->
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(text = item.subject, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "${item.count} 道",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { item.count.toFloat() / max.toFloat() },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun HintCard(text: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
