package com.cuotixingqiu.app.data.db

import androidx.room.TypeConverter
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * Room 类型转换器：
 * 把“字符串列表”（知识点、图片文件名）自动转换成数据库里的 JSON 文本，读取时再转回来。
 */
class Converters {

    private val json = Json { ignoreUnknownKeys = true }
    private val listSerializer = ListSerializer(String.serializer())

    @TypeConverter
    fun fromStringList(value: List<String>): String = json.encodeToString(listSerializer, value)

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        // 数据损坏时不崩溃，返回空列表
        return runCatching { json.decodeFromString(listSerializer, value) }.getOrDefault(emptyList())
    }
}
