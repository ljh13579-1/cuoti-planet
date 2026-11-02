# 错题星球（CuotiPlanet）

一款**完全离线**的安卓错题本 App，用来整理高中阶段的数学、物理、化学、生物、英语错题。

- 界面：Jetpack Compose + Material 3（暖灰底 + 白卡片 + 品牌蓝紫 `#3539DF`），支持浅色 / 深色 / 跟随系统，支持跟随壁纸取色（Android 12+，可在设置里关闭）
- 数据：Room 本地数据库 + 应用私有目录图片，退出应用数据不丢
- 架构：单模块 MVVM（界面 → ViewModel → 仓库 → 数据库），依赖由手写的 `AppContainer` 装配（不用 Hilt）
- 最低支持：Android 8.0（API 26）；目标平台：Android 16（API 36）
- **不申请任何敏感权限**：无网络、无存储、无相机权限（已用 apksigner / aapt2 实测 APK 确认）

---

## 一、交付物与验证结果（本机实际跑出来的结果）

| 项目 | 结果 |
|---|---|
| debug APK | `app\build\outputs\apk\debug\app-debug.apk` |
| APK 大小 | 14,697,853 字节（约 14.02 MiB / 14.70 MB），生成时间 2026-11-02 23:35（v1.3.10 悬浮胶囊保存按钮 + 返回键真实采样） |
| APK SHA-256 | `D8F7E3B1B7DE2A9AF9AF9D00D65FC2107FC1699724C27D1B8299D0B957E05620` |
| 包名 / 版本 | `com.cuotixingqiu.app`，versionCode 13，versionName 1.3.10 |
| SDK 范围 | minSdk 26（Android 8.0）～ targetSdk 36（Android 16），compileSdk 37 |
| 签名 | debug 签名（Android Debug 证书，V2 签名方案），与上一版同一证书，可直接覆盖安装 |
| 单元测试 | **66 项全部通过，0 失败**（见下表；本轮已重新编译、打包并执行 `:app:testDebugUnitTest`） |

> Dock 在 Android 13+ 使用 `io.github.kyant0:backdrop:2.0.1`（Apache-2.0，仅 Dock 局部使用），
> 依赖树确认实际落地为 `backdrop:2.0.1 -> backdrop-android:2.0.1 -> shapes:1.2.1`。
> 本轮重设计没有修改 Dock 实现，也没有新增权限。

单元测试明细（`gradlew testDebugUnitTest` 实测）：

| 测试类 | 数量 | 覆盖内容 |
|---|---|---|
| `WrongQuestionDaoTest` | 11 | 数据库增删改查、筛选、排序、复习、统计查询 |
| `WrongQuestionRepositoryTest` | 7 | 仓库逻辑：图片联动删除、批量操作、复习安排 |
| `BackupManagerTest` | 7 | JSON 导出 / 导入 / 校验 / 失败不破坏数据 |
| `ImageStoreTest` | 4 | 图片保存、压缩、EXIF 旋转、删除 |
| `ReviewSchedulerTest` | 4 | 复习间隔规则（1/2/4/7/15/30 天） |
| `SubjectTest` | 3 | 学科常量 |
| `WindowSizeTest` | 3 | 响应式断点（< 600dp 紧凑 / 600–839dp 中等 / ≥ 840dp 展开） |
| `TimeUtilsTest` | 8 | 时间格式化、今天边界、近 7 天复习统计、连续学习、加入天数、星期单字 |
| `CameraCaptureRecoveryTest` | 4 | 拍照兜底：没拍到会明确提示、空文件清理、照片导入后清临时文件、过期残留清理 |
| `DockTabsTest` | 4 | 悬浮 Dock 导航契约：4 个目的地的数量、路由顺序、标签文字、图标可用性 |
| `DockWidthTest` | 5 | 悬浮 Dock 宽度规则：竖屏铺满留边、横屏比竖屏宽 20%～30%、不接近全屏、随窗口单调不减、项数参与计算 |
| `ViewModelLogicTest` | 6 | 页面逻辑：列表搜索筛选排序、批量标记删除、复习会话、首页统计、组卷范围 |

> 合计 66 项：11 + 7 + 7 + 4 + 4 + 3 + 3 + 8 + 4 + 4 + 5 + 6。

> 说明：本机没有真机也没有模拟器（CPU 虚拟化不可用），**界面外观和触控行为需要在手机上按「第十章 自测清单」过一遍**；
> 以上数字都是实际运行结果，不是估计值。

---

## 二、功能清单

### 1. 首页
- 品牌日期头、按时间变化的问候语和真实今日任务标题
- 深色学习统计卡：连续学习天数、今日待复习、已掌握数量（全部由数据库推导）
- 全宽「添加错题」按钮（首页不再使用右下角悬浮 FAB）
- 今日待复习任务（来自真实 `nextReviewAt`）与最近添加的错题
- 点击统计卡进入统计页，点击任务 / 最近错题进入详情，点击「开始复习」进入复习会话

### 2. 添加 / 编辑错题
- 题目内容、学科、知识点标签（回车添加为标签）、错因、正确解题思路、个人总结
- 难度星级（0–5 星）、所属错题本分类
- 图片：从相册选择（可多选）、拍照（调用系统相机），自动压缩到最长边 1600px 并纠正旋转方向
- **所有内容都允许留空**，不会强制填写

### 3. 错题列表
- 搜索：题目 / 内容 / 知识点 / 错因 / 解法 / 总结
- 筛选：学科、错题本分类、难度（≥N 星）、复习状态、知识点
- 排序：最近添加 / 最早添加 / 待复习优先
- 长按或点「多选」进入批量模式：批量删除、批量标记已掌握
- 添加错题的入口在标题右侧的圆形加号；删除前有二次确认；没有错题时显示友好的空状态

### 4. 错题详情
- 二级页面：圆形玻璃返回键、标题、「更多」菜单；**不显示底部 Dock**
- 真实学科、添加日期、复习次数、知识点标签；题目卡与图片（点击可全屏放大）
- 错因、正确解法、个人总结分区展示
- 底部「编辑 / 加入今日复习」；「更多」菜单支持编辑、标记已掌握 / 重新加入复习、删除（二次确认）

### 5. 复习
- 日期标题、圆环进度统计卡：今日待复习、今日已复习、已掌握（真实数据）
- 全宽「开始复习」；没有到期题目时按钮禁用
- 真实学科掌握进度（按 `subject + status` 聚合）
- 未来三天复习安排（按真实 `nextReviewAt` 统计）
- 复习卡片：先看题目 → 「显示答案」→ 不会 / 基本掌握 / 已掌握 三档自评

### 6. 统计
- 掌握率环形图、学科分布柱状图、错题本分布
- 难度分布（0–5 星）、复习阶段分布、近 7 天复习次数
- 全部用 Compose 自带 Canvas 绘制，不引入第三方图表库

### 7. 组卷导出（PDF）
- 在错题列表进入多选，选择若干错题后导出 A4 多页 PDF（自动分页排版、题干图片等比缩放）
- 可选「是否附答案与解析页」；导出后可直接分享到微信 / QQ 或用其它应用打开（通过 FileProvider 临时授权，不需要存储权限）

### 8. 我的
- 通用身份「同学 / 星」（项目没有姓名、头像字段，不展示虚构个人信息）
- 真实统计：错题累计、已掌握、连续复习；近 7 天复习柱状图与学科数
- 外观与主题：跟随系统 / 浅色 / 深色；可开关「跟随系统壁纸取色」（Android 12+）
- 错题本分类管理、数据备份（JSON 导出 / 导入，可选图片）
- 组卷导出 PDF 入口、清空数据（二次确认）、版本与离线说明
- 页面底部为 Dock 预留安全区，不放悬浮 FAB

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
| Backdrop（Android 13+ Dock 透镜，Apache-2.0） | 2.0.1 |
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
      └─ test/java/com/cuotixingqiu/app/  66 项 JVM 单元测试（Robolectric + 纯 JVM）
```

---

## 五、构建与测试

### 0. 本机（这台开发电脑）的完整命令

```powershell
$env:JAVA_HOME='D:\Android\jdk21\jdk-21.0.12.1+1'
$env:ANDROID_HOME='D:\Android\Sdk'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
cd D:\aaakaifa
& 'D:\Android\gradle\gradle-9.8.1\bin\gradle.bat' assembleDebug testDebugUnitTest --console=plain
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

> 本机说明：`local.properties` 里 SDK 路径为 `D:/Android/Sdk`，JDK 用
> `D:\Android\jdk21\jdk-21.0.12.1+1`。换电脑时改这两个位置即可。

---

## 六、安装到真实安卓手机

### 方法一：直接传文件（最简单）
1. 把 `app-debug.apk` 通过微信 / QQ / 数据线复制到手机
2. 在手机上点击安装；如果提示「禁止安装未知应用」，在弹窗里点
   「设置 → 允许来自此来源的应用」再返回继续安装
3. 安装完成后打开「错题星球」（桌面图标名）

> 手机要求：Android 8.0 及以上。App 只有约 14.0 MiB（14.70 MB），装完不占什么空间。

### 方法二：用 adb 安装（需要手机的 USB 调试）
1. 手机开启「开发者选项 → USB 调试」，用数据线连接电脑
2. 电脑上执行：

```powershell
D:\Android\Sdk\platform-tools\adb.exe devices        # 确认手机已授权
D:\Android\Sdk\platform-tools\adb.exe install -r D:\aaakaifa\app\build\outputs\apk\debug\app-debug.apk
```

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
A：首次构建需要联网（约几百 MB）。网络慢时可在 `settings.gradle.kts` 里把仓库换成国内镜像
（清华 TUNA / 腾讯云），或直接用本机已装好的 Gradle 构建（见第五章第 0 节命令）。

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

1. **未做真机 / 模拟器界面验证**：本机（开发电脑）没有真实手机、也没有可用模拟器，
   界面外观与触控行为需要在手机上按第十章清单自测；逻辑层已用 66 项测试覆盖。
2. **release 构建未做签名配置**：`assembleRelease` 实测可以编译通过，产物为
   `app\build\outputs\apk\release\app-release-unsigned.apk`（10,330,240 字节，约 9.85 MiB，**未签名，不能直接安装**）。
   正式发布前需要生成自己的签名（Android Studio → Build → Generate Signed App Bundle/APK），
   并把签名文件保管好。第一版未开启代码混淆（`isMinifyEnabled = false`），方便出问题时排查。
3. **组卷 PDF 存放在应用私有目录**：每导出一次生成一个新文件，多次导出会占一点空间；
   如果需要长期保存，请用「分享」把文件发到「文件 / 下载」里。
4. **统计页的「近 7 天」是滚动窗口**：从 6 天前到今天的最近 7 天（不是自然周）。
5. **换机迁移**：目前靠 JSON 导出 / 导入（含图片），没有云同步（离线 App 的设计取舍）。
6. 底部 4 个标签固定显示；完整错题详情页按设计隐藏 Dock，大屏「列表 + 详情」分栏仍显示 Dock；
   首页和错题列表页已不再使用悬浮 FAB（添加错题使用页内按钮）。

---

## 十、真机自测清单（建议按顺序过一遍，5～10 分钟）

安装完成后：

1. **首页**：0 道错题时仍有完整品牌头、深色统计卡和全宽「添加错题」按钮；
   添加后首页统计、连续学习、今日任务和最近添加同步变化
