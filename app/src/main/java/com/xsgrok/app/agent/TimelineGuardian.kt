package com.xsgrok.app.agent

import com.xsgrok.app.data.model.*

/**
 * 时间线守护者 - 全局时间线与一致性监控
 */

/** 时间线节点 */
data class TimelineNode(
    val id: String = System.currentTimeMillis().toString(),
    val chapter: Int,
    val timestamp: String,      // 故事内时间
    val location: String,       // 发生地点
    val event: String,          // 事件描述
    val participants: List<String>,  // 参与角色ID
    val consequences: List<String> = emptyList(),  // 产生的后果
    val anchorRelated: String? = null  // 关联的锚点ID
)

/** 一致性检查结果 */
data class ConsistencyCheckResult(
    val isValid: Boolean,
    val issues: List<ConsistencyIssue>,
    val autoFixedCount: Int
)

data class ConsistencyIssue(
    val type: IssueType,
    val severity: Float,
    val description: String,
    val suggestion: String,
    val relatedChapter: Int
)

enum class IssueType {
    TIMELINE_CONFLICT,      // 时间线冲突
    LOCATION_MISMATCH,      // 地点不一致
    CHARACTER_ABSENT,       // 角色不应在场
    ABILITY_EXCEED,         // 能力超限
    KNOWLEDGE_LEAK,         // 信息泄露（角色不应知道的信息）
    RELATIONSHIP_JUMP,      // 关系突变
    ITEM_DUPLICATE,         // 物品重复出现
    PLOT_HOLE               // 剧情漏洞
}

class TimelineGuardian {
    
    private val timeline = mutableListOf<TimelineNode>()
    private val knownFacts = mutableMapOf<String, MutableSet<String>>()  // characterId -> 已知事实
    private val characterLocations = mutableMapOf<String, String>()      // characterId -> 当前位置
    
    /** 添加时间线节点 */
    fun addTimelineNode(node: TimelineNode) {
        timeline.add(node)
        // 更新角色位置
        node.participants.forEach { charId ->
            characterLocations[charId] = node.location
        }
    }
    
    /** 记录角色已知事实 */
    fun addKnownFact(characterId: String, fact: String) {
        knownFacts.getOrPut(characterId) { mutableSetOf() }.add(fact)
    }
    
    /** 检查一致性 */
    fun checkConsistency(
        novel: Novel,
        newContent: String,
        chapterNum: Int
    ): ConsistencyCheckResult {
        val issues = mutableListOf<ConsistencyIssue>()
        
        // 1. 时间线检查
        checkTimelineConsistency(issues, chapterNum)
        
        // 2. 角色位置检查
        checkCharacterLocations(novel, newContent, issues, chapterNum)
        
        // 3. 能力限制检查
        checkAbilityLimits(novel, newContent, issues, chapterNum)
        
        // 4. 关系突变检查
        checkRelationshipJumps(novel, issues, chapterNum)
        
        // 5. 信息泄露检查
        checkInformationLeak(novel, newContent, issues, chapterNum)
        
        return ConsistencyCheckResult(
            isValid = issues.none { it.severity > 0.8f },
            issues = issues,
            autoFixedCount = 0
        )
    }
    
    /** 时间线一致性检查 */
    private fun checkTimelineConsistency(issues: MutableList<ConsistencyIssue>, chapterNum: Int) {
        if (timeline.size < 2) return
        
        val recent = timeline.takeLast(5)
        for (i in 1 until recent.size) {
            val prev = recent[i - 1]
            val curr = recent[i]
            
            // 同一角色在不同地点同时出现
            val commonChars = prev.participants.intersect(curr.participants.toSet())
            if (commonChars.isNotEmpty() && prev.location != curr.location && prev.chapter == curr.chapter) {
                issues.add(ConsistencyIssue(
                    type = IssueType.TIMELINE_CONFLICT,
                    severity = 0.9f,
                    description = "同一章节中角色出现在不同地点",
                    suggestion = "需要交代角色从${prev.location}移动到${curr.location}的过程",
                    relatedChapter = chapterNum
                ))
            }
        }
    }
    
