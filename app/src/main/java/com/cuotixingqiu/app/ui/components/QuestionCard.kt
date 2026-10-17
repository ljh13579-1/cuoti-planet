package com.cuotixingqiu.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cuotixingqiu.app.data.db.WrongQuestionEntity
import com.cuotixingqiu.app.domain.ReviewStatus
import com.cuotixingqiu.app.util.TimeUtils

/**
 * 错题卡片：首页、列表、复习页都用它来显示一道错题，保证风格一致。
 *
 * @param onClick      点击卡片（打开详情）
 * @param onLongClick  长按卡片（进入多选模式）
 * @param selectionMode 是否处于多选模式（此时右边显示勾选框）
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun QuestionCard(
    question: WrongQuestionEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    notebookName: String? = null,
    selectionMode: Boolean = false,
    selected: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    trailingText: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val statusInfo = reviewStatusInfo(question)
    val mastered = question.status == ReviewStatus.MASTERED.name

    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
                onLongClickLabel = "多选",
            ),
        border = if (selected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            null
        },
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
        ),
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = questionDisplayTitle(question),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                val preview = question.content.trim()
                if (preview.isNotEmpty() && preview != questionDisplayTitle(question)) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = preview,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    SubjectBadge(subject = question.subject)
                    if (notebookName != null) {
                        SmallBadge(
                            text = notebookName,
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                    }
                    if (question.difficulty > 0) {
                        DifficultyStars(difficulty = question.difficulty, starSize = 14.dp)
                    }
                    if (question.imageNames.isNotEmpty()) {
                        SmallBadge(text = "图 ${question.imageNames.size}")
                    }
                }

                val knowledge = question.knowledgePoints
                if (knowledge.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = knowledge.joinToString("、"),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (mastered) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "已掌握",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    } else {
                        Text(
                            text = statusInfo.first,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (statusInfo.second) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = TimeUtils.formatDate(question.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (trailingText != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = trailingText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (selectionMode) {
                Checkbox(
                    checked = selected,
                    onCheckedChange = null,
                    modifier = Modifier.padding(start = 8.dp),
                )
            } else if (trailing != null) {
                Box(modifier = Modifier.padding(start = 4.dp)) {
                    trailing()
                }
            }
        }
    }
}