2. **添加**：点全宽「添加错题」或错题页标题右侧的圆形加号 → 只填题目 → 保存 →
   首页总数 +1、今日新增 +1、出现在「最近添加」
3. **图片**：编辑这道题 → 从相册选 1～2 张 + 拍 1 张照 → 保存后详情页能看到 3 张图，点开可放大、旋转过的照片方向正确
4. **列表**：切到「错题」标签 → 搜索关键词、按学科筛选、切排序方式，结果变化正确；
   大屏下点左侧题目，右侧直接显示详情
5. **多选**：长按一道题进入多选 → 底部悬浮操作栏（全选 / 掌握 / 需复习 / 组卷 / 删除）→ 批量标记「已掌握」→ 首页已掌握数 +1
6. **复习**：再添加一道新题（明天才到期，今天看不到是正常的）；复习页顶部是日期 + 圆环统计卡，
   中间是全宽「开始复习」，下面是真实学科进度和未来三天安排；
   在详情页点「加入今日复习」→ 走完「不会 / 基本掌握 / 已掌握」三档 → 列表页和统计页数字同步变化
7. **删除**：删除一道题 → 二次确认弹窗 → 删除成功；「清空数据」也有二次确认（红色文字入口）
8. **我的**：主题模式**只有三个选项（跟随系统 / 浅色 / 深色），没有重复**；切换后立刻生效；
   开关「跟随壁纸取色」（Android 12+ 才有明显效果）；浅色下页面是「白卡片 + 暖灰底」，深色下对比清晰
9. **导出**：导出 JSON（先不勾图片）→ 在文件管理器里确认文件存在且能打开（内容是文本）
10. **导入**：把刚导出的 JSON 导入 → 提示新增了几道题，原有数据没变
11. **组卷**：错题列表多选 2～3 道 → 导出 PDF → 「分享」到微信给自己或「打开」，确认排版正常
12. **旋转 / 深色模式**：手机竖屏 ↔ 横屏来回旋转两次，界面不崩、文字清晰；
    宽屏时首页 / 复习 / 我的内容居中限宽，不会被拉成一整行；
    展开宽度（≥840dp，平板横向最常见）时错题列表变成**左侧列表 + 右侧详情**，
    点左侧题目右侧直接显示；底部四个标签在列表页始终可用，完整详情页隐藏
13. **杀进程重开**：从后台划掉 App 再打开，数据还在
14. **大屏（平板 / 横屏）**：错题列表分栏时，右侧详情里「编辑 / 加入今日复习 / 更多菜单」都能正常用，
    删除后右侧回到“选一道错题”的提示；详情底栏不被 Dock 遮挡

如任何一步与上面描述不符，请记录：手机型号、Android 版本、操作步骤、现象截图，便于排查。

---

## 十一、界面与视觉升级（v1.1，2026-10-18）

本次升级只改「怎么显示」，没有改数据、复习算法和导入导出格式。全部修改都在现有工程里，
并重新执行了 `assembleDebug + testDebugUnitTest`（44 项测试全部通过）。

### 1. 修好的一个真实缺陷
「我的 → 主题模式」原来会**重复显示两个「跟随系统」**（一个永远无法选中）。
原因是 `ThemeMode` 枚举本身包含「跟随系统」，而通用筛选组件又自动加了一个“全部”标签。
现在主题选项是唯一的三个：**跟随系统 / 浅色 / 深色**，选中状态清楚。

### 2. 设计规范（改配色、圆角、字号时优先改这几处）
- 配色：`ui/theme/Color.kt`（浅色 + 深色两套完整颜色）
- 字体 / 圆角：`ui/theme/Type.kt`（`CuotiTypography`、`CuotiShapes`）
- 主题装配：`ui/theme/Theme.kt`（浅色、深色、跟随壁纸取色）

浅色主题的关键颜色（与需求参考一致）：

| 用途 | 颜色 |
|---|---|
| 品牌靛蓝（主色） | `#4359A8` |
| 青绿（强调容器色，选中胶囊 / 完成提示） | `#C7EEE9`（`#9FE7E2` 的浅版，更耐看） |
| 青绿前景（文字 / 图标） | `#116E72`（保证对比度） |
| 页面底色 | `#F6F7FB` |
| 卡片 | `#FFFFFF`（白卡浮在浅底上，替代原来的灰紫卡片） |
| 主要文字 | `#202536` |
| 次级文字 | `#747D90` |
| 分割线 | `#E6E9F0` |
| 危险操作 | `#B3261E` |
| 学科标签 | 六个学科各自的低饱和色，由 `SubjectBadge` 按浅色 / 深色自动调淡或调亮 |

> 特别说明：原版本“灰紫色卡片”的根源是 Material 3 默认的 `surfaceContainer*`
> 是薰衣草紫灰（#F7F2FA 一类）。本次把 `surfaceContainer` 系列全部显式指定为中性色，
> 白卡 + 浅灰底，深色主题则是一套独立的深蓝灰体系。跟随壁纸取色开关保持可用。

圆角规范：输入框 / 标签 12dp，普通卡片 16dp，大卡片和底部操作栏 20dp，对话框 26dp；
按钮沿用 Material 3 标准胶囊形状。

### 3. 组件与页面改进点
- 公共组件（`ui/components/Common.kt`）：
  - `ScreenScaffold` 改为自适应高度顶栏（标题 + 副标题两行也不会挤，系统字体放大不会被裁）；
  - 新增 `CuotiTextField`（浅灰填充、无下划线、圆角 12dp，替代重边框输入框）；
  - 新增 `CuotiFilterChip`（未选中浅灰底、选中品牌靛蓝），`FilterChipRow` 支持不显示“全部”标签；
  - `SubjectBadge` 每个学科独立柔和配色；`EmptyState` 改为品牌色圆底图标 + 更友好的文案。
- 首页：顶部品牌头（应用名「错题星球」突出显示 + 副标题）；概览卡重排为“2×2 大数字 + 今日复习进度条”，
  零数据、低进度、已全部完成都有合理显示；学科分布用学科色进度条，**点一行会跳到该学科的错题列表**；
  悬浮「＋ 添加错题」不再遮挡最后一张卡片（列表底部预留了空间）。
- 错题列表：搜索框改为轻量圆角；“更多筛选”（带已启用条件个数）和“排序”合并成同一行统一控件，可横向滚动；
  多选操作栏改为悬浮圆角卡片，5 个操作等宽分布、图标在上文字在下，小屏不挤、点击区域更大。
- 复习页：新增品牌色「今日待复习」任务卡（大数字 + 开始复习按钮，无任务时显示完成状态而不是灰色禁用按钮）；
  统计卡保留 4 个数字并新增**掌握率圆环**；「接下来 7 天」按天分组显示（今天 / 明天 + 日期 + 星期）；
  「复习规则」改为可展开 / 收起的说明卡。
- 我的：分为「外观 / 数据管理 / 危险操作 / 应用信息」四个区域；设置行统一为
  圆角图标容器 + 标题 + 说明 + 右箭头；危险操作单独一张卡片（红色图标 + 红色标题），二次确认保持不变。
- 添加 / 编辑：统一浅色输入框；图片预览圆角统一，右上角删除按钮加了半透明圆底更好点；
  底部“保存”按钮固定在底部并做了键盘（IME）适配，输入长文本时不会挡住保存。
- 详情页：内容顺序调整为“题目内容 → 知识点 → 解题思路 → 错因 → 总结 → 复习情况”，让题目和解法先占视觉焦点。
- 统计页：环形图 / 图例与学科标签同色（深浅主题自动适配）；柱状图与进度条统一为品牌色 + 浅色轨道。
- 底部导航：顶部一条极浅分割线代替阴影；选中态为柔和青绿胶囊 + 深青图标，未选中为次级灰。

### 4. 本次改动的文件清单
新增：`ui/theme/Type.kt`、`ui/components/ProgressRing.kt`
修改：`ui/theme/Color.kt`、`ui/theme/Theme.kt`、`ui/components/Common.kt`、`ui/components/QuestionCard.kt`、
`ui/CuotiApp.kt`、`ui/home/HomeScreen.kt`、`ui/list/QuestionListScreen.kt`、`ui/review/ReviewScreen.kt`、
`ui/review/ReviewSessionScreen.kt`、`ui/profile/ProfileScreen.kt`、`ui/edit/EditScreen.kt`、
`ui/detail/DetailScreen.kt`、`ui/stats/StatsScreen.kt`、`ui/notebook/NotebookScreen.kt`、`ui/paper/PaperScreen.kt`
（数据库 Entity / DAO / 复习算法 / 导入导出 / 组卷 PDF 的代码**没有改动**）。

---

## 十二、响应式布局与信息密度升级（v1.2，2026-10-18）

针对“大屏空旷、层次不足、自适应缺失”的第二轮升级：在**不改数据模型、复习算法、导入导出和导航结构**
的前提下，按“窗口实际宽度”做三档自适应（`ui/adaptive/WindowSize.kt`）：

| 档位 | 宽度 | 布局 |
|---|---|---|
| 紧凑 COMPACT | < 600dp（手机竖屏） | 单列；底部四个标签；悬浮「＋ 添加错题」 |
| 中等 MEDIUM | 600–839dp（手机横屏、小平板） | 内容限宽 + 关键卡片双列（首页统计与任务并排、错题卡片两列） |
| 展开 EXPANDED | ≥ 840dp（平板横向、大屏） | 错题列表「列表 + 详情」主从分栏；页面不再无限横向拉宽 |

判定用当前窗口实际宽度 `LocalConfiguration.screenWidthDp`（不看设备型号），分屏 / 折叠屏 / 旋转都会即时重算；
断点规则抽成纯函数 `windowWidthSizeOf()`，并新增 3 个单元测试。

### 1. 页面级变化
- **首页**：顺序调整为「品牌 → 核心统计 → 今日复习任务 → 学科分布 → 最近添加 → 添加入口」；
  「今日复习任务」独立成品牌色卡片（大数字 + 真实进度条 + 开始复习；完成时显示完成状态而不是灰色禁用按钮）；
  学科分布点击跳转保持原样。**没有错题时**只显示品牌头 + 精致空状态（说明可以拍照或手动录入）
  + 主按钮「添加第一道错题」+ 次按钮「先看看复习规则」，并且**此时自动隐藏右下角悬浮按钮**，
  同一个屏幕不再同时出现两个添加入口；空状态不展示任何统计数字或虚构记录。
- **错题列表**：搜索框 / 学科标签（可横向滚动）/「更多筛选 · n」「排序」「清除筛选」分成三层，
  与结果区用分割线分开；两种空状态明确区分——“一道错题都没有”给「添加第一道错题」，
  “有题但筛不出来”给「清除搜索和筛选」。
  **展开宽度下变为左侧列表 + 右侧详情**：点左侧任意一道题，右侧直接显示完整内容、图片和复习操作；
  编辑、删除、批量标记、立即复习全部沿用原有页面与逻辑（删除后在右侧回到“选一道错题查看详情”提示）；
  分栏时添加按钮移到顶栏，避免悬浮按钮压住详情底栏。
- **复习页**：宽屏上「今日任务卡」与「统计卡」并排、待复习卡片两列；**一道错题都没有时**给出
  真实说明（新题什么时候进入复习计划）+「去添加错题」按钮 + 可展开的复习规则摘要，不用假数字填充。
