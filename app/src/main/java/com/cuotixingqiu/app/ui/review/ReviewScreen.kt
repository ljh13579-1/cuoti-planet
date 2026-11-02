package com.cuotixingqiu.app.ui.review

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cuotixingqiu.app.ui.appViewModel
import com.cuotixingqiu.app.ui.components.CuotiCard
import com.cuotixingqiu.app.ui.components.CuotiVisual
import com.cuotixingqiu.app.ui.components.PrimaryActionButton
import com.cuotixingqiu.app.ui.components.ProgressRing
import com.cuotixingqiu.app.ui.components.SectionHeader
import com.cuotixingqiu.app.ui.components.SubjectDot
import com.cuotixingqiu.app.ui.components.pageSidePadding
import com.cuotixingqiu.app.ui.components.rememberDockClearance
import com.cuotixingqiu.app.ui.components.subjectChartColor
import com.cuotixingqiu.app.util.TimeUtils
import java.time.LocalDate

/** 未来三天的一张日期卡 */
private data class FutureReviewDay(
    val date: LocalDate,
    val label: String,
    val count: Int,
)

/**
 * 复习页：今日进度环 → 开始复习 → 各学科真实掌握进度 → 未来三天安排。
 *
 * 不再显示重复的“今日待复习卡片列表”：开始复习按钮直接进入原复习会话，
 * 所有统计均来自 Room 中的题目状态。
 */
@Composable
fun ReviewScreen(
    onStartSession: (Long?) -> Unit,
    onOpenStats: () -> Unit,
    onAddQuestion: () -> Unit,
) {
    val viewModel: ReviewViewModel = appViewModel { container ->
        ReviewViewModel(container.wrongQuestionRepository, container.notebookRepository)
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.refresh() }

    val today = LocalDate.now()
    val futureDays = remember(uiState.upcoming, today) {
        val counts = uiState.upcoming
            .mapNotNull { it.nextReviewAt }
            .groupingBy { TimeUtils.localDateOf(it) }
            .eachCount()
        (1L..3L).map { offset ->
            val date = today.plusDays(offset)
            FutureReviewDay(
                date = date,
                label = if (offset == 1L) "明天" else {
                    TimeUtils.formatWeekday(TimeUtils.startOfDayMillis(date))
                },
                count = counts[date] ?: 0,
            )
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
                top = 8.dp,
                bottom = rememberDockClearance() + 26.dp,
            ),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "复习",
                        style = MaterialTheme.typography.headlineMedium.copy(fontSize = 32.sp),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = TimeUtils.formatWeekdayMonthDay(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
                TodayReviewCard(
                    uiState = uiState,
                    onOpenStats = onOpenStats,
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                PrimaryActionButton(
                    text = if (uiState.dueCount > 0) "开始复习" else "今日复习已完成",
                    onClick = { onStartSession(null) },
                    leadingIcon = Icons.Filled.PlayArrow,
                    enabled = uiState.dueCount > 0,
                )
            }

            item {
                Spacer(modifier = Modifier.height(CuotiVisual.SectionGap))
                SectionHeader(title = "各学科进度")
                Spacer(modifier = Modifier.height(12.dp))
                SubjectProgressCard(
                    progress = uiState.subjectProgress,
                    totalCount = uiState.totalCount,
                    onAddQuestion = onAddQuestion,
                )
            }

            item {
                Spacer(modifier = Modifier.height(CuotiVisual.SectionGap))
                SectionHeader(title = "未来三天")
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    futureDays.forEach { day ->
                        FutureDayCard(
                            day = day,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun TodayReviewCard(
    uiState: ReviewUiState,
    onOpenStats: () -> Unit,
) {
    val total = uiState.todayTaskTotal
    val progress = if (total == 0) 0f else {
        uiState.reviewedTodayCount.toFloat() / total.toFloat()
    }
    val estimateMinutes = maxOf(1, uiState.dueCount * 2).takeIf { uiState.dueCount > 0 } ?: 0

    CuotiCard(
        modifier = Modifier
            .clip(RoundedCornerShape(22.dp))
            .clickable(
                onClickLabel = "查看学习统计",
                onClick = onOpenStats,
            ),
        contentPadding = PaddingValues(20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProgressRing(
                progress = progress,
                size = 88.dp,
                strokeWidth = 8.dp,
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
            ) {
                Text(
                    text = "${uiState.reviewedTodayCount}/${total}",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(modifier = Modifier.width(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "今日待复习",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = uiState.dueCount.toString(),
                        style = MaterialTheme.typography.headlineMedium.copy(fontSize = 32.sp),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "题",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 3.dp),
                    )
                }
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = if (uiState.dueCount > 0) {
                        "预计用时 $estimateMinutes 分钟"
                    } else {
                        "今天的任务已经完成"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SubjectProgressCard(
    progress: List<SubjectProgress>,
    totalCount: Int,
    onAddQuestion: () -> Unit,
) {
    CuotiCard(contentPadding = PaddingValues(18.dp)) {
        if (progress.isEmpty()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (totalCount == 0) "还没有错题" else "暂时没有学科数据",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (totalCount == 0) "添加错题后，这里会显示各学科的真实掌握进度。" else "复习并标记掌握后，这里会出现进度。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (totalCount == 0) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "添加错题",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onAddQuestion)
                            .padding(vertical = 3.dp),
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(15.dp)) {
                progress.forEach { item ->
                    SubjectProgressRow(item = item)
                }
            }
        }
    }
}

@Composable
private fun SubjectProgressRow(item: SubjectProgress) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SubjectDot(subject = item.subject, size = 8.dp)
        Spacer(modifier = Modifier.width(9.dp))
        Text(
            text = item.subject,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(40.dp),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            if (item.rate > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(item.rate.coerceIn(0f, 1f))
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(subjectChartColor(item.subject)),
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "${item.mastered}/${item.total}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
            modifier = Modifier.width(34.dp),
        )
    }
}

@Composable
private fun FutureDayCard(
    day: FutureReviewDay,
    modifier: Modifier = Modifier,
) {
    CuotiCard(
        modifier = modifier.height(94.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
    ) {
        Text(
            text = day.label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = day.count.toString(),
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 25.sp),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "题",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 2.dp),
            )
        }
    }
}
