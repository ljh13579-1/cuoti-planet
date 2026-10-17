package com.cuotixingqiu.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cuotixingqiu.app.CuotiApplication
import com.cuotixingqiu.app.di.AppContainer

/**
 * 从 Compose 界面里拿到“依赖容器”（数据库、仓库等）。
 * 容器在 Application 里只创建一次，所以这里只是把它取出来，不会重复创建。
 */
@Composable
fun rememberAppContainer(): AppContainer {
    val context = LocalContext.current
    return remember(context) { (context.applicationContext as CuotiApplication).container }
}

/**
 * 创建页面的 ViewModel 的小工具，用法：
 *
 * ```
 * val viewModel: HomeViewModel = appViewModel { container -> HomeViewModel(container.wrongQuestionRepository) }
 * ```
 *
 * 这样就不用为每个 ViewModel 单独写一个 Factory 类，初学者也容易看懂。
 */
@Composable
inline fun <reified VM : ViewModel> appViewModel(
    noinline create: (AppContainer) -> VM,
): VM {
    val container = rememberAppContainer()
    return viewModel(factory = viewModelFactory { initializer { create(container) } })
}
