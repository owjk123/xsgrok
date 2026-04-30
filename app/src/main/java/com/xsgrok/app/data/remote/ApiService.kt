package com.xsgrok.app.data.remote

import com.google.gson.Gson
import com.xsgrok.app.data.model.ChatMessage
import com.xsgrok.app.data.model.ChatRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class ApiService {
    private val gson = Gson()
    
    /**
     * 生成内容 - 支持动态temperature参数
     */
    fun generateContent(
        apiKey: String,
        endpoint: String,
        model: String,
        systemPrompt: String,
        userPrompt: String,
        temperature: Float = 0.75f
    ): Flow<String> = flow {
        var connection: HttpURLConnection? = null
        try {
            val url = URL("$endpoint/chat/completions")
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Authorization", "Bearer $apiKey")
            connection.doOutput = true
            connection.connectTimeout = 60 * 1000
            connection.readTimeout = 180 * 1000  // 增加超时时间
            
            val requestBody = ChatRequest(
                model = model,
                messages = listOf(
                    ChatMessage(role = "system", content = systemPrompt),
                    ChatMessage(role = "user", content = userPrompt)
                ),
                stream = true,
                temperature = temperature
            )
            
            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(gson.toJson(requestBody))
                writer.flush()
            }
            
            // 检查HTTP响应码
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                val errorStream = connection.errorStream
                val errorBody = if (errorStream != null) {
                    BufferedReader(InputStreamReader(errorStream)).use { it.readText() }
                } else {
                    "HTTP $responseCode"
                }
                emit("[ERROR] API请求失败($responseCode): $errorBody")
                return@flow
            }
            
            BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    line?.let {
                        if (it.startsWith("data: ")) {
                            val data = it.removePrefix("data: ")
                            if (data != "[DONE]") {
                                try {
                                    val response = gson.fromJson(data, StreamResponse::class.java)
                                    val content = response.choices?.firstOrNull()?.delta?.content
                                    if (!content.isNullOrEmpty()) {
                                        emit(content)
                                    }
                                } catch (e: Exception) {
                                    // 忽略解析错误
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            emit("[ERROR] 网络错误: ${e.message}")
        } finally {
            connection?.disconnect()
        }
    }.flowOn(Dispatchers.IO)
    
    data class StreamResponse(
        val choices: List<StreamChoice>?
    )
    
    data class StreamChoice(
        val delta: StreamDelta?
    )
    
    data class StreamDelta(
        val content: String?
    )
}

/**
 * API端点配置
 */
object ApiEndpoints {
    // 主要端点
    const val PRIMARY = "https://api.edgefn.net/v1"
    // 备用端点
    const val BACKUP1 = "https://api.openai.com/v1"
    const val BACKUP2 = "https://api.anthropic.com/v1"
    
    // 可用模型列表
    val MODELS = listOf(
        "GLM-5.1",
        "gpt-4o",
        "gpt-4o-mini",
        "claude-3-opus",
        "claude-3-sonnet"
    )
}
