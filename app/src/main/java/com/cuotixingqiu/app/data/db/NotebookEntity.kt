package com.cuotixingqiu.app.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 错题本分类（例如“高一上”“导数专题”）。
 *
 * 删除分类时，只会把错题里的 notebookId 置空（回到“未分类”），
 * 不会删除任何错题。
 */
@Entity(
    tableName = "notebooks",
    indices = [Index(value = ["name"], unique = true)],
)
data class NotebookEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    /** 分类名称（不允许重名） */
    val name: String,

    val createdAt: Long,
)

/** 统计用：某个学科下的错题数量（不是数据库表，只是查询结果的“投影”） */
data class SubjectCount(
    val subject: String,
    val count: Int,
)

/** 统计用：某个分类下的错题数量（notebookId 为 null 表示“未分类”） */
data class NotebookCount(
    val notebookId: Long?,
    val count: Int,
)
