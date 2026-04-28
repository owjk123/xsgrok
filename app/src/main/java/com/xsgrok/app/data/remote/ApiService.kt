package com.xsgrok.app.data.remote

import com.google.gson.Gson
import com.xsgrok.app.data.model.ChatMessage
import com.xsgrok.app.data.model.ChatRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

class ApiService {
    private val gson = Gson()
    
    /**
     * 生成内容 - 支持动态temperature参数
     * @param temperature 温度参数，控制输出的随机性
     *         - 开场章 0.85~0.95：高创意，多样化表达
     *         - 推进章 0.7~0.8：平衡创意与连贯
     *         - 收束章 0.6~0.7：更确定性，确保结局完整
     */
    fun generateContent(
        apiKey: String,
        endpoint: String,
        model: String,
        systemPrompt: String,
        userPrompt: String,
        temperature: Float = 0.75f  // P0新增：动态temperature
    ): Flow<String> = flow {
        try {
            val url = URL("$endpoint/chat/completions")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Authorization", "Bearer $apiKey")
            connection.doOutput = true
            connection.connectTimeout = 60 * 1000
            connection.readTimeout = 120 * 1000
            
            val requestBody = ChatRequest(
                model = model,
                messages = listOf(
                    ChatMessage(role = "system", content = systemPrompt),
                    ChatMessage(role = "user", content = userPrompt)
                ),
                stream = true,
                temperature = temperature  // P0：传递temperature参数
            )
            
            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(gson.toJson(requestBody))
                writer.flush()
            }
            
            BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                var line: String?
                val buffer = StringBuilder()
                
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
                                    // Ignore parsing errors for partial responses
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            emit("[ERROR] ${e.message}")
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

class ApiEndpoints {
    companion object {
        val PRIMARY = "https://api.apiyi.com/v1"
        val BACKUP1 = "http://vip.apiyi.com:16888"
        val BACKUP2 = "http://api-cf.apiyi.com:16888"
        
        val MODELS = listOf(
            "grok-4.20-beta",
            "grok-4.20-beta-0309-reasoning",
            "grok-4.20-beta-0309-non-reasoning",
            "grok-4.20-multi-agent-beta-0309"
        )
    }
}
