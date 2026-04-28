package com.xsgrok.app.generation

import com.xsgrok.app.data.model.*
import com.xsgrok.app.prompt.PromptTemplates

/**
 * 多阶段生成器
 * 将章节生成拆分为多个阶段，每个阶段专注于特定任务
 */
class MultiStageGenerator {
    
    private val sceneDetector = SceneDetector()
    private val rhythmController = RhythmController()
    
    /**
     * 执行多阶段生成
     */
    suspend fun generateChapter(
        novel: Novel,
        chapterNum: Int,
        previousContent: String?,
        userGuide: String?,
        onStageComplete: (ChapterPhase, String) -> Unit,
        onError: (String) -> Unit
    ): String {
        try {
            // 阶段1: 生成大纲
            val outline = generateOutline(novel, chapterNum, userGuide)
            onStageComplete(ChapterPhase.OUTLINE, outline)
            
            // 阶段2: 生成章节框架
            val framework = generateChapterFramework(novel, chapterNum, outline, previousContent)
            onStageComplete(ChapterPhase.FRAMEWORK, framework)
            
            // 阶段3: 填充感官细节
            val withSensory = fillSensoryDetails(novel, framework)
            onStageComplete(ChapterPhase.DETAIL_FILL, withSensory)
            
            // 阶段4: 情绪润色
            val polished = polishEmotions(novel, withSensory)
            onStageComplete(ChapterPhase.EMOTION_POLISH, polished)
            
            // 阶段5: 一致性检查
            val final = checkConsistency(novel, polished, chapterNum)
            onStageComplete(ChapterPhase.CONSISTENCY_CHECK, final)
            
            return final
        } catch (e: Exception) {
            onError(e.message ?: "生成失败")
            return ""
        }
    }
    
    /**
     * 阶段1: 生成大纲
     */
    private fun generateOutline(
        novel: Novel,
        chapterNum: Int,
        userGuide: String?
    ): String {
        val progressInfo = novel.getProgressInfo(chapterNum)
        
        return buildString {
            appendLine("=== 第${chapterNum}章大纲 ===")
            appendLine()
            
            // 章节目标
            appendLine("【本章目标】")
            if (userGuide.isNullOrBlank()) {
                appendLine("- 推进主线剧情")
                appendLine("- 深化角色关系")
                appendLine("- 铺垫下一个关键节点")
            } else {
                appendLine("- $userGuide")
            }
            appendLine()
            
            // 关键情节点
            appendLine("【关键情节点】")
            appendLine("1. 开场：设定场景和氛围")
            appendLine("2. 发展：引入矛盾或互动")
            appendLine("3. 高潮：情感或情节的爆发点")
            appendLine("4. 收尾：留下悬念或推进关系")
            appendLine()
            
            // 节奏规划
            appendLine("【节奏规划】")
            val rhythmPhase = rhythmController.determinePhase(novel, chapterNum)
            appendLine("- 当前节奏阶段：${rhythmPhase.displayName}")
            appendLine("- ${rhythmPhase.description}")
            appendLine("- 目标字数比例：${(rhythmPhase.targetWordRatio * 100).toInt()}%")
        }
    }
    
    /**
     * 阶段2: 生成章节框架
     */
    private fun generateChapterFramework(
        novel: Novel,
        chapterNum: Int,
        outline: String,
        previousContent: String?
    ): String {
        val progressInfo = novel.getProgressInfo(chapterNum)
        
        return buildString {
            appendLine("=== 第${chapterNum}章框架 ===")
            appendLine()
            
            // 场景规划
            appendLine("【场景规划】")
            
            // 根据节奏确定场景数量
            val sceneCount = when (rhythmController.determinePhase(novel, chapterNum)) {
                RhythmPhase.SLOW_BUILDUP -> 3
                RhythmPhase.PROGRESSION -> 4
                RhythmPhase.CLIMAX -> 2
                RhythmPhase.AFTERMATH -> 3
            }
            
            for (i in 1..sceneCount) {
                val sceneType = determineSceneType(i, sceneCount, novel)
                appendLine("场景${i}：$sceneType")
            }
            appendLine()
            
            // 角色互动规划
            appendLine("【角色互动】")
            if (novel.characters.size >= 2) {
                val mainChars = novel.characters.take(3)
                mainChars.forEachIndexed { index, char ->
                    appendLine("${index + 1}. ${char.name}（${char.role}）")
                }
            }
            appendLine()
            
            // 情感线规划
            appendLine("【情感线】")
            appendLine("- 起始状态：${getEmotionalState(novel, chapterNum, true)}")
            appendLine("- 目标状态：${getEmotionalState(novel, chapterNum, false)}")
            appendLine("- 情感变化：${getEmotionalArc(novel, chapterNum)}")
        }
    }
    
