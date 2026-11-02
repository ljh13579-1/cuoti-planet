package com.cuotixingqiu.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cuotixingqiu.app.ui.components.DockItem
import com.cuotixingqiu.app.ui.components.FloatingGlassDock
import com.cuotixingqiu.app.ui.components.isGlassBackdropSupported
import com.cuotixingqiu.app.ui.detail.DetailScreen
import com.cuotixingqiu.app.ui.edit.EditScreen
import com.cuotixingqiu.app.ui.home.HomeScreen
import com.cuotixingqiu.app.ui.list.QuestionListScreen
import com.cuotixingqiu.app.ui.navigation.Routes
import com.cuotixingqiu.app.ui.notebook.NotebookScreen
import com.cuotixingqiu.app.ui.paper.PaperScreen
import com.cuotixingqiu.app.ui.profile.ProfileScreen
import com.cuotixingqiu.app.ui.review.ReviewScreen
import com.cuotixingqiu.app.ui.review.ReviewSessionScreen
import com.cuotixingqiu.app.ui.stats.StatsScreen
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

/**
 * 底部导航的 4 个标签（悬浮 Dock 的数据来源）。
 * 顺序、图标、标签文字、跳转目标与改造前的底部导航完全一致，不要随意增删。
 */
internal val cuotiTabs = listOf(
    DockItem(Routes.HOME, "首页", Icons.Filled.Home),
    DockItem(Routes.QUESTIONS, "错题", Icons.AutoMirrored.Filled.List),
    DockItem(Routes.REVIEW, "复习", Icons.Filled.Refresh),
    DockItem(Routes.PROFILE, "我的", Icons.Filled.Person),
)

/**
 * 应用的主界面：悬浮玻璃 Dock + 所有二级页面。
 * 所有页面都在同一个 NavHost 里，靠“路由地址”切换；
 * 底部导航悬浮在内容之上，页面内容可以从它后面滚过（最后一条内容用 rememberDockClearance() 留出余量）。
 */
