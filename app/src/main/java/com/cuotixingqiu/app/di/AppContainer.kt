package com.cuotixingqiu.app.di

import android.content.Context
import androidx.room.Room
import com.cuotixingqiu.app.data.backup.BackupManager
import com.cuotixingqiu.app.data.db.AppDatabase
import com.cuotixingqiu.app.data.export.PaperExporter
import com.cuotixingqiu.app.data.image.ImageStore
import com.cuotixingqiu.app.data.repository.NotebookRepository
import com.cuotixingqiu.app.data.repository.WrongQuestionRepository
import com.cuotixingqiu.app.data.settings.SettingsRepository

/**
 * 依赖容器：应用启动时创建一次，之后所有页面都从这里拿数据库和仓库。
 *
 * 为什么不用 Hilt 之类的框架？
 * 这个项目只有几个依赖对象，手写容器更直观、出错时更好排查，
 * 对初学者来说也更容易理解。
 */
class AppContainer(context: Context) {

    /** 全程只用 applicationContext，避免不小心持有 Activity 造成内存泄漏 */
    private val appContext: Context = context.applicationContext

    /** 给需要读写文件（导出/导入、生成 PDF）的 ViewModel 使用 */
    val applicationContext: Context get() = appContext

    private val database: AppDatabase = Room.databaseBuilder(
        appContext,
        AppDatabase::class.java,
        "cuoti-planet.db",
    ).build()

    /** 图片仓库：保存/读取错题图片（应用私有目录） */
    val imageStore: ImageStore = ImageStore(appContext)

    /** 设置仓库：主题等开关 */
    val settingsRepository: SettingsRepository = SettingsRepository(appContext)

    val wrongQuestionRepository: WrongQuestionRepository =
        WrongQuestionRepository(database, database.wrongQuestionDao(), imageStore)

    val notebookRepository: NotebookRepository =
        NotebookRepository(database, database.notebookDao(), database.wrongQuestionDao())

    /** 导出/导入 JSON 备份 */
    val backupManager: BackupManager = BackupManager(
        context = appContext,
        db = database,
        questionDao = database.wrongQuestionDao(),
        notebookDao = database.notebookDao(),
        imageStore = imageStore,
    )

    /** 组卷导出 PDF */
    val paperExporter: PaperExporter = PaperExporter(appContext, imageStore)
}
