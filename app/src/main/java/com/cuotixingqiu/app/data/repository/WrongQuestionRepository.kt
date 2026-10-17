package com.cuotixingqiu.app.data.repository

import androidx.room.withTransaction
import com.cuotixingqiu.app.data.db.AppDatabase
import com.cuotixingqiu.app.data.db.NotebookCount
import com.cuotixingqiu.app.data.db.SubjectCount
import com.cuotixingqiu.app.data.db.WrongQuestionDao
import com.cuotixingqiu.app.data.db.WrongQuestionEntity
import com.cuotixingqiu.app.data.image.ImageStore
import com.cuotixingqiu.app.domain.ReviewResult
import com.cuotixingqiu.app.domain.ReviewScheduler
import com.cuotixingqiu.app.domain.ReviewStatus
import kotlinx.coroutines.flow.Flow

/**
 * 错题仓库：界面层唯一允许访问数据的地方。
 *
 * 好处：
 *  - 界面代码不直接写 SQL，逻辑集中在一处；
 *  - 新增错题时统一补上“创建时间、首次复习时间”等规则；
 *  - 导入数据时保证“要么全部成功、要么全部不写”。
 */
class WrongQuestionRepository(
    private val db: AppDatabase,
    private val questionDao: WrongQuestionDao,
    private val imageStore: ImageStore,
) {

    // ---------------- 查询 ----------------

    fun observeAll(): Flow<List<WrongQuestionEntity>> = questionDao.observeAll()

    fun observeById(id: Long): Flow<WrongQuestionEntity?> = questionDao.observeById(id)

    fun observeRecent(limit: Int): Flow<List<WrongQuestionEntity>> = questionDao.observeRecent(limit)

    fun observeFiltered(
        subject: String?,
        notebookId: Long?,
        status: String?,
        minDifficulty: Int,
        search: String,
        sort: Int,
    ): Flow<List<WrongQuestionEntity>> =
        questionDao.observeFiltered(subject, notebookId, status, minDifficulty, search.trim(), sort)

    fun observeTotalCount(): Flow<Int> = questionDao.observeTotalCount()

    fun observeCountCreatedSince(startMillis: Long): Flow<Int> =
        questionDao.observeCountCreatedSince(startMillis)

    fun observeCountByStatus(status: ReviewStatus): Flow<Int> =
        questionDao.observeCountByStatus(status.name)

    fun observeDueCount(dueUntil: Long): Flow<Int> =
        questionDao.observeDueCount(ReviewStatus.MASTERED.name, dueUntil)

    fun observeReviewedCountSince(startMillis: Long): Flow<Int> =
        questionDao.observeReviewedCountSince(startMillis)

    fun observeSubjectCounts(): Flow<List<SubjectCount>> = questionDao.observeSubjectCounts()

    /** 各分类下的错题数量（分类管理页、统计页使用） */
    fun observeNotebookCounts(): Flow<List<NotebookCount>> = questionDao.observeNotebookCounts()

    fun observeDueList(dueUntil: Long): Flow<List<WrongQuestionEntity>> =
        questionDao.observeDueList(ReviewStatus.MASTERED.name, dueUntil)

    /** 未来的复习安排（从 from 到 to 之间要到期的错题） */
    fun observeUpcoming(from: Long, to: Long, limit: Int = 20): Flow<List<WrongQuestionEntity>> =
        questionDao.observeUpcoming(ReviewStatus.MASTERED.name, from, to, limit)

    fun observeReviewTimestampsSince(since: Long): Flow<List<Long>> =
        questionDao.observeReviewTimestampsSince(since)

    suspend fun getById(id: Long): WrongQuestionEntity? = questionDao.getById(id)

    /** 导出数据用：取出全部错题 */
    suspend fun getAllOnce(): List<WrongQuestionEntity> = questionDao.getAllOnce()

    // ---------------- 新增 / 修改 ----------------

    /**
     * 新增一道错题。
     * 会自动：补上创建/更新时间、把状态设为“需要复习”、安排首次复习（次日）。
     */
    suspend fun add(item: WrongQuestionEntity): Long {
        val now = System.currentTimeMillis()
        val prepared = item.copy(
            id = 0L,
            status = ReviewStatus.NEEDS_REVIEW.name,
            reviewStage = 0,
            reviewCount = 0,
            lastReviewResult = null,
            lastReviewedAt = null,
            nextReviewAt = ReviewScheduler.initialNextReviewAt(now),
            createdAt = now,
            updatedAt = now,
        )
        return questionDao.insert(prepared)
    }

    /** 保存编辑后的错题（只更新内容，不改变复习进度） */
    suspend fun update(item: WrongQuestionEntity) {
        questionDao.update(item.copy(updatedAt = System.currentTimeMillis()))
    }

    /** 记录一次复习结果：按规则计算下次复习时间 */
    suspend fun applyReview(question: WrongQuestionEntity, result: ReviewResult) {
        val now = System.currentTimeMillis()
        val schedule = ReviewScheduler.schedule(result, question.reviewStage, now)
        questionDao.applyReviewResult(
            id = question.id,
            status = schedule.status.name,
            stage = schedule.stage,
            lastResult = result.name,
            nextReviewAt = schedule.nextReviewAt,
            now = now,
        )
    }

    /** 详情页：手动标记为“已掌握”或“需要复习” */
    suspend fun setStatus(id: Long, status: ReviewStatus) {
        val now = System.currentTimeMillis()
        questionDao.updateStatus(id, status.name, now)
    }

    /** 详情页：“重新加入复习计划”（阶段归零，明天再复习） */
    suspend fun reopenForReview(id: Long) {
        val now = System.currentTimeMillis()
        questionDao.reopenForReview(
            id = id,
            status = ReviewStatus.NEEDS_REVIEW.name,
            stage = 0,
            nextReviewAt = ReviewScheduler.initialNextReviewAt(now),
            now = now,
        )
    }

    /** 列表页：批量标记状态 */
    suspend fun setStatusForIds(ids: List<Long>, status: ReviewStatus) {
        if (ids.isEmpty()) return
        val now = System.currentTimeMillis()
        questionDao.updateStatusByIds(ids, status.name, now)
    }

    // ---------------- 删除 ----------------

    /** 删除一道错题：连同它的图片文件一起删掉（避免私有目录里留下没用的图片） */
    suspend fun delete(id: Long) {
        val item = questionDao.getById(id) ?: return
        questionDao.deleteById(id)
        item.imageNames.forEach { imageStore.delete(it) }
    }

    suspend fun deleteByIds(ids: List<Long>) {
        if (ids.isEmpty()) return
        val items = questionDao.getByIds(ids)
        questionDao.deleteByIds(ids)
        items.forEach { question -> question.imageNames.forEach { imageStore.delete(it) } }
    }

    /**
     * 清空所有错题（调用方必须已经做过二次确认）。
     * 同时会删除图片目录里的所有图片，做到“清空 = 真正清空”。
     */
    suspend fun deleteAll() {
        db.withTransaction { questionDao.deleteAll() }
        imageStore.deleteAll()
    }

    // ---------------- 导入 ----------------

    /**
     * 导入错题（合并模式：生成新的 id，绝不覆盖现有数据）。
     * 整个导入放在一个数据库事务里：任何一条失败，全部回滚。
     */
    suspend fun importQuestions(items: List<WrongQuestionEntity>) {
        if (items.isEmpty()) return
        val fresh = items.map { it.copy(id = 0L) }
        db.withTransaction { questionDao.insertAll(fresh) }
    }
}