- **我的**：内容列宽限制为 720dp，平板上不再是一张无限宽的设置卡片；主题三选项、动态取色、
  危险操作二次确认、滚动不被底栏遮挡均保持原样。
- **详情 / 添加编辑 / 统计 / 分类 / 组卷**：这些页面通过统一的 `ScreenScaffold` 自动获得
  “宽屏限宽居中”的能力，没有单独改动逻辑。
- **公共组件**：`ScreenScaffold` 新增 `maxContentWidth`（宽屏自动居中限宽）和 `statusBarInset`
  （供分栏右栏使用，避免重复留状态栏高度）；`EmptyState` 支持限宽和次操作按钮，
  图标从大圆底改为更精致的圆角方形容器，文案按各页面真实语境分别撰写。

### 2. 本次改动的文件清单
新增：`ui/adaptive/WindowSize.kt`、`app/src/test/java/com/cuotixingqiu/app/ui/adaptive/WindowSizeTest.kt`

修改：`ui/AppSupport.kt`（ViewModel 支持 key，供分栏内按题切换使用）、`ui/components/Common.kt`、
`ui/CuotiApp.kt`、`ui/home/HomeScreen.kt`、`ui/list/QuestionListScreen.kt`、`ui/review/ReviewScreen.kt`、
`ui/profile/ProfileScreen.kt`、`ui/detail/DetailScreen.kt`

未改动：`ui/components/QuestionCard.kt`（仅复用）、主题文件、Room Entity/DAO/数据库版本、
复习算法与间隔、JSON 备份格式、PDF 组卷逻辑、底部导航路由。

### 3. 验证情况
- `assembleDebug + testDebugUnitTest` 重新执行：**BUILD SUCCESSFUL，47 项测试全部通过（0 失败 / 0 错误 / 0 跳过）**。
- 本机没有真机 / 模拟器（CPU 虚拟化不可用），**视觉与触控仍需按第十章清单在手机上验证**，
  重点：横竖屏切换、平板/大屏下的错题分栏、字体放大后的顶栏与空状态、深色模式下的进度条与卡片层次。

---

## 十三、鸿蒙「卓易通」拍照兼容修复（v1.2.1，2026-10-18）

### 1. 问题现象
在鸿蒙手机的「卓易通」安卓兼容环境里，添加 / 编辑错题时用「拍照」加图片：系统相机能正常
打开、能拍照，返回应用后**既没有缩略图，也没有任何错误提示**（静默失败）；同一台设备上
「从相册选择」完全正常。

### 2. 排查结论
- 应用内部的图片链路（导入 → 压缩 → 存私有目录 → 显示）经相册路径验证是完好的；
  问题出在“相机把照片写回应用、并把结果交还给应用”这一步。
- 系统拍照用的是 `ActivityResultContracts.TakePicture`：必须先创建应用私有临时文件，
  由相机写入，再回传“成功标志”。**真机实测确认**：鸿蒙卓易通会**吞掉结果标志**，
  但照片其实已经正常写进了临时文件；原实现只看标志不看文件，标志一丢就判失败且不给提示，
  于是表现为“什么都没发生”。

### 3. 修复内容
- **以文件为准**：拍照前记下临时文件路径；相机返回后（结果回调 + 页面回到前台两个时机，
  重复调用安全）检查该文件——**只要文件里有照片就导入**，不再依赖“成功标志”。
- **失败必有提示**：相机没回传照片、临时文件创建失败、导入进行中重复加图，都会给出明确
  提示，不再静默。
- **保存竞态修复**：图片导入期间禁用「保存」按钮、保存接口同样拦截，避免刚拍的照片
  来不及写进错题。
- **自动清理**：导入完成后删除拍照临时文件；创建新拍照文件时顺手清理 0 字节的失败文件
  和超过一天的残留。

改动文件：`ui/edit/EditScreen.kt`、`ui/edit/EditViewModel.kt`、`util/MediaUtils.kt`、
`app/build.gradle.kts`（版本号 1.0.0 → 1.2.1，versionCode 1 → 2），新增测试
`app/src/test/java/com/cuotixingqiu/app/ui/edit/CameraCaptureRecoveryTest.kt`（4 项）。
数据模型、复习算法、导入导出、PDF 组卷、导航结构均未改动，也未新增任何权限或第三方依赖。

### 4. 设备验证结果（已确认，2026-10-18）
- 覆盖安装 v1.2.1 后实机复测：添加错题页用「拍照」加图片，**缩略图正常出现，修复确认有效**
  （添加 / 编辑是同一套代码，编辑页行为一致）。
- 结论：卓易通**丢失的是“拍照成功”的结果标志，照片写入应用私有临时文件一直是正常的**；
  原实现只看标志、不看文件，因此误判失败并静默丢弃。改为“以文件为准”后问题消除。
- 因此**不需要**改用应用内相机（那一步会新增相机权限与安装包体积），当前零权限、
  零新增依赖的方案已足够。
- 小提示：如果主动取消拍照（没拍就退出相机），会看到「拍照没有返回照片」的提示——
  这是预期行为（确实没有拍到照片），不影响正常拍摄。

> 本机没有真机 / 模拟器（CPU 虚拟化不可用），以上修复已通过编译与 51 项单元测试，
> 真机验证由用户在鸿蒙「卓易通」环境完成。

---

## 十四、悬浮玻璃 Dock 底部导航（v1.3.0 / v1.3.1 / v1.3.2 / v1.3.3 / v1.3.4，2026-10-18 ~ 19）

### 1. 改了什么
- 底部导航从「实心底栏（1dp 分隔线 + NavigationBar）」改为 **iOS 26/27 风格的悬浮玻璃 Dock**：
  一颗独立圆角胶囊、水平居中、悬浮在页面内容之上；每个目的地同时显示图标 + 文字。
- 4 个导航项（首页 / 错题 / 复习 / 我的）的**顺序、图标、标签文字、跳转目标和选中状态管理
  与改造前完全一致**，没有新增或删除任何目的地；二级页面（详情 / 编辑 / 统计等）也照旧显示 Dock。
- 页面内容**可以从 Dock 后面滚过**：每个滚动列表的底部留白都按 Dock 的实际高度计算，
  最后一条内容能完整滚到 Dock 上方并正常点击。
- 「有自家底栏」的页面（编辑保存条、详情操作条、列表多选条）改为**浮在 Dock 上方**，
  不再被 Dock 压住；空状态、Snackbar、悬浮的「添加错题」按钮也都统一避让。

### 2. 视觉与实现要点
- **零新增依赖**：基础观感用「半透明主题色底 + 细渐变边框高光 + 轻微投影」模拟玻璃质感，
  所有颜色从 `MaterialTheme` 派生，浅色 / 深色模式都有足够对比度；
  v1.3.3 / v1.3.4 起按系统能力叠加**真实背景采样**（见 §8）：
  Android 13+ 是 AGSL 边缘折射，Android 12/12L 是真背景模糊（磨砂玻璃），
  Android 11 及以下自动退回纯材质模拟（全程不引入任何第三方模糊库）。
- 选中态 = 柔和胶囊底色 + 图标 / 文字颜色 + 字重三处同时变化，**不只依赖颜色**；
  切换只用 160ms 的颜色短过渡，没有循环或夸张动画。
- 宽度自适应按**窗口宽度**而不是设备型号：紧凑窗口（< 600dp）铺满可用宽度、每项等宽；
  更宽的窗口收窄到 440dp 以内居中，平板上不无限拉长；胶囊与屏幕左右边缘至少留 14dp。
- 无障碍：每一项是 `Role.Tab` 的可选中节点（读屏会播报“已选中 / 未选中”），
  文字标签自带语义，图标 `contentDescription = null`（纯装饰）不重复朗读；触控高度 ≥ 52dp。

### 3. insets（系统栏）处理
- Dock 自己消费系统栏：底部避开手势条 / 三按键导航栏，横屏时避开侧边手势区
  （`WindowInsets.systemBars` 的 Bottom + Horizontal）。
- 其他页面**不重复消费**底部 inset：避让值统一由 `rememberDockClearance()` 推导
  （系统栏高度 + 胶囊 64dp + 下边留白 10dp + 呼吸空隙 8dp），滚动列表、页面底栏、空状态、
  Snackbar、悬浮按钮全部用同一个函数，不会出现“页面留白和 Dock 实际高度对不上”。
- **没有对所有页面统一加大 padding**：只在各滚动容器的底部留白 / 有底栏页面的底栏位置上
  按同一个函数加避让；顶部状态栏处理保持原样（`ScreenScaffold` 的顶部逻辑未改）。

### 4. 本次改动的文件清单
新增：`ui/components/FloatingGlassDock.kt`（Dock 组件 + 避让值计算）、
`app/src/test/java/com/cuotixingqiu/app/ui/DockTabsTest.kt`（4 项导航契约测试）。

修改：`ui/CuotiApp.kt`（根布局：实心 Scaffold 底栏 → Box + 悬浮 Dock + 悬浮按钮）、
`ui/components/Common.kt`（`ScreenScaffold` 底栏 / Snackbar 避让，`EmptyState` 底部避让）、
`ui/home/HomeScreen.kt`、`ui/list/QuestionListScreen.kt`、`ui/review/ReviewScreen.kt`、
`ui/review/ReviewSessionScreen.kt`、`ui/stats/StatsScreen.kt`、`ui/notebook/NotebookScreen.kt`、
`ui/paper/PaperScreen.kt`、`ui/profile/ProfileScreen.kt`（以上页面只改底部留白）、
`app/build.gradle.kts`（版本号 1.2.1 → 1.3.0，versionCode 2 → 3）。

未改动：Room Entity / DAO / 数据库版本与迁移、ViewModel 业务逻辑、错题增删改与拍照 / 相册链路、
复习算法与间隔、统计、搜索、备份 / PDF 导出、`MainActivity.kt`、主题文件、`ui/components/QuestionCard.kt`、
`ui/detail/DetailScreen.kt`（详情底栏的避让由 `ScreenScaffold` 统一处理，无需单独改）。

### 5. 验证情况
- `assembleDebug + testDebugUnitTest`：**BUILD SUCCESSFUL**；
  另做了一次 `testDebugUnitTest --rerun-tasks --no-build-cache` 强制全量重跑：
  **55 项测试全部通过（0 失败 / 0 错误 / 0 跳过）**。
- APK 实测：versionCode 3 / versionName 1.3.0；与上一版**同一 debug 证书**（可直接覆盖安装）；
  权限清单与上一版完全一致（**无新增权限**）；Dex 中已实测包含新的 Dock 组件。
- 本机没有真机 / 模拟器（CPU 虚拟化不可用），**Dock 的实际观感、滚动手感与手势区表现需要在手机上验证**。
  重点：手势条与三按键导航两种模式下 Dock 是否被系统栏遮挡、内容滚到最底时最后一条是否完整露出、
  深色模式下的对比度、平板 / 横屏时 Dock 宽度是否符合预期、系统字体放大后 Dock 文字是否被截断。

### 6. 横屏自适应宽度修订（v1.3.1，2026-10-18）
- **竖屏保持现状**：手机竖屏（窗口 < 600dp）仍是“铺满可用宽度、左右各留 14dp、四项等宽”，
  尺寸、间距、内边距与 v1.3.0 完全一致，未做任何放大。
