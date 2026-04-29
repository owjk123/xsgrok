package com.xsgrok.app.agent

import com.xsgrok.app.data.model.Character

/**
 * 深度角色心智系统
 * 让角色决策以"当前心理状态 + 核心动机"为首要驱动
 */

/** 心理状态 */
data class MentalState(
    val characterId: String,
    val emotionalBaseline: EmotionType = EmotionType.CALM,
    val currentEmotion: EmotionType = EmotionType.CALM,
    val emotionIntensity: Int = 5,  // 1-10
    val stress: Int = 0,           // 压力值 0-100
    val trust: Int = 50,           // 信任度 0-100
    val arousal: Int = 0,          // 激活度 0-100
    val dominantDesire: String = "",  // 当前主导欲望
    val suppressedDesire: String = "", // 被压抑的欲望
    val recentTrauma: String = "",     // 近期创伤
    val currentGoal: String = "",      // 当前目标
    val conflictSource: String = ""    // 内心冲突来源
)

enum class EmotionType(val displayName: String) {
    CALM("平静"),
    HAPPY("开心"),
    ANGRY("愤怒"),
    SAD("悲伤"),
    FEAR("恐惧"),
    SURPRISE("惊讶"),
    DISGUST("厌恶"),
    DESIRE("渴望"),
    GUILT("内疚"),
    SHAME("羞耻"),
    PRIDE("骄傲"),
    JEALOUSY("嫉妒"),
    LONELINESS("孤独"),
    HOPE("希望"),
    DESPAIR("绝望")
}

/** 角色核心动机 */
data class CoreMotivation(
    val characterId: String,
    val primaryDrive: String,     // 核心驱动力（如：生存、复仇、爱情、权力）
    val secondaryDrives: List<String> = emptyList(),  // 次要驱动力
    val fears: List<String> = emptyList(),             // 恐惧
    val values: List<String> = emptyList(),            // 价值观
    val boundaries: List<String> = emptyList(),        // 底线
    val triggerWords: List<String> = emptyList()       // 触发词（会强烈反应的关键词）
)

/** 角色决策记录 */
data class CharacterDecision(
    val id: String = System.currentTimeMillis().toString(),
    val characterId: String,
    val chapter: Int,
    val situation: String,       // 面临的情境
    val mentalState: MentalState, // 决策时的心理状态
    val motivation: String,      // 决策动机
    val decision: String,        // 做出的决策
    val consequence: String,     // 导致的后果
    val emotionAfter: EmotionType, // 决策后情绪
    val consistencyScore: Float = 1.0f  // 一致性评分 0-1
)

/** 角色心智管理器 */
class CharacterMindSystem {
    
    private val mentalStates = mutableMapOf<String, MentalState>()
    private val motivations = mutableMapOf<String, CoreMotivation>()
    private val decisionHistory = mutableListOf<CharacterDecision>()
    
    /** 初始化角色心智 */
    fun initializeCharacter(character: Character, novelType: String) {
        val charId = character.id
        
        mentalStates[charId] = MentalState(
            characterId = charId,
            currentGoal = when (character.role) {
                "主角" -> "推进主线目标"
                "反派" -> "阻碍主角"
                else -> "维护自身利益"
            }
        )
        
        motivations[charId] = CoreMotivation(
            characterId = charId,
            primaryDrive = inferPrimaryDrive(character),
            secondaryDrives = inferSecondaryDrives(character),
            fears = inferFears(character),
            values = inferValues(character, novelType),
            boundaries = inferBoundaries(character)
        )
    }
    
    /** 更新心理状态 */
    fun updateMentalState(characterId: String, event: String, emotionImpact: EmotionType, intensity: Int) {
        val current = mentalStates[characterId] ?: return
        mentalStates[characterId] = current.copy(
            currentEmotion = emotionImpact,
            emotionIntensity = minOf(10, current.emotionIntensity + intensity / 2),
            stress = minOf(100, current.stress + when (emotionImpact) {
                EmotionType.FEAR, EmotionType.ANGER, EmotionType.DESPAIR -> intensity * 5
                else -> 0
            }),
            recentTrauma = if (emotionImpact in listOf(EmotionType.FEAR, EmotionType.DESPAIR, EmotionType.SHAME)) {
                event.take(100)
            } else current.recentTrauma
        )
    }
    
    /** 记录决策 */
    fun recordDecision(decision: CharacterDecision) {
        decisionHistory.add(decision)
    }
    
    /** 检测OOC（角色崩坏） */
    fun detectOOC(characterId: String, proposedAction: String): OOCReport? {
        val motivation = motivations[characterId] ?: return null
        val mentalState = mentalStates[characterId] ?: return null
        
        // 检查是否违反底线
        val violatesBoundary = motivation.boundaries.any { boundary ->
            proposedAction.contains(boundary)
        }
        if (violatesBoundary) {
            return OOCReport(
                characterId = characterId,
                violationType = "底线违反",
                description = "该行为违反了角色底线",
                severity = 1.0f,
                correction = "角色绝不会${motivation.boundaries.first { proposedAction.contains(it) }}"
            )
        }
        
        // 检查是否与核心动机矛盾
        val contradictsDrive = proposedAction.contains("放弃") && motivation.primaryDrive.contains("复仇")
        if (contradictsDrive) {
            return OOCReport(
                characterId = characterId,
                violationType = "动机矛盾",
                description = "该行为与核心驱动力矛盾",
                severity = 0.8f,
                correction = "角色核心驱动是「${motivation.primaryDrive}」，不应轻易放弃"
            )
        }
        
        // 检查高压状态下的异常冷静
        if (mentalState.stress > 80 && proposedAction.contains("平静") && mentalState.currentEmotion == EmotionType.ANGER) {
            return OOCReport(
                characterId = characterId,
                violationType = "情绪不连贯",
                description = "角色在高压愤怒状态下不应突然平静",
                severity = 0.6f,
                correction = "角色当前压力值${mentalState.stress}，情绪为${mentalState.currentEmotion.displayName}，应表现出相应的情绪反应"
            )
        }
        
        return null
    }
    
