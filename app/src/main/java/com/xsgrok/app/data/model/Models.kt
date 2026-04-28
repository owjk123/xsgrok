package com.xsgrok.app.data.model

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

// ========== P0新增功能数据模型 ==========

/**
 * 感官描写配置 - 控制不同场景下的描写强度和侧重
 */
data class SensoryProfile(
    val descriptionDensity: Int = 5,
    val sensoryFocus: List<SensoryType> = listOf(
        SensoryType.VISUAL,
        SensoryType.TACTILE,
        SensoryType.AUDITORY
    ),
    val tabooLevel: TabooLevel = TabooLevel.MODERATE,
    val intimateSceneIntensity: Int = 5,
    val emotionExpressionStyle: EmotionStyle = EmotionStyle.IMPLICIT
)

enum class SensoryType {
    VISUAL, TACTILE, AUDITORY, OLFACTORY, TASTE, PROPRIOCEPTION
}

enum class TabooLevel(val displayName: String, val description: String) {
    LIGHT("轻度", "温馨日常向，含蓄暗示"),
    MODERATE("中度", "情感细腻，适度感官描写"),
    DEEP("深度", "沉浸式体验，丰富细节"),
    UNRESTRICTED("无限制", "完整表达，无限制")
}

enum class EmotionStyle {
    IMPLICIT, MODERATE, EXPLICIT, RAW
}

/**
 * 角色身体档案
 */
data class CharacterBodyProfile(
    val characterId: String,
    val characterName: String,
    val bodyFeatures: BodyFeatures = BodyFeatures(),
    val sensitivePoints: MutableList<SensitivePoint> = mutableListOf(),
    val preferences: BodyPreferences = BodyPreferences(),
    val boundaries: MutableList<String> = mutableListOf(),
    val currentState: CharacterState = CharacterState()
)

data class BodyFeatures(
    val height: String = "",
    val build: String = "",
    val skinTone: String = "",
    val hairColor: String = "",
    val eyeColor: String = "",
    val distinctiveMarks: String = "",
    val voiceDescription: String = "",
    val scent: String = ""
)

data class SensitivePoint(
    val id: String = System.currentTimeMillis().toString(),
    val name: String,
    val location: String,
    val triggerType: SensitiveTriggerType,
    val responseDescription: String,
    val intensityLevel: Int = 5
)

enum class SensitiveTriggerType {
    LIGHT_TOUCH, DEEP_PRESSURE, TEMPERATURE, VIBRATION, TEXTURE, BREATH, SOUND
}

data class BodyPreferences(
    val temperaturePreference: String = "",
    val pressurePreference: String = "",
    val environmentPreference: String = "",
    val rhythmPreference: String = "",
    val durationPreference: String = ""
)

data class CharacterState(
    val physicalState: String = "正常",
    val emotionalState: String = "平静",
    val energyLevel: Int = 5,
    val arousalLevel: Int = 0,
    val clothingState: String = "完整",
    val position: String = "",
    val lastTouchedArea: String = "",
    val lastInteractionTime: Long = 0
)

/**
 * 关系状态
 */
data class RelationshipState(
    val relationshipId: String = System.currentTimeMillis().toString(),
    val participants: List<String>,
    val participantNames: List<String>,
    val intimacyLevel: Int = 0,
    val trustLevel: Int = 0,
    val powerDynamic: PowerDynamic = PowerDynamic.EQUAL,
    val relationshipType: RelationshipType = RelationshipType.FRIENDLY,
    val history: MutableList<RelationshipEvent> = mutableListOf(),
    val currentStage: RelationshipStage = RelationshipStage.INITIAL,
    val tags: MutableList<String> = mutableListOf(),
    val lastInteractionTime: Long = 0
)

enum class PowerDynamic(val displayName: String) {
    DOMINANT("主导"), SUBMISSIVE("顺从"), EQUAL("平等"), FLUID("流动")
}

enum class RelationshipType {
    ROMANTIC, FRIENDLY, PROFESSIONAL, FAMILIAL, ANTAGONISTIC, COMPLEX
}