- **横屏 / 平板更舒展**：窗口 ≥ 600dp 时，每个导航项的目标宽度按**实际可用窗口宽度**
  （`BoxWithConstraints` 的真实约束，已扣掉系统栏）在 84dp → 112dp 之间插值；
  胶囊宽度 = 项数 × 单项目标宽度 + 左右内边距，并限制在可用宽度的 62% 以内。
  以 390dp 手机竖屏（约 362dp）为参照：约 11 寸平板横屏（1194dp）约 463dp（+28%），
  极宽窗口最多 464dp——明显更舒展，但不会接近全屏宽度，也不依赖设备型号或固定 dp。
- 宽度规则抽成纯函数 `dockWidthFor(可用宽度, 项数)`，新增 5 项单元测试
  （竖屏铺满留边 / 横屏比竖屏宽 20%～30% / 不超过可用宽度 62% / 随窗口宽度单调不减 / 项数参与计算）。
- 玻璃材质同步小幅优化（透明度、内侧“上亮下暗”的层次、边缘高光与更柔和的彩色阴影），
  全部只影响观感，不改变任何尺寸。
- 导航项本身（4 个目的地、图标、文字、点击事件、选中状态、导航逻辑）完全未变；
  `assembleDebug + testDebugUnitTest` 与强制全量重跑均通过，**60 项测试 0 失败**。
- 改动文件：`ui/components/FloatingGlassDock.kt`（宽度规则 + 玻璃材质微调）、
  新增 `app/src/test/java/com/cuotixingqiu/app/ui/components/DockWidthTest.kt`、
  `app/build.gradle.kts`（版本号 1.3.0 → 1.3.1，versionCode 3 → 4）。
  导航结构、各页面、`Common.kt`、`CuotiApp.kt`、主题文件均未改动。

### 7. 液态玻璃材质精修（v1.3.2，2026-10-18）
- **目标问题**：旧材质底色是“纯白 0.84 不透明度”，叠在近白页面底上像白色磨砂塑料；
  选中底是实色青绿；阴影偏重。
- **真背景模糊评估（结论：不做）**：`Modifier.blur` / `graphicsLayer.renderEffect` 只能模糊
  组件自己绘制的内容，无法模糊 Dock 身后的页面内容；Android 12 的 `Window.setBackgroundBlurRadius`
  只作用于“窗口背后”（对话框那种），不适用于窗口内的悬浮组件；Haze 类第三方库与“零新增依赖”冲突。
  因此继续用透明底色 + 高光 + 边缘层次模拟玻璃，无每帧模糊、无持续动画、无新增依赖或权限。
  （v1.3.3 在此基础上解决了“背景采样”问题：不做模糊，改为录制页面图层 + AGSL 边缘折射，见 §8。）
- **新材质（一次绘制分层，替换原多层 background/border；`drawWithCache` 按尺寸 + 主题缓存，不每帧重建）**：
  ①底色：半透明竖向渐变——浅色“白 0.60 → surfaceContainerHighest 0.72”（近白 → 冷灰蓝），
  深色“surfaceContainerHigh 0.72 → surfaceContainerLow 0.78”（不再整片实心白 / 发灰）；
  ②顶部反光带：上半部分“白 0.40 / 0.10 → 透明”渐变；③左上镜面光斑：径向高光 0.12 / 0.05；
  ④底部内侧暗层：0.04 / 0.22，给下沿一点厚度；⑤内侧折射细环：离边缘约 2dp 的 1dp 细白线
  （浅 0.70 → 0.28，深 0.16 → 0.06）；⑥外圈发丝边：浅色“白 0.90 → 黑 0.06”渐变、
  深色两级低透明白；⑦阴影：12dp → 9dp 并减淡（ambient 0.05 / spot 0.10；彩色阴影在 Android 9+ 生效）。
- **选中项**：实色 `secondaryContainer` → 同色度半透明渐变（浅 0.58 → 0.44、深 0.50 → 0.38，
  上略亮下略深）；过渡仍为 160ms，且改为只动透明度（不再经过 `Color.Transparent` 的染黑中间态）；
  图标 / 文字仍用 `onSecondaryContainer`，对比度不变。
- **本次不改任何尺寸**：宽度规则、单项目标宽度、断点、62% 上限、胶囊高度、内边距、
  Insets 避让全部与 v1.3.1 一致（`DockWidthTest` 5 项断言未动）；观感以外的行为零变化。
- 改动文件：仅 `ui/components/FloatingGlassDock.kt`（材质 + 选中底 + 阴影）、
  `app/build.gradle.kts`（版本号 1.3.1 → 1.3.2，versionCode 4 → 5）、`README.md`。
  导航结构、各页面、`Common.kt`、`CuotiApp.kt`、主题文件、Room / ViewModel 等未改动。
- **验证边界**：本机无真机 / 模拟器，玻璃观感（通透度、明暗层次、深色模式、平板横屏比例）
  必须在手机 / 平板上装包实机对比；本节只声明“编译 + 60 项测试通过”，不声明视觉效果已达标。

### 8. 真实背景采样：AGSL 边缘折射 / 真背景模糊（v1.3.3 / v1.3.4，2026-10-19）
- **背景采样（不是截屏）**：`CuotiApp` 把整个 NavHost 的绘制录制进一个 GraphicsLayer
  （`rememberGraphicsLayer()` + `drawWithContent { GraphicsLayer.record(...) { drawContent() }; drawLayer(...) }`）。
  这里必须使用 **DrawScope 作用域内的 `GraphicsLayer.record` 扩展**（不是 `GraphicsLayer` 的成员重载）：
  扩展会把当前绘制上下文临时切到图层画布，`drawContent()` 才会真正录进图层。该修正于 v1.3.5 一并完成。
  这是同一帧、GPU 渲染节点级别的重录：没有 CPU 读回、没有 Bitmap、没有轮询或持续捕获；
  NavHost 先录、Dock 后画，同一帧内完成，静止时零额外开销；内容变化帧（滚动 / 切换动画）
  页面会多走一遍绘制（近似 ×2，只针对页面部分）。
- **折射子节点**：Dock 内新增与胶囊完全同尺寸的“背景采样”节点（`GlassRefraction.kt` 的
  `GlassBackdrop`），按 Dock 的屏幕位置（`positionInRoot`）把录制图层平移绘制，
  因此采到的就是 Dock 身后的真实像素；绘制被裁在胶囊圆角内，真材质（底色 / 高光 / 细环 /
  发丝边）照旧叠在它上面，导航项图标与文字不受任何影响。
- **AGSL 折射**：`android.graphics.RuntimeShader` + `RenderEffect.createRuntimeShaderEffect`，
  经 Compose 公开扩展 `asComposeRenderEffect()` 挂到 `Modifier.graphicsLayer { renderEffect }`
  （Compose 1.12 已把旧的 `androidx.compose.ui.graphics.RuntimeShader` 包装类移除，直接用框架类）。
  Shader 用「圆角矩形有符号距离场（SDF）」计算每个像素离边缘的远近：边缘 14dp 带内沿内向法线
  做轻微采样位移（最大 4dp、平方衰减，圆角处由数值梯度保证连续平滑、无矩形硬边）；
  中央区域位移为 0（保持清晰）；另加 4.5% 以内的边缘聚光。位移只向内，
  永远不会采到裁剪范围外的像素。Shader 只在组合时编译一次（remember），uniforms 只在尺寸
  变化时刷新——不逐帧编译、不逐帧设 uniform、无持续动画。
- **Android 12 / 12L（v1.3.4）：真背景模糊（磨砂玻璃）**：平台从 API 31 起才有 RenderEffect，
  但没有 AGSL（RuntimeShader 是 API 33 才有）；因此 12/12L 把采到的真实背景交给
  `Modifier.blur(16.dp, BlurredEdgeTreatment.Unbounded)`，得到 iOS 风格的磨砂玻璃。
  它是真背景 + 真模糊（不是纯色模拟），但不是逐像素折射位移；模糊只发生在胶囊内，
  用 Unbounded 避免胶囊边缘的模糊衰减成透明。Android 11 及以下没有可用的 GPU 后处理，
  继续用 v1.3.2 纯材质（不录制、不采样、零额外开销）。
- **透明底回调**：背景采样层启用时（Android 12+：13+ 折射 / 12 磨砂）底色略微调透
  （浅 0.60/0.72 → 0.50/0.62，深 0.72/0.78 → 0.62/0.70），让折射位移 / 磨砂背景看得见；
  Android 11 及以下保持 v1.3.2 原数值。
- **降级链**：API 31–32 → 真背景模糊（磨砂玻璃）；API < 31 → 不创建采样层、不录制图层
  （`SDK_INT` 判定，零额外开销、观感 = v1.3.2）；Android 13+ 上 Shader 构造包 `runCatching`，
  个别设备编译失败时自动退回磨砂模糊，不闪退。
- **尺寸与结构零变化**：宽度规则、胶囊高度、Insets 避让、导航目的地 / 图标 / 文字 / 点击 /
  选中状态、页面架构全部与 v1.3.2 一致；v1.3.3 / v1.3.4 都只涉及这 4 个文件：
  新增 `ui/components/GlassRefraction.kt`，改 `ui/components/FloatingGlassDock.kt`（采样层接线）、
  `ui/CuotiApp.kt`（录制层，唯一录制点）、`app/build.gradle.kts`（1.3.2 → 1.3.4，versionCode 5 → 7）。
- **验证边界**：本机无真机 / 模拟器，折射观感（Android 13+：滚动时边缘位移是否可见、圆角连续性）
  与磨砂观感（Android 12/12L：模糊强度、胶囊边缘是否发白）都必须在对应系统版本的手机上装包
  实机验收；本节只声明“编译 + 60 项测试通过 + dex 内含相关代码（`GlassBackdropState` / AGSL
  源码字符串 / `createRuntimeShaderEffect` / `modifier.blur` 调用）”，不声明视觉效果已达标。
  已知范围限制：采样对象是页面内容（NavHost）；Dock 上方悬浮的「添加错题」按钮等其它兄弟节点
  不在采样范围内（与 Dock 区域不重叠）。性能提示：Android 12/12L 的磨砂在内容滚动时每帧都要
  重算胶囊区域的模糊，老设备上若出现掉帧，可下调 `GlassBlurRadius`（16dp）或只在静止时模糊。

---

## 十五、顶栏 / 返回键 / 添加按钮 Liquid Glass 统一（v1.3.5，2026-10-20）

### 1. 本轮目标
把首页「添加错题」、所有二级页返回键、所有页面顶栏统一到 Dock 的玻璃语言；
让 7 个滚动页的正文可以滚到顶栏下方，顶栏对身后的真实页面内容做**真实背景模糊**，
模糊和透明度向正文柔和渐隐，不出现横向硬线或色带。只改 UI 组件与绘制，不动 Room / Entity / DAO /
ViewModel / 导航 / 图片处理 / 数据存储，也不改页面结构与尺寸。

### 2. 主要实现
- **共享材质**：新增 `ui/components/LiquidGlass.kt`，把 Dock 原有的 6 层玻璃材质抽成
  `liquidGlassSurface()`（半透明上亮下暗底 + 顶部反光带 + 左上镜面光斑 + 底部内暗层 +
  内侧折射细环 + 外圈发丝边），Dock、圆角 FAB、圆形返回键复用同一套颜色与层次。
  所有颜色仍从 `MaterialTheme` 派生，浅色 / 深色 / 动态取色自动适配。
