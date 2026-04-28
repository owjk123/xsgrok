package com.xsgrok.app.data.model

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

data class Novel(
    val id: String = System.currentTimeMillis().toString(),
    val title: String,
    val type: String,
    val style: String,
    val mainCharacter: String,
    val outline: String = "",
    val characters: MutableList<Character> = mutableListOf(),
    val chapters: MutableList<Chapter> = mutableListOf(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toJson(): String = Gson().toJson(this)
    
    companion object {
        fun fromJson(json: String): Novel = Gson().fromJson(json, Novel::class.java)
    }
}

data class Character(
    val id: String = System.currentTimeMillis().toString(),
    val name: String,
    val description: String,
    val role: String
)

data class Chapter(
    val id: String = System.currentTimeMillis().toString(),
    val title: String,
    val content: String,
    val order: Int,
    val createdAt: Long = System.currentTimeMillis()
)

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
