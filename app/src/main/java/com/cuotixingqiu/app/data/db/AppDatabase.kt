package com.cuotixingqiu.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * 应用的本地数据库。
 *
 *  - 数据库文件保存在应用私有目录里（卸载应用时会一起删除，
 *    所以请定期在「我的 → 数据管理」里导出 JSON 做备份）。
 *  - version = 1：第一版结构。以后若要改表结构，需要升级版本号并写迁移代码。
 */
@Database(
    entities = [
        WrongQuestionEntity::class,
        NotebookEntity::class,
    ],
    version = 1,
    exportSchema = true, // 导出表结构（保存在 app/schemas 目录，便于以后写迁移）
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun wrongQuestionDao(): WrongQuestionDao

    abstract fun notebookDao(): NotebookDao
}
