package com.cuotixingqiu.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cuotixingqiu.app.data.settings.ThemeMode
import com.cuotixingqiu.app.ui.CuotiApp
import com.cuotixingqiu.app.ui.MainViewModel
import com.cuotixingqiu.app.ui.appViewModel
import com.cuotixingqiu.app.ui.theme.CuotiPlanetTheme

/**
 * 应用唯一的 Activity：所有页面都在这一个 Activity 里用 Compose 绘制。
 *
 * 这里还负责“主题”：先从设置里读出用户的选择（跟随系统 / 浅色 / 深色），
 * 再把主题传给所有页面。
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 全面屏（edge-to-edge）显示
        enableEdgeToEdge()
        setContent {
            val mainViewModel: MainViewModel = appViewModel { container ->
                MainViewModel(container.settingsRepository)
            }
            val themeMode by mainViewModel.themeMode.collectAsStateWithLifecycle()
            val dynamicColor by mainViewModel.dynamicColor.collectAsStateWithLifecycle()

            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            CuotiPlanetTheme(darkTheme = darkTheme, dynamicColor = dynamicColor) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    CuotiApp()
                }
            }
        }
    }
}
