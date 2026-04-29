package com.xsgrok.app.agent

import kotlin.math.max

/**
 * 剧情锚点系统 - 多层目标管理
 * 确保长篇生成不跑偏的核心机制
 */

/** 锚点层级 */
enum class AnchorLevel(val displayName: String, val description: String) {
    MAIN_PLOT("主线锚点", "贯穿全书的核心目标，不可偏离"),
    ARC_GOAL("中期目标", "当前剧情弧的任务目标，跨5-10章"),
    SCENE_TARGET("场景目标", "单场景的即时目标，1-3章内完成")
}

/** 锚点状态 */
enum class AnchorStatus {
    PENDING,    // 待执行
    ACTIVE,     // 当前活跃
    COMPLETED,  // 已完成
    DEVIATED,   // 已偏离
    SKIPPED     // 跳过
}

/** 剧情锚点 */
data class PlotAnchor(
    val id: String = System.currentTimeMillis().toString(),
    val level: AnchorLevel,
    val title: String,
    val description: String,
    val status: AnchorStatus = AnchorStatus.PENDING,
    val priority: Int = 5,  // 1-10, 10最高
    val targetChapter: Int = 0,  // 目标章节(0=不限)
    val completedChapter: Int? = null,
    val parentAnchorId: String? = null,  // 父锚点ID(场景目标->中期目标->主线)
    val constraints: List<String> = emptyList(),  // 约束条件
    val requiredForeshadowings: List<String> = emptyList(),  // 关联伏笔ID
    val deviationTolerance: Float = 0.3f  // 允许的偏离程度
)

/** 动机-行动-后果链 */
data class CausalChain(
    val id: String = System.currentTimeMillis().toString(),
    val chapter: Int,
    val motivation: String,    // 动机
    val action: String,        // 行动
    val consequence: String,   // 后果
    val affectedAnchors: List<String> = emptyList(),  // 影响的锚点ID
    val nextChainId: String? = null  // 下一环ID
)

/** 偏差检测结果 */
data class DeviationReport(
    val anchorId: String,
    val anchorTitle: String,
    val deviationType: DeviationType,
    val severity: Float,  // 0.0-1.0
    val description: String,
    val correctionHint: String,
    val detectedAtChapter: Int
)

enum class DeviationType {
    PLOT_DRIFT,          // 剧情跑偏
    LOGIC_BREAK,         // 逻辑断裂
    CHARACTER_OOC,       // 角色崩坏
    TIMELINE_CONFLICT,   // 时间线冲突
    MOTIVATION_LOST,     // 动机丢失
    FORESHADOWING_IGNORED // 伏笔遗忘
}

/** 剧情控制器 - 管理所有锚点和偏差检测 */
class PlotController {
    
    private val anchors = mutableListOf<PlotAnchor>()
    private val causalChains = mutableListOf<CausalChain>()
    private val deviationHistory = mutableListOf<DeviationReport>()
    
    /** 添加锚点 */
    fun addAnchor(anchor: PlotAnchor) {
        anchors.add(anchor)
    }
    
    /** 获取当前活跃锚点 */
    fun getActiveAnchors(): List<PlotAnchor> {
        return anchors.filter { it.status == AnchorStatus.ACTIVE }
    }
    
    /** 获取当前章节应推进的锚点 */
    fun getAnchorsForChapter(chapterNum: Int): List<PlotAnchor> {
        return anchors.filter { 
            it.status == AnchorStatus.ACTIVE || 
            (it.status == AnchorStatus.PENDING && (it.targetChapter == 0 || it.targetChapter <= chapterNum + 2))
        }.sortedByDescending { it.priority }
    }
    
    /** 激活下一个待执行锚点 */
    fun activateNextAnchors(chapterNum: Int, maxActive: Int = 3): List<PlotAnchor> {
        val pending = anchors.filter { 
            it.status == AnchorStatus.PENDING && (it.targetChapter == 0 || it.targetChapter <= chapterNum)
        }.sortedByDescending { it.priority }
        
        val currentActive = anchors.count { it.status == AnchorStatus.ACTIVE }
        val toActivate = pending.take(if (maxActive > currentActive) maxActive - currentActive else 0)
        
        toActivate.forEach { anchor ->
            val index = anchors.indexOf(anchor)
            if (index >= 0) {
                anchors[index] = anchor.copy(status = AnchorStatus.ACTIVE)
            }
        }
        
        return toActivate
    }
    
    /** 完成锚点 */
    fun completeAnchor(anchorId: String, chapterNum: Int) {
        val index = anchors.indexOfFirst { it.id == anchorId }
        if (index >= 0) {
            anchors[index] = anchors[index].copy(
                status = AnchorStatus.COMPLETED,
                completedChapter = chapterNum
            )
            // 激活子锚点
            anchors.filter { it.parentAnchorId == anchorId && it.status == AnchorStatus.PENDING }
                .forEach { child ->
                    val childIndex = anchors.indexOf(child)
                    if (childIndex >= 0) {
                        anchors[childIndex] = child.copy(status = AnchorStatus.ACTIVE)
                    }
                }
        }
    }
    
