package com.xsgrok.app.ui

/**
 * 屏幕枚举 - 精简版
 * 3个主Tab: Creation, Bookshelf, Settings
 */
enum class Screen {
    // 主Tab页面
    Creation,      // 创作页（合并了首页+全自动+新建功能）
    Bookshelf,     // 书架页
    Settings,      // 设置页
    
    // 子页面
    Home,          // 旧首页（保留用于兼容）
    NewNovel,      // 新建小说
    NovelDetail,   // 小说详情
    Characters,    // 角色管理
    ChapterGeneration,  // 章节生成
    AutoMode,      // 自动模式
    Reading,       // 阅读页面
    WorldBuilding  // 世界观设定
    
    // 已删除: Drafts - 合并到书架页
}