@Composable
fun CuotiApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route?.substringBefore("?")

    // 首页点“学科分布”时，把学科带到错题列表页（一次性请求，用完即清）
    var requestedSubject by rememberSaveable { mutableStateOf<String?>(null) }

    // 错题详情是二级页面：按需求不显示 Dock；其它标签页保持原有 Dock 行为。
    val showDock = currentRoute != Routes.DETAIL

    // 页面背景录制（Android 12+）：Dock 的模糊/折射必须采样“身后的真实像素”。
    // 这里使用与 AndroidLiquidGlass 相同的 LayerBackdrop 录制方式：
    //  - 同一帧 GPU 图层，无截屏、无 CPU 读回、无轮询；
    //  - Android 13+ Dock 的 drawBackdrop 直接消费这个页面图层；
    //  - Android 12/12L 仍由项目原有的 GlassBackdrop 消费同一个 GraphicsLayer；
    //  - Android 11 及以下不创建图层，保持零额外绘制。
    val glassBackdropSupported = remember { isGlassBackdropSupported() }
    val pageBackdrop = if (glassBackdropSupported) rememberLayerBackdrop() else null
    val backgroundLayer = pageBackdrop?.graphicsLayer

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier
                .fillMaxSize()
                // 主题背景色在 LayerBackdrop 内侧，录制内容包含不透明背景，避免玻璃采到透明黑。
                .then(
                    if (pageBackdrop != null) {
                        Modifier.layerBackdrop(pageBackdrop)
                    } else {
                        Modifier
                    },
                )
                .background(MaterialTheme.colorScheme.background),
        ) {
            // ---------- 首页 ----------
            composable(Routes.HOME) {
                HomeScreen(
                    onAddQuestion = { navController.navigate(Routes.edit()) },
                    onOpenQuestion = { id -> navController.navigate(Routes.detail(id)) },
                    onStartReview = { navController.navigate(Routes.reviewSession()) },
                    onOpenReview = { navController.navigateToTab(Routes.REVIEW) },
                    onOpenQuestions = { navController.navigateToTab(Routes.QUESTIONS) },
                    onOpenSubject = { subject ->
                        requestedSubject = subject
                        navController.navigateToTab(Routes.QUESTIONS)
                    },
                    onOpenStats = { navController.navigate(Routes.STATS) },
                )
            }

            // ---------- 错题列表 ----------
            composable(Routes.QUESTIONS) {
                QuestionListScreen(
                    onOpenQuestion = { id -> navController.navigate(Routes.detail(id)) },
                    onEditQuestion = { id -> navController.navigate(Routes.edit(id)) },
                    onAddQuestion = { navController.navigate(Routes.edit()) },
                    onExportPaper = { navController.navigate(Routes.PAPER) },
                    onStartReview = { id -> navController.navigate(Routes.reviewSession(id)) },
                    requestedSubject = requestedSubject,
                    onRequestedSubjectApplied = { requestedSubject = null },
                )
            }

            // ---------- 复习 ----------
            composable(Routes.REVIEW) {
                ReviewScreen(
                    onStartSession = { questionId ->
                        navController.navigate(Routes.reviewSession(questionId))
                    },
                    onOpenStats = { navController.navigate(Routes.STATS) },
                    onAddQuestion = { navController.navigate(Routes.edit()) },
                )
            }

            // ---------- 我的 ----------
            composable(Routes.PROFILE) {
                ProfileScreen(
                    onOpenStats = { navController.navigate(Routes.STATS) },
                    onOpenNotebooks = { navController.navigate(Routes.NOTEBOOKS) },
                    onOpenPaper = { navController.navigate(Routes.PAPER) },
                )
            }

            // ---------- 错题详情 ----------
            composable(
                route = Routes.DETAIL,
                arguments = listOf(navArgument("questionId") { type = NavType.LongType }),
            ) { entry ->
                val questionId = entry.arguments?.getLong("questionId") ?: 0L
                DetailScreen(
                    questionId = questionId,
                    onBack = { navController.popBackStack() },
                    onEdit = { navController.navigate(Routes.edit(questionId)) },
                    onStartReview = { navController.navigate(Routes.reviewSession(questionId)) },
                    reserveDockClearance = false,
                )
            }

            // ---------- 添加 / 编辑错题 ----------
            composable(
                route = Routes.EDIT,
                arguments = listOf(
                    navArgument("questionId") {
                        type = NavType.LongType
                        defaultValue = Routes.NEW_QUESTION_ID
                    },
                ),
            ) { entry ->
                val rawId = entry.arguments?.getLong("questionId") ?: Routes.NEW_QUESTION_ID
                val questionId = rawId.takeIf { it > 0 }
                EditScreen(
                    questionId = questionId,
                    onBack = { navController.popBackStack() },
                    onSaved = { savedId ->
                        // 保存后：如果是新增，跳转到详情；如果是编辑，直接返回上一页
                        if (questionId == null) {
                            navController.popBackStack()
                            navController.navigate(Routes.detail(savedId))
                        } else {
                            navController.popBackStack()
                        }
                    },
                )
            }

            // ---------- 复习会话 ----------
            composable(
                route = Routes.REVIEW_SESSION_PATTERN,
                arguments = listOf(
                    navArgument("questionId") {
                        type = NavType.LongType
                        defaultValue = Routes.NEW_QUESTION_ID
                    },
                ),
            ) { entry ->
                val rawId = entry.arguments?.getLong("questionId") ?: Routes.NEW_QUESTION_ID
                ReviewSessionScreen(
                    onlyQuestionId = rawId.takeIf { it > 0 },
                    onBack = { navController.popBackStack() },
                    onOpenQuestion = { id -> navController.navigate(Routes.detail(id)) },
                )
            }

            // ---------- 统计图表 ----------
            composable(Routes.STATS) {
                StatsScreen(onBack = { navController.popBackStack() })
            }

            // ---------- 错题本分类管理 ----------
            composable(Routes.NOTEBOOKS) {
                NotebookScreen(onBack = { navController.popBackStack() })
            }

            // ---------- 组卷导出 ----------
            composable(Routes.PAPER) {
                PaperScreen(
                    onBack = { navController.popBackStack() },
                    onOpenQuestion = { id -> navController.navigate(Routes.detail(id)) },
                )
            }
        }

        // 悬浮玻璃 Dock：内容和动画完全沿用原实现；只在错题详情页隐藏。
        if (showDock) {
            FloatingGlassDock(
                items = cuotiTabs,
                selectedRoute = currentRoute,
                onSelect = { route -> navController.navigateToTab(route) },
                modifier = Modifier.align(Alignment.BottomCenter),
                backgroundLayer = backgroundLayer,
                pageBackdrop = pageBackdrop,
            )
        }
    }
}

/** 切换底部标签：保留各自标签的滚动位置，不会一层层堆页面 */
private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
