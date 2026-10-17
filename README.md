# 错题星球（CuotiPlanet）

一款**完全离线**的安卓错题本 App，用来整理高中阶段的数学、物理、化学、生物、英语错题。

- 界面：Jetpack Compose + Material 3（靛蓝 + 青绿），支持浅色 / 深色 / 跟随系统，支持跟随壁纸取色（Android 12+，可在设置里关闭）
- 数据：Room 本地数据库 + 应用私有目录图片，退出应用数据不丢
- 架构：单模块 MVVM（界面 → ViewModel → 仓库 → 数据库），依赖由手写的 `AppContainer` 装配（不用 Hilt）
- 最低支持：Android 8.0（API 26）；目标平台：Android 16（API 36）
- **不申请任何敏感权限**：无网络、无存储、无相机权限（已用 apksigner / aapt2 实测 APK 确认）

---

## 一、交付物与验证结果（实际构建产物）

| 项目 | 结果 |
|---|---|
| debug APK | `app\build\outputs\apk\debug\app-debug.apk` |
| APK 大小 | 14,419,325 字节（约 13.8 MB），生成时间 2026-10-17 |
| 包名 / 版本 | `com.cuotixingqiu.app`，versionCode 1，versionName 1.0.0 |
| SDK 范围 | minSdk 26（Android 8.0）～ targetSdk 36（Android 16），compileSdk 37 |
| 签名 | debug 签名（Android Debug 证书，V2 签名方案），可直接装到手机 |
| 单元测试 | **44 项全部通过，0 失败**（见下表） |

单元测试明细（`gradlew testDebugUnitTest` 实测）：

| 测试类 | 数量 | 覆盖内容 |
|---|---|---|
| `WrongQuestionDaoTest` | 11 | 数据库增删改查、筛选、排序、复习、统计查询 |
| `WrongQuestionRepositoryTest` | 7 | 仓库逻辑：图片联动删除、批量操作、复习安排 |
| `BackupManagerTest` | 7 | JSON 导出 / 导入 / 校验 / 失败不破坏数据 |
| `ImageStoreTest` | 4 | 图片保存、压缩、EXIF 旋转、删除 |
| `ReviewSchedulerTest` | 4 | 复习间隔规则（1/2/4/7/15/30 天） |
| `SubjectTest` | 3 | 学科常量 |
| `TimeUtilsTest` | 2 | 时间格式化与“今天”边界 |
| `ViewModelLogicTest` | 6 | 页面逻辑：列表搜索筛选排序、批量标记删除、复习会话、首页统计、组卷范围 |

> 说明：项目开发时没有真机也没有模拟器（CPU 虚拟化不可用），**界面外观和触控行为需要在手机上按「第十章 自测清单」过一遍**；
> 以上数字都是实际运行结果，不是估计值。

---

## 二、功能清单

### 1. 首页
- 错题总数、今日新增、今日待复习、已掌握数量
- 各学科错题分布（点击可跳到对应学科的错题列表）
- 今日待复习列表（点击直接进入详情）
- 最近添加的错题
- 右下角「＋ 添加错题」按钮

### 2. 添加 / 编辑错题
- 题目内容、学科、知识点标签（回车添加为标签）、错因、正确解题思路、个人总结
- 难度星级（0–5 星）、所属错题本分类
- 图片：从相册选择（可多选）、拍照（调用系统相机），自动压缩到最长边 1600px 并纠正旋转方向
- **所有内容都允许留空**，不会强制填写

### 3. 错题列表
- 搜索：题目 / 内容 / 知识点 / 错因 / 解法 / 总结
- 筛选：学科、错题本分类、难度（≥N 星）、复习状态、知识点
- 排序：最新优先 / 最早优先 / 待复习优先
- 长按或点「多选」进入批量模式：批量删除、批量标记已掌握
- 删除前有二次确认；没有错题时显示友好的空状态

### 4. 错题详情
- 查看图片（点击可全屏放大）、题目、知识点、错因、正确解法、个人总结
- 编辑、标记为「已掌握 / 需要复习」、单独复习这一道、删除（二次确认）

### 5. 复习
- 今日待复习列表，点击进入复习卡片：先看题目 → 「显示答案」→ 三档自评
- 三档：不会 / 基本掌握 / 已掌握
- 复习统计：今日待复习、今日已复习、已掌握、总题数

### 6. 统计
- 掌握率环形图、学科分布柱状图、错题本分布
- 难度分布（0–5 星）、复习阶段分布、近 7 天复习次数
- 全部用 Compose 自带 Canvas 绘制，不引入第三方图表库

