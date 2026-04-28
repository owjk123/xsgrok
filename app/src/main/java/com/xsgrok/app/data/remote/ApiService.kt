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
            connection.readTimeout = 120 * 1000
            
            val requestBody = ChatRequest(
                model = model,
                messages = listOf(
                    ChatMessage(role = "system", content = systemPrompt),
                    ChatMessage(role = "user", content = userPrompt)
                ),
                stream = true,
                temperature = temperature
            )
            
            val jsonBody = gson.toJson(requestBody)
            
            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(jsonBody)
                writer.flush()
            }
            
            // 检查响应码
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                // 读取错误流
                val errorStream = connection.errorStream
                val errorMessage = if (errorStream != null) {
                    BufferedReader(InputStreamReader(errorStream)).use { reader ->
                        reader.readText()
                    }
                } else {
                    "HTTP $responseCode: ${connection.responseMessage}"
                }
                emit("[ERROR] API错误($responseCode): $errorMessage")
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
                                    // 尝试解析错误响应
                                    if (data.contains("error", ignoreCase = true)) {
                                        try {
                                            val errorResp = gson.fromJson(data, ErrorResponse::class.java)
                                            if (!errorResp.error?.message.isNullOrBlank()) {
                                                emit("[ERROR] ${errorResp.error.message}")
                                                return@flow
                                            }
                                        } catch (e2: Exception) {
                                            // 忽略解析错误
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            emit("[ERROR] ${e.javaClass.simpleName}: ${e.message}")
        } finally {
            connection?.disconnect()
        }
    }.flowOn(Dispatchers.IO)
    
    // 错误响应模型
    data class ErrorResponse(
        val error: ErrorDetail?
    )
    
    data class ErrorDetail(
        val message: String?,
        val type: String?,
        val code: String?
    )
    
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