    /** 角色位置检查 */
    private fun checkCharacterLocations(
        novel: Novel, content: String, 
        issues: MutableList<ConsistencyIssue>, chapterNum: Int
    ) {
        novel.characters.forEach { char ->
            val lastKnownLocation = characterLocations[char.id]
            if (lastKnownLocation != null && content.contains(char.name)) {
                // 检查是否提到了位置转换
                val locationKeywords = listOf("来到", "前往", "赶往", "抵达", "回到", "出现在")
                val hasTransition = locationKeywords.any { content.contains("$it${lastKnownLocation}") } ||
                    content.contains("从${lastKnownLocation}")
                
                // 如果角色突然出现在新地点但没有交代过程
                novel.worldBuilding.geography.forEach { loc ->
                    if (content.contains(loc.name) && loc.name != lastKnownLocation && 
                        !hasTransition && content.contains(char.name)) {
                        // 轻度提醒，不一定是问题
                    }
                }
            }
        }
    }
    
    /** 能力限制检查 */
    private fun checkAbilityLimits(
        novel: Novel, content: String,
        issues: MutableList<ConsistencyIssue>, chapterNum: Int
    ) {
        // 检查力量体系限制是否被违反
        val powerSystem = novel.worldBuilding.powerSystem
        if (powerSystem.isNotBlank()) {
            // 提取等级限制关键词
            val levelKeywords = powerSystem.split("，", "、", "。").filter { 
                it.contains("级") || it.contains("阶") || it.contains("境") || it.contains("段")
            }
            
            // 简单检查：如果提到了超越当前等级的表现
            // 这需要更复杂的NLP，目前仅做基础检查
        }
    }
    
    /** 关系突变检查 */
    private fun checkRelationshipJumps(
        novel: Novel, 
        issues: MutableList<ConsistencyIssue>, chapterNum: Int
    ) {
        novel.relationshipStates.forEach { relationship ->
            // 亲密度不应在单章内变化超过30
            val recentEvents = relationship.history.filter { it.chapter >= chapterNum - 1 }
            val totalChange = recentEvents.sumOf { it.intimacyChange }
            
            if (totalChange > 30 || totalChange < -30) {
                issues.add(ConsistencyIssue(
                    type = IssueType.RELATIONSHIP_JUMP,
                    severity = 0.7f,
                    description = "${relationship.participantNames.joinToString("&")}关系变化过快",
                    suggestion = "关系变化应循序渐进，需要更多铺垫",
                    relatedChapter = chapterNum
                ))
            }
        }
    }
    
    /** 信息泄露检查 */
    private fun checkInformationLeak(
        novel: Novel, content: String,
        issues: MutableList<ConsistencyIssue>, chapterNum: Int
    ) {
        novel.characters.forEach { char ->
            val charKnownFacts = knownFacts[char.id] ?: return@forEach
            
            // 检查角色是否提到了不应知道的信息
            // 这需要更复杂的实现，目前跳过
        }
    }
    
    /** 生成时间线Prompt */
    fun generateTimelinePrompt(): String {
        if (timeline.isEmpty()) return ""
        
        return buildString {
            appendLine("【时间线】")
            timeline.takeLast(5).forEach { node ->
                appendLine("- 第${node.chapter}章 | ${node.timestamp} | ${node.location} | ${node.event.take(50)}")
            }
            appendLine()
        }
    }
    
    /** 导出/导入状态 */
    fun exportState(): GuardianState {
        return GuardianState(
            timeline = timeline.toList(),
            knownFacts = knownFacts.mapValues { it.value.toSet() },
            characterLocations = characterLocations.toMap()
        )
    }
    
    fun importState(state: GuardianState) {
        timeline.clear()
        timeline.addAll(state.timeline)
        knownFacts.clear()
        state.knownFacts.forEach { (k, v) -> knownFacts[k] = v.toMutableSet() }
        characterLocations.clear()
        characterLocations.putAll(state.characterLocations)
    }
}

data class GuardianState(
    val timeline: List<TimelineNode>,
    val knownFacts: Map<String, Set<String>>,
    val characterLocations: Map<String, String>
)
