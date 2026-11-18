package com.example.myrunapp.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * 设计 token 的唯一来源。
 *
 * 使用约定：**新增页面/组件从这里取值**；现有页面保持原样，改动到哪一页再顺手替换，
 * 不做一次性全量搬迁（纯色值搬家的 diff 太大、review 成本高）。
 */
object AppColors {
    val background = AppBackground
    val surface = AppSurface
    val textPrimary = AppPrimaryText
    val textSecondary = AppSecondaryText
    val accent = Accent
    val highlight = Color(0xFF34D399)

    /** 顶部主卡片的渐变背景（首页运动卡、运动页汇总卡、统计详情卡）。 */
    val cardTop = Color(0xFF1A2530)
    val cardBottom = Color(0xFF10161D)

    /** 1dp 描边与列表分割线。 */
    val hairline = Color(0x14FFFFFF)
    val divider = Color(0x0DFFFFFF)
}

object AppRadius {
    /** 大卡片（首页今日运动、运动汇总卡）。 */
    val card = 22.dp

    /** 次级卡片 / 缩略图容器。 */
    val block = 16.dp

    /** 胶囊 / chip。 */
    val chip = 50.dp
}

object AppSpacing {
    /** 页面左右边距。 */
    val page = 16.dp

    /** 卡片内部 padding。 */
    val card = 18.dp

    /** 区块之间。 */
    val section = 14.dp

    /** 列表项之间。 */
    val item = 6.dp
}
