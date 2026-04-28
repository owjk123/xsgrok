package com.xsgrok.app.memory

import com.xsgrok.app.data.model.*

/**
 * 记忆一致性管理器
 * 负责注入相关记忆、检查一致性、更新角色状态
 */
class ConsistencyManager {
    
    /**
     * 注入相关记忆到Prompt
     */
    fun injectMemories(novel: Novel, chapterNum: Int): String {
        val relevantMemories = findRelevantMemories(novel, chapterNum)
        
        return buildString {
            // 核心记忆（必须注入）
            appendLine("【核心记忆（必须遵守）】")
            appendLine()
            
            // 角色身体特征
            if (novel.generationConfig.checkBodyConsistency) {
                appendLine("【角色身体特征】")
                novel.characterBodyProfiles.forEach { profile ->
                    appendLine("${profile.characterName}：")
                    if (profile.bodyFeatures.height.isNotBlank()) {
                        appendLine("  - 身高：${profile.bodyFeatures.height}")
                    }
                    if (profile.bodyFeatures.build.isNotBlank()) {
                        appendLine("  - 体型：${profile.bodyFeatures.build}")
                    }
                    if (profile.bodyFeatures.skinTone.isNotBlank()) {
                        appendLine("  - 肤色：${profile.bodyFeatures.skinTone}")
                    }
                    if (profile.bodyFeatures.hairColor.isNotBlank()) {
                        appendLine("  - 发色：${profile.bodyFeatures.hairColor}")
                    }
                    if (profile.bodyFeatures.eyeColor.isNotBlank()) {
                        appendLine("  - 瞳色：${profile.bodyFeatures.eyeColor}")
                    }
                    if (profile.bodyFeatures.voiceDescription.isNotBlank()) {
                        appendLine("  - 声音：${profile.bodyFeatures.voiceDescription}")
                    }
                    if (profile.bodyFeatures.scent.isNotBlank()) {
                        appendLine("  - 体味：${profile.bodyFeatures.scent}")
                    }
                    
                    // 当前状态
                    appendLine("  当前状态：")
                    appendLine("    - 情绪：${profile.currentState.emotionalState}")
                    appendLine("    - 衣着：${profile.currentState.clothingState}")
                    if (profile.currentState.position.isNotBlank()) {
                        appendLine("    - 姿势：${profile.currentState.position}")
                    }
                    appendLine()
                }
            }
            
            // 关系状态
            if (novel.generationConfig.checkRelationshipConsistency) {
                appendLine("【角色关系】")
                novel.relationshipStates.forEach { relationship ->
                    appendLine("${relationship.participantNames.joinToString(" & ")}：")
                    appendLine("  - 亲密度：${relationship.intimacyLevel}%")
                    appendLine("  - 信任度：${relationship.trustLevel}%")
                    appendLine("  - 当前阶段：${relationship.currentStage.displayName}")
                    appendLine("  - 权力动态：${relationship.powerDynamic.displayName}")
                    
                    // 最近关系事件
                    val recentEvents = relationship.history.takeLast(3)
                    if (recentEvents.isNotEmpty()) {
                        appendLine("  最近事件：")
                        recentEvents.forEach { event ->
                            appendLine("    - ${event.type.name}（第${event.chapter}章）：${event.description}")
                        }
                    }
                    appendLine()
                }
            }
            
            // 相关记忆（场景匹配时注入）
            appendLine("【相关场景记忆】")
            relevantMemories.forEach { memory ->
                appendLine("场景：${memory.sceneName}（第${memory.chapter}章）")
                appendLine("  参与者：${memory.participants.joinToString()}")
                if (memory.atmosphere.isNotBlank()) {
                    appendLine("  氛围：${memory.atmosphere}")
                }
                if (memory.triggeredEvents.isNotEmpty()) {
                    appendLine("  已发生事件：")
                    memory.triggeredEvents.takeLast(3).forEach { event ->
                        appendLine("    - ${event.type.name}：${event.description}")
                    }
                }
                appendLine()
            }
            
            // 背景记忆（可选注入）
            appendLine("【背景记忆】")
            val unresolvedForeshadowings = novel.getUnresolvedForeshadowings()
            if (unresolvedForeshadowings.isNotEmpty()) {
                appendLine("未回收伏笔：")
                unresolvedForeshadowings.take(5).forEach { f ->
                    appendLine("  - ${f.content}（第${f.plantedChapter}章埋下）")
                }
            }
        }
    }
    
    /**
     * 更新角色状态
     */
    fun updateCharacterState(
        novel: Novel,
        characterId: String,
        newState: CharacterState
    ): Novel {
        val index = novel.characterBodyProfiles.indexOfFirst { it.characterId == characterId }
        
        if (index >= 0) {
            val profile = novel.characterBodyProfiles[index]
            novel.characterBodyProfiles[index] = profile.copy(
                currentState = newState.copy(
                    lastInteractionTime = System.currentTimeMillis()
                )
            )
        }
        
        return novel
    }
    