    /**
     * 阶段3: 填充感官细节
     */
    private fun fillSensoryDetails(
        novel: Novel,
        framework: String
    ): String {
        val profile = novel.sensoryProfile
        val template = PromptTemplates.getTemplate(profile.tabooLevel)
        
        return buildString {
            appendLine(framework)
            appendLine()
            
            // 感官描写注入
            appendLine("=== 感官描写注入 ===")
            appendLine()
            
            appendLine("【视觉描写注入】")
            template.instructionLibrary.visualDescriptions
                .shuffled()
                .take(profile.descriptionDensity)
                .forEach { appendLine("- $it") }
            appendLine()
            
            appendLine("【触觉描写注入】")
            template.instructionLibrary.tactileDescriptions
                .shuffled()
                .take(profile.descriptionDensity)
                .forEach { appendLine("- $it") }
            appendLine()
            
            if (profile.sensoryFocus.contains(SensoryType.AUDITORY)) {
                appendLine("【听觉描写注入】")
                template.instructionLibrary.auditoryDescriptions
                    .shuffled()
                    .take(profile.descriptionDensity)
                    .forEach { appendLine("- $it") }
                appendLine()
            }
            
            if (profile.sensoryFocus.contains(SensoryType.OLFACTORY)) {
                appendLine("【嗅觉描写注入】")
                template.instructionLibrary.olfactoryDescriptions
                    .shuffled()
                    .take(minOf(3, profile.descriptionDensity))
                    .forEach { appendLine("- $it") }
                appendLine()
            }
            
            appendLine("【心理描写注入】")
            template.instructionLibrary.psychologicalDescriptions
                .shuffled()
                .take(profile.descriptionDensity)
                .forEach { appendLine("- $it") }
        }
    }
    
    /**
     * 阶段4: 情绪润色
     */
    private fun polishEmotions(
        novel: Novel,
        content: String
    ): String {
        return buildString {
            appendLine(content)
            appendLine()
            
            appendLine("=== 情绪润色指导 ===")
            appendLine()
            
            // 情感表达风格
            appendLine("【情感表达】")
            when (novel.sensoryProfile.emotionExpressionStyle) {
                EmotionStyle.IMPLICIT -> {
                    appendLine("- 方式：含蓄暗示，用动作代替语言")
                    appendLine("- 避免直接描述情感")
                    appendLine("- 通过细节传达情绪")
                }
                EmotionStyle.MODERATE -> {
                    appendLine("- 方式：适度表达，情感与动作结合")
                    appendLine("- 可以有简短的内心独白")
                    appendLine("- 情感描写点到为止")
                }
                EmotionStyle.EXPLICIT -> {
                    appendLine("- 方式：直接表达，深入心理")
                    appendLine("- 可以有较多内心活动")
                    appendLine("- 情感描写较为深入")
                }
                EmotionStyle.RAW -> {
                    appendLine("- 方式：原生表达，完全真实")
                    appendLine("- 不加修饰的情感流露")
                    appendLine("- 心理活动直接呈现")
                }
            }
        }
    }
    
    /**
     * 阶段5: 一致性检查
     */
    private fun checkConsistency(
        novel: Novel,
        content: String,
        chapterNum: Int
    ): String {
        return buildString {
            appendLine(content)
            appendLine()
            
            appendLine("=== 一致性检查清单 ===")
            appendLine()
            
            // 身体一致性检查
            if (novel.generationConfig.checkBodyConsistency) {
                appendLine("【身体一致性】")
                novel.characterBodyProfiles.forEach { profile ->
                    appendLine("- ${profile.characterName}特征：")
                    if (profile.bodyFeatures.height.isNotBlank()) {
                        appendLine("  身高：${profile.bodyFeatures.height}")
                    }
                    if (profile.bodyFeatures.build.isNotBlank()) {
                        appendLine("  体型：${profile.bodyFeatures.build}")
                    }
                    if (profile.currentState.position.isNotBlank()) {
                        appendLine("  当前状态：${profile.currentState.position}")
                    }
                }
                appendLine()
            }
            
            // 关系一致性检查
            if (novel.generationConfig.checkRelationshipConsistency) {
                appendLine("【关系一致性】")
                novel.relationshipStates.forEach { relationship ->
                    appendLine("- ${relationship.participantNames.joinToString(" & ")}：")
                    appendLine("  亲密度：${relationship.intimacyLevel}%")
                    appendLine("  当前阶段：${relationship.currentStage.displayName}")
                    appendLine("  权力动态：${relationship.powerDynamic.displayName}")
                }
                appendLine()
            }
            
            // 伏笔一致性检查
            appendLine("【伏笔一致性】")
            val unresolved = novel.getUnresolvedForeshadowings()
            if (unresolved.isNotEmpty()) {
                appendLine("未回收伏笔（需在本章或后续章节处理）：")
                unresolved.take(3).forEach { f ->
                    appendLine("- ${f.content}（第${f.plantedChapter}章）")
                }
            } else {
                appendLine("暂无未回收伏笔")
            }
            appendLine()
            
            // 场景记忆检查
            appendLine("【场景记忆】")
            val currentScene = novel.getCurrentSceneMemory()
            if (currentScene != null) {
                appendLine("当前场景：${currentScene.sceneName}")
                appendLine("参与者：${currentScene.participants.joinToString()}")
                appendLine("氛围：${currentScene.atmosphere}")
            } else {
                appendLine("新场景（需建立场景记忆）")
            }
        }
    }
    
