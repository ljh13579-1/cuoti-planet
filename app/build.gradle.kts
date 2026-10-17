// app 模块的构建脚本
//
// 所有依赖版本集中写在下面，方便以后升级：只需要改这里的版本号。
plugins {
    id("com.android.application")
    id("com.google.devtools.ksp")                     // Room 的注解处理器（KSP）
    id("org.jetbrains.kotlin.plugin.compose")          // Compose 编译器
    id("org.jetbrains.kotlin.plugin.serialization")    // JSON 序列化（导出/导入）
}

android {
    namespace = "com.cuotixingqiu.app"

    // 编译时使用的 Android SDK 版本（安装 SDK 时用 platforms;android-37.0）
    // 说明：Compose 等库要求最低用 android-37 编译，所以这里必须 >= 37。
    compileSdk = 37

    defaultConfig {
        applicationId = "com.cuotixingqiu.app"
        minSdk = 26          // 最低支持 Android 8.0
        targetSdk = 36       // 目标 Android 16
        versionCode = 1
        versionName = "1.0.0"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            // 第一版不开启代码混淆：出问题时更容易排查（后续熟悉后可再开启）
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true       // 启用 Jetpack Compose
        buildConfig = true   // 生成 BuildConfig（“我的”页面要显示版本号）
    }

    testOptions {
        unitTests {
            // 让单元测试能读取 Android 资源（Robolectric 需要）
            isIncludeAndroidResources = true
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// Room：把数据库结构导出的位置（以后升级数据库版本时，用它来对比和写迁移）
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    // ---------- Compose（界面） ----------
    val composeBom = platform("androidx.compose:compose-bom:2026.09.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // ---------- 基础框架 ----------
    implementation("androidx.core:core-ktx:1.19.1")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("androidx.navigation:navigation-compose:2.10.2")

    // ---------- 本地数据库（Room） ----------
    implementation("androidx.room:room-runtime:2.8.5")
    implementation("androidx.room:room-ktx:2.8.5")
    ksp("androidx.room:room-compiler:2.8.5")

    // ---------- 设置存储（主题开关等） ----------
    implementation("androidx.datastore:datastore-preferences:1.2.1")

    // ---------- 图片加载 ----------
    implementation("io.coil-kt.coil3:coil-compose:3.6.3")
    // 读取照片的旋转信息（手机竖拍的照片方向才对）
    implementation("androidx.exifinterface:exifinterface:1.4.2")

    // ---------- JSON 序列化（错题导出/导入） ----------
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")

    // ---------- 测试（运行在电脑上的 JVM 测试） ----------
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.17")
    testImplementation("androidx.test:core:1.7.0")
    testImplementation("androidx.test.ext:junit:1.3.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
    testImplementation("androidx.room:room-testing:2.8.5")
}
