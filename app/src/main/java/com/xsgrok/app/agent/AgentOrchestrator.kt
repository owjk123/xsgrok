package com.xsgrok.app.agent

import com.xsgrok.app.data.model.*

/**
 * Agent编排器 - 分层Agent系统的核心调度
 * 规划Agent → 执行Agent → 审查Agent → 润色Agent
 */
class AgentOrchestrator {
    
    private val plotController = PlotController()
    
    /** Agent角色定义 */
    enum class AgentRole(val displayName: String, val systemPromptSuffix: String) {
        PLANNER("规划Agent", "你负责规划章节大纲和场景结构，确保剧情推进符合锚点目标。"),
        EXECUTOR("执行Agent", "你负责根据大纲创作章节正文，保持文笔流畅和角色一致。"),
        REVIEWER("审查Agent", "你负责审查生成内容的一致性和逻辑性，检测偏差并生成修正意见。"),
        POLISHER("润色Agent", "你负责润色文笔，增强感官描写和情感张力。")
    }
    
    /** 生成阶段 */
    enum class GenerationStage(val agent: AgentRole, val description: String) {
        PLANNING(AgentRole.PLANNER, "规划章节大纲和场景结构"),
        EXECUTING(AgentRole.EXECUTOR, "执行章节正文创作"),
        REVIEWING(AgentRole.REVIEWER, "审查一致性和逻辑"),
        POLISHING(AgentRole.POLISHER, "润色文笔和情感")
    }
    
    /** 获取阶段对应的系统Prompt */
    fun getSystemPrompt(stage: GenerationStage, novel: Novel, chapterNum: Int): String {
        val basePrompt = buildBasePrompt(novel, chapterNum)
        val anchorPrompt = plotController.generateAnchorPrompt(chapterNum)
        val stagePrompt = stage.agent.systemPromptSuffix
        
        return buildString {
            appendLine("你是一位专业的中文长篇小说作家，当前担任【${stage.agent.displayName}】角色。")
            appendLine()
            appendLine(stagePrompt)
            appendLine()
            appendLine(basePrompt)
            if (anchorPrompt.isNotBlank()) {
                appendLine(anchorPrompt)
            }
        }
    }
    
    /** 获取阶段对应的用户Prompt */
    fun getUserPrompt(
        stage: GenerationStage, 
        novel: Novel, 
        chapterNum: Int, 
        previousContent: String?,
        userGuide: String?,
        previousStageOutput: String? = null,
        reviewFeedback: String? = null
    ): String {
        return when (stage) {
            GenerationStage.PLANNING -> buildPlanningPrompt(novel, chapterNum, userGuide)
            GenerationStage.EXECUTING -> buildExecutionPrompt(novel, chapterNum, previousContent, previousStageOutput, userGuide)
            GenerationStage.REVIEWING -> buildReviewPrompt(novel, chapterNum, previousStageOutput ?: "")
            GenerationStage.POLISHING -> buildPolishPrompt(novel, chapterNum, previousStageOutput ?: "", reviewFeedback ?: "")
        }
    }
    
    /** 审查生成内容，返回偏差报告 */
    fun reviewContent(content: String, chapterNum: Int): List<DeviationReport> {
        val activeAnchors = plotController.getActiveAnchors()
        return plotController.detectDeviation(content, chapterNum, activeAnchors)
    }
    
    /** 生成矫正指令 */
    fun getCorrectionPrompt(reports: List<DeviationReport>): String {
        return plotController.generateCorrectionPrompt(reports)
    }
    
    /** 获取PlotController */
    fun getPlotController(): PlotController = plotController
    
    /** 从大纲初始化锚点 */
    fun initializeFromOutline(novel: Novel) {
        plotController.generateAnchorsFromOutline(novel.outline, novel.keyNodes)
    }
    
    /** 构建基础Prompt */
    private fun buildBasePrompt(novel: Novel, chapterNum: Int): String {
        val progressInfo = novel.getProgressInfo(chapterNum)
        val isConvergence = novel.isConvergenceMode()
        val unresolved = novel.getUnresolvedForeshadowings()
        
        return buildString {
            appendLine("【小说信息】")
            appendLine("- 标题：《${novel.title}》")
            appendLine("- 类型：${novel.type}")
            appendLine("- 风格：${novel.style}")
            appendLine("- 主角：${novel.mainCharacter}")
            appendLine("- 当前进度：第${chapterNum}章")
            appendLine()
            
            appendLine("【世界观】")
            if (novel.worldBuilding.worldBackground.isNotBlank()) {
                appendLine("世界背景：${novel.worldBuilding.worldBackground}")
            }
            if (novel.worldBuilding.powerSystem.isNotBlank()) {
                appendLine("核心设定：${novel.worldBuilding.powerSystem}")
            }
            if (novel.worldBuilding.rules.isNotBlank()) {
                appendLine("世界规则：${novel.worldBuilding.rules}")
            }
            appendLine()
            
            appendLine("【大纲】")
            appendLine(novel.outline)
            appendLine()
            
            appendLine("【进度】")
            appendLine(progressInfo.toModelHint())
            appendLine()
            
            if (unresolved.isNotEmpty()) {
                appendLine("【未回收伏笔】")
                unresolved.take(5).forEach { f ->
                    appendLine("- ${f.content}（第${f.plantedChapter}章埋下）")
                }
                appendLine()
            }
            
            // 角色信息
            if (novel.characters.isNotEmpty()) {
                appendLine("【主要角色】")
                novel.characters.forEach { char ->
                    appendLine("- ${char.name}（${char.role}）：${char.personality}")
                }
                appendLine()
            }
            
            // 地点信息
            if (novel.worldBuilding.geography.isNotEmpty()) {
                appendLine("【重要地点】")
                novel.worldBuilding.geography.forEach { loc ->
                    appendLine("- ${loc.name}：${loc.description.take(50)}")
                }
                appendLine()
            }
            
            if (isConvergence) {
                appendLine("【⚠️ 收束模式】本章必须推进主线结局，回收至少一条伏笔")
                appendLine()
            }
            
            appendLine("【写作法则】")
            appendLine("1. 纯中文写作，文笔流畅")
            appendLine("2. 严格遵循角色性格，禁止OOC")
            appendLine("3. 动机-行动-后果必须形成因果链")
            appendLine("4. 每章3000-5000字")
            appendLine("5. 结尾留悬念或推进关系")
            appendLine("6. 禁止突然引入新设定或遗忘已有设定")
        }
    }
    
