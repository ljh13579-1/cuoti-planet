package com.cuotixingqiu.app

import android.app.Application
import com.cuotixingqiu.app.di.AppContainer

/**
 * 应用的 Application 类（整个 App 生命周期内只有一个实例）。
 *
 * 在这里创建“依赖容器”：数据库、仓库等对象只会创建一次，
 * 各个页面通过 container 拿到它们。
 */
class CuotiApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
