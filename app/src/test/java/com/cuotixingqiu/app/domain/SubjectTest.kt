package com.cuotixingqiu.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test

/** 学科枚举的测试：重点是“导入数据时的容错” */
class SubjectTest {

    @Test
    fun `中文名能正确还原为学科`() {
        assertEquals(Subject.MATH, Subject.fromDisplayName("数学"))
        assertEquals(Subject.PHYSICS, Subject.fromDisplayName("物理"))
        assertEquals(Subject.CHEMISTRY, Subject.fromDisplayName("化学"))
        assertEquals(Subject.BIOLOGY, Subject.fromDisplayName("生物"))
        assertEquals(Subject.ENGLISH, Subject.fromDisplayName("英语"))
        assertEquals(Subject.OTHER, Subject.fromDisplayName("其他"))
    }

    @Test
    fun `未知或空的学科名会归入其他`() {
        assertEquals(Subject.OTHER, Subject.fromDisplayName("地理"))
        assertEquals(Subject.OTHER, Subject.fromDisplayName(""))
        assertEquals(Subject.OTHER, Subject.fromDisplayName(null))
    }

    @Test
    fun `复习状态的名字还原`() {
        assertEquals(ReviewStatus.MASTERED, ReviewStatus.fromName("MASTERED"))
        assertEquals(ReviewStatus.NEEDS_REVIEW, ReviewStatus.fromName("SOMETHING_ELSE"))
        assertEquals(ReviewStatus.NEEDS_REVIEW, ReviewStatus.fromName(null))
    }
}
