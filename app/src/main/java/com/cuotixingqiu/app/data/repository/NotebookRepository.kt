package com.cuotixingqiu.app.data.repository

import androidx.room.withTransaction
import com.cuotixingqiu.app.data.db.AppDatabase
import com.cuotixingqiu.app.data.db.NotebookCount
import com.cuotixingqiu.app.data.db.NotebookDao
import com.cuotixingqiu.app.data.db.NotebookEntity
import com.cuotixingqiu.app.data.db.WrongQuestionDao
import kotlinx.coroutines.flow.Flow

/** 错题本分类（分册/专题）仓库 */
class NotebookRepository(
    private val db: AppDatabase,
    private val notebookDao: NotebookDao,
    private val questionDao: WrongQuestionDao,
) {

    fun observeAll(): Flow<List<NotebookEntity>> = notebookDao.observeAll()

    fun observeCounts(): Flow<List<NotebookCount>> = questionDao.observeNotebookCounts()

    suspend fun getAll(): List<NotebookEntity> = notebookDao.getAllOnce()

    /** 新建分类；名字重复时数据库会报错，界面层需要提前检查 */
    suspend fun create(name: String): Long {
        require(name.isNotBlank()) { "分类名不能为空" }
        return notebookDao.insert(
            NotebookEntity(name = name.trim(), createdAt = System.currentTimeMillis()),
        )
    }

    suspend fun rename(id: Long, newName: String) {
        require(newName.isNotBlank()) { "分类名不能为空" }
        val existing = notebookDao.getById(id) ?: return
        notebookDao.update(existing.copy(name = newName.trim()))
    }

    /** 删除分类：分类下的错题会回到“未分类”，错题本身不会被删除 */
    suspend fun delete(id: Long) {
        db.withTransaction {
            questionDao.clearNotebookReferences(id)
            notebookDao.deleteById(id)
        }
    }

    /** 清空所有分类（清空全部数据时使用） */
    suspend fun deleteAllNotebooks() {
        notebookDao.deleteAll()
    }
}