- **顶栏真实模糊（Android 12+）**：`ScreenScaffold` 在内容槽里只录**顶栏高度的一条**
  页面内容（先铺主题背景色，再录正文），顶栏用 `Modifier.blur(20dp, Rectangle)` 对该图层做平台模糊，
  再用 `BlendMode.DstIn` 让模糊从顶栏 56% 处开始向底部渐隐；顶栏本身只叠色调 + 顶部高光，
  不画分隔线。Scaffold 的 body 和 topBar 都在 (0,0)，采样层直接按 0 偏移绘制，
  不需要等 `onGloballyPositioned` 回调，第一帧也不会整层跳过；DstIn 放在 blur 图层内部，
  不再额外套一层 `graphicsLayer(Offscreen)`。模糊只发生在顶栏区域，不整页模糊，
  不影响顶栏文字与正文清晰度。启用真模糊时顶栏色调降到浅色 0.55 / 深色 0.58、
  高光降到 0.10 / 0.035，保证模糊纹理能透出来；没有模糊的降级页仍用厚色调保证可读性。
- **7 个穿透页**：首页、统计、错题本分类、组卷导出、我的、复习、错题详情把顶部避让移进
  `LazyColumn.contentPadding`，内容容器用 `PaddingValues.withoutTopInset()` 只去掉顶部一份，
  左右和底部（Dock / 底栏避让）保持不变。正文初始位置仍在顶栏下方，滚动时才从顶栏后面穿过。
- **低版本降级**：Android 11 及以下没有可用的平台模糊，不录制顶栏内容、零额外开销，
  只显示半透明玻璃色调 + 顶部高光；不把普通透明伪装成“模糊”。
- **返回键**：所有 `ScreenScaffold` 的 `onBack` 统一替换为 `GlassBackButton`：
  48dp 触摸区 + 40dp 可见圆形玻璃，箭头居中；位于内容录制层内部，使用静态玻璃材质（不自采样），
  返回逻辑完全不变。首页（无 `onBack`）不出现返回键。
- **添加错题按钮**：`GlassExtendedFab` 保留原 M3 ExtendedFAB 的位置、尺寸、圆角比例、
  文案、图标和点击逻辑；容器透明、elevation 归零，玻璃画在其后，并复用 NavHost 录制层做
  Android 13+ 边缘折射 / Android 12/12L 真背景模糊，更低版本退回静态玻璃。
  按下时缩到 0.96 并轻微提亮，动画 120ms，不做夸张效果。
- **录制修正**：改用 DrawScope 作用域内的 `GraphicsLayer.record` 扩展，确保 `drawContent()`
  真正录进图层画布；Dock 的真实采样与顶栏、FAB 的采样都基于这一修正后的录制链路。

### 3. 性能约定
- Shader 仍在组合时创建一次（`remember`），uniforms 只在尺寸变化时刷新，不逐帧编译 / 设置。
- 顶栏只录一条（宽度 × 顶栏高度，约 1–2 MB 级别），不重复捕获整屏；
  整屏录制仍只有 Dock / FAB 共用的那一个 NavHost 图层。
- 渐变、路径按“尺寸 + 主题 + 按下状态”缓存；没有循环动画或持续模糊刷新。
- 未启用穿透的页面（错题列表、编辑、复习会话、大屏列表栏）不创建顶栏采样层，零额外开销。

### 4. 改动文件
新增：`ui/components/LiquidGlass.kt`
修改：`ui/components/Common.kt`（顶栏玻璃、内容薄条录制、返回键、`withoutTopInset`）、
`ui/components/GlassRefraction.kt`（`uRadius`、按组件圆角 / 模糊半径）、
`ui/components/FloatingGlassDock.kt`（改用共享材质）、`ui/CuotiApp.kt`（玻璃 FAB、录制扩展修正）、
`ui/home/HomeScreen.kt`、`ui/stats/StatsScreen.kt`、`ui/notebook/NotebookScreen.kt`、
`ui/paper/PaperScreen.kt`、`ui/profile/ProfileScreen.kt`、`ui/review/ReviewScreen.kt`、
`ui/detail/DetailScreen.kt`（7 个滚动页的顶部穿透）、`app/build.gradle.kts`（1.3.4 → 1.3.5，versionCode 7 → 8）、`README.md`。

### 5. 验证结果
- `assembleDebug + testDebugUnitTest --rerun-tasks --no-build-cache`：**BUILD SUCCESSFUL**，
  **60 项测试全部通过（0 失败 / 0 错误 / 0 跳过）**。
- APK 实测：versionCode 8 / versionName 1.3.5，minSdk 26 / targetSdk 36，权限清单与上一版一致
  （无新增权限）；dex 中已确认包含 `LiquidGlassKt`、`withoutTopInset` 与 `uRadius` 相关代码。
- 顶栏模糊链路修正：去掉采样层对 `sourceOrigin/targetOrigin` 就绪状态的整层门控，
  避免首帧或回调未到时顶栏只剩半透明色调；把 DstIn 移进 blur 图层、去掉多余的外层离屏层，
  并降低顶栏色调 / 高光不透明度，使 Android 12+ 的真实模糊在浅色和深色页面都能看见。
  低版本仍走既定降级，不会因为这次修正产生额外采样开销。
- **验证边界**：本机没有真机 / 模拟器，实际模糊观感、滚动跟手程度、浅 / 深色对比度和
  手机 / 平板布局必须装包实机验收。建议重点检查：
  1. Android 13+ 真机：任选统计 / 详情 / 我的页，上下滚动时顶栏是否真实模糊身后卡片，
     底边是否柔和渐隐、没有横向硬线；
  2. 浅色与深色模式来回切换：顶栏、返回键、FAB 是否有突兀色带或过暗 / 过白；
  3. Android 12 / 12L：同一套顶栏应显示磨砂模糊；Android 11 及以下应优雅退回半透明玻璃；
  4. 详情页 / 统计页返回键：48dp 触摸区是否好点，箭头是否居中，返回逻辑是否正常；
  5. 首页 / 错题页「添加错题」：位置、尺寸、文案、点击是否与旧版一致，按下缩放是否自然；
  6. 手机竖屏与平板横屏：顶栏宽度、内容限宽、FAB 位置是否正常。

---

## 十六、底部 Dock Liquid Glass 第二轮重构（v1.3.6，2026-10-21）

### 1. 本轮目标
上一版的 Dock 已经有真实背景采样，但材质仍是一套“主题色半透明胶囊”：
浅色底部发灰、深色接近深灰板，内圈 + 外圈形成双描边，选中项是一块高不透明度青绿色块。
本轮不改变 Dock 的位置、整体尺寸、四个导航项、图标、文字、选中逻辑和导航功能，
只重构材质层次与选中态，让颜色来自背景、边缘来自受光、选中态融入同一套玻璃系统。

### 2. 主要实现
- **Dock 专用材质**：新增 `dockGlassSurface()`，不再复用 `liquidGlassSurface()` 的
  `surfaceContainer*` 底材。浅色使用中性白 scrim（有采样 0.24→0.16，无采样 0.56→0.44），
  深色使用中性黑 scrim（有采样 0.30→0.22，无采样 0.60→0.50）。
  这样背景颜色不会再被灰蓝色底材洗掉，动态取色也不会把 Dock 染成紫色 / 灰蓝色。
- **去掉双描边**：删除 2dp 内缩的 1dp 内环，只保留一条 0.75dp 的 `sweepGradient` 扫光边缘；
  左上白、右上淡、右下带一点暗边，边缘亮度随轮廓自然变化，不再像套了两圈边框。
- **高光重做**：删除覆盖上半部分的 42% 全宽白色亮带，改成左上局部径向镜面 +
  很淡的右侧第二光斑 + 顶部轻提亮；底部暗层从深色 0.22 降到 0.10，浅色仅 0.015。
- **背景后处理**：Android 13+ 在 AGSL 边缘折射之外增加 6dp 小半径平台模糊，
  shader 内增加 1.08 饱和度 / 1.02 对比度；折射带 14→16dp、最大位移 4→4.5dp、
  边缘聚光 0.045→0.03。Android 12/12L 的磨砂模糊从 16dp 降到 12dp；Android 11 及以下
  仍不采样，使用加厚一档的中性静态玻璃回退。
- **选中态改为共享玻璃透镜**：整排只画一个选中透镜，位置和尺寸来自每个 item 的真实 bounds，
  用 220–260ms 弹簧在导航项之间滑动；相对 item 水平内缩 4dp、垂直内缩 3dp，
  不再铺满整个单元格。透镜由低透明度 `secondary` 青绿（浅色 0.14→0.08，深色 0.20→0.12）
  + 顶部柔光 + 0.5dp 同款扫光边组成；选中图标 / 文字改用 `secondary` 并保持 SemiBold，
  未选中文字从偏浅的 `onSurfaceVariant` 改为 `onSurface` 约 0.74–0.78 透明度，提升可读性。
- **阴影减重**：Dock 阴影从 9dp / 黑 0.05+0.10 降到 8dp / 黑 0.03+0.05，
  减少浅色背景上的暗晕，轮廓主要交给单圈边缘光。
- **作用范围**：`liquidGlassSurface()` 保持原样，继续服务圆角 FAB 和返回键；顶栏
  `TopBarGlassBackdrop` / `liquidTopBarTint` 完全不动，避免回退 v1.3.5 的顶栏修复。

### 3. 保持不变
Dock 位置、64dp 最小高度、10dp 底部留白、14dp 侧边距、8dp 内容内边距、
四等分宽度、平板 62% 上限、图标 22dp、文字样式、`selectable` / `Role.Tab`、
`item.route == selectedRoute` 的选中逻辑、所有导航路由与点击回调均未改动。
共享透镜只做视觉层，不参与点击和语义，触摸目标仍是原来的完整导航项。

### 4. 改动文件
修改：`ui/components/FloatingGlassDock.kt`（Dock 材质调用、单圈边缘、共享选中透镜、
选中 / 未选中文字色）、`ui/components/LiquidGlass.kt`（新增 `dockGlassSurface()` /
`dockSelectionSurface()`）、`ui/components/GlassRefraction.kt`（6dp 模糊 + 饱和度 / 对比度、
折射参数、12dp 磨砂回退）、`app/build.gradle.kts`（1.3.5 → 1.3.6，versionCode 8 → 9）、
`README.md`。Room / Entity / DAO / ViewModel / 导航 / 图片处理 / 数据存储均未改动。

### 5. 验证结果
- `assembleDebug + testDebugUnitTest --rerun-tasks --no-build-cache`：**BUILD SUCCESSFUL**，
  **60 项测试全部通过（0 失败 / 0 错误 / 0 跳过）**。
- APK 实测（第二轮当时构建，非当前 APK）：14,566,781 字节，versionCode 9 / versionName 1.3.6，
  minSdk 26 / targetSdk 36；与上一版同一 debug 证书，可直接覆盖安装；
  当时未新增依赖、权限或敏感 API。