enum class RelationshipStage(val displayName: String, val intimacyRange: IntRange) {
    INITIAL("初识", 0..10),
    ACQUAINTANCE("相识", 11..25),
    FAMILIAR("熟悉", 26..40),
    CLOSE("亲密", 41..60),
    DEEP("深入", 61..80),
    INTIMATE("私密", 81..95),
    BONDED("绑定", 96..100)
}

data class RelationshipEvent(
    val id: String = System.currentTimeMillis().toString(),
    val type: RelationshipEventType,
    val description: String,
    val chapter: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val intimacyChange: Int = 0,
    val trustChange: Int = 0,
    val emotionalImpact: String = ""
)

enum class RelationshipEventType(val intimacyImpact: Int, val trustImpact: Int) {
    FIRST_MEETING(5, 5), CONVERSATION(1, 1), HELP(3, 5), CONFLICT(-5, -3),
    APOLOGY(2, 3), GIFT(3, 3), TOUCH(5, 2), KISS(10, 5), INTIMATE(15, 5),
    CONFESSION(10, 10), REJECTION(-10, -5), REUNION(8, 8), BETRAYAL(-15, -20),
    FORGIVENESS(5, 10)
}

/**
 * 场景记忆
 */
data class SceneMemory(
    val sceneId: String = System.currentTimeMillis().toString(),
    val sceneName: String,
    val sceneDescription: String,
    val location: String,
    val chapter: Int,
    val triggeredEvents: MutableList<SceneEvent> = mutableListOf(),
    val bodyStateChanges: MutableList<BodyStateChange> = mutableListOf(),
    val emotionalTrajectory: MutableList<EmotionalMoment> = mutableListOf(),
    val environmentalDetails: MutableList<String> = mutableListOf(),
    val participants: List<String> = emptyList(),
    val atmosphere: String = "",
    val lighting: String = "",
    val soundscape: String = ""
)

data class SceneEvent(
    val id: String = System.currentTimeMillis().toString(),
    val type: SceneEventType,
    val description: String,
    val timestamp: Long = System.currentTimeMillis(),
    val participants: List<String> = emptyList(),
    val emotionalTags: MutableList<String> = mutableListOf()
)

enum class SceneEventType {
    MEETING, CONVERSATION, TOUCH, INTIMATE, CONFLICT, RESOLUTION,
    REVELATION, DECISION, TRANSITION
}

