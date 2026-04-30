package com.xsgrok.app.data.model

import com.google.gson.Gson

/**
 * 精简后的数据模型 - 第一性原理优化
 * 核心原则：只保留对小说生成真正必要的数据结构
 */

// ========== 核心数据模型 ==========

/**
 * 小说主体 - 精简版
 * 只保留生成真正需要的字段
 */
data class Novel(
    val id: String = System.currentTimeMillis().toString(),
    val title: String,
    val genre: String = "",
    val style: String = "",
    val mainCharacter: String = "",
    val outline: String = "",
    val globalSummary: String = "",
    val chapters: MutableList<Chapter> = mutableListOf(),
    val characters: MutableList<Character> = mutableListOf(),
    val worldBuilding: WorldBuilding = WorldBuilding(),
    val keyNodes: MutableList<KeyNode> = mutableListOf(),
    val foreshadowings: MutableList<Foreshadowing> = mutableListOf(),
    val currentNodeIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toJson(): String = Gson().toJson(this)
    
    companion object {
        fun fromJson(json: String): Novel = Gson().fromJson(json, Novel::class.java)
    }
    
    fun chapterCount(): Int = chapters.size
    
    fun getLastChapterEnding(): String {
        return chapters.lastOrNull()?.content?.takeLast(500) ?: ""
    }
    
    fun getRecentSummaries(count: Int = 3): String {
        return chapters.takeLast(count).joinToString("\n\n") { chapter ->
            "第${chapter.order}章《${chapter.title}》：${chapter.summary}"
        }
    }
    
    fun getProgressHint(currentChapter: Int): String {
        return if (keyNodes.isNotEmpty()) {
            val currentNode = keyNodes.getOrNull(currentNodeIndex)
            "【第${currentChapter}章】${currentNode?.title ?: "进行中"}"
        } else {
            "【第${currentChapter}章】"
        }
    }
    
    fun getUnresolvedForeshadowings(): List<Foreshadowing> {
        return foreshadowings.filter { !it.isResolved }
    }
}

/**
 * 章节
 */
data class Chapter(
    val id: String = System.currentTimeMillis().toString(),
    val title: String,
    val content: String,
    val order: Int,
    val summary: String = "",
    val wordCount: Int = content.length,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 角色
 */
data class Character(
    val id: String = System.currentTimeMillis().toString(),
    val name: String,
    val description: String,
    val role: String = "配角"
)

/**
 * 世界观设定 - 精简版
 */
data class WorldBuilding(
    val worldBackground: String = "",
    val powerSystem: String = "",
    val rules: String = ""
)

/**
 * 关键节点
 */
data class KeyNode(
    val id: String = System.currentTimeMillis().toString(),
    val title: String,
    val description: String = "",
    val targetChapter: Int = 0,
    val isCompleted: Boolean = false
)

/**
 * 伏笔
 */
data class Foreshadowing(
    val id: String = System.currentTimeMillis().toString(),
    val content: String,
    val plantedChapter: Int,
    val isResolved: Boolean = false,
    val resolvedChapter: Int? = null
)

/**
 * 伏笔统计
 */
data class ForeshadowingStats(
    val total: Int,
    val unresolved: Int,
    val resolved: Int,
    val resolutionRate: Float
)

// ========== 生成配置 ==========

/**
 * 生成预设 - 精简版
 */
data class GenerationPreset(
    val id: String,
    val name: String,
    val description: String,
    val temperature: Float = 0.75f,
    val maxTokens: Int = 8192,
    val styleHint: String = ""
)

/**
 * 预置生成模式
 */
object GenerationPresets {
    val FAST = GenerationPreset(
        id = "fast",
        name = "快速模式",
        description = "快速生成，注重情节推进",
        temperature = 0.8f,
        styleHint = "节奏紧凑，情节推进快，对话简洁"
    )
    
    val BALANCED = GenerationPreset(
        id = "balanced",
        name = "平衡模式",
        description = "情节与描写平衡",
        temperature = 0.75f,
        styleHint = "叙事平衡，描写与对话兼顾"
    )
    
    val DETAILED = GenerationPreset(
        id = "detailed",
        name = "细腻模式",
        description = "注重细节和情感描写",
        temperature = 0.7f,
        styleHint = "描写细腻，情感丰富，环境渲染充分"
    )
    
    val CREATIVE = GenerationPreset(
        id = "creative",
        name = "创意模式",
        description = "高创意，注重情节创新",
        temperature = 0.9f,
        styleHint = "情节新颖，创意丰富，可能有意外转折"
    )
    
    fun getAll(): List<GenerationPreset> = listOf(FAST, BALANCED, DETAILED, CREATIVE)
    
    fun getById(id: String): GenerationPreset {
        return getAll().find { it.id == id } ?: BALANCED
    }
}

// ========== API配置 ==========

data class ApiConfig(
    val apiKey: String = "",
    val endpoint: String = "https://api.edgefn.net/v1",
    val model: String = "GLM-5.1",
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

// ========== 简化版生成状态 ==========

enum class AutoModeState {
    IDLE,
    GENERATING,
    REVIEW,
    COMPLETED
}