- **验证边界**：本机没有真机 / 模拟器，无法直接验证实际模糊、折射、饱和度和浅 / 深色对比度。
  上面的参数是按现有实现与目标观感给出的起点值，装包后重点检查：
  1. Android 13+：Dock 中央是否保留轻微磨砂而不是完全清晰，边缘折射是否自然、没有发灰；
  2. Android 12/12L：12dp 磨砂是否足够柔和，浅色 / 深色下文字是否清晰；
  3. Android 11 及以下：静态回退是否仍然通透，不会重新变成深灰板；
  4. 浅色 / 深色切换：Dock 是否不再有灰蓝色底、选中透镜是否和主体材质连续；
  5. 手机竖屏 / 平板横屏：选中透镜滑动位置是否对准，RTL 和字体放大是否正常；
  6. 彩色卡片 / 图片滚到 Dock 后方时，未选中文字和选中青绿文字是否仍然可读。
  如果实机上文字对比不足，优先小幅提高中性 scrim（约 3–5 个百分点）或加极淡的文字光晕，
  而不是退回旧版的高不透明度底材。

---

## 十七、Dock Liquid Glass 第三轮：Android 13+ 真透镜（v1.3.6 后续构建，2026-10-22）

### 1. 这轮解决什么

第二轮已经把 Dock 的灰蓝底材、双描边和厚重阴影压下去了，但选中项仍然主要靠
“青绿色半透明胶囊 + 高光边”表达，所以看起来像一块色块；底板在深色页面上也还是偏暗。
这轮不改 Dock 的位置、尺寸、四个入口、图标、文字、选中逻辑和导航，只把材质换成
**页面背景采样 + 底板折射 + 选中项组合采样**，让颜色真正来自身后内容。

### 2. 选型与许可证

- 使用 `io.github.kyant0:backdrop:2.0.1`（AndroidLiquidGlass 作者的 Backdrop 库），
  实际依赖为 `backdrop-android:2.0.1`，传递依赖 `io.github.kyant0:shapes:1.2.1`。
- 许可证为 **Apache-2.0**，允许商业使用和修改；不使用许可证元数据不清的同类库。
- 兼容门槛来自库本身：`RenderEffect` 需要 API 31+，RuntimeShader / AGSL 需要 API 33+；
  低于门槛时不会强行启用，因此不会为了这个效果提高 `minSdk 26`。
- 没有全量替换 UI，只在 Dock 局部接入；Room、ViewModel、导航、图片处理均未改动。

### 3. 实现结构

- **页面背景层**：`CuotiApp` 在 API 31+ 用 `rememberLayerBackdrop()` 创建页面背景，
  NavHost 通过 `Modifier.layerBackdrop(pageBackdrop)` 录制；API 26–30 不创建图层，保持零额外开销。
- **Dock 底板**：API 33+ 由 `LiquidDockBackground` 使用 `drawBackdrop`：
  `colorControls(1.08 / 1.02) → blur(6dp) → lens(16dp / 18dp)`。
  浅色中性白 scrim 28%，深色中性黑 22%；只保留库的一套 Ambient 高光，`shadow = null`，
  外层继续用项目原有的 8dp 悬浮阴影，避免双层阴影和双描边。
- **选中透镜**：API 33+ 使用 `rememberCombinedBackdrop(pageBackdrop, dockBackdrop)`，
  让选中项同时采样“页面 + Dock 底板”；透镜参数为 `lens(10dp / 14dp, chromaticAberration = true)`，
  浅色品牌色 tint 9%，深色 12%。图标和文字不进入采样，保持清晰；没有引入官方示例里的透明复制 Row。
- **导出层**：Dock 背景用 `exportedBackdrop = dockBackdrop` 导出，只包含背景、底板表面和
  前层材质，不包含图标、文字、选中透镜，避免图层循环。
- **快速回退**：`DockLiquidGlass.kt` 里保留编译期开关 `DockLiquidGlassEnabled`。
  如果真机出现崩溃、卡顿或图层错误，先改为 `false` 回到第二轮路径，再决定是否继续移植库核心。

### 4. API 分档

| 版本 | Dock 行为 |
|---|---|
| Android 13+（API 33+） | 页面 LayerBackdrop + 库底板 blur/lens + 选中项组合背景 + 色散透镜 |
| Android 12/12L（API 31–32） | 保留项目原有的 `GlassBackdrop` 真背景模糊（12dp）和静态选中材质 |
| Android 8.0–11（API 26–30） | 保持中性静态玻璃 + 单圈边缘，不录制页面、不做模糊 |

### 5. 改动文件

- 新增：`ui/components/DockLiquidGlass.kt`（库封装、底板、选中透镜、开关）
- 修改：`ui/CuotiApp.kt`（页面 LayerBackdrop 接线、FAB/Dock 参数）
- 修改：`ui/components/FloatingGlassDock.kt`（API 分档、底板/透镜接入、旧路径保留）
- 修改：`app/build.gradle.kts`（新增 `backdrop:2.0.1` 依赖）
- 未改动：Dock 的布局尺寸、四个导航项、图标、文字、`selectedRoute` 判断、所有路由和点击回调。

### 6. 构建与验证结果

- `:app:compileDebugKotlin`：成功。
- `:app:assembleDebug`：成功。
- `:app:testDebugUnitTest`：**60 项，0 失败 / 0 错误 / 0 跳过**。
- APK：15,562,252 字节，SHA-256 `7178D80F8393FC3C66FC23DAEDD86E6D35448830E9CAE85F3DEF64CD9127FFAA`；
  `aapt2` 确认 package `com.cuotixingqiu.app`、versionCode 9、versionName 1.3.6、
  minSdk 26、targetSdk 36，权限清单未新增敏感权限。

### 7. 尚未完成的验收（必须真机）

本机没有真机 / 模拟器，以下项目**没有实际跑过**，不能把“有 shader / 编译通过”当成效果验收：

1. Android 13+：底板是否真的透出背后内容、边缘折射是否自然、选中项是否不再像色块；
2. AGSL 在目标 GPU 上是否编译成功，滚动、页面切换和 Dock 选中动画是否掉帧；
3. Android 12/12L 的 12dp 磨砂和 Android 11 及以下的静态回退是否仍清晰；
4. 浅色 / 深色、彩色图片、平板横屏、RTL、字体放大下的文字对比度和透镜对位；
5. 首帧、连续快速切换 Tab、进出详情页时是否出现空白、闪黑、图层错位或崩溃。

如果 API 33+ 出现异常，先关闭 `DockLiquidGlassEnabled` 回退；低版本路径不受这次库接入影响。

---

## 十八、五页视觉重设计（v1.3.7，2026-10-31）

本轮只重做 Dock **上方**的五个页面（首页、错题、错题详情、复习、我的），目标是更接近
「极简、留白、轻卡片」的参考视觉，同时保留真实数据、ViewModel、导航、Room、拍照 / 图片
逻辑和原有悬浮玻璃 Dock。没有新增数据库表，也没有用示例题目或示例统计替换真实数据。

### 1. 设计规范

| 角色 | 浅色值 | 说明 |
|---|---|---|
| 页面背景 | `#F1F2EF` | 暖灰底，和纯白卡片拉开层次 |
| 卡片 | `#FFFFFF` | 细边框、无厚重阴影 |
| 品牌主色 | `#3539DF` | 主按钮、选中态、强调数字 |
| 主文字 | `#17191D` | 标题与正文 |
| 次要文字 | `#858B96` | 日期、说明、辅助信息 |
| 分割线 | `#E5E7E4` | 1dp 轻分割 |
| 浅蓝标签 | `#E9EBFF / #4653D5` | 知识点、浅色选中底 |

学科色：数学 `#3446E8`、物理 `#087F95`、化学 `#C65A00`、生物绿色、英语 `#D41460`。
卡片圆角 22dp、按钮高度 54dp、按钮圆角 16dp；宽屏内容最大 680dp 并居中。
深色主题使用同一套语义色，`surfaceContainer` 系列显式指定，不落回 Material 默认紫色。
状态栏 / 导航栏图标在 `MainActivity` 按当前主题切换明暗，手动切深色时也清楚。

### 2. 页面结构

- **首页**：品牌日期头 + 动态问候 / 今日任务标题 → 深色学习统计卡（连续学习、今日待复习、
  已掌握）→ 全宽「添加错题」→ 今日复习任务（最多 3 条真实到期题）→ 最近添加（真实数据）。
  统计卡可点进统计页；今日任务为空时点卡片进入复习页。
- **错题**：标题 + 真实总数 + 圆形加号 → 搜索 → 横向学科筛选 → 筛选 / 排序行 →
  轻量纵向列表。搜索、分类 / 状态 / 难度 / 知识点筛选、三种排序、多选批量操作、
  大屏左右分栏全部保留。
- **错题详情**：圆形玻璃返回键 + 标题 + 更多菜单 → 真实学科 / 日期 / 复习次数 →
  知识点 → 题目卡（图片点击放大）→ 错因 / 解析 / 总结 → 复习信息 →
  底部「编辑 / 加入今日复习」。更多菜单支持编辑、标记已掌握 / 重新加入复习、删除。
- **复习**：日期标题 → 圆环进度统计卡 → 全宽「开始复习」→ 真实学科掌握进度 →
  未来三天安排。各学科进度由真实错题按 `subject + status` 聚合，未来安排来自
  `nextReviewAt` 查询。
- **我的**：通用身份「同学 / 星」→ 真实累计 / 已掌握 / 连续复习 → 近 7 天真实柱状图 →
  外观与主题、错题本分类、JSON 备份 / 导入、PDF 组卷、清空数据。身份使用通用占位，
  因为现有数据模型没有姓名 / 头像字段，不伪造用户信息。

### 3. Dock 与底部安全区

- `FloatingGlassDock.kt`、`LiquidGlass.kt`、`GlassRefraction.kt` 等 Dock 实现文件未修改；
  胶囊尺寸、液态玻璃、模糊 / 折射、高光、图标文字、选中动画和导航逻辑保持原样。
- 仅通过 `CuotiApp.kt` 的 `showDock` 在**完整错题详情路由**隐藏 Dock；大屏
  「列表 + 详情」分栏仍显示 Dock，并给详情底栏传入 `reserveDockClearance = true`。
- 首页和错题页移除了原悬浮添加 FAB：首页使用全宽按钮，错题页使用标题右侧圆形加号。
  其余页面仍按原有 `rememberDockClearance()` 给最后一条内容留出 Dock 避让。

### 4. 数据来源

- 首页「连续学习」和我的页「近 7 天」由现有 `lastReviewedAt` 推导，
  `TimeUtils` 提供按本地日期聚合，不新增表。
- 我的页累计 / 已掌握 / 学科数 / 加入天数来自真实 `WrongQuestionEntity`；
  错题本数量和图片数量来自现有仓库 Flow。
- 复习页统计、学科进度、未来三天安排均来自真实查询；没有写死示例数字。

### 5. 修改文件

新增：`ui/components/RedesignComponents.kt`。