data class BodyStateChange(
    val id: String = System.currentTimeMillis().toString(),
    val characterId: String,
    val characterName: String,
    val changeType: BodyChangeType,
    val beforeState: String,
    val afterState: String,
    val description: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class BodyChangeType {
    CLOTHING_CHANGE, POSITION_CHANGE, SENSORY_RESPONSE, PHYSICAL_REACTION,
    TEMPERATURE_CHANGE, TENSION_LEVEL, AROUSAL_CHANGE
}

data class EmotionalMoment(
    val id: String = System.currentTimeMillis().toString(),
    val characterId: String,
    val characterName: String,
    val emotion: String,
    val intensity: Int = 5,
    val trigger: String,
    val expression: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Prompt模板
 */
data class PromptTemplate(
    val templateId: String = System.currentTimeMillis().toString(),
    val name: String,
    val level: TabooLevel,
    val basePrompt: String,
    val scenePrompts: MutableMap<SceneType, String> = mutableMapOf(),
    val instructionLibrary: InstructionLibrary = InstructionLibrary()
)

enum class SceneType {
    DAILY, AMBIGUOUS, EMOTIONAL, PHYSICAL, INTIMATE, CLIMAX,
    AFTERMATH, TENSION, CONFLICT, RESOLUTION
}

data class InstructionLibrary(
    val visualDescriptions: MutableList<String> = mutableListOf(
        "光线如何投射在皮肤上", "面部微表情的细微变化", "身体轮廓的剪影效果",
        "衣物褶皱与光影", "呼吸起伏的节奏", "眼神交汇时的光芒变化",
        "皮肤颜色的细微变化", "嘴唇颤抖的细节", "手指关节的弯曲弧度"
    ),
    val tactileDescriptions: MutableList<String> = mutableListOf(
        "指尖触碰的温度差异", "掌心传来的脉搏跳动", "皮肤相贴时的电流感",
        "不同力度下的触感变化", "材质接触的细腻差别", "压力传递的层次感",
        "温度传递的速度", "震动传导的频率"
    ),
    val psychologicalDescriptions: MutableList<String> = mutableListOf(
        "内心独白的层次递进", "情绪波动的心电图式描写", "潜意识流动的隐喻",
        "记忆闪回的触发机制", "欲望与克制的拉锯", "安全感的来源追溯",
        "脆弱暴露时的防御机制", "渴望与恐惧的交织"
    ),
    val auditoryDescriptions: MutableList<String> = mutableListOf(
        "呼吸声的变化层次", "心跳加速的听觉感受", "低语的呢喃质感",
        "沉默中的暗流涌动", "环境音的烘托作用", "声音颤抖的频率",
        "呼吸交错的节奏", "声带振动的共鸣"
    ),
    val olfactoryDescriptions: MutableList<String> = mutableListOf(
        "体香的微妙变化", "气息的温度质感", "香水与体味的交融",
        "情绪影响下的气味差异", "亲近时的嗅觉记忆"
    ),
    val tasteDescriptions: MutableList<String> = mutableListOf(
        "吻的味道层次", "唇齿间的温度传递", "唾液交换的质感",
        "呼吸中的气息味道", "亲密距离的味觉体验"
    )
)

/**
 * 生成配置
 */
data class GenerationConfig(
    val rhythmPreference: RhythmPreference = RhythmPreference.BALANCED,
    val perspectiveMode: PerspectiveMode = PerspectiveMode.THIRD_PERSON,
    val intensityLevel: Int = 5,
    val pacingConfig: PacingConfig = PacingConfig(),
    val coherenceWeight: Float = 0.7f,
    val creativityWeight: Float = 0.3f,
    val autoEnhanceIntimate: Boolean = true,
    val maintainConsistency: Boolean = true,
    val checkBodyConsistency: Boolean = true,
    val checkRelationshipConsistency: Boolean = true
)

enum class RhythmPreference(val displayName: String, val description: String) {
    SLOW_BURN("慢热型", "循序渐进，积累情感"),
    BALANCED("平衡型", "张弛有度，节奏适中"),
    FAST_PACED("快节奏", "快速推进，情节紧凑")
}

enum class PerspectiveMode(val displayName: String) {
    FIRST_PERSON("第一人称"),
    THIRD_PERSON("第三人称"),
    OMNISCIENT("全知视角")
}

data class PacingConfig(
    val slowBuildupRatio: Float = 0.4f,
    val progressionRatio: Float = 0.3f,
    val climaxRatio: Float = 0.15f,
    val aftermathRatio: Float = 0.15f,
    val tensionPoints: Int = 3,
    val releasePoints: Int = 2
)

enum class ChapterPhase {
    OUTLINE, FRAMEWORK, DETAIL_FILL, EMOTION_POLISH, CONSISTENCY_CHECK
}

enum class RhythmPhase(
    val displayName: String,
    val description: String,
    val targetWordRatio: Float
) {
    SLOW_BUILDUP("慢烧阶段", "积累情感，建立张力", 0.4f),
    PROGRESSION("推进阶段", "逐渐升温，层层递进", 0.3f),
    CLIMAX("爆发阶段", "情感高潮，顶点体验", 0.15f),
    AFTERMATH("余韵阶段", "回味悠长，情感沉淀", 0.15f)
}

// ========== 原有数据模型 ==========

data class KeyNode(
    val id: String = System.currentTimeMillis().toString(),
    val title: String,
    val description: String,
    val targetChapter: Int = 0,
    val isCompleted: Boolean = false,
    val completedChapter: Int? = null
)

data class Foreshadowing(
    val id: String = System.currentTimeMillis().toString(),
    val content: String,
    val plantedChapter: Int,
    val isResolved: Boolean = false,
    val resolvedChapter: Int? = null,
    val hint: String = ""
)

data class ForeshadowingStats(
    val total: Int,
    val unresolved: Int,
    val resolved: Int,
    val resolutionRate: Float
)

data class ProgressInfo(
    val currentChapter: Int,
    val totalChapters: Int,
    val currentNodeIndex: Int,
    val totalNodes: Int,
    val nodeTitle: String,
    val nextNodeTitle: String?,
    val remainingToNode: Int,
    val overallProgress: Float,
    val emotionalProgress: Float = 0f,
    val relationshipProgress: Float = 0f,
    val sensoryIntensity: Int = 5
) {
    fun toModelHint(): String {
        return if (remainingToNode > 0) {
            "【进度：第${currentChapter}章 | 节点${currentNodeIndex}/${totalNodes}「${nodeTitle}」| 距下一节点：${remainingToNode}章】"
        } else {
            "【进度：第${currentChapter}章 | 节点${currentNodeIndex}/${totalNodes}「${nodeTitle}」已完成 | 下一节点：${nextNodeTitle ?: "无"}】"
        }
    }
    
    fun toSensoryHint(): String {
        return "【感官描写：强度${sensoryIntensity}/10 | 情感进度：${(emotionalProgress * 100).toInt()}%】"
    }
}

// 小说主体
data class Novel(
    val id: String = System.currentTimeMillis().toString(),
    val title: String,
    val type: String,
    val style: String,
    val mainCharacter: String,
    val outline: String = "",
    val characters: MutableList<Character> = mutableListOf(),
    val chapters: MutableList<Chapter> = mutableListOf(),
    val worldBuilding: WorldBuilding = WorldBuilding(),
    val keyNodes: MutableList<KeyNode> = mutableListOf(),
    val foreshadowings: MutableList<Foreshadowing> = mutableListOf(),
    val wordCountGoal: Int = 50000,
    val currentNodeIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    // P0新增字段
    val sensoryProfile: SensoryProfile = SensoryProfile(),
    val characterBodyProfiles: MutableList<CharacterBodyProfile> = mutableListOf(),
    val relationshipStates: MutableList<RelationshipState> = mutableListOf(),
    val sceneMemories: MutableList<SceneMemory> = mutableListOf(),
    val promptTemplate: PromptTemplate = PromptTemplate(
        name = "默认模板",
        level = TabooLevel.MODERATE,
        basePrompt = ""
    ),
    val generationConfig: GenerationConfig = GenerationConfig(),
    val currentSceneId: String? = null,
    val intimacyProgress: Float = 0f
) {
    fun toJson(): String = Gson().toJson(this)
    
    companion object {
        fun fromJson(json: String): Novel = Gson().fromJson(json, Novel::class.java)
    }
    
    fun getProgressInfo(currentChapter: Int): ProgressInfo {
        val currentNode = keyNodes.getOrNull(currentNodeIndex)
        val nextNode = keyNodes.getOrNull(currentNodeIndex + 1)
        val remainingToNode = if (currentNode != null) {
            maxOf(0, currentNode.targetChapter - currentChapter)
        } else 0
        val emotionalProgress = if (relationshipStates.isNotEmpty()) {
            relationshipStates.maxOfOrNull { it.intimacyLevel }?.toFloat()?.div(100f) ?: 0f
        } else 0f
        val relationshipProgress = if (relationshipStates.isNotEmpty()) {
            relationshipStates.map { it.intimacyLevel }.average().toFloat() / 100f
        } else 0f
        return ProgressInfo(
            currentChapter = currentChapter,
            totalChapters = chapters.size,
            currentNodeIndex = currentNodeIndex,
            totalNodes = keyNodes.size,
            nodeTitle = currentNode?.title ?: "开场",
            nextNodeTitle = nextNode?.title,
            remainingToNode = remainingToNode,
            overallProgress = if (keyNodes.isNotEmpty()) currentNodeIndex.toFloat() / keyNodes.size else 0f,
            emotionalProgress = emotionalProgress,
            relationshipProgress = relationshipProgress,
            sensoryIntensity = sensoryProfile.descriptionDensity
        )
    }
    
    fun getUnresolvedForeshadowings(): List<Foreshadowing> {
        return foreshadowings.filter { !it.isResolved }
    }
    
    fun getForeshadowingStats(): ForeshadowingStats {
        val total = foreshadowings.size
        val resolved = foreshadowings.count { it.isResolved }
        val unresolved = total - resolved
        return ForeshadowingStats(
            total = total,
            unresolved = unresolved,
            resolved = resolved,
            resolutionRate = if (total > 0) resolved.toFloat() / total else 0f
        )
    }
    
    fun isConvergenceMode(): Boolean {
        return if (keyNodes.isNotEmpty()) {
            currentNodeIndex.toFloat() / keyNodes.size > 0.8f
        } else {
            chapters.size > 5 && (chapters.size - currentNodeIndex) <= 2
        }
    }
    
    fun getCharacterBodyProfile(characterId: String): CharacterBodyProfile? {
        return characterBodyProfiles.find { it.characterId == characterId }
    }
    
    fun getRelationship(characterId1: String, characterId2: String): RelationshipState? {
        return relationshipStates.find { 
            it.participants.contains(characterId1) && it.participants.contains(characterId2) 
        }
    }
    
    fun getCurrentSceneMemory(): SceneMemory? {
        return currentSceneId?.let { sceneId ->
            sceneMemories.find { it.sceneId == sceneId }
        }
    }
    
    fun getCurrentSceneRelationships(): List<RelationshipState> {
        val currentScene = getCurrentSceneMemory() ?: return emptyList()
        return relationshipStates.filter { relationship ->
            relationship.participants.any { it in currentScene.participants }
        }
    }
    
    fun getIntimateKeywords(): List<String> {
        return listOf(
            "拥抱", "亲吻", "牵手", "抚摸", "靠近", "依偎",
            "暧昧", "心动", "亲密", "肌肤", "温度", "呼吸",
            "心跳", "目光", "嘴唇", "触碰", "相拥", "缠绵"
        )
    }
}

// 原有角色管理
data class Character(
    val id: String = System.currentTimeMillis().toString(),
    val name: String,
    val description: String,
    val role: String,
    val appearance: String = "",
    val personality: String = "",
    val background: String = "",
    val abilities: String = "",
    val relationships: String = ""
)

// 章节
data class Chapter(
    val id: String = System.currentTimeMillis().toString(),
    val title: String,
    val content: String,
    val order: Int,
    val wordCount: Int = content.length,
    val createdAt: Long = System.currentTimeMillis()
)

// 世界观设定
data class WorldBuilding(
    val worldBackground: String = "",
    val powerSystem: String = "",
    val geography: MutableList<Location> = mutableListOf(),
    val factions: MutableList<Faction> = mutableListOf(),
    val items: MutableList<GameItem> = mutableListOf(),
    val skills: MutableList<Skill> = mutableListOf(),
    val timeline: MutableList<TimelineEvent> = mutableListOf(),
    val rules: String = ""
)

data class Location(
    val id: String = System.currentTimeMillis().toString(),
    val name: String,
    val description: String,
    val type: String = "",
    val significance: String = ""
)

data class Faction(
    val id: String = System.currentTimeMillis().toString(),
    val name: String,
    val description: String,
    val leader: String = "",
    val goals: String = "",
    val relationships: String = ""
)

data class GameItem(
    val id: String = System.currentTimeMillis().toString(),
    val name: String,
    val description: String,
    val type: String = "",
    val abilities: String = "",
    val origin: String = ""
)

data class Skill(
    val id: String = System.currentTimeMillis().toString(),
    val name: String,
    val description: String,
    val type: String = "",
    val requirements: String = "",
    val effects: String = ""
)

data class TimelineEvent(
    val id: String = System.currentTimeMillis().toString(),
    val title: String,
    val description: String,
    val time: String = "",
    val significance: String = ""
)

// API配置
data class ApiConfig(
    val apiKey: String = "",
    val endpoint: String = "https://api.apiyi.com/v1",
    val model: String = "grok-4.20-beta",
    val isDarkMode: Boolean = false
)

data class ChatMessage(
    val role: String,
    val content: String
)

data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val stream: Boolean = true,
    val temperature: Float = 0.75f,
    val max_tokens: Int = 8192
)