### 7. 组卷导出（PDF）
- 在错题列表进入多选，选择若干错题后导出 A4 多页 PDF（自动分页排版、题干图片等比缩放）
- 可选「是否附答案与解析页」；导出后可直接分享到微信 / QQ 或用其它应用打开（通过 FileProvider 临时授权，不需要存储权限）

### 8. 我的
- 主题：跟随系统 / 浅色 / 深色；可开关「跟随系统壁纸取色」（Android 12+）
- 错题本分类管理（新建 / 重命名 / 删除；删除分类不会删除错题）
- 数据管理：导出 JSON（可选是否包含图片）、导入 JSON（合并模式）、清空数据（二次确认）
- 关于：版本号、数据说明

### 复习规则（简单、可解释）
- 新错题：默认「需要复习」，次日进入待复习列表（阶段 0）
- 「不会」：阶段归零，1 天后重新复习
- 「基本掌握」：阶段 +1，下次间隔依次拉长为 2 → 4 → 7 → 15 → 30 天（封顶 30 天）
- 「已掌握」：退出待复习列表（之后可在详情页手动改回「需要复习」）

### 数据安全设计
- 导入前完整校验（版本号、字段长度、时间、图片格式等）；任何一步失败都不写入，**不会破坏已有数据**
- 导入为「合并」模式：生成新 id，不覆盖、不删除现有错题
- 导入过程中数据库写入失败会自动回滚，并删除刚写入的图片文件
- 删除错题 / 批量删除 / 清空数据都会联动删除对应图片文件，不留垃圾

---

## 三、开发环境（本项目实际使用并验证过的版本）

| 组件 | 版本 |
|---|---|
| JDK | Temurin 21（AGP 9 要求 ≥ 17） |
| Gradle | 9.8.1（项目自带 wrapper，AGP 9.4 要求 ≥ 9.6） |
| Android Gradle Plugin | 9.4.1 |
| Kotlin | 2.4.21（AGP 9 内置 Kotlin 支持） |
| KSP | 2.3.12（Room 注解处理） |
| Compose BOM | 2026.09.00（含 Material 3、material-icons） |
| Navigation Compose | 2.10.2 |
| Lifecycle / ViewModel Compose | 2.11.0 |
| Room | 2.8.5（runtime / ktx / compiler） |
| DataStore Preferences | 1.2.1 |
| Coil（图片加载） | 3.6.3（coil3） |
| kotlinx-serialization-json | 1.11.0 |
| ExifInterface | 1.4.2 |
| Robolectric（JVM 测试） | 4.17 |
| compileSdk / targetSdk / minSdk | 37 / 36 / 26 |
| SDK Build Tools | 37.0.0 |

> AGP 9 起内置 Kotlin 支持，所以本项目**不应用** `org.jetbrains.kotlin.android` 插件；
> Kotlin 编译器版本在根目录 `build.gradle.kts` 的 `buildscript` 中通过
> `org.jetbrains.kotlin:kotlin-gradle-plugin` 指定，Compose 编译器改用 `org.jetbrains.kotlin.plugin.compose`。
> compileSdk 必须用 37：Compose 2026.09 的依赖要求被 android-37 编译（不影响最低支持到 Android 8.0）。

---

## 四、项目目录结构

```
错题星球（项目根目录）
├─ settings.gradle.kts        模块与仓库配置
├─ build.gradle.kts           插件版本（AGP / Kotlin / KSP）
├─ gradle.properties          构建参数
├─ gradlew / gradlew.bat      Gradle Wrapper（固定 9.8.1）
├─ gradle/wrapper/            Wrapper 配置
├─ local.properties           本机 Android SDK 路径（不要提交到 git）
├─ README.md                  本文档
└─ app/
   ├─ build.gradle.kts        依赖与 Android 配置（版本号集中在这里）
   ├─ schemas/                Room 导出的数据库结构（以后升级数据库时用）
   └─ src/
      ├─ main/
      │  ├─ AndroidManifest.xml           不申请任何敏感权限
      │  ├─ res/                          图标、app_name、主题、FileProvider 路径
      │  └─ java/com/cuotixingqiu/app/
      │     ├─ CuotiApplication.kt        应用入口（创建依赖容器）
      │     ├─ MainActivity.kt            唯一的 Activity
      │     ├─ di/AppContainer.kt         手写依赖容器（数据库、仓库、导出器等）
      │     ├─ domain/                    学科、复习状态、复习规则（纯逻辑，有测试）
      │     ├─ util/                      TimeUtils（时间）、MediaUtils（文件 / URI）
      │     ├─ data/
      │     │  ├─ db/                     实体、DAO、AppDatabase、类型转换器
      │     │  ├─ repository/             WrongQuestionRepository、NotebookRepository
      │     │  ├─ image/ImageStore.kt     图片保存到私有目录（压缩 + 旋转纠正）
      │     │  ├─ settings/               设置存储（主题、取色开关，DataStore）
      │     │  ├─ backup/                 JSON 导出 / 导入 / 校验（BackupManager）
      │     │  └─ export/PaperExporter.kt 组卷 PDF 生成
      │     └─ ui/
      │        ├─ CuotiApp.kt             底部导航 4 标签 + 页面路由总入口
      │        ├─ AppSupport.kt           rememberAppContainer / appViewModel 工具
      │        ├─ MainViewModel.kt        主题设置状态
      │        ├─ theme/                  配色与主题
      │        ├─ navigation/Routes.kt    路由定义
      │        ├─ components/             公共组件（卡片、标签、星级、空状态、Scaffold 等）
      │        ├─ home/                   首页
      │        ├─ list/                   错题列表（搜索 / 筛选 / 排序 / 多选）
      │        ├─ detail/                 错题详情
      │        ├─ edit/                   添加 / 编辑
      │        ├─ review/                 复习列表与复习会话
      │        ├─ stats/                  统计
      │        ├─ notebook/               错题本分类管理
      │        ├─ paper/                  组卷导出
      │        └─ profile/                我的（主题 / 数据管理 / 关于）
      └─ test/java/com/cuotixingqiu/app/  44 项 JVM 单元测试（Robolectric）
```