修改：`test/.../util/TimeUtilsTest.kt`（原有 2 项 + 新增 6 项统计覆盖）；
`ui/theme/Color.kt`、`Type.kt`、`Theme.kt`；`ui/components/Common.kt`（未知学科回退）；
`ui/CuotiApp.kt`（详情隐藏 Dock、移除 FAB、详情路由参数）；`MainActivity.kt`（系统栏图标）；
`ui/home/HomeScreen.kt`、`HomeViewModel.kt`；`ui/list/QuestionListScreen.kt`、`QuestionListViewModel.kt`；
`ui/detail/DetailScreen.kt`；`ui/review/ReviewScreen.kt`、`ReviewViewModel.kt`；
`ui/profile/ProfileScreen.kt`、`ProfileViewModel.kt`；`util/TimeUtils.kt`；
`app/build.gradle.kts`（1.3.6 → 1.3.7，versionCode 9 → 10）；`README.md`。

### 6. 验证结果

- `:app:compileDebugKotlin`：成功，无警告。
- `:app:assembleDebug`：成功；APK 14,878,545 字节，versionCode 10 / versionName 1.3.7，
  SHA-256 `CB536EE85D58122812E4431EC289F16D1D8624D796B056DB677944313D011EAB`。
- `:app:testDebugUnitTest`：**66 项全部通过，0 失败 / 0 错误 / 0 跳过**。
- `aapt2 dump badging` 确认包名 `com.cuotixingqiu.app`、minSdk 26 / targetSdk 36、
  versionCode 10 / versionName 1.3.7。
- **验收边界**：本机 `adb devices` 为空，没有真机或模拟器。以上只证明编译、打包、
  单元测试和 APK 元数据；视觉观感、滚动性能、AGSL 玻璃效果、横竖屏、
  深色 / 浅色对比度和拍照录入流程必须装到真机上验收。

## 十九、其余五页视觉收口 + 工程清理（v1.3.8，2026-11-02）

### 1. 这轮解决什么

v1.3.7 只重做了 Dock 上方的五个页面，**其余 5 个页面仍在用 v1.1 的组件体系**
（`Card` + `SectionTitle` + `MetricCell` + `M3 FilterChip` 风格）。从「我的」进错题本 / 组卷、
从列表进添加编辑、从复习页进复习卡片时，底色会从 `#F1F2EF` 跳回 `#F6F7FB`，
圆角、输入框和统计数字的观感也明显是上一代。

本轮把这 5 个页面并入同一套视觉语言，并顺手清掉被取代的死代码；
**没有改动 Room、DAO、ViewModel、导航、复习算法、导入导出和 PDF 逻辑**。

### 2. 页面改动

| 页面 | 主要变化 |
|---|---|
| 添加 / 编辑 | 顶部区域改用 `SectionHeader`；保存按钮换成统一的 54dp 主按钮（含加载态）；页面左右边距 20dp、内容最大 680dp；知识点标签改为浅蓝胶囊（点一下删除）；图片缩略图圆角 16dp |
| 复习（复习卡片） | 顶部进度条改为 4dp 细圆角条；学科标签改为 `SubjectDot` + 文字；题干/错因/解法/总结改用 `CuotiCard`；三档自评按钮统一 54dp 高、16dp 圆角，靠底色区分轻重（白 / 青绿 / 品牌蓝）；完成页小结改为卡片 + 数字行 |
| 统计 | 概览四数字改为卡片内 2×2 + 细分隔线；学科环形图与图例重排；近 7 天柱状图改为品牌色圆角柱（今天实色、其余 24% 透明度）；难度与阶段分布改用细圆角进度条 |
| 错题本分类 | 说明卡 + 分类列表合并成一张卡（行间细分隔线）；右上角 + 号改成品牌色；空状态用 `QuietEmptyCard` |
| 组卷导出 | 范围选择、试卷设置合并为卡片；开关行统一；生成按钮改为主按钮（加载态显示转圈）；「将导出 N 道题」单独成行 |

统一后的公共约定（都在 `ui/components/RedesignComponents.kt` 的 `CuotiVisual` 里）：
页面左右边距 20dp、卡片圆角 22dp、按钮高 54dp / 圆角 16dp、内容最大宽 680dp。

### 3. 死代码清理

- 删除 `ui/components/QuestionCard.kt`（v1.1 的题目卡，重设计后已无调用）。
- 删除 `Common.kt` 里已无调用点的 `SectionTitle`、`GroupLabel`、`SubjectBadge`、`SmallBadge`、
  `MetricCell`、`InfoRow` 与私有辅助 `softColors`。
- 修掉 `MenuAnchorType` 的弃用警告（改用 `ExposedDropdownMenuAnchorType`），
  编译输出现在**零警告**。

### 4. 版本与验证

- `:app:compileDebugKotlin`：成功，**无警告**。
- `:app:assembleDebug`：成功；APK **14,697,853 字节**，versionCode 11 / versionName 1.3.8，
  SHA-256 `D8C3C1FC53800077D5FF7C9A3BECD248AF133B647511D68087C688B7E9094886`，
  生成时间 2026-11-02 23:08:53。
- `:app:assembleRelease`：成功；未签名产物 10,330,240 字节（约 9.85 MiB）。
- `:app:testDebugUnitTest`：**66 项全部通过，0 失败 / 0 错误 / 0 跳过**（本轮未改业务逻辑，测试数量不变）。
- APK 内 19 个 dex、4 个 ABI 的 2 个原生库，包名 `com.cuotixingqiu.app`、
  minSdk 26 / targetSdk 36 / compileSdk 37，权限清单与上一版一致（无新增权限）。
- **验收边界**：本机 `adb devices` 为空，没有真机也没有模拟器，所以以上仍然只证明
  “编译 + 打包 + 单测 + APK 元数据”；这 5 个页面的实际观感、字体放大、深色模式和触控
  必须装到手机上按第十章清单验收。
- 本版单独的实机清单见 [`docs/verify-latest.md`](docs/verify-latest.md)（针对这 5 个页面，约 10 分钟）。

## 二十、顶栏 / 返回键 / 保存按钮与 Dock 材质统一（v1.3.9，2026-11-02）

### 1. 用户实机反馈的三个问题（都发生在添加 / 编辑页）

1. 二级页顶栏**没有背景模糊**；
2. 返回按钮的玻璃和底部 Dock **不是同一套材质**；
3. 底部「保存错题」还是**实心蓝色 M3 按钮**。

### 2. 原因

- **顶栏没有模糊**：添加 / 编辑页没有开启 `ScreenScaffold` 的 `contentBehindTopBar`，
  正文被顶栏 inset 整体下推，录制层录不到“顶栏身后那一条内容”，所以只剩半透明色调。
  复核后发现复习卡片页（`ReviewSessionScreen`）也是同样情况。
- **返回键材质不一致**：`GlassBackButton` 用的还是旧的 `liquidGlassSurface`
  （`surfaceContainer*` 主题底材 + 内圈 / 外圈双描边），而 Dock 在 v1.3.6 已换成
  `dockGlassSurface`（中性白 / 黑 scrim + 单圈 sweep 扫光边 + 左上局部镜面 + 8dp 轻阴影），
  两者并排就能看出差别。
- **保存按钮**：用的是 M3 `Button` 的实心容器。

### 3. 改动

- **顶栏模糊**：添加 / 编辑页与复习卡片页开启 `contentBehindTopBar`，
  并把顶部避让移进滚动内容（第一项 `Spacer(topInset)`）。正文初始位置仍在顶栏下方，
  只有滚动时才从顶栏后面穿过；Android 12+ 走原有的
  「只录顶栏高度一条内容 → `blur(20dp, Rectangle)` → `DstIn` 底部渐隐」链路。
- **顶栏色调**：从 `surfaceContainer*` 灰蓝底材改为**中性白 / 黑 scrim**
  （有模糊时浅色 0.46、深色 0.50），与 Dock 同一族；没有模糊的降级页仍是 0.84 / 0.90，
  保证标题可读。不画横向分隔线。
- **返回键**：`GlassBackButton` 改用 `dockGlassSurface`（中性 scrim + 单圈扫光边 + 左上镜面），
  阴影 3dp → 4dp 并按 Dock 的减重口径调淡，按下 0.94 缩放 + 轻微提亮。
  这个组件被所有二级页共用，一处改动全部生效。
- **保存按钮**：新增 `GlassActionButton`（`LiquidGlass.kt`）：全宽 54dp / 16dp 圆角，
  容器透明、elevation 归零，玻璃画在按钮后面；支持 loading 转圈与 disabled 态；
  按下 0.97 缩放 + 提亮，节奏和 Dock 选中一致。底部「保存错题 / 保存修改」已换用它。
  它接受可选的 `backgroundLayer`：传入录制层时（API 33+）可以和 Dock 一样做背景采样 / 折射，
  当前底部栏不传，因此用的是与 Dock 在 Android 11 及以下相同的**静态中性玻璃**。

### 4. 保持不变

- 顶栏高度、标题层级、返回按键的 48dp 触摸区与返回逻辑、保存按钮的尺寸 / 文案 / 点击 / 加载态
  / 键盘避让全部未变；`ScreenScaffold` 的 API 没有破坏性改动。
- Room、DAO、ViewModel、导航、复习算法、导入导出、PDF 组卷逻辑均未改动。

### 5. 验证结果

- `:app:compileDebugKotlin`：成功，**零警告**。
- `:app:assembleDebug`：成功；APK **14,697,853 字节**，versionCode 12 / versionName 1.3.9，
  SHA-256 `BEC88385FFF34B726E642BA1B6E693EFEA3D443D36909C738E31DAC017120D87`，
  生成时间 2026-11-02 23:22:59。
- `:app:assembleRelease`：成功；未签名产物 10,330,240 字节（约 9.85 MiB）。
- `:app:testDebugUnitTest`：**66 项全部通过，0 失败 / 0 错误 / 0 跳过**。
- **验收边界**：本机仍然没有真机 / 模拟器，所以“顶栏是否真的模糊、玻璃观感是否一致”
  只能通过实机确认，因此这一版的重点验收项就是这三处（见 `docs/verify-latest.md` 第 1、6 节）。

### 6. 尚未统一的（可选，等你决定）

- 组卷页「生成 PDF」等其它页面主按钮仍是实心品牌蓝；如果希望全 App 的主动作都换成玻璃，
  把调用从 `PrimaryActionButton` 换成 `GlassActionButton` 即可，一处一行。

### 7. 后续修正：保存按钮改为独立悬浮胶囊（v1.3.10，2026-11-02）

实机看过 v1.3.9 之后，用户提了两点：**保存按钮下面还有一整条深色底栏**、**按钮不是 Dock 那种形状**，
并且希望返回键和保存按钮都真正具备 Dock 的液态玻璃观感。这一轮做的都是表现层：

- **去掉底栏**：删掉 `bottomBar`（连同 `Surface(background)` 和分隔线），保存按钮改为
  `Box(align = BottomCenter)` 悬浮在内容之上，底下不再有任何实色条。
- **形状改成胶囊**：`GlassActionButton` 的 shape 从 16dp 圆角矩形改为 `RoundedCornerShape(percent = 50)`，
  与 Dock 本体同一种形状语言；高度仍是 54dp。
- **安装真实背景采样**：新增 `Modifier.glassButtonBackdropRecorder`（`Common.kt`），
  只把**按钮大小的一块**（约 54dp 高、宽 = 屏宽 - 40dp）内容录进离屏图层；
  按钮用 `GlassBackdrop` 按 (0,0) 对齐采样，于是 Android 12/12L 是真实背景模糊、
  Android 13+ 是模糊 + 边缘折射，和 Dock / FAB 走的是同一条链路。低于 Android 12 不创建图层，
  退回中性静态玻璃（与 Dock 在低版本上一致）。