    /**
     * 确定场景类型
     */
    private fun determineSceneType(
        sceneIndex: Int,
        totalScenes: Int,
        novel: Novel
    ): SceneType {
        val ratio = sceneIndex.toFloat() / totalScenes
        
        return when {
            ratio < 0.25 -> SceneType.DAILY
            ratio < 0.5 -> SceneType.AMBIGUOUS
            ratio < 0.75 -> SceneType.EMOTIONAL
            ratio < 1.0 -> {
                // 根据小说配置确定最后场景类型
                if (novel.sensoryProfile.tabooLevel.ordinal >= TabooLevel.DEEP.ordinal) {
                    SceneType.INTIMATE
                } else {
                    SceneType.PHYSICAL
                }
            }
            else -> SceneType.AFTERMATH
        }
    }
    
    /**
     * 获取情感状态
     */
    private fun getEmotionalState(
        novel: Novel,
        chapterNum: Int,
        isStart: Boolean
    ): String {
        val relationship = novel.relationshipStates.lastOrNull()
        
        return if (relationship != null) {
            if (isStart) {
                "基于当前亲密度${relationship.intimacyLevel}%的状态"
            } else {
                "比上一阶段更亲密的状态"
            }
        } else {
            if (isStart) "普通互动状态" else "情感有所升温的状态"
        }
    }
    
    /**
     * 获取情感弧线
     */
    private fun getEmotionalArc(novel: Novel, chapterNum: Int): String {
        val relationship = novel.relationshipStates.lastOrNull()
        
        return when {
            relationship == null -> "关系初步建立"
            relationship.intimacyLevel < 30 -> "暧昧升温"
            relationship.intimacyLevel < 60 -> "关系深入"
            relationship.intimacyLevel < 80 -> "亲密发展"
            else -> "关系稳定"
        }
    }
    
    /**
     * 构建用户Prompt
     */
    fun buildUserPrompt(
        novel: Novel,
        chapterNum: Int,
        stage: ChapterPhase,
        previousContent: String?,
        userGuide: String?
    ): String {
        return buildString {
            when (stage) {
                ChapterPhase.OUTLINE -> {
                    appendLine("请为第${chapterNum}章生成详细大纲。")
                    appendLine()
                    appendLine("【小说信息】")
                    appendLine("- 标题：《${novel.title}》")
                    appendLine("- 类型：${novel.type}")
                    appendLine("- 风格：${novel.style}")
                    appendLine()
                    userGuide?.let {
                        appendLine("【用户引导】$it")
                        appendLine()
                    }
                    appendLine("请生成包含开场、发展、高潮、收尾的完整大纲。")
                }
                
                ChapterPhase.FRAMEWORK -> {
                    appendLine("请基于以下大纲生成第${chapterNum}章的详细内容。")
                    appendLine()
                    appendLine("【写作要求】")
                    appendLine("- 字数：3000-5000字")
                    appendLine("- 视角：${novel.generationConfig.perspectiveMode.displayName}")
                    appendLine("- 节奏：${novel.generationConfig.rhythmPreference.description}")
                    appendLine()
                    appendLine("请开始创作。")
                }
                
                ChapterPhase.DETAIL_FILL -> {
                    appendLine("请在上一稿基础上，增加更多感官描写细节。")
                    appendLine()
                    appendLine("【感官侧重】")
                    novel.sensoryProfile.sensoryFocus.forEach { sensory ->
                        appendLine("- ${sensory.name}")
                    }
                    appendLine()
                    appendLine("请润色内容，增强沉浸感。")
                }
                
                ChapterPhase.EMOTION_POLISH -> {
                    appendLine("请润色情感描写，增强情感张力。")
                    appendLine()
                    appendLine("【情感表达风格】${novel.sensoryProfile.emotionExpressionStyle}")
                    appendLine()
                    appendLine("请优化情感表达方式。")
                }
                
                ChapterPhase.CONSISTENCY_CHECK -> {
                    appendLine("请检查并优化章节内容的一致性。")
                    appendLine()
                    appendLine("确保以下内容一致：")
                    appendLine("- 角色身体特征")
                    appendLine("- 角色关系状态")
                    appendLine("- 情节逻辑连贯")
                    appendLine("- 伏笔合理回收")
                    appendLine()
                    appendLine("请输出最终版本。")
                }
            }
        }
    }
}
