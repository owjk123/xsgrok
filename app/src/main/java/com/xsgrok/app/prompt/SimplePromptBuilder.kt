package com.xsgrok.app.prompt

import com.xsgrok.app.data.model.Chapter
import com.xsgrok.app.data.model.Novel
import com.xsgrok.app.data.model.GenerationPreset
import com.xsgrok.app.data.model.NovelFoundation

/**
 * 简化版Prompt构建器 - 第一性原理优化
 * 单次API调用完成章节生成，无需复杂的多阶段系统
 * 
 * 新流程：用户输入一句话 → AI生成6大基础设定 → 用户确认 → 生成章节
 */
object SimplePromptBuilder {
    
    // ========== 阶段1：生成基础设定 ==========
    
    /**
     * 构建基础设定生成Prompt
     * 输入：用户的一句话创意
     * 输出：结构化的6大基础设定
     */
    fun buildFoundationPrompt(userIdea: String): Pair<String, String> {
        val systemPrompt = """
你是一位专业的中文网络小说策划师，擅长从一句话创意扩展为完整的作品设定。

请根据用户提供的创意，自动分解补全为以下6大基础设定：

1. 【角色设定】：详细描述主角和重要配角的性格、外貌、背景、目标、困境等
2. 【人物关系】：描述角色之间的相互关系、矛盾冲突、合作联盟等
3. 【时间线】：简要说明故事的时间跨度、重要节点、章节大致安排
4. 【章节主要剧情走向】：描述1-10章的主要剧情脉络和发展方向
5. 【写作风格】：明确作品的文风特点（热血/悬疑/轻松/虐心等）、叙事手法
6. 【目前为止的章节摘要】：新作品暂无，填"（暂无）"

请严格按照以下格式返回，使用【】标记各部分，不要使用JSON格式，方便用户直接编辑：
"""
        
        val userPrompt = """
用户的创意想法：
"$userIdea"

请根据这个创意，生成完整的6大基础设定。
"""
        
        return Pair(systemPrompt.trimIndent(), userPrompt.trimIndent())
    }
    
    /**
     * 解析AI返回的基础设定文本
     * 返回 NovelFoundation 对象
     */
    fun parseFoundationResponse(response: String): NovelFoundation {
        val foundation = NovelFoundation()
        
        // 按【】标记分割内容
        val sections = mutableMapOf<String, String>()
        
        val pattern = Regex("""【([^】]+)】\s*([\s\S]*?)(?=【[^】]+】|$)""")
        pattern.findAll(response).forEach { match ->
            val title = match.groupValues[1].trim()
            val content = match.groupValues[2].trim()
            sections[title] = content
        }
        
        return foundation.copy(
            characterSettings = sections["角色设定"] ?: "",
            characterRelationships = sections["人物关系"] ?: sections["人物关系图"] ?: "",
            timeline = sections["时间线"] ?: "",
            chapterPlotDirection = sections["章节主要剧情走向"] ?: sections["剧情走向"] ?: "",
            writingStyle = sections["写作风格"] ?: sections["文风"] ?: "",
            chapterSummaries = sections["目前为止的章节摘要"] ?: "（暂无）"
        )
    }
    
