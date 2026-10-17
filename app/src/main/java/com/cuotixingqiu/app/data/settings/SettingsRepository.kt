package com.cuotixingqiu.app.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** 主题模式 */
enum class ThemeMode(val displayName: String) {
    SYSTEM("跟随系统"),
    LIGHT("浅色"),
    DARK("深色");

    companion object {
        fun fromName(value: String?): ThemeMode =
            entries.firstOrNull { it.name == value } ?: SYSTEM
    }
}

// DataStore 的文件名（保存在应用私有目录，卸载应用时一起删除）
private val Context.settingsDataStore by preferencesDataStore(name = "settings")

/**
 * 设置仓库：保存主题相关的少量开关。
 * 使用 DataStore（安卓官方推荐的轻量存储），保存后立刻生效，重启应用依然保留。
 */
class SettingsRepository(private val context: Context) {

    private val themeModeKey = stringPreferencesKey("theme_mode")
    private val dynamicColorKey = booleanPreferencesKey("dynamic_color")

    /** 主题模式：跟随系统 / 浅色 / 深色 */
    val themeMode: Flow<ThemeMode> = context.settingsDataStore.data.map { preferences ->
        ThemeMode.fromName(preferences[themeModeKey])
    }

    /** 是否使用“跟随壁纸取色”（仅 Android 12 及以上有效） */
    val dynamicColor: Flow<Boolean> = context.settingsDataStore.data.map { preferences ->
        preferences[dynamicColorKey] ?: false
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { preferences ->
            preferences[themeModeKey] = mode.name
        }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[dynamicColorKey] = enabled
        }
    }
}
