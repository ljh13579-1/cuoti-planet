package com.cuotixingqiu.app.ui.review

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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cuotixingqiu.app.domain.ReviewScheduler
import com.cuotixingqiu.app.ui.appViewModel
import com.cuotixingqiu.app.ui.components.QuestionCard
import com.cuotixingqiu.app.ui.components.ScreenScaffold
import com.cuotixingqiu.app.ui.components.SectionTitle
import com.cuotixingqiu.app.util.TimeUtils

/**
 * 复习页：先看今天要复习多少道，再一键开始复习；
 * 下面还列出“今天待复习”的清单和“接下来 7 天”的安排。
 */
@Composable
fun ReviewScreen(
    onOpenQuestion: (Long) -> Unit,
    onStartSession: (Long?) -> Unit,
    onOpenStats: () -> Unit,
) {
    val viewModel: ReviewViewModel = appViewModel { container ->
        ReviewViewModel(container.wrongQuestionRepository, container.notebookRepository)
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.refresh() }

    ScreenScaffold(title = "复习", subtitle = "按遗忘规律自动安排，不用自己记时间") { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { ReviewOverviewCard(uiState) }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { onStartSession(null) },
                        enabled = uiState.dueCount > 0,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            if (uiState.dueCount > 0) {
                                "开始复习（${uiState.dueCount} 道）"
                            } else {
                                "今天没有待复习"
                            },
                        )
                    }
                    OutlinedButton(onClick = onOpenStats) { Text("看统计") }
                }
            }

            item { SectionTitle(text = "今天待复习（${uiState.dueQuestions.size} 道）") }
            if (uiState.dueQuestions.isEmpty()) {
                item {
                    InfoCard("今天的复习任务都完成了 👏 休息一下，或者去添加新的错题。")
                }
            } else {
                items(uiState.dueQuestions, key = { it.id }) { question ->
                    QuestionCard(
                        question = question,
                        onClick = { onOpenQuestion(question.id) },
                        notebookName = question.notebookId?.let { uiState.notebookNames[it] },
                    )
                }
            }

            item { SectionTitle(text = "接下来 7 天") }
            if (uiState.upcoming.isEmpty()) {
                item { InfoCard("未来 7 天暂时没有新的复习安排。") }
            } else {
                items(uiState.upcoming, key = { it.id }) { question ->
                    QuestionCard(
                        question = question,
                        onClick = { onOpenQuestion(question.id) },
                        notebookName = question.notebookId?.let { uiState.notebookNames[it] },
                        trailingText = question.nextReviewAt?.let {
                            "安排：" + TimeUtils.formatDate(it) + "（" + TimeUtils.describeRelativeDay(it) + "）"
                        },
                    )
                }
            }

            item { ReviewRuleCard() }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

/** 复习概览：今日待复习、今日已复习、累计、已掌握 + 掌握率 */
@Composable
private fun ReviewOverviewCard(uiState: ReviewUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                NumberCell("今日待复习", uiState.dueCount.toString(), Modifier.weight(1f))
                NumberCell("今日已复习", uiState.reviewedTodayCount.toString(), Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                NumberCell("错题总数", uiState.totalCount.toString(), Modifier.weight(1f))
                NumberCell("已掌握", uiState.masteredCount.toString(), Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "掌握率：${(uiState.masteredRate * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { uiState.masteredRate },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun NumberCell(label: String, value: String, modifier: Modifier = Modifier) {
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

@Composable
private fun InfoCard(text: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        )
    }
}

/** 复习规则说明：让用户知道“什么时候该复习”，也方便家长/老师理解 */
@Composable
private fun ReviewRuleCard() {
    val intervals = ReviewScheduler.INTERVAL_DAYS.joinToString(" → ") { "$it 天" }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            SectionTitle(text = "复习规则")
            Spacer(modifier = Modifier.height(8.dp))
            listOf(
                "新添加的错题：第二天开始复习",
                "答「不会」：重新从 1 天后开始",
                "答「基本掌握」：间隔逐渐拉长（$intervals）",
                "答「已掌握」：退出复习计划（可以在详情页重新加入）",
            ).forEach { line ->
                Text(
                    text = "· $line",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 2.dp),
                )
            }
        }
    }
}