---

## 五、构建与测试

### 0. 准备工作（第一次构建前，只做一次）

1. 安装 **JDK 21**（或任意 17+ 版本），设置好 `JAVA_HOME`
2. 安装 **Android SDK**：用 Android Studio 自带的即可；纯命令行方式的话，
   装好 `platforms;android-37.0`、`build-tools;37.0.0`、`platform-tools` 这几个包
3. 在项目根目录新建 `local.properties`，写上本机 SDK 路径（该文件不会提交到 git）：

```properties
sdk.dir=C:/Android/Sdk
```

### 1. 通用方式（任何装了 JDK 17+ 的电脑）

```powershell
cd 项目目录
.\gradlew.bat assembleDebug          # 构建 debug APK
.\gradlew.bat testDebugUnitTest      # 运行全部单元测试
```

- 首次构建需要联网下载依赖（约几百 MB）；之后可离线构建
- APK 输出：`app\build\outputs\apk\debug\app-debug.apk`
- 测试报告：`app\build\reports\tests\testDebugUnitTest\index.html`

### 2. 用 Android Studio 打开

1. Android Studio → Open → 选择本项目根目录（含 `settings.gradle.kts` 的那一层）
2. 等待 Gradle 同步完成（第一次会下载依赖）
3. 点绿色三角形运行到手机；或 Build → Build APK(s)

> 说明：`local.properties` 记录本机的 Android SDK 路径（每台电脑可以不同，
> 不会提交到 git）。如果构建报「SDK location not found」，检查它的 `sdk.dir` 是否写对。

---

## 六、安装到真实安卓手机

### 方法一：直接传文件（最简单）
1. 把 `app-debug.apk` 通过微信 / QQ / 数据线复制到手机
2. 在手机上点击安装；如果提示「禁止安装未知应用」，在弹窗里点
   「设置 → 允许来自此来源的应用」再返回继续安装
3. 安装完成后打开「错题星球」（桌面图标名）

> 手机要求：Android 8.0 及以上。App 只有约 13.8 MB，装完不占什么空间。

### 方法二：用 adb 安装（需要手机的 USB 调试）
1. 手机开启「开发者选项 → USB 调试」，用数据线连接电脑
2. 电脑上执行：

