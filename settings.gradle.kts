// 项目设置文件：声明去哪里下载插件和依赖库，以及项目包含哪些模块。
import org.gradle.api.initialization.resolve.RepositoriesMode

pluginManagement {
    repositories {
        google()            // 安卓官方插件仓库
        mavenCentral()      // Maven 中央仓库
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    // 依赖统一在下面的仓库中解析，禁止各个模块自己乱加仓库
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "CuotiPlanet"
include(":app")
