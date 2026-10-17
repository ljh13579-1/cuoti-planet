package com.cuotixingqiu.app.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
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

/** 底部导航的 4 个标签 */
private data class TabItem(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    TabItem(Routes.HOME, "首页", Icons.Filled.Home),
    TabItem(Routes.QUESTIONS, "错题", Icons.AutoMirrored.Filled.List),
    TabItem(Routes.REVIEW, "复习", Icons.Filled.Refresh),
    TabItem(Routes.PROFILE, "我的", Icons.Filled.Person),
)

/**
 * 应用的主界面：底部 4 个标签 + 所有二级页面。
 * 所有页面都在同一个 NavHost 里，靠“路由地址”切换。
 */
@Composable
fun CuotiApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showAddButton = currentRoute == Routes.HOME || currentRoute == Routes.QUESTIONS

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            NavigationBar {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick = { navController.navigateToTab(tab.route) },
                        icon = { Icon(imageVector = tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                        alwaysShowLabel = true,
                    )
                }
            }
        },
        floatingActionButton = {
            if (showAddButton) {
                ExtendedFloatingActionButton(
                    onClick = { navController.navigate(Routes.edit()) },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("添加错题") },
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(innerPadding),
        ) {
            // ---------- 首页 ----------
            composable(Routes.HOME) {
                HomeScreen(
                    onAddQuestion = { navController.navigate(Routes.edit()) },
                    onOpenQuestion = { id -> navController.navigate(Routes.detail(id)) },
                    onStartReview = { navController.navigate(Routes.reviewSession()) },
                    onOpenQuestions = { navController.navigateToTab(Routes.QUESTIONS) },
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
                )
            }

            // ---------- 复习 ----------
            composable(Routes.REVIEW) {
                ReviewScreen(
                    onOpenQuestion = { id -> navController.navigate(Routes.detail(id)) },
                    onStartSession = { questionId ->
                        navController.navigate(Routes.reviewSession(questionId))
                    },
                    onOpenStats = { navController.navigate(Routes.STATS) },
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