    /**
     * 更新关系状态
     */
    fun updateRelationshipState(
        novel: Novel,
        characterId1: String,
        characterId2: String,
        eventType: RelationshipEventType,
        description: String,
        chapterNum: Int
    ): Novel {
        val relationship = novel.getRelationship(characterId1, characterId2)
        
        if (relationship != null) {
            val index = novel.relationshipStates.indexOfFirst { 
                it.relationshipId == relationship.relationshipId 
            }
            
            if (index >= 0) {
                val updatedRelationship = relationship.copy(
                    intimacyLevel = (relationship.intimacyLevel + eventType.intimacyImpact).coerceIn(0, 100),
                    trustLevel = (relationship.trustLevel + eventType.trustImpact).coerceIn(0, 100),
                    currentStage = calculateNewStage(
                        relationship.intimacyLevel + eventType.intimacyImpact
                    ),
                    history = relationship.history.apply {
                        add(
                            RelationshipEvent(
                                type = eventType,
                                description = description,
                                chapter = chapterNum,
                                intimacyChange = eventType.intimacyImpact,
                                trustChange = eventType.trustImpact
                            )
                        )
                    },
                    lastInteractionTime = System.currentTimeMillis()
                )
                
                novel.relationshipStates[index] = updatedRelationship
                
                // 更新小说的亲密度进度
                val maxIntimacy = novel.relationshipStates.maxOfOrNull { it.intimacyLevel } ?: 0
                novel.intimacyProgress = maxIntimacy.toFloat() / 100f
            }
        } else {
            // 创建新的关系
            val character1 = novel.getCharacterBodyProfile(characterId1)
            val character2 = novel.getCharacterBodyProfile(characterId2)
            
            val newRelationship = RelationshipState(
                participants = listOf(characterId1, characterId2),
                participantNames = listOf(
                    character1?.characterName ?: characterId1,
                    character2?.characterName ?: characterId2
                ),
                intimacyLevel = eventType.intimacyImpact.coerceAtLeast(0),
                trustLevel = eventType.trustImpact.coerceAtLeast(0),
                currentStage = calculateNewStage(eventType.intimacyImpact.coerceAtLeast(0)),
                history = mutableListOf(
                    RelationshipEvent(
                        type = eventType,
                        description = description,
                        chapter = chapterNum,
                        intimacyChange = eventType.intimacyImpact,
                        trustChange = eventType.trustImpact
                    )
                ),
                lastInteractionTime = System.currentTimeMillis()
            )
            
            novel.relationshipStates.add(newRelationship)
        }
        
        return novel
    }
    
    /**
     * 根据亲密度计算阶段
     */
    private fun calculateNewStage(intimacyLevel: Int): RelationshipStage {
        return RelationshipStage.entries.find { 
            intimacyLevel in it.intimacyRange 
        } ?: RelationshipStage.INITIAL
    }
    
    /**
     * 检查身体描写一致性
     */
    fun checkBodyConsistency(
        novel: Novel,
        content: String
    ): List<ConsistencyIssue> {
        val issues = mutableListOf<ConsistencyIssue>()
        
        for (profile in novel.characterBodyProfiles) {
            // 检查身高一致性
            if (profile.bodyFeatures.height.isNotBlank() && content.contains(profile.characterName)) {
                // 检查是否有冲突的描述
                val heightPatterns = listOf("高大", "矮小", "娇小", "高挑", "矮胖", "瘦高")
                for (pattern in heightPatterns) {
                    if (content.contains(pattern) && !profile.bodyFeatures.height.contains(pattern)) {
                        // 可能是冲突，需要注意
                        issues.add(
                            ConsistencyIssue(
                                type = IssueType.BODY_INCONSISTENCY,
                                characterId = profile.characterId,
                                description = "身高描述可能不一致：$pattern",
                                severity = IssueSeverity.WARNING
                            )
                        )
                    }
                }
            }
            
            // 检查肤色一致性
            if (profile.bodyFeatures.skinTone.isNotBlank() && content.contains(profile.characterName)) {
                val skinPatterns = listOf("白皙", "黝黑", "古铜", "小麦色", "苍白", "红润")
                for (pattern in skinPatterns) {
                    if (content.contains(pattern) && !profile.bodyFeatures.skinTone.contains(pattern)) {
                        issues.add(
                            ConsistencyIssue(
                                type = IssueType.BODY_INCONSISTENCY,
                                characterId = profile.characterId,
                                description = "肤色描述可能不一致：$pattern",
                                severity = IssueSeverity.WARNING
                            )
                        )
                    }
                }
            }
        }
        
        return issues
    }
    
