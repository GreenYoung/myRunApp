package com.example.myrunapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Set of Material typography styles to start with
val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
    /* Other default text styles to override
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
    */
)

/**
 * 数值型文本统一风格：等宽数字 + 零字距。
 *
 * `Typography.bodyLarge` 自带的 0.5sp 字距会被所有 Text 继承，
 * 一行 20~30 个字符会多占 10~15dp，容易把末尾信息（如 kcal）挤出屏幕。
 * 所有"数值 + 单位"的文本请显式使用这个 style。
 */
val NumericText = TextStyle(
    fontFeatureSettings = "tnum",
    letterSpacing = 0.sp
)