package com.cuotixingqiu.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** 错题本分类表的数据访问对象 */
@Dao
interface NotebookDao {

    @Query("SELECT * FROM notebooks ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<NotebookEntity>>

    @Query("SELECT * FROM notebooks ORDER BY createdAt ASC")
    suspend fun getAllOnce(): List<NotebookEntity>

    @Query("SELECT * FROM notebooks WHERE id = :id")
    suspend fun getById(id: Long): NotebookEntity?

    @Insert
    suspend fun insert(item: NotebookEntity): Long

    @Update
    suspend fun update(item: NotebookEntity)

    @Query("DELETE FROM notebooks WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM notebooks")
    suspend fun deleteAll()
}