    /** 规划Agent的Prompt */
    private fun buildPlanningPrompt(novel: Novel, chapterNum: Int, userGuide: String?): String {
        return buildString {
            appendLine("请为第${chapterNum}章生成详细规划。")
            appendLine()
            appendLine("输出格式要求（严格遵守）：")
            appendLine("【章节标题】...")
            appendLine("【场景1】地点 | 参与角色 | 目标 | 预期氛围")
            appendLine("【场景2】...")
            appendLine("【因果链】动机 → 行动 → 预期后果")
            appendLine("【伏笔处理】需要埋下的新伏笔 / 需要推进的已有伏笔")
            appendLine("【本章锚点】需要推进的活跃锚点")
            appendLine("【节奏标记】慢热/推进/高潮/余韵")
            
            userGuide?.let {
                appendLine()
                appendLine("【用户指令】$it（最高优先级）")
            }
        }
    }
    
    /** 执行Agent的Prompt */
    private fun buildExecutionPrompt(
        novel: Novel, 
        chapterNum: Int, 
        previousContent: String?, 
        planOutput: String?,
        userGuide: String?
    ): String {
        return buildString {
            if (planOutput != null) {
                appendLine("请严格按照以下规划创作第${chapterNum}章正文。")
                appendLine()
                appendLine("【章节规划】")
                appendLine(planOutput)
                appendLine()
            } else {
                appendLine("请创作第${chapterNum}章。")
                appendLine()
            }
            
            if (previousContent != null && previousContent.isNotBlank()) {
                appendLine("【上一章结尾】")
                appendLine(previousContent.takeLast(500))
                appendLine()
            }
            
            appendLine("要求：")
            appendLine("- 严格按规划的场景顺序展开")
            appendLine("- 每个场景有明确的目标和推进")
            appendLine("- 角色行为必须符合性格和当前心理状态")
            appendLine("- 结尾推进到下一个悬念点")
            appendLine("- 字数3000-5000字")
            
            userGuide?.let {
                appendLine()
                appendLine("【用户指令】$it（最高优先级，可覆盖规划）")
            }
        }
    }
    
    /** 审查Agent的Prompt */
    private fun buildReviewPrompt(novel: Novel, chapterNum: Int, content: String): String {
        return buildString {
            appendLine("请审查以下章节内容，检查一致性和逻辑性。")
            appendLine()
            appendLine("【待审查内容】")
            appendLine(content)
            appendLine()
            appendLine("审查要点：")
            appendLine("1. 角色行为是否符合性格设定（OOC检测）")
            appendLine("2. 因果链是否连贯（逻辑断裂检测）")
            appendLine("3. 世界观设定是否一致（设定冲突检测）")
            appendLine("4. 时间线是否合理（时间冲突检测）")
            appendLine("5. 伏笔是否合理推进（遗忘检测）")
            appendLine()
            appendLine("输出格式：")
            appendLine("【通过项】...")
            appendLine("【问题项】问题类型 | 具体描述 | 修改建议")
            appendLine("【总体评分】1-10")
            appendLine("【修改指令】如果有问题，给出具体的修改方向")
        }
    }
    
    /** 润色Agent的Prompt */
    private fun buildPolishPrompt(novel: Novel, chapterNum: Int, content: String, reviewFeedback: String): String {
        return buildString {
            appendLine("请润色以下章节内容，增强文学表现力。")
            appendLine()
            
            if (reviewFeedback.isNotBlank()) {
                appendLine("【审查反馈】")
                appendLine(reviewFeedback)
                appendLine()
            }
            
            appendLine("【待润色内容】")
            appendLine(content)
            appendLine()
            appendLine("润色要点：")
            appendLine("- 增强${novel.sensoryProfile.emotionExpressionStyle.name}风格的情感表达")
            appendLine("- 描写密度：${novel.sensoryProfile.descriptionDensity}/10")
            appendLine("- 增加感官细节（${novel.sensoryProfile.sensoryFocus.joinToString { it.name }}）")
            appendLine("- 优化场景过渡的自然度")
            appendLine("- 保持段落节奏变化（长短交替）")
            appendLine("- 去AI味：避免工整排比、避免过度使用比喻、避免空泛描写")
            appendLine()
            appendLine("输出完整的润色后章节。")
        }
    }
}
