package com.cuotixingqiu.app.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cuotixingqiu.app.domain.Subject
import com.cuotixingqiu.app.ui.appViewModel
import com.cuotixingqiu.app.ui.components.ScreenScaffold
import com.cuotixingqiu.app.ui.components.SectionTitle

/** 图表配色（靛蓝 + 青绿 + 几个辅助色） */
private val chartColors = listOf(
    Color(0xFF3D5AA9),
    Color(0xFF00696E),
    Color(0xFF7B5EA7),
    Color(0xFFB25E00),
    Color(0xFF0E7C6B),
    Color(0xFF5B5D72),
    Color(0xFF9E4A5E),
)

/**
 * 统计页：用 Canvas 自己画的图表（不依赖任何第三方图表库）。
 * 包含：概览数字、学科分布饼图、近 7 天复习次数柱状图、难度与复习阶段分布。
 */
@Composable
fun StatsScreen(onBack: () -> Unit) {
    val viewModel: StatsViewModel = appViewModel { container ->
        StatsViewModel(container.wrongQuestionRepository, container.notebookRepository)
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ScreenScaffold(title = "统计图表", onBack = onBack) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { OverviewCard(uiState) }

            if (uiState.totalCount == 0) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "还没有数据。添加几道错题之后，这里会显示学科分布、复习趋势等图表。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
                return@LazyColumn
            }

            item { SectionTitle(text = "学科分布") }
            item { SubjectPieCard(uiState) }

            item { SectionTitle(text = "近 7 天复习次数（共 ${uiState.weeklyTotal} 次）") }
            item { WeeklyBarCard(uiState) }

            item { SectionTitle(text = "难度分布") }
            item { DifficultyCard(uiState) }

            item { SectionTitle(text = "复习阶段分布") }
            item { StageCard(uiState) }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun OverviewCard(uiState: StatsUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                BigNumber("错题总数", uiState.totalCount.toString(), Modifier.weight(1f))
                BigNumber("已掌握", uiState.masteredCount.toString(), Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                BigNumber("需要复习", uiState.needsReviewCount.toString(), Modifier.weight(1f))
                BigNumber("今日待复习", uiState.dueTodayCount.toString(), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun BigNumber(label: String, value: String, modifier: Modifier = Modifier) {
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

/** 学科分布：环形图 + 图例 */
@Composable
private fun SubjectPieCard(uiState: StatsUiState) {
    val items = remember(uiState.subjectCounts) {
        uiState.subjectCounts
            .sortedBy { count ->
                val index = Subject.displayOrder.indexOfFirst { it.displayName == count.subject }
                if (index < 0) Int.MAX_VALUE else index
            }
            .map { it.subject to it.count }
    }
    val total = items.sumOf { it.second }.coerceAtLeast(1)

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(modifier = Modifier.size(160.dp)) {
                    val stroke = 34f
                    val diameter = minOf(size.width, size.height) - stroke
                    val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                    val arcSize = Size(diameter, diameter)
                    var startAngle = -90f
                    items.forEachIndexed { index, item ->
                        val sweep = 360f * item.second.toFloat() / total.toFloat()
                        drawArc(
                            color = chartColors[index % chartColors.size],
                            startAngle = startAngle,
                            sweepAngle = sweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = stroke),
                        )
                        startAngle += sweep
                    }
                }
                Text(
                    text = "$total\n道",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }

            HorizontalDivider()

            items.forEachIndexed { index, item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Canvas(modifier = Modifier.size(12.dp)) {
                        drawCircle(color = chartColors[index % chartColors.size])
                    }
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(text = item.first, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = "${item.second} 道 · ${uiState.percentText(item.second)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** 近 7 天复习次数：柱状图 */
@Composable
private fun WeeklyBarCard(uiState: StatsUiState) {
    val counts = uiState.weeklyReviewCounts
    val max = (counts.maxOrNull() ?: 0).coerceAtLeast(1)

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
            ) {
                val barCount = counts.size.coerceAtLeast(1)
                val slot = size.width / barCount
                val barWidth = slot * 0.55f
                counts.forEachIndexed { index, value ->
                    val barHeight = size.height * (value.toFloat() / max.toFloat())
                    val left = index * slot + (slot - barWidth) / 2f
                    // 背景槽
                    drawRoundRect(
                        color = Color(0x223D5AA9),
                        topLeft = Offset(left, 0f),
                        size = Size(barWidth, size.height),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f),
                    )
                    if (value > 0) {
                        drawRoundRect(
                            color = chartColors[0],
                            topLeft = Offset(left, size.height - barHeight),
                            size = Size(barWidth, barHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f),
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                counts.indices.forEach { index ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = counts.getOrElse(index) { 0 }.toString(),
                            style = MaterialTheme.typography.labelSmall,
                        )
                        Text(
                            text = uiState.weeklyLabels.getOrElse(index) { "" },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

/** 难度分布：横向条形（1~5 星） */
@Composable
private fun DifficultyCard(uiState: StatsUiState) {
    val counts = uiState.difficultyCounts
    val max = (counts.drop(1).maxOrNull() ?: 0).coerceAtLeast(1)

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            (1..5).forEach { level ->
                val value = counts.getOrElse(level) { 0 }
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(text = "★".repeat(level), style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "$value 道",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { value.toFloat() / max.toFloat() },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            val unmarked = counts.getOrElse(0) { 0 }
            Text(
                text = "未标注难度的有 $unmarked 道",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** 复习阶段分布 */
@Composable
private fun StageCard(uiState: StatsUiState) {
    val counts = uiState.stageCounts
    val max = (counts.maxOrNull() ?: 0).coerceAtLeast(1)

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            counts.forEachIndexed { stage, value ->
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(text = stageLabel(stage), style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "$value 道",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { value.toFloat() / max.toFloat() },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
