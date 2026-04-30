package com.xsgrok.app.prompt

import com.xsgrok.app.data.model.Chapter
import com.xsgrok.app.data.model.Novel
import com.xsgrok.app.data.model.GenerationPreset

/**
 * 简化版Prompt构建器 - 第一性原理优化
 * 单次API调用完成章节生成，无需复杂的多阶段系统
 */
object SimplePromptBuilder {
    
    /**
     * 构建章节生成Prompt
     * 将所有必要上下文一次性发送给大模型
     */
    fun buildChapterPrompt(
        novel: Novel,
        chapterNum: Int,
        userGuide: String?,
        preset: GenerationPreset
    ): Pair<String, String> {
        val systemPrompt = buildSystemPrompt(novel, preset)
        val userPrompt = buildUserPrompt(novel, chapterNum, userGuide)
        return Pair(systemPrompt, userPrompt)
    }
    
    /**
     * 构建系统Prompt
     */
    private fun buildSystemPrompt(novel: Novel, preset: GenerationPreset): String {
        return buildString {
            appendLine("你是一位专业的中文网络小说作家。")
            appendLine()
            
            // 小说基本信息
            appendLine("【小说信息】")
            appendLine("标题：《${novel.title}》")
            if (novel.genre.isNotBlank()) appendLine("类型：${novel.genre}")
            if (novel.style.isNotBlank()) appendLine("风格：${novel.style}")
            appendLine()
            
            // 主角设定
            if (novel.mainCharacter.isNotBlank()) {
                appendLine("【主角】${novel.mainCharacter}")
                appendLine()
            }
            
            // 角色信息
            if (novel.characters.isNotEmpty()) {
                appendLine("【角色】")
                novel.characters.take(5).forEach { char ->
                    appendLine("- ${char.name}（${char.role}）：${char.description}")
                }
                appendLine()
            }
            
            // 世界观
            if (novel.worldBuilding.worldBackground.isNotBlank()) {
                appendLine("【世界背景】${novel.worldBuilding.worldBackground}")
                appendLine()
            }
            if (novel.worldBuilding.powerSystem.isNotBlank()) {
                appendLine("【核心设定】${novel.worldBuilding.powerSystem}")
                appendLine()
            }
            
            // 大纲
            if (novel.outline.isNotBlank()) {
                appendLine("【大纲】${novel.outline}")
                appendLine()
            }
            
            // 全局摘要（如果有）
            if (novel.globalSummary.isNotBlank()) {
                appendLine("【故事摘要】${novel.globalSummary}")
                appendLine()
            }
            
            // 风格提示
            if (preset.styleHint.isNotBlank()) {
                appendLine("【写作风格】${preset.styleHint}")
                appendLine()
            }
            
            // 写作要求
            appendLine("【写作要求】")
            appendLine("1. 纯中文写作，文笔流畅")
            appendLine("2. 章节字数3000-5000字")
            appendLine("3. 用具体动作、细节代替空泛的心理描写")
            appendLine("4. 对话自然，避免工整的一问一答")
            appendLine("5. 结尾留悬念，吸引继续阅读")
            appendLine("6. 严格遵循已建立的角色性格和世界观")
        }
    }
    
    /**
     * 构建用户Prompt
     */
    private fun buildUserPrompt(novel: Novel, chapterNum: Int, userGuide: String?): String {
        return buildString {
            appendLine("请创作第${chapterNum}章。")
            appendLine()
            
            // 进度提示
            appendLine(novel.getProgressHint(chapterNum))
            appendLine()
            
            // 前几章摘要
            if (novel.chapters.isNotEmpty()) {
                appendLine("【前几章摘要】")
                appendLine(novel.getRecentSummaries(2))
                appendLine()
            }
            
            // 上一章结尾
            val lastEnding = novel.getLastChapterEnding()
            if (lastEnding.isNotBlank()) {
                appendLine("【上一章结尾】")
                appendLine(lastEnding)
                appendLine()
            }
            
            // 用户引导
            if (!userGuide.isNullOrBlank()) {
                appendLine("【本章目标】$userGuide")
                appendLine()
            }
            
            // 伏笔提示
            val unresolved = novel.getUnresolvedForeshadowings()
            if (unresolved.isNotEmpty()) {
                appendLine("【待回收伏笔】（如有合适机会请处理）")
                unresolved.take(3).forEach { f ->
                    appendLine("- ${f.content}")
                }
                appendLine()
            }
            
            appendLine("请开始创作第${chapterNum}章。")
        }
    }
    
    /**
     * 构建摘要生成Prompt
     */
    fun buildSummaryPrompt(chapter: Chapter): Pair<String, String> {
        val systemPrompt = """
            你是一位专业的小说编辑，负责为章节生成简短的摘要。
            摘要应该：
            1. 简洁明了，50-100字
            2. 概括本章主要情节
            3. 包含关键角色互动
            4. 不剧透关键转折
        """.trimIndent()
        
        val userPrompt = """
            请为以下章节生成摘要：
            
            章节标题：${chapter.title}
            章节内容：
            ${chapter.content.take(2000)}...
            
            只输出摘要内容，不要其他说明。
        """.trimIndent()
        
        return Pair(systemPrompt, userPrompt)
    }
    
    /**
     * 构建全局摘要更新Prompt
     */
    fun buildGlobalSummaryUpdatePrompt(novel: Novel): Pair<String, String> {
        val systemPrompt = """
            你是一位专业的小说编辑，负责维护故事的全局摘要。
            根据新增章节，更新摘要，保持故事的连贯性。
            摘要应该简洁，200-300字，概括：
            1. 主要角色的当前状态/关系
            2. 主线剧情进展
            3. 已埋下的重要伏笔
            4. 当前面临的主要冲突
        """.trimIndent()
        
        val userPrompt = buildString {
            appendLine("当前全局摘要：")
            appendLine(novel.globalSummary.ifBlank { "（暂无）" })
            appendLine()
            appendLine("新增章节：")
            novel.chapters.takeLast(2).forEach { chapter ->
                appendLine("第${chapter.order}章《${chapter.title}》：${chapter.summary}")
            }
            appendLine()
            appendLine("请更新全局摘要，保持之前的重要内容。")
        }
        
        return Pair(systemPrompt, userPrompt)
    }
}
