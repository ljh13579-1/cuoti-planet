package com.cuotixingqiu.app.ui.paper

/**
 * 一个小“传递箱”：列表页多选后点“组卷”，把选中的错题 id 传给组卷页面。
 *
 * 为什么不用导航参数？因为 id 列表可能很长，放进路由字符串不合适。
 * 放在这里既简单又直观——只在本应用内部使用，不会影响其它功能。
 */
object PaperRequest {

    /** 上一次多选时选中的错题 id；空表示“没有指定，按其它范围导出” */
    var questionIds: List<Long> = emptyList()

    /** 取出并清空（避免下次进入组卷页还记着上一次的选择） */
    fun consume(): List<Long> {
        val ids = questionIds
        questionIds = emptyList()
        return ids
    }
}