    /** 生成角色状态Prompt */
    fun generateCharacterPrompt(characterId: String): String {
        val mentalState = mentalStates[characterId] ?: return ""
        val motivation = motivations[characterId] ?: return ""
        val recentDecisions = decisionHistory.filter { it.characterId == characterId }.takeLast(3)
        
        return buildString {
            appendLine("【角色心理状态】")
            appendLine("- 当前情绪：${mentalState.currentEmotion.displayName}（强度${mentalState.emotionIntensity}/10）")
            appendLine("- 压力值：${mentalState.stress}/100")
            appendLine("- 当前目标：${mentalState.currentGoal}")
            if (mentalState.dominantDesire.isNotBlank()) {
                appendLine("- 主导欲望：${mentalState.dominantDesire}")
            }
            if (mentalState.conflictSource.isNotBlank()) {
                appendLine("- 内心冲突：${mentalState.conflictSource}")
            }
            appendLine()
            appendLine("【核心驱动力】${motivation.primaryDrive}")
            if (motivation.fears.isNotEmpty()) {
                appendLine("【恐惧】${motivation.fears.joinToString("、")}")
            }
            if (motivation.boundaries.isNotEmpty()) {
                appendLine("【底线】${motivation.boundaries.joinToString("、")}（绝不可逾越）")
            }
            
            if (recentDecisions.isNotEmpty()) {
                appendLine()
                appendLine("【近期决策】")
                recentDecisions.forEach { d ->
                    appendLine("- 第${d.chapter}章：面对「${d.situation}」→ 选择「${d.decision}」→ 导致「${d.consequence}」")
                }
            }
        }
    }
    
    /** 导出/导入状态 */
    fun exportState(): CharacterMindState {
        return CharacterMindState(
            mentalStates = mentalStates.toMap(),
            motivations = motivations.toMap(),
            decisionHistory = decisionHistory.toList()
        )
    }
    
    fun importState(state: CharacterMindState) {
        mentalStates.clear()
        mentalStates.putAll(state.mentalStates)
        motivations.clear()
        motivations.putAll(state.motivations)
        decisionHistory.clear()
        decisionHistory.addAll(state.decisionHistory)
    }
    
    // 辅助推断方法
    private fun inferPrimaryDrive(character: Character): String {
        val desc = character.description + character.personality + character.background
        return when {
            desc.contains("复仇") || desc.contains("报仇") -> "复仇"
            desc.contains("权力") || desc.contains("统治") -> "权力"
            desc.contains("守护") || desc.contains("保护") -> "守护"
            desc.contains("自由") || desc.contains("逃脱") -> "自由"
            desc.contains("真相") || desc.contains("谜") -> "探寻真相"
            desc.contains("爱情") || desc.contains("爱") -> "爱情"
            else -> "生存与成长"
        }
    }
    
    private fun inferSecondaryDrives(character: Character): List<String> {
        val drives = mutableListOf<String>()
        val desc = character.description + character.personality
        if (desc.contains("友谊")) drives.add("友谊")
        if (desc.contains("荣誉")) drives.add("荣誉")
        if (desc.contains("知识")) drives.add("知识")
        if (desc.contains("财富")) drives.add("财富")
        return drives.ifEmpty { listOf("自我认同") }
    }
    
    private fun inferFears(character: Character): List<String> {
        val fears = mutableListOf<String>()
        val desc = character.description + character.background
        if (desc.contains("失去")) fears.add("失去重要之人")
        if (desc.contains("背叛")) fears.add("被背叛")
        if (desc.contains("失败")) fears.add("失败")
        return fears.ifEmpty { listOf("无能为力") }
    }
    
    private fun inferValues(character: Character, novelType: String): List<String> {
        return when (novelType) {
            "玄幻", "仙侠" -> listOf("实力", "道心", "传承")
            "都市" -> listOf("尊严", "自由", "真情")
            "科幻" -> listOf("理性", "真相", "生存")
            else -> listOf("正义", "勇气", "信念")
        }
    }
    
    private fun inferBoundaries(character: Character): List<String> {
        val bounds = mutableListOf<String>()
        val desc = character.description + character.personality
        if (desc.contains("正义") || desc.contains("善良")) bounds.add("伤害无辜")
        if (desc.contains("忠诚")) bounds.add("背叛同伴")
        if (desc.contains("骄傲")) bounds.add("屈膝求饶")
        return bounds.ifEmpty { listOf("放弃底线") }
    }
}

data class OOCReport(
    val characterId: String,
    val violationType: String,
    val description: String,
    val severity: Float,
    val correction: String
)

data class CharacterMindState(
    val mentalStates: Map<String, MentalState>,
    val motivations: Map<String, CoreMotivation>,
    val decisionHistory: List<CharacterDecision>
)
