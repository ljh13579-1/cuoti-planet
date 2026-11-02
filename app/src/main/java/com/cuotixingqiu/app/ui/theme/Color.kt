package com.cuotixingqiu.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 「错题星球」的配色：暖灰底 + 品牌蓝紫。
 *
 * 视觉约定（浅色）：
 *  - 页面底色 #F1F2EF，卡片纯白，靠“白卡 + 暖灰底”形成层次；
 *  - 主色 #3539DF，用于主操作、进度和选中态；
 *  - 主要文字 #17191D，次要文字 #858B96，分割线 #E5E7E4；
 *  - 浅蓝标签 #E9EBFF / #4653D5；
 *  - 不使用重边框和大阴影，深色主题有独立的一套对应颜色。
 */

// ---------- 浅色主题 ----------
val IndigoPrimaryLight = Color(0xFF3539DF)
val IndigoOnPrimaryLight = Color(0xFFFFFFFF)
val IndigoPrimaryContainerLight = Color(0xFFE9EBFF)
val IndigoOnPrimaryContainerLight = Color(0xFF4653D5)

val TealSecondaryLight = Color(0xFF2F7D5B)
val TealOnSecondaryLight = Color(0xFFFFFFFF)
val TealSecondaryContainerLight = Color(0xFFE3F3EA)
val TealOnSecondaryContainerLight = Color(0xFF123D2B)

val SlateTertiaryLight = Color(0xFF5E6370)
val SlateOnTertiaryLight = Color(0xFFFFFFFF)
val SlateTertiaryContainerLight = Color(0xFFEDEFEA)
val SlateOnTertiaryContainerLight = Color(0xFF424650)

// 页面与卡片
val BackgroundLight = Color(0xFFF1F2EF)
val OnSurfaceLight = Color(0xFF17191D)
val SurfaceVariantLight = Color(0xFFEDEFEA)
val OnSurfaceVariantLight = Color(0xFF858B96)
val OutlineLight = Color(0xFF858B96)
val OutlineVariantLight = Color(0xFFE5E7E4)

// 表面容器（卡片/输入框/对话框都从这里取色）
val SurfaceContainerLowestLight = Color(0xFFFFFFFF)
val SurfaceContainerLowLight = Color(0xFFFFFFFF)
val SurfaceContainerLight = Color(0xFFFFFFFF)
val SurfaceContainerHighLight = Color(0xFFFFFFFF)
val SurfaceContainerHighestLight = Color(0xFFF3F4F1)

val ErrorLight = Color(0xFFB3261E)
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorContainerLight = Color(0xFFF9DEDC)
val OnErrorContainerLight = Color(0xFF410E0B)

// ---------- 深色主题 ----------
val IndigoPrimaryDark = Color(0xFFAEB2FF)
val IndigoOnPrimaryDark = Color(0xFF14164F)
val IndigoPrimaryContainerDark = Color(0xFF2C3178)
val IndigoOnPrimaryContainerDark = Color(0xFFE4E6FF)

val TealSecondaryDark = Color(0xFF7BD5AE)
val TealOnSecondaryDark = Color(0xFF073624)
val TealSecondaryContainerDark = Color(0xFF1B533D)
val TealOnSecondaryContainerDark = Color(0xFFC9F2DF)

val SlateTertiaryDark = Color(0xFFC4C7D0)
val SlateOnTertiaryDark = Color(0xFF2D3037)
val SlateTertiaryContainerDark = Color(0xFF3B3F48)
val SlateOnTertiaryContainerDark = Color(0xFFE4E6EC)

// 页面与卡片
val BackgroundDark = Color(0xFF0F1115)
val OnSurfaceDark = Color(0xFFF2F3F0)
val SurfaceVariantDark = Color(0xFF24272E)
val OnSurfaceVariantDark = Color(0xFF9CA3AE)
val OutlineDark = Color(0xFF6F7683)
val OutlineVariantDark = Color(0xFF2A2D33)

val SurfaceContainerLowestDark = Color(0xFF0B0D10)
val SurfaceContainerLowDark = Color(0xFF1A1D22)
val SurfaceContainerDark = Color(0xFF1E2127)
val SurfaceContainerHighDark = Color(0xFF242830)
val SurfaceContainerHighestDark = Color(0xFF2B2F36)

val ErrorDark = Color(0xFFFFB4AB)
val OnErrorDark = Color(0xFF690005)
val ErrorContainerDark = Color(0xFF93000A)
val OnErrorContainerDark = Color(0xFFFFDAD6)

// ---------- 学科配色 ----------
/**
 * 六个学科的强调色，按 [com.cuotixingqiu.app.domain.Subject.displayOrder] 一一对应：
 * 数学、物理、化学、生物、英语、其他。
 *
 * 这些颜色由 [com.cuotixingqiu.app.ui.components.SubjectBadge] 等组件
 * 按当前主题自动调淡/加深，浅色和深色下都能看清。
 */
val SubjectPalette = listOf(
    Color(0xFF3446E8), // 数学：蓝
    Color(0xFF087F95), // 物理：青
    Color(0xFFC65A00), // 化学：橙
    Color(0xFF2F7D4A), // 生物：绿
    Color(0xFFD41460), // 英语：粉
    Color(0xFF858B96), // 其他：灰
)