    /**
     * 检查关系连贯性
     */
    fun checkRelationshipConsistency(
        novel: Novel,
        content: String,
        chapterNum: Int
    ): List<ConsistencyIssue> {
        val issues = mutableListOf<ConsistencyIssue>()
        
        for (relationship in novel.relationshipStates) {
            // 检查是否跳过了应有的阶段
            val recentEvents = relationship.history.filter { 
                it.chapter in (chapterNum - 3)..chapterNum 
            }
            
            if (recentEvents.isEmpty() && relationship.intimacyLevel > 50) {
                // 高亲密度但最近没有互动事件
                issues.add(
                    ConsistencyIssue(
                        type = IssueType.RELATIONSHIP_GAP,
                        description = "${relationship.participantNames.joinToString(" & ")}亲密度较高但缺少近期互动描写",
                        severity = IssueSeverity.INFO
                    )
                )
            }
            
            // 检查信任度异常
            if (relationship.trustLevel < 20 && relationship.history.any { 
                it.type == RelationshipEventType.CONFESSION 
            }) {
                issues.add(
                    ConsistencyIssue(
                        type = IssueType.RELATIONSHIP_INCONSISTENCY,
                        description = "表白但信任度极低，逻辑可能有问题",
                        severity = IssueSeverity.ERROR
                    )
                )
            }
        }
        
        return issues
    }
    
    /**
     * 添加场景记忆
     */
    fun addSceneMemory(
        novel: Novel,
        sceneName: String,
        sceneDescription: String,
        location: String,
        chapterNum: Int,
        participants: List<String>
    ): Novel {
        val sceneMemory = SceneMemory(
            sceneName = sceneName,
            sceneDescription = sceneDescription,
            location = location,
            chapter = chapterNum,
            participants = participants
        )
        
        novel.sceneMemories.add(sceneMemory)
        novel.currentSceneId = sceneMemory.sceneId
        
        return novel
    }
    
    /**
     * 更新场景记忆
     */
    fun updateSceneMemory(
        novel: Novel,
        sceneId: String,
        event: SceneEvent? = null,
        bodyChange: BodyStateChange? = null,
        emotionalMoment: EmotionalMoment? = null
    ): Novel {
        val index = novel.sceneMemories.indexOfFirst { it.sceneId == sceneId }
        
        if (index >= 0) {
            val memory = novel.sceneMemories[index]
            
            event?.let {
                memory.triggeredEvents.add(it)
            }
            
            bodyChange?.let {
                memory.bodyStateChanges.add(it)
            }
            
            emotionalMoment?.let {
                memory.emotionalTrajectory.add(it)
            }
            
            novel.sceneMemories[index] = memory
        }
        
        return novel
    }
    
    /**
     * 查找相关记忆
     */
    private fun findRelevantMemories(novel: Novel, chapterNum: Int): List<SceneMemory> {
        return novel.sceneMemories.filter { memory ->
            // 最近3章的场景记忆
            memory.chapter >= chapterNum - 3 ||
            // 涉及当前章节角色的记忆
            memory.participants.any { participant ->
                novel.sceneMemories.any { 
                    it.chapter >= chapterNum - 2 && it.participants.contains(participant) 
                }
            }
        }.take(5)
    }
    
    /**
     * 生成一致性提示
     */
    fun generateConsistencyHint(novel: Novel): String {
        val hints = mutableListOf<String>()
        
        // 身体一致性提示
        if (novel.generationConfig.checkBodyConsistency && novel.characterBodyProfiles.isNotEmpty()) {
            hints.add("【身体特征一致性】")
            novel.characterBodyProfiles.forEach { profile ->
                hints.add("${profile.characterName}的特征：${profile.bodyFeatures.height}、${profile.bodyFeatures.build}")
            }
            hints.add("")
        }
        
        // 关系一致性提示
        if (novel.generationConfig.checkRelationshipConsistency && novel.relationshipStates.isNotEmpty()) {
            hints.add("【关系状态一致性】")
            novel.relationshipStates.forEach { relationship ->
                hints.add("${relationship.participantNames.joinToString("→")}：亲密度${relationship.intimacyLevel}%（${relationship.currentStage.displayName}）")
            }
            hints.add("")
        }
        
        // 场景连续性提示
        val currentScene = novel.getCurrentSceneMemory()
        if (currentScene != null) {
            hints.add("【场景连续性】")
            hints.add("当前场景：${currentScene.sceneName}")
            hints.add("场景氛围：${currentScene.atmosphere}")
        }
        
        return hints.joinToString("\n")
    }
    
    /**
     * 一致性问题数据类
     */
    data class ConsistencyIssue(
        val type: IssueType,
        val characterId: String? = null,
        val description: String,
        val severity: IssueSeverity
    )
    
    enum class IssueType {
        BODY_INCONSISTENCY,
        RELATIONSHIP_INCONSISTENCY,
        RELATIONSHIP_GAP,
        SCENE_INCONSISTENCY,
        BEHAVIOR_INCONSISTENCY
    }
    
    enum class IssueSeverity {
        INFO,      // 信息提示
        WARNING,   // 警告
        ERROR      // 错误
    }
}
