package com.xsgrok.app.ui.theme

import androidx.compose.ui.graphics.Color

// ========== 文学色系主题 ==========
// 灵感来源：古籍书卷、墨香、暖色调

// 浅色模式色系
val InkBrown = Color(0xFF5D4037)           // 墨棕 - 主色
val WarmGold = Color(0xFFD4A574)            // 暖金 - 辅助色
val LightBrown = Color(0xFF8D6E63)          // 浅棕
val CreamWhite = Color(0xFFF5F0E8)         // 米白 - 背景色
val PaperWhite = Color(0xFFFFFBF5)         // 纸张白
val DeepBrown = Color(0xFF3E2723)           // 深棕 - 文字色

// 深色模式色系
val DarkInkGreen = Color(0xFF2C3E2F)       // 深墨绿
val DarkBrown = Color(0xFF3E2723)          // 暗棕
val MutedGold = Color(0xFFB8956E)          // 柔和金
val DarkSurface = Color(0xFF1A1A1A)        // 深色背景（非纯黑）
val DarkCard = Color(0xFF252525)            // 深色卡片背景
val LightTextDark = Color(0xFFE8E0D5)      // 深色模式浅色文字

// 功能色
val SuccessGreen = Color(0xFF4CAF50)       // 成功绿
val WarningOrange = Color(0xFFFF9800)       // 警告橙
val ErrorRed = Color(0xFFE53935)            // 错误红
val InfoBlue = Color(0xFF2196F3)            // 信息蓝

// 复古色系（备选）
val VintageRed = Color(0xFF8B4513)          // 古红
val VintageBlue = Color(0xFF4A5568)         // 古蓝
val VintageGreen = Color(0xFF2D5A27)        // 古绿

// 兼容旧代码
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// 新的主色（替换旧紫色）
val PrimaryLight = InkBrown
val OnPrimaryLight = Color.White
val PrimaryContainerLight = Color(0xFFE8DDD5)
val OnPrimaryContainerLight = DeepBrown

val PrimaryDark = WarmGold
val OnPrimaryDark = DarkBrown
val PrimaryContainerDark = Color(0xFF4A3F35)
val OnPrimaryContainerDark = LightTextDark