    // ========== 阶段2：生成章节 ==========
    
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
     * 构建系统Prompt - 使用6大基础设定
     */
    private fun buildSystemPrompt(novel: Novel, preset: GenerationPreset): String {
        val foundation = novel.foundation
        
        return buildString {
            appendLine("你是一位专业的中文网络小说作家。")
            appendLine()
            
            // 小说基本信息
            appendLine("【小说信息】")
            appendLine("标题：《${novel.title}》")
            if (novel.genre.isNotBlank()) appendLine("类型：${novel.genre}")
            appendLine()
            
            // 6大基础设定 - 角色设定
            if (foundation.characterSettings.isNotBlank()) {
                appendLine("【角色设定】")
                appendLine(foundation.characterSettings)
                appendLine()
            }
            
            // 6大基础设定 - 人物关系
            if (foundation.characterRelationships.isNotBlank()) {
                appendLine("【人物关系】")
                appendLine(foundation.characterRelationships)
                appendLine()
            }
            
            // 6大基础设定 - 时间线
            if (foundation.timeline.isNotBlank()) {
                appendLine("【时间线】")
                appendLine(foundation.timeline)
                appendLine()
            }
            
            // 6大基础设定 - 写作风格
            if (foundation.writingStyle.isNotBlank()) {
                appendLine("【写作风格】")
                appendLine(foundation.writingStyle)
                appendLine()
            } else if (preset.styleHint.isNotBlank()) {
                appendLine("【写作风格】${preset.styleHint}")
                appendLine()
            }
            
            // 6大基础设定 - 章节剧情走向
            if (foundation.chapterPlotDirection.isNotBlank()) {
                appendLine("【章节主要剧情走向】")
                appendLine(foundation.chapterPlotDirection)
                appendLine()
            }
            
            // 额外备注
            if (novel.outline.isNotBlank()) {
                appendLine("【备注】${novel.outline}")
                appendLine()
            }
            
            // 6大基础设定 - 章节摘要
            if (foundation.chapterSummaries.isNotBlank()) {
                appendLine("【故事摘要】")
                appendLine(foundation.chapterSummaries)
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
            
            appendLine("请开始创作第${chapterNum}章。")
        }
    }
    
    // ========== 阶段3：更新章节摘要 ==========
    
    /**
     * 构建章节摘要更新Prompt
     * 每写完一章后，更新 foundation.chapterSummaries
     */
    fun buildChapterSummaryUpdatePrompt(novel: Novel, newChapter: Chapter): Pair<String, String> {
        val systemPrompt = """
你是一位专业的小说编辑，负责维护故事的章节摘要。

【重要规则】
1. 保持摘要简洁，300-500字左右
2. 按时间顺序描述主要情节发展
3. 包含关键角色互动和重要事件
4. 不透露核心悬念的答案
5. 不要列出章节标题，用自然段落描述剧情进展

请直接输出更新后的完整摘要，不要添加任何说明。
"""
        
        val userPrompt = buildString {
            appendLine("当前章节摘要：")
            appendLine(novel.foundation.chapterSummaries.ifBlank { "（暂无）" })
            appendLine()
            appendLine("最新完成的章节：")
            appendLine("章节标题：${newChapter.title}")
            appendLine("章节摘要：${newChapter.summary}")
            appendLine()
            appendLine("请将新章节的内容整合到摘要中，保持故事的连贯性。")
        }
        
        return Pair(systemPrompt.trimIndent(), userPrompt.trimIndent())
    }
    
    /**
     * 构建摘要生成Prompt - 为单个章节生成摘要
     */
    fun buildSummaryPrompt(chapter: Chapter): Pair<String, String> {
        val systemPrompt = """
你是一位专业的小说编辑，负责为章节生成简短的摘要。
摘要应该：
1. 简洁明了，50-100字
2. 概括本章主要情节
3. 包含关键角色互动
4. 不剧透关键转折
请直接输出摘要内容，不要添加任何说明。
"""
        
        val userPrompt = """
请为以下章节生成摘要：

章节标题：${chapter.title}
章节内容：
${chapter.content.take(2000)}...

只输出摘要内容，不要其他说明。
"""
        
        return Pair(systemPrompt.trimIndent(), userPrompt.trimIndent())
    }
    
    /**
     * 构建小说标题生成Prompt
     * 从用户创意中提取或生成标题
     */
    fun buildTitlePrompt(userIdea: String): Pair<String, String> {
        val systemPrompt = """
你是一位专业的小说策划师，负责为作品起名。
请根据用户的创意，生成一个吸引人的小说标题。
要求：
1. 标题简洁，2-8个字
2. 能体现作品类型或核心元素
3. 有吸引力，让人想点击阅读

请只输出标题，不要添加任何说明。
"""
        
        val userPrompt = "用户的创意：$userIdea"
        
        return Pair(systemPrompt.trimIndent(), userPrompt.trimIndent())
    }
    
    /**
     * 解析标题
     */
    fun parseTitle(response: String): String {
        // 去除引号、括号等
        var title = response.trim()
        title = title.replace(Regex("^[《\"'【\\[「『]+"), "")
        title = title.replace(Regex("[》\"'】\\]」』]+$"), "")
        return title.ifBlank { "新小说" }
    }
}
