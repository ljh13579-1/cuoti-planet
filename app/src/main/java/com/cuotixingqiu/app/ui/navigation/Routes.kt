package com.cuotixingqiu.app.ui.navigation

/**
 * 所有页面的“路由地址”。
 * 把它集中在一个文件里，改名字的时候只需改一处，避免拼写错误。
 */
object Routes {

    // ---------- 底部导航的 4 个标签 ----------
    const val HOME = "home"
    const val QUESTIONS = "questions"
    const val REVIEW = "review"
    const val PROFILE = "profile"

    // ---------- 二级页面 ----------
    const val STATS = "stats"
    const val NOTEBOOKS = "notebooks"
    const val PAPER = "paper"
    const val REVIEW_SESSION = "review_session"
    const val DETAIL = "detail/{questionId}"
    const val EDIT = "edit?questionId={questionId}"

    /** 新增错题时用的“假 id” */
    const val NEW_QUESTION_ID = -1L

    fun detail(questionId: Long): String = "detail/$questionId"

    fun edit(questionId: Long? = null): String = "edit?questionId=${questionId ?: NEW_QUESTION_ID}"

    fun reviewSession(questionId: Long? = null): String =
        "review_session?questionId=${questionId ?: NEW_QUESTION_ID}"

    const val REVIEW_SESSION_PATTERN = "review_session?questionId={questionId}"
}