    /** 检测偏差 */
    fun detectDeviation(
        generatedContent: String,
        chapterNum: Int,
        activeAnchors: List<PlotAnchor>
    ): List<DeviationReport> {
        val reports = mutableListOf<DeviationReport>()
        
        for (anchor in activeAnchors) {
            // 检查锚点关键词是否在内容中被提及或推进
            val keywords = anchor.description.split("，", "、", "。").filter { it.length > 2 }
            val mentionedCount = keywords.count { keyword -> generatedContent.contains(keyword) }
            val mentionRate = if (keywords.isNotEmpty()) mentionedCount.toFloat() / keywords.size else 0f
            
            if (mentionRate < anchor.deviationTolerance && chapterNum >= anchor.targetChapter - 1) {
                reports.add(DeviationReport(
                    anchorId = anchor.id,
                    anchorTitle = anchor.title,
                    deviationType = DeviationType.PLOT_DRIFT,
                    severity = 1f - mentionRate,
                    description = "锚点「${anchor.title}」在生成内容中未被推进",
                    correctionHint = "需要在后续内容中推进：${anchor.description}",
                    detectedAtChapter = chapterNum
                ))
            }
        }
        
        // 检测逻辑断裂 - 因果链不连续
        val recentChains = causalChains.filter { it.chapter >= chapterNum - 2 }
        if (recentChains.isNotEmpty()) {
            val lastChain = recentChains.last()
            val consequenceKeywords = lastChain.consequence.split("，", "、", "。").filter { it.length > 2 }
            val consequenceAddressed = consequenceKeywords.any { generatedContent.contains(it) }
            
            if (!consequenceAddressed && lastChain.nextChainId == null) {
                reports.add(DeviationReport(
                    anchorId = lastChain.id,
                    anchorTitle = "因果链连续性",
                    deviationType = DeviationType.LOGIC_BREAK,
                    severity = 0.7f,
                    description = "上一章后果未被本章处理",
                    correctionHint = "需要回应上章后果：${lastChain.consequence}",
                    detectedAtChapter = chapterNum
                ))
            }
        }
        
        deviationHistory.addAll(reports)
        return reports
    }
    
    /** 生成矫正指令 */
    fun generateCorrectionPrompt(reports: List<DeviationReport>): String {
        if (reports.isEmpty()) return ""
        
        return buildString {
            appendLine("【⚠️ 剧情矫正指令】")
            appendLine("检测到以下偏差，必须在后续内容中修正：")
            appendLine()
            
            reports.sortedByDescending { it.severity }.forEach { report ->
                appendLine("- [${report.deviationType.name}] ${report.description}")
                appendLine("  矫正方向：${report.correctionHint}")
            }
            
            appendLine()
            appendLine("请优先处理严重度最高的偏差，确保剧情回归正轨。")
        }
    }
    
    /** 添加因果链 */
    fun addCausalChain(chain: CausalChain) {
        // 链接上一环
        if (causalChains.isNotEmpty()) {
            val lastIndex = causalChains.lastIndex
            causalChains[lastIndex] = causalChains[lastIndex].copy(nextChainId = chain.id)
        }
        causalChains.add(chain)
    }
    
    /** 获取当前因果链 */
    fun getCurrentCausalChain(): CausalChain? {
        return causalChains.lastOrNull()
    }
    
    /** 生成锚点提示文本（注入Prompt） */
    fun generateAnchorPrompt(chapterNum: Int): String {
        val active = getActiveAnchors()
        val upcoming = getAnchorsForChapter(chapterNum)
        
        return buildString {
            if (active.isNotEmpty()) {
                appendLine("【当前活跃锚点】")
                active.forEach { anchor ->
                    appendLine("- [${anchor.level.displayName}] ${anchor.title}：${anchor.description}")
                    if (anchor.constraints.isNotEmpty()) {
                        appendLine("  约束：${anchor.constraints.joinToString("；")}")
                    }
                }
                appendLine()
            }
            
            if (upcoming.isNotEmpty() && upcoming != active) {
                appendLine("【近期目标】")
                upcoming.filter { it !in active }.take(3).forEach { anchor ->
                    appendLine("- [${anchor.level.displayName}] ${anchor.title}")
                }
                appendLine()
            }
            
            val recentChain = getCurrentCausalChain()
            if (recentChain != null) {
                appendLine("【上章因果链】")
                appendLine("动机：${recentChain.motivation}")
                appendLine("行动：${recentChain.action}")
                appendLine("后果：${recentChain.consequence}")
                appendLine("→ 本章必须承接此后果继续推进")
                appendLine()
            }
        }
    }
    
    /** 从大纲自动生成锚点 */
    fun generateAnchorsFromOutline(outline: String, keyNodes: List<com.xsgrok.app.data.model.KeyNode>): List<PlotAnchor> {
        val result = mutableListOf<PlotAnchor>()
        
        // 主线锚点 - 从大纲提取
        result.add(PlotAnchor(
            level = AnchorLevel.MAIN_PLOT,
            title = "核心主线",
            description = outline.take(200),
            priority = 10,
            status = AnchorStatus.ACTIVE
        ))
        
        // 中期目标 - 从关键节点生成
        keyNodes.forEachIndexed { index, node ->
            result.add(PlotAnchor(
                level = AnchorLevel.ARC_GOAL,
                title = node.title,
                description = node.description,
                priority = 7,
                targetChapter = node.targetChapter,
                status = if (index == 0) AnchorStatus.ACTIVE else AnchorStatus.PENDING,
                parentAnchorId = result.first().id
            ))
        }
        
        anchors.addAll(result)
        return result
    }
    
    /** 导出状态用于持久化 */
    fun exportState(): PlotControllerState {
        return PlotControllerState(
            anchors = anchors.toList(),
            causalChains = causalChains.toList(),
            deviationHistory = deviationHistory.toList()
        )
    }
    
    /** 导入状态 */
    fun importState(state: PlotControllerState) {
        anchors.clear()
        anchors.addAll(state.anchors)
        causalChains.clear()
        causalChains.addAll(state.causalChains)
        deviationHistory.clear()
        deviationHistory.addAll(state.deviationHistory)
    }
}

data class PlotControllerState(
    val anchors: List<PlotAnchor>,
    val causalChains: List<CausalChain>,
    val deviationHistory: List<DeviationReport>
)
