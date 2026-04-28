package com.xsgrok.app.data.model

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

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
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toJson(): String = Gson().toJson(this)
    
    companion object {
        fun fromJson(json: String): Novel = Gson().fromJson(json, Novel::class.java)
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
    val chapter: Int = -1
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
    val stream: Boolean = true
)

data class ChatResponse(
    val id: String?,
    @SerializedName("choices")
    val choices: List<Choice>?
)

data class Choice(
    @SerializedName("delta")
    val delta: Delta?
)

data class Delta(
    val content: String?
)