- **返回键也采样**：`ScreenScaffold` 把顶栏那条内容图层传给 `GlassBackButton`，
  返回圆钮因此不再是"纯静态玻璃"，滚动时能透出身后的真实内容；图层只含正文、不含顶栏，
  不会自采样。
- **内容避让**：表单底部改为 `Spacer(按钮高度 + Dock 避让 + 18dp)`，最后一条内容能完整滚出按钮上方；
  键盘弹出时按钮跟随 `imePadding` 上移。
- **性能**：新增的录制只覆盖按钮那一块（不是整页），和顶栏那条同一量级；
  图层与 shader 都在 `remember` 里创建一次，不逐帧重建。

验证：`compileDebugKotlin` 零警告；`assembleDebug` / `assembleRelease` / `testDebugUnitTest`
（66 项）全部成功；APK 14,697,853 字节，versionCode 13 / 1.3.10，
SHA-256 `D8F7E3B1B7DE2A9AF9AF9D00D65FC2107FC1699724C27D1B8299D0B957E05620`。
真机观感仍未验收，重点就是"胶囊形状 / 没有底栏 / 能透出身后内容"这三件事。

## 二十一、版本记录

- v1.3.10（2026-11-02）：保存按钮改为独立悬浮胶囊。删掉编辑页的底栏（不再有一整条深色条），
  按钮形状改为与 Dock 一致的胶囊；新增 `glassButtonBackdropRecorder` 只录制按钮大小的区域，
  保存按钮与返回键都改用真实背景采样（Android 12+ 模糊 / 13+ 折射），低于 12 退回中性静态玻璃；
  表单底部重新留出按钮高度 + Dock 避让。
  `:app:compileDebugKotlin`（零警告）、`:app:assembleDebug`、`:app:assembleRelease`、
  `:app:testDebugUnitTest` 均成功，66 项测试全部通过；versionCode 13。真机观感未验收。
  详见第二十章第 7 节。
- v1.3.9（2026-11-02）：顶栏 / 返回键 / 保存按钮与 Dock 材质统一。添加/编辑与复习卡片页
  开启顶栏真实背景模糊（顶部避让移进滚动内容）；顶栏色调改中性白/黑 scrim；
  `GlassBackButton` 改用 `dockGlassSurface`；新增 `GlassActionButton` 并用于底部保存按钮。
  `:app:compileDebugKotlin`（零警告）、`:app:assembleDebug`、`:app:assembleRelease`、
  `:app:testDebugUnitTest` 均成功，66 项测试全部通过；versionCode 12。真机观感未验收。
  详见第二十章。

- v1.3.8（2026-11-02）：其余五页视觉收口 + 工程清理。添加/编辑、复习卡片、统计、
  错题本分类、组卷导出并入 v1.3.7 的暖灰底 + 白卡 + 品牌蓝紫体系；统一页面边距、
  卡片圆角与按钮尺寸；删除 `QuestionCard.kt` 与 6 个已无调用的公共组件；修掉弃用警告。
  `:app:compileDebugKotlin`（零警告）、`:app:assembleDebug`、`:app:assembleRelease`、
  `:app:testDebugUnitTest` 均成功，66 项测试全部通过；versionCode 11。真机视觉尚未验收。
  详见第十九章。

- v1.3.7（2026-10-31）：五页视觉重设计。首页、错题、错题详情、复习、我的统一为
  暖灰背景 + 白卡片 + 品牌蓝紫；新增深色学习统计卡、圆环复习统计、真实学科进度、
  近 7 天柱状图、通用身份头；首页 / 错题页移除悬浮 FAB；完整详情页隐藏 Dock，
  大屏分栏保留 Dock；新增 `RedesignComponents.kt` 与 6 项时间统计测试。
  `:app:compileDebugKotlin`、`:app:assembleDebug`、`:app:testDebugUnitTest` 均成功，
  66 项测试全部通过；versionCode 10。真机视觉 / 触控尚未验收。详见第十八章。
- v1.3.6（2026-10-22，第三轮构建）：Android 13+ Dock 接入 `backdrop:2.0.1`，
  用页面 + Dock 组合背景实现选中透镜，底板使用模糊 + 折射 + 单套高光；API 31–32 和
  API 26–30 保持原有降级路径。Dock 位置、尺寸、四个入口和导航逻辑未动；
  `:app:compileDebugKotlin`、`:app:assembleDebug`、`:app:testDebugUnitTest` 均成功，
  60 项测试全部通过；真机视觉 / 性能尚未验收。详见第十七章。
- v1.3.6（2026-10-21，第二轮）：底部 Dock Liquid Glass 第二轮重构。Dock 改用专用 `dockGlassSurface()`：
  删除主题色底材与内圈，改中性白 / 黑 scrim + 左上局部镜面 + 单圈 0.75dp 扫光边 + 8dp 轻阴影；
  选中态改为共享青绿玻璃透镜，按 item 真实 bounds 弹簧滑动并内缩 4dp / 3dp，内容色改用 `secondary`；
  Android 13+ 增加 6dp 模糊 + AGSL 折射 / 饱和度 / 对比度，Android 12/12L 磨砂降为 12dp，
  Android 11 及以下中性静态玻璃回退。位置 / 尺寸 / 导航 / 选择逻辑未动；60 项测试全部通过
  （含强制全量重跑）；versionCode 9。
- v1.3.5（2026-10-20）：顶栏 / 返回键 / 添加按钮 Liquid Glass 统一。新增共享材质
  `ui/components/LiquidGlass.kt`；顶栏在 Android 12+ 对身后一条真实页面内容做平台模糊
  （20dp + 底部 DstIn 渐隐，无横向硬线），Android 11 及以下退回半透明 + 高光；
  采样层直接按 Scaffold 的 (0,0) 对齐，不依赖原点回调，DstIn 在 blur 层内完成；
  7 个滚动页正文可穿到顶栏下方；所有二级页返回键统一为 48dp 触摸区 + 40dp 圆形玻璃；
  首页 / 错题页「添加错题」保留原 M3 尺寸与位置，改为玻璃容器 + 按下 0.96 缩放；
  修正 GraphicsLayer 录制为 DrawScope 扩展调用，确保真实采样。只改 UI，不动数据 / 业务 / 导航；
  60 项测试全部通过（含强制全量重跑）；versionCode 8。
- v1.3.4（2026-10-19）：悬浮 Dock 低版本回退补齐。Android 12 / 12L（API 31–32）启用
  真背景模糊（磨砂玻璃：录制层 + `Modifier.blur(16dp, Unbounded)`），背景是真实采样、
  不再只是纯色模拟；Android 13+ 仍为 AGSL 边缘折射；Android 11 及以下保持 v1.3.2 纯材质
  （不录制、零额外开销）。折射 Shader 创建失败时自动退回磨砂。尺寸 / 宽度 / 避让 / 导航结构未动；
  60 项测试全部通过（含强制全量重跑）。versionCode 7。
- v1.3.3（2026-10-19）：悬浮 Dock 真实背景采样 + AGSL 边缘折射（Liquid Glass 精修）。
  页面内容经 GraphicsLayer 录制（同帧 GPU 节点，无截屏 / 无读回 / 无轮询），Dock 采样身后真实像素，
  在边缘 14dp 带内做最大 4dp 的向内采样位移（圆角矩形 SDF，圆角连续、中央清晰），另加极淡边缘聚光；
  折射启用时底色微调透；Android 12 及以下 / Shader 异常自动退回 v1.3.2 纯材质（零开销）。
  尺寸 / 宽度 / 避让 / 导航结构全部未动；60 项测试全部通过（含强制全量重跑）。versionCode 6。
- v1.3.2（2026-10-18）：悬浮 Dock 液态玻璃材质精修（只改观感，不改任何尺寸）。
  真背景模糊实测不可行（blur / RenderEffect 只能糊自己、窗口级模糊不适用、不引第三方依赖），
  改为一次绘制分层模拟：半透明上亮下暗渐变底、顶部反光带、左上镜面光斑、底部内侧暗层、
  内侧折射细环、外圈发丝边；阴影 12→9dp 并减淡；选中底从实色改为半透明青绿渐变（过渡只动透明度）。
  宽度 / 高度 / 避让与 v1.3.1 完全一致；60 项测试全部通过（含强制全量重跑）。versionCode 5。
- v1.3.1（2026-10-18）：悬浮 Dock 横屏自适应宽度修订。手机竖屏保持 v1.3.0 的尺寸不变；
  窗口 ≥ 600dp 时按实际可用宽度插值（单项目标宽度 84→112dp、上限 62% 可用宽度），
  平板横屏明显更舒展且不接近全屏；宽度规则抽成纯函数并新增 5 项单元测试，
  60 项测试全部通过（含强制全量重跑）；玻璃材质小幅优化（明暗层次 / 边缘高光 / 柔和阴影）。
  versionCode 4。
- v1.3.0（2026-10-18）：底部导航改为 iOS 26/27 风格悬浮玻璃 Dock（独立胶囊、水平居中、图标 + 文字）；
  内容可从 Dock 后滚过，各页面按统一的 `rememberDockClearance()` 留出底部余量，有底栏的页面
  底栏浮到 Dock 上方；深色 / 浅色主题适配，零新增依赖与权限；新增 4 项导航契约测试，
  55 项测试全部通过（含强制全量重跑）；versionCode 3。
- v1.2.1（2026-10-18）：鸿蒙「卓易通」拍照兼容修复。拍照成败改为以“临时文件里是否真的有
  照片”为准（结果标志丢失也照样导入）；结果回调 + 回到前台双重兜底检查；三条静默失败分支
  改为明确提示；修复“图片导入期间保存会漏图”的竞态；自动清理拍照临时文件；新增 4 项单元测试，
  51 项全部通过；**卓易通真机实测缩略图正常出现，修复确认有效**。
  versionName 从 1.0.0 对齐 README 轮次为 1.2.1（versionCode 2）。
- v1.2（2026-10-18）：响应式布局与信息密度升级。按窗口宽度三档自适应（紧凑单列 / 中等双列 /
  展开主从分栏）；首页重排并重做空状态、隐藏重复的添加入口；错题列表工具栏分层、两种空状态区分；
  大屏错题列表「列表 + 详情」分栏；复习页任务区与辅助区分栏；「我的」限宽 720dp；
  `EmptyState` / `ScreenScaffold` 升级；新增 3 项断点单元测试，47 项测试全部通过。
- v1.1（2026-10-18）：UI / UX 专项升级。统一配色（白卡 + 浅灰底，替代灰紫卡片）、字体与圆角规范；
  首页品牌头与概览卡、列表轻量搜索框与统一筛选行、复习任务卡与掌握率圆环、我的页面分区；
  **修复主题选项重复显示「跟随系统」的缺陷**；首页学科分布点击跳转功能落地；
  `assembleDebug + testDebugUnitTest` 重新执行，44 项测试全部通过。
- v1.0.0（2026-10-17）：首个版本。首页、错题列表（搜索 / 筛选 / 排序 / 批量操作）、详情、
  添加 / 编辑（相册 + 拍照）、复习（三档自评 + 间隔计划）、统计图表、错题本分类、
  JSON 导出 / 导入（可选含图片）、组卷导出 PDF、主题与壁纸取色；44 项单元测试全部通过。
