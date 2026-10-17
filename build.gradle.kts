// 根构建脚本：只声明“构建插件”的版本，不在这里直接使用。
//
// 关键说明（AGP 9 的变化）：
//   AGP 9 起“内置 Kotlin 支持”，因此不需要（也不允许）再应用 org.jetbrains.kotlin.android 插件。
//   AGP 内置的 Kotlin Gradle 插件（KGP）默认版本较旧，这里按官方文档通过 buildscript
//   把内置 Kotlin 使用的 KGP 升级到与依赖库匹配的 2.4.21，并把 KSP 指定为 2.3.12。
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.21")
        classpath("com.google.devtools.ksp:symbol-processing-gradle-plugin:2.3.12")
    }
}

plugins {
    id("com.android.application") version "9.4.1" apply false
    id("com.google.devtools.ksp") version "2.3.12" apply false
    // Compose 编译器插件（版本必须与 Kotlin 版本一致）
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.21" apply false
    // JSON 序列化插件（错题数据导出/导入用）
    id("org.jetbrains.kotlin.plugin.serialization") version "2.4.21" apply false
}
