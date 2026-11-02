package com.cuotixingqiu.app.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cuotixingqiu.app.domain.Subject
import com.cuotixingqiu.app.ui.appViewModel
import com.cuotixingqiu.app.ui.components.CuotiCard
import com.cuotixingqiu.app.ui.components.CuotiVisual
import com.cuotixingqiu.app.ui.components.QuietEmptyCard
import com.cuotixingqiu.app.ui.components.ScreenScaffold
import com.cuotixingqiu.app.ui.components.SectionHeader
import com.cuotixingqiu.app.ui.components.SubjectDot
import com.cuotixingqiu.app.ui.components.ThinDivider
import com.cuotixingqiu.app.ui.components.rememberDockClearance
import com.cuotixingqiu.app.ui.components.subjectChartColor
import com.cuotixingqiu.app.ui.components.withoutTopInset

/** 图表配色统一用学科色（见 [subjectChartColor]），与列表里的学科标签保持一致 */

/**
 * 统计页：用 Canvas 自己画的图表（不依赖任何第三方图表库）。
 * 包含：概览数字、学科分布环形图、近 7 天复习次数、难度与复习阶段分布。
 *
 * v1.3.8：只重做视觉（暖灰底 + 白卡 + 品牌蓝紫），所有数字仍然来自真实数据库查询。
 */
@Composable
fun StatsScreen(onBack: () -> Unit) {
    val viewModel: StatsViewModel = appViewModel { container ->
        StatsViewModel(container.wrongQuestionRepository, container.notebookRepository)
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ScreenScaffold(
        title = "统计图表",
        subtitle = "全部由真实错题数据统计",
        onBack = onBack,
        maxContentWidth = 680.dp,
        contentBehindTopBar = true,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding.withoutTopInset()),
            contentPadding = PaddingValues(
                start = CuotiVisual.PageHorizontal,
                end = CuotiVisual.PageHorizontal,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = rememberDockClearance() + 26.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { OverviewCard(uiState) }

            if (uiState.totalCount == 0) {
                item {
                    QuietEmptyCard(
                        title = "还没有可以统计的数据",
                        message = "添加几道错题之后，这里会显示学科分布、复习趋势和难度分布。",
                    )
                }
                return@LazyColumn
            }

            item { Spacer(modifier = Modifier.height(6.dp)) }
            item { SectionHeader(title = "学科分布") }
            item { SubjectPieCard(uiState) }

            item { Spacer(modifier = Modifier.height(6.dp)) }
            item { SectionHeader(title = "近 7 天复习") }
            item { WeeklyBarCard(uiState) }

            item { Spacer(modifier = Modifier.height(6.dp)) }
            item { SectionHeader(title = "难度分布") }
            item { DifficultyCard(uiState) }

            item { Spacer(modifier = Modifier.height(6.dp)) }
            item { SectionHeader(title = "复习阶段") }
            item { StageCard(uiState) }
        }
    }
}

/** 概览：四个核心数字，两行两列。 */
@Composable
private fun OverviewCard(uiState: StatsUiState) {
    CuotiCard(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            StatMetric("错题总数", uiState.totalCount.toString(), Modifier.weight(1f))
            StatMetric("已掌握", uiState.masteredCount.toString(), Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(16.dp))
        ThinDivider()
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            StatMetric("需要复习", uiState.needsReviewCount.toString(), Modifier.weight(1f))
            StatMetric("今日待复习", uiState.dueTodayCount.toString(), Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall.copy(fontSize = 26.sp),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
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
    // 图表颜色在组合作用域里先算好（Canvas 的绘制 lambda 里不能调用 @Composable）
    val sliceColors = items.map { subjectChartColor(it.first) }

    CuotiCard {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(176.dp),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.size(150.dp)) {
                val stroke = 30f
                val diameter = minOf(size.width, size.height) - stroke
                val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                val arcSize = Size(diameter, diameter)
                var startAngle = -90f
                items.forEachIndexed { index, item ->
                    val sweep = 360f * item.second.toFloat() / total.toFloat()
                    drawArc(
                        color = sliceColors[index],
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
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = total.toString(),
                    style = MaterialTheme.typography.headlineSmall.copy(fontSize = 27.sp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "道错题",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        ThinDivider()
        Spacer(modifier = Modifier.height(4.dp))

        items.forEachIndexed { index, item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(sliceColors[index]),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = item.first,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "${item.second} 道",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = uiState.percentText(item.second),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(44.dp),
                )
            }
        }
    }
}

/** 近 7 天复习次数：柱状图（今天用品牌色，其余淡一些） */
@Composable
private fun WeeklyBarCard(uiState: StatsUiState) {
    val counts = uiState.weeklyReviewCounts
    val max = (counts.maxOrNull() ?: 0).coerceAtLeast(1)
    val lastIndex = counts.lastIndex

    CuotiCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(132.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            counts.forEachIndexed { index, value ->
                val ratio = value.toFloat() / max.toFloat()
                val barHeight = if (value <= 0) 4.dp else (72.dp * (0.26f + 0.74f * ratio))
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = value.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (value > 0) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        Box(
                            modifier = Modifier
                                .width(18.dp)
                                .height(barHeight)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (index == lastIndex) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
                                    },
                                ),
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = uiState.weeklyLabels.getOrElse(index) { "" },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (index == lastIndex) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "近 7 天一共复习 ${uiState.weeklyTotal} 次",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** 难度分布：1~5 星 + 未标注 */
@Composable
private fun DifficultyCard(uiState: StatsUiState) {
    val counts = uiState.difficultyCounts
    val max = (counts.drop(1).maxOrNull() ?: 0).coerceAtLeast(1)

    CuotiCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            (1..5).forEach { level ->
                val value = counts.getOrElse(level) { 0 }
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "★".repeat(level),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "$value 道",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    QuietBar(progress = value.toFloat() / max.toFloat())
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

    CuotiCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            counts.forEachIndexed { stage, value ->
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = stageLabel(stage),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "$value 道",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    QuietBar(progress = value.toFloat() / max.toFloat())
                }
            }
        }
    }
}

/** 细进度条：品牌色 + 浅轨道，统一圆角。 */
@Composable
private fun QuietBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        val safe = progress.coerceIn(0f, 1f)
        if (safe > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(safe)
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}
