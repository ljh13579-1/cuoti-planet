package com.cuotixingqiu.app.domain

/**
 * 学科。
 * 第一版固定为这几个选项；以后想加学科，只需要在这里加一行。
 * 导入数据时遇到不认识的学科，会被归入“其他”。
 */
enum class Subject(val displayName: String) {
    MATH("数学"),
    PHYSICS("物理"),
    CHEMISTRY("化学"),
    BIOLOGY("生物"),
    ENGLISH("英语"),
    OTHER("其他");

    companion object {
        /** 界面上的展示顺序（与上面的声明顺序一致） */
        val displayOrder: List<Subject> = entries.toList()

        /** 根据中文名找回枚举（导入数据时使用） */
        fun fromDisplayName(name: String?): Subject =
            entries.firstOrNull { it.displayName == name } ?: OTHER
    }
}
