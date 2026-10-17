package com.cuotixingqiu.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * 错题表的“数据访问对象”（DAO）：所有对数据库的读写都在这里定义。
 *
 * 返回 Flow 的查询会“自动刷新”：数据库内容一变，界面就收到新数据，
 * 不需要手动刷新列表。
 */
@Dao
interface WrongQuestionDao {

    // ---------------- 查询 ----------------

    @Query("SELECT * FROM wrong_questions ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<WrongQuestionEntity>>

    @Query("SELECT * FROM wrong_questions WHERE id = :id")
    fun observeById(id: Long): Flow<WrongQuestionEntity?>

    @Query("SELECT * FROM wrong_questions WHERE id = :id")
    suspend fun getById(id: Long): WrongQuestionEntity?

    /** 批量删除前用：一次性取出这些错题（为了知道它们带哪些图片） */
    @Query("SELECT * FROM wrong_questions WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Long>): List<WrongQuestionEntity>

    @Query("SELECT * FROM wrong_questions ORDER BY createdAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<WrongQuestionEntity>>

    /**
     * 列表页的“搜索 + 筛选 + 排序”查询。
     * 传入 null（或 0）表示该条件不生效，这样一条 SQL 就能覆盖所有筛选组合。
     *
     * @param subject      学科（null = 全部）
     * @param notebookId   错题本分类（null = 全部）
     * @param status       复习状态（null = 全部）
     * @param minDifficulty 只看难度 >= 该值（0 = 不限）
     * @param search       搜索词，空串 = 不过滤
     * @param sort         0 = 最新优先，1 = 最早优先，2 = 待复习优先
     */
    @Query(
        """
        SELECT * FROM wrong_questions
        WHERE (:subject IS NULL OR subject = :subject)
          AND (:notebookId IS NULL OR notebookId = :notebookId)
          AND (:status IS NULL OR status = :status)
          AND (:minDifficulty = 0 OR difficulty >= :minDifficulty)
          AND (:search = ''
               OR title LIKE '%' || :search || '%'
               OR content LIKE '%' || :search || '%'
               OR knowledgePoints LIKE '%' || :search || '%'
               OR reason LIKE '%' || :search || '%'
               OR solution LIKE '%' || :search || '%'
               OR summary LIKE '%' || :search || '%')
        ORDER BY
          CASE WHEN :sort = 0 THEN createdAt END DESC,
          CASE WHEN :sort = 1 THEN createdAt END ASC,
          CASE WHEN :sort = 2 THEN COALESCE(nextReviewAt, 9223372036854775807) END ASC,
          id DESC
        """
    )
    fun observeFiltered(
        subject: String?,
        notebookId: Long?,
        status: String?,
        minDifficulty: Int,
        search: String,
        sort: Int,
    ): Flow<List<WrongQuestionEntity>>

    /** 错题总数 */
    @Query("SELECT COUNT(*) FROM wrong_questions")
    fun observeTotalCount(): Flow<Int>

    /** 某个时间点之后新增的数量（首页“今日新增”用） */
    @Query("SELECT COUNT(*) FROM wrong_questions WHERE createdAt >= :startMillis")
    fun observeCountCreatedSince(startMillis: Long): Flow<Int>

    /** 指定复习状态的数量 */
    @Query("SELECT COUNT(*) FROM wrong_questions WHERE status = :status")
    fun observeCountByStatus(status: String): Flow<Int>

    /** 待复习数量：不是“已掌握”，且下次复习时间已到 */
    @Query(
        """
        SELECT COUNT(*) FROM wrong_questions
        WHERE status != :masteredStatus
          AND nextReviewAt IS NOT NULL
          AND nextReviewAt <= :dueUntil
        """
    )
    fun observeDueCount(masteredStatus: String, dueUntil: Long): Flow<Int>

    /** 今天已复习的数量 */
    @Query(
        """
        SELECT COUNT(*) FROM wrong_questions
        WHERE lastReviewedAt IS NOT NULL AND lastReviewedAt >= :startMillis
        """
    )
    fun observeReviewedCountSince(startMillis: Long): Flow<Int>

    /** 各学科的错题数量（首页/统计页用） */
    @Query("SELECT subject AS subject, COUNT(*) AS count FROM wrong_questions GROUP BY subject")
    fun observeSubjectCounts(): Flow<List<SubjectCount>>

    /** 各分类的错题数量（分类管理页用） */
    @Query("SELECT notebookId AS notebookId, COUNT(*) AS count FROM wrong_questions GROUP BY notebookId")
    fun observeNotebookCounts(): Flow<List<NotebookCount>>

    /** 待复习列表：按下次复习时间从早到晚 */
    @Query(
        """
        SELECT * FROM wrong_questions
        WHERE status != :masteredStatus
          AND nextReviewAt IS NOT NULL
          AND nextReviewAt <= :dueUntil
        ORDER BY nextReviewAt ASC
        """
    )
    fun observeDueList(masteredStatus: String, dueUntil: Long): Flow<List<WrongQuestionEntity>>

    /** 未来的复习安排（复习页“接下来几天”用） */
    @Query(
        """
        SELECT * FROM wrong_questions
        WHERE status != :masteredStatus
          AND nextReviewAt IS NOT NULL
          AND nextReviewAt > :from
          AND nextReviewAt <= :to
        ORDER BY nextReviewAt ASC
        LIMIT :limit
        """
    )
    fun observeUpcoming(
        masteredStatus: String,
        from: Long,
        to: Long,
        limit: Int,
    ): Flow<List<WrongQuestionEntity>>

    /** 用于“近 7 天复习趋势”的原始时间戳列表 */
    @Query(
        """
        SELECT lastReviewedAt FROM wrong_questions
        WHERE lastReviewedAt IS NOT NULL AND lastReviewedAt >= :since
        """
    )
    fun observeReviewTimestampsSince(since: Long): Flow<List<Long>>

    /** 导出数据时使用：一次性取出全部错题 */
    @Query("SELECT * FROM wrong_questions ORDER BY createdAt ASC")
    suspend fun getAllOnce(): List<WrongQuestionEntity>

    // ---------------- 新增 / 修改 ----------------

    @Insert
    suspend fun insert(item: WrongQuestionEntity): Long

    @Insert
    suspend fun insertAll(items: List<WrongQuestionEntity>)

    @Update
    suspend fun update(item: WrongQuestionEntity)

    /** 保存一次复习结果：更新状态、阶段、次数和下次复习时间 */
    @Query(
        """
        UPDATE wrong_questions
        SET status = :status,
            reviewStage = :stage,
            lastReviewResult = :lastResult,
            lastReviewedAt = :now,
            reviewCount = reviewCount + 1,
            nextReviewAt = :nextReviewAt,
            updatedAt = :now
        WHERE id = :id
        """
    )
    suspend fun applyReviewResult(
        id: Long,
        status: String,
        stage: Int,
        lastResult: String,
        nextReviewAt: Long,
        now: Long,
    )

    /** 详情页“标记为已掌握 / 需要复习” */
    @Query("UPDATE wrong_questions SET status = :status, updatedAt = :now WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, now: Long)

    /** 详情页“重新加入复习计划”：回到阶段 0，并安排到次日复习 */
    @Query(
        """
        UPDATE wrong_questions
        SET status = :status, reviewStage = :stage, nextReviewAt = :nextReviewAt, updatedAt = :now
        WHERE id = :id
        """
    )
    suspend fun reopenForReview(id: Long, status: String, stage: Int, nextReviewAt: Long, now: Long)

    /** 列表页批量标记 */
    @Query("UPDATE wrong_questions SET status = :status, updatedAt = :now WHERE id IN (:ids)")
    suspend fun updateStatusByIds(ids: List<Long>, status: String, now: Long)

    // ---------------- 删除 ----------------

    @Query("DELETE FROM wrong_questions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM wrong_questions WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM wrong_questions")
    suspend fun deleteAll()

    /** 删除分类时，把属于它的错题放回“未分类”（错题本身不删） */
    @Query("UPDATE wrong_questions SET notebookId = NULL WHERE notebookId = :notebookId")
    suspend fun clearNotebookReferences(notebookId: Long)
}
