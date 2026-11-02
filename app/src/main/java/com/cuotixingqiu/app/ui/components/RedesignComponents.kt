package com.cuotixingqiu.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cuotixingqiu.app.util.DailyReviewCount
import com.cuotixingqiu.app.util.TimeUtils

/** 五个页面共用的尺寸，保证边距、圆角和按钮高度一致。 */
object CuotiVisual {
    val PageHorizontal: Dp = 20.dp
    val SectionGap: Dp = 28.dp
    val CardCorner: Dp = 22.dp
    val ButtonHeight: Dp = 54.dp
    val ButtonCorner: Dp = 16.dp
    val ContentMaxWidth: Dp = 680.dp
}

/** 宽屏上让内容居中，手机竖屏保持 20dp 左右边距。 */
fun pageSidePadding(maxWidth: Dp): Dp =
    if (maxWidth > 760.dp) ((maxWidth - CuotiVisual.ContentMaxWidth) / 2f).coerceAtLeast(20.dp)
    else CuotiVisual.PageHorizontal

/** 统一的白色卡片：细边框、无厚重阴影。 */
@Composable
fun CuotiCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CuotiVisual.CardCorner),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f),
        ),
        shadowElevation = 0.dp,
    ) {
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
}

/** 页面里的分组标题，右侧可以放一个轻量的文字操作。 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        trailing?.invoke()
    }
}

/** 学科颜色圆点：在深色主题下自动提亮。 */
@Composable
fun SubjectDot(subject: String, size: Dp = 9.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(subjectChartColor(subject)),
    )
}

/** 浅蓝知识点标签。 */
@Composable
fun KnowledgeTag(
    text: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

/** 一行知识点标签；数量较多时可以横向滑动，不挤压题目正文。 */
@Composable
fun KnowledgeTagRow(
    tags: List<String>,
    modifier: Modifier = Modifier,
    maxTags: Int = 3,
) {
    if (tags.isEmpty()) return
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        tags.take(maxTags).forEach { tag ->
            KnowledgeTag(text = tag)
        }
        if (tags.size > maxTags) {
            Text(
                text = "+${tags.size - maxTags}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
fun reviewStatusColor(mastered: Boolean): Color =
    if (mastered) {
        if (isDarkScheme()) Color(0xFF7BD5AE) else Color(0xFF2F7D5B)
    } else {
        if (isDarkScheme()) Color(0xFFFFB86B) else Color(0xFFB35C00)
    }

/** 列表行底部的复习状态：小圆点 + 文字，保持克制。 */
@Composable
fun ReviewStatusLabel(
    mastered: Boolean,
    modifier: Modifier = Modifier,
    pendingText: String = "待复习",
    masteredText: String = "已掌握",
) {
    val color = reviewStatusColor(mastered)
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = if (mastered) masteredText else pendingText,
            style = MaterialTheme.typography.labelMedium,
            color = color,
        )
    }
}

/** 带细边框的白色搜索框。 */
@Composable
fun CuotiSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "搜索题目、知识点",
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                        inner()
                    }
                },
            )
            if (value.isNotEmpty()) {
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .clickable { onValueChange("") },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Clear,
                        contentDescription = "清空搜索",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

/** 学科筛选胶囊：选中是深色底、白字；未选中是白底细边框。 */
@Composable
fun SubjectFilterPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedBackground = if (isDarkScheme()) {
        MaterialTheme.colorScheme.primary
    } else {
        Color(0xFF17191D)
    }
    val selectedContent = if (isDarkScheme()) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        Color.White
    }
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) selectedBackground else MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = if (selected) selectedContent else MaterialTheme.colorScheme.onSurface,
        border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 15.dp, vertical = 9.dp),
        )
    }
}

/** 主操作按钮：全宽、蓝色、统一高度；[loading] 时显示转圈并自动禁用。 */
@Composable
fun PrimaryActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(CuotiVisual.ButtonHeight),
        enabled = enabled && !loading,
        shape = RoundedCornerShape(CuotiVisual.ButtonCorner),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.38f),
            disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f),
        ),
        contentPadding = PaddingValues(horizontal = 20.dp),
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            Spacer(modifier = Modifier.width(8.dp))
        } else if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                modifier = Modifier.size(19.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp),
        )
    }
}

/** 次要操作按钮：白底、细边框，与主按钮同高。 */
@Composable
fun SecondaryActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(CuotiVisual.ButtonHeight),
        shape = RoundedCornerShape(CuotiVisual.ButtonCorner),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        contentPadding = PaddingValues(horizontal = 18.dp),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

/** 一条很轻的分割线。 */
@Composable
fun ThinDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier,
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

/** 筛选图标（画三条滑杆，不依赖扩展图标库）。 */
@Composable
fun FilterSlidersIcon(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface,
) {
    Canvas(modifier = modifier.size(18.dp)) {
        val stroke = 1.5.dp.toPx()
        val y1 = size.height * 0.25f
        val y2 = size.height * 0.5f
        val y3 = size.height * 0.75f
        drawLine(tint, Offset(0f, y1), Offset(size.width, y1), stroke, StrokeCap.Round)
        drawLine(tint, Offset(0f, y2), Offset(size.width, y2), stroke, StrokeCap.Round)
        drawLine(tint, Offset(0f, y3), Offset(size.width, y3), stroke, StrokeCap.Round)
        val r = 2.1.dp.toPx()
        drawCircle(tint, r, Offset(size.width * 0.34f, y1))
        drawCircle(tint, r, Offset(size.width * 0.66f, y2))
        drawCircle(tint, r, Offset(size.width * 0.42f, y3))
    }
}

/** 品牌星球小标识：实心星球 + 一圈倾斜轨道。 */
@Composable
fun PlanetMark(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
) {
    Canvas(modifier = modifier.size(26.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension * 0.27f
        drawCircle(color = tint, radius = radius, center = center)
        drawOval(
            color = tint.copy(alpha = 0.58f),
            topLeft = Offset(size.width * 0.08f, size.height * 0.36f),
            size = Size(size.width * 0.84f, size.height * 0.30f),
            style = Stroke(width = 1.4.dp.toPx()),
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.32f),
            radius = radius * 0.30f,
            center = Offset(center.x - radius * 0.28f, center.y - radius * 0.24f),
        )
    }
}

/** 近 7 天复习柱状图：只用柱子和星期文字，没有坐标轴。 */
@Composable
fun WeekReviewChart(
    days: List<DailyReviewCount>,
    modifier: Modifier = Modifier,
) {
    if (days.isEmpty()) return
    val max = remember(days) { days.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1 }
    val lastIndex = days.lastIndex
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(104.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        days.forEachIndexed { index, day ->
            val ratio = day.count.toFloat() / max.toFloat()
            val barHeight = if (day.count <= 0) 4.dp else (54.dp * (0.28f + 0.72f * ratio))
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
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
                    text = TimeUtils.weekdayNarrow(day.date),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (index == lastIndex) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** 页面底部空状态卡片：用于没有数据但仍希望保持页面骨架的场景。 */
@Composable
fun QuietEmptyCard(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    CuotiCard(modifier = modifier, contentPadding = PaddingValues(22.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onAction)
                    .padding(vertical = 4.dp),
            )
        }
    }
}

/** 顶部小标题（例如“题目”“错误原因”）。 */
@Composable
fun QuietSectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        color = color,
        modifier = modifier,
    )
}
