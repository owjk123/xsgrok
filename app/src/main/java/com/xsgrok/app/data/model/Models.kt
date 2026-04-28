package com.xsgrok.app.data.model

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

// ========== 新增P0功能数据模型 ==========

/**
 * 关键节点 - 用于主线进度追踪
 * 将故事拆分为8~12个关键节点，每章注入当前位置和剩余距离
 */
data class KeyNode(
    val id: String = System.currentTimeMillis().toString(),
    val title: String,              // 节点标题
    val description: String,       // 节点描述
    val targetChapter: Int = 0,     // 目标章节
    val isCompleted: Boolean = false,
    val completedChapter: Int? = null
)

/**
 * 伏笔 - 用于伏笔强制回收机制
 * 每章上下文携带未回收伏笔列表，进度超80%进入收束模式
 */
data class Foreshadowing(
    val id: String = System.currentTimeMillis().toString(),
    val content: String,           // 伏笔内容
    val plantedChapter: Int,       // 埋下伏笔的章节
    val isResolved: Boolean = false,
    val resolvedChapter: Int? = null,
    val hint: String = ""          // 回收提示（可选）
)

/**
 * 伏笔状态统计
 */
data class ForeshadowingStats(
    val total: Int,
    val unresolved: Int,
    val resolved: Int,
    val resolutionRate: Float
)

/**
 * 进度信息 - 用于UI显示和模型提示
 */
data class ProgressInfo(
    val currentChapter: Int,
    val totalChapters: Int,
    val currentNodeIndex: Int,
    val totalNodes: Int,
    val nodeTitle: String,
    val nextNodeTitle: String?,
    val remainingToNode: Int,
    val overallProgress: Float  // 0.0 ~ 1.0
) {
    fun toModelHint(): String {
        return if (remainingToNode > 0) {
            "【进度：第${currentChapter}章 | 节点${currentNodeIndex}/${totalNodes}「${nodeTitle}」| 距下一节点：${remainingToNode}章】"
        } else {
            "【进度：第${currentChapter}章 | 节点${currentNodeIndex}/${totalNodes}「${nodeTitle}」已完成 | 下一节点：${nextNodeTitle ?: "无"}】"
        }
    }
}

// ========== 原有数据模型 ==========

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
    // ========== P0新增字段 ==========
    val keyNodes: MutableList<KeyNode> = mutableListOf(),      // 关键节点列表
    val foreshadowings: MutableList<Foreshadowing> = mutableListOf(),  // 伏笔列表
    val wordCountGoal: Int = 50000,                            // 目标字数
    val currentNodeIndex: Int = 0,                             // 当前节点索引
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toJson(): String = Gson().toJson(this)
    
    companion object {
        fun fromJson(json: String): Novel = Gson().fromJson(json, Novel::class.java)
    }
    
    /**
     * 获取当前进度信息
     */
    fun getProgressInfo(currentChapter: Int): ProgressInfo {
        val currentNode = keyNodes.getOrNull(currentNodeIndex)
        val nextNode = keyNodes.getOrNull(currentNodeIndex + 1)
        
        val remainingToNode = if (currentNode != null) {
            maxOf(0, currentNode.targetChapter - currentChapter)
        } else 0
        
        return ProgressInfo(
            currentChapter = currentChapter,
            totalChapters = chapters.size,
            currentNodeIndex = currentNodeIndex,
            totalNodes = keyNodes.size,
            nodeTitle = currentNode?.title ?: "开场",
            nextNodeTitle = nextNode?.title,
            remainingToNode = remainingToNode,
            overallProgress = if (keyNodes.isNotEmpty()) currentNodeIndex.toFloat() / keyNodes.size else 0f
        )
    }
    
    /**
     * 获取未回收伏笔列表
     */
    fun getUnresolvedForeshadowings(): List<Foreshadowing> {
        return foreshadowings.filter { !it.isResolved }
    }
    
    /**
     * 获取伏笔统计
     */
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
    
    /**
     * 是否进入收束模式（进度超80%）
     */
    fun isConvergenceMode(): Boolean {
        return if (keyNodes.isNotEmpty()) {
            currentNodeIndex.toFloat() / keyNodes.size > 0.8f
        } else {
            chapters.size > 5 && (chapters.size - currentNodeIndex) <= 2
        }
    }
}

// 角色管理
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
    val worldBackground: String = "",           // 世界背景
    val powerSystem: String = "",               // 力量体系
    val geography: MutableList<Location> = mutableListOf(),  // 地理/场景
    val factions: MutableList<Faction> = mutableListOf(),    // 势力/组织
    val items: MutableList<GameItem> = mutableListOf(),      // 物品/装备
    val skills: MutableList<Skill> = mutableListOf(),        // 技能/功法
    val timeline: MutableList<TimelineEvent> = mutableListOf(), // 时间线事件
    val rules: String = ""                       // 世界规则
)

// 地点/场景
data class Location(
    val id: String = System.currentTimeMillis().toString(),
    val name: String,
    val description: String,
    val type: String = "",
    val significance: String = ""
)

// 势力/组织
data class Faction(
    val id: String = System.currentTimeMillis().toString(),
    val name: String,
    val description: String,
    val leader: String = "",
    val goals: String = "",
    val relationships: String = ""
)

// 物品/装备
data class GameItem(
    val id: String = System.currentTimeMillis().toString(),
    val name: String,
    val description: String,
    val type: String = "",
    val abilities: String = "",
    val origin: String = ""
)

// 技能/功法
data class Skill(
    val id: String = System.currentTimeMillis().toString(),
    val name: String,
    val description: String,
    val type: String = "",
    val requirements: String = "",
    val effects: String = ""
)

// 时间线事件
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

// API请求相关
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
