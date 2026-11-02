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
 *
 * @param key 同一个页面里需要用多份不同 ViewModel 时（例如大屏的列表-详情分栏，
 *            每选中一道题就要一个对应 questionId 的详情 ViewModel），传不同的 key 即可。
 */
@Composable
inline fun <reified VM : ViewModel> appViewModel(
    key: String? = null,
    noinline create: (AppContainer) -> VM,
): VM {
    val container = rememberAppContainer()
    return viewModel(
        key = key,
        factory = viewModelFactory { initializer { create(container) } },
    )
}