```powershell
adb devices        # 确认手机已授权
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

> 若提示找不到 `adb`，改用它在本机的完整路径，例如
> `C:\Android\Sdk\platform-tools\adb.exe`（在项目根目录执行）。

---

## 七、数据与备份说明（重要）

- 错题数据保存在手机应用私有目录（数据库 + 图片），**卸载 App 会一起删除**
- 建议养成习惯：在「我的 → 数据管理」里定期**导出 JSON 备份**
- 导出位置由系统文件选择器决定（可以存到「下载」或网盘），不需要存储权限：
  - 勾选「包含图片」：一个文件完整恢复，文件较大
  - 不勾选：文件小，但恢复后图片缺失（文字内容不受影响）
- 导入是「合并」模式：不会覆盖现有数据；同一份备份导入两次会出现两份记录（设计如此）

---

## 八、常见问题

**Q：构建时卡在下载 Gradle / 依赖？**
A：首次构建需要联网下载 Gradle 发行版和全部依赖（共约几百 MB），属正常现象，耐心等待即可。
网络慢时，可以把 `settings.gradle.kts` 里的 `google()` / `mavenCentral()` 换成国内镜像仓库。

**Q：为什么 App 不需要相机权限？**
A：拍照通过「调用系统相机」完成（`ACTION_IMAGE_CAPTURE`），照片由系统相机应用直接写入本应用提供的
FileProvider 临时地址；相册选择用系统 Photo Picker（Android 13+ 原生，旧系统自动退回系统文件选择器），
所以相机和存储权限都不需要。

**Q：为什么不需要 INTERNET 权限？**
A：App 完全离线运行，没有一行联网代码；导出、导入、PDF 组卷全部在本机完成。

**Q：备份文件被别的应用改过还能导入吗？**
A：导入前会逐条校验（版本号、字段长度、时间、图片数据格式等），校验不通过会给出具体原因，
并且**不会写入任何数据**，现有错题不受影响。

**Q：导出 PDF 后点「打开」没反应？**
A：说明手机上没有能打开 PDF 的应用，改用「分享」发到微信 / QQ / 邮箱，在任何能看 PDF 的设备上打开即可。

---

## 九、已知限制与遗留问题

1. **未做真机 / 模拟器界面验证**：开发时没有真实手机、也没有可用模拟器，
   界面外观与触控行为需要在手机上按第十章清单自测；逻辑层已用 44 项测试覆盖。
2. **release 构建未做签名配置**：`assembleRelease` 实测可以编译通过，产物为
   `app\build\outputs\apk\release\app-release-unsigned.apk`（约 9.7 MB，**未签名，不能直接安装**）。
   正式发布前需要生成自己的签名（Android Studio → Build → Generate Signed App Bundle/APK），
   并把签名文件保管好。第一版未开启代码混淆（`isMinifyEnabled = false`），方便出问题时排查。
3. **组卷 PDF 存放在应用私有目录**：每导出一次生成一个新文件，多次导出会占一点空间；
   如果需要长期保存，请用「分享」把文件发到「文件 / 下载」里。
4. **统计页的「近 7 天」是滚动窗口**：从 6 天前到今天的最近 7 天（不是自然周）。
5. **换机迁移**：目前靠 JSON 导出 / 导入（含图片），没有云同步（离线 App 的设计取舍）。
6. 底部 4 个标签固定显示；FAB（＋ 添加错题）只在首页和错题列表页出现，属预期设计。

---

## 十、真机自测清单（建议按顺序过一遍，5～10 分钟）

安装完成后：

1. **首页**：显示 0 道错题的空状态；右下角「＋」可点
2. **添加**：点「＋」→ 只填题目 → 保存 → 首页总数 +1、今日新增 +1、出现在「最近添加」
3. **图片**：编辑这道题 → 从相册选 1～2 张 + 拍 1 张照 → 保存后详情页能看到 3 张图，点开可放大、旋转过的照片方向正确
4. **列表**：切到「错题」标签 → 搜索关键词、按学科筛选、切排序方式，结果变化正确
5. **多选**：长按一道题进入多选 → 批量标记「已掌握」→ 首页已掌握数 +1
6. **复习**：再添加一道新题（明天才到期，今天看不到是正常的）；
   对已有错题在详情页点「立即复习」→ 走完「不会 / 基本掌握 / 已掌握」三档 → 列表页和统计页数字同步变化
7. **删除**：删除一道题 → 二次确认弹窗 → 删除成功；「清空数据」也有二次确认
8. **我的**：切换浅色 / 深色 / 跟随系统；开关「跟随壁纸取色」（Android 12+ 才有明显效果）
9. **导出**：导出 JSON（先不勾图片）→ 在文件管理器里确认文件存在且能打开（内容是文本）
10. **导入**：把刚导出的 JSON 导入 → 提示新增了几道题，原有数据没变
11. **组卷**：错题列表多选 2～3 道 → 导出 PDF → 「分享」到微信给自己或「打开」，确认排版正常
12. **旋转 / 深色模式**：旋转屏幕、切深色模式，界面不崩、文字清晰
13. **杀进程重开**：从后台划掉 App 再打开，数据还在

如任何一步与上面描述不符，请记录：手机型号、Android 版本、操作步骤、现象截图，便于排查。

---

## 十一、版本记录

- v1.0.0（2026-10-17）：首个版本。首页、错题列表（搜索 / 筛选 / 排序 / 批量操作）、详情、
  添加 / 编辑（相册 + 拍照）、复习（三档自评 + 间隔计划）、统计图表、错题本分类、
  JSON 导出 / 导入（可选含图片）、组卷导出 PDF、主题与壁纸取色；44 项单元测试全部通过。
