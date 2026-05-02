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

/**
 * API服务 - 增强版
 * - 重试逻辑（最多3次）
 * - 改进错误信息展示（隐藏原始API错误）
 * - 增加网络状态检测
 */
class ApiService {
    
    private val gson = Gson()
    
    companion object {
        private const val MAX_RETRIES = 3
        private const val RETRY_DELAY_MS = 1000L
        private const val CONNECT_TIMEOUT = 30 * 1000  // 30秒
        private const val READ_TIMEOUT = 120 * 1000   // 120秒
    }
    
    /**
     * 生成内容 - 支持流式输出和重试
     */
    fun generateContent(
        apiKey: String,
        endpoint: String,
        model: String,
        systemPrompt: String,
        userPrompt: String,
        temperature: Float = 0.75f,
        maxRetries: Int = MAX_RETRIES
    ): Flow<String> = flow {
        var lastError: Exception? = null
        
        for (attempt in 1..maxRetries) {
            try {
                var connection: HttpURLConnection? = null
                try {
                    val url = URL("$endpoint/chat/completions")
                    connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "POST"
                    connection.setRequestProperty("Content-Type", "application/json")
                    connection.setRequestProperty("Authorization", "Bearer $apiKey")
                    connection.doOutput = true
                    connection.connectTimeout = CONNECT_TIMEOUT
                    connection.readTimeout = READ_TIMEOUT
                    
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
                        val errorBody = connection.errorStream?.bufferedReader()?.readText()
                        val friendlyMessage = getFriendlyErrorMessage(responseCode, errorBody)
                        emit("[ERROR] $friendlyMessage")
                        return@flow
                    }
                    
                    // 读取流式响应
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
                    
                    // 成功完成
                    return@flow
                    
                } finally {
                    connection?.disconnect()
                }
                
            } catch (e: Exception) {
                lastError = e
                
                // 如果不是最后一次尝试，等待后重试
                if (attempt < maxRetries) {
                    kotlinx.coroutines.delay(RETRY_DELAY_MS * attempt)
                }
            }
        }
        
        // 所有重试都失败
        val friendlyMessage = getFriendlyErrorMessage(null, lastError?.message)
        emit("[ERROR] $friendlyMessage")
        
    }.flowOn(Dispatchers.IO)
    
    /**
     * 将原始错误转换为友好提示
     */
    private fun getFriendlyErrorMessage(responseCode: Int?, errorBody: String?): String {
        return when {
            responseCode == 401 -> "API Key无效，请检查配置"
            responseCode == 403 -> "访问被拒绝，请检查API权限"
            responseCode == 429 -> "请求过于频繁，请稍后重试"
            responseCode == 500 -> "服务器内部错误，请稍后重试"
            responseCode == 503 -> "服务暂时不可用，请稍后重试"
            errorBody?.contains("timeout", ignoreCase = true) == true -> "请求超时，请检查网络连接"
            errorBody?.contains("connection", ignoreCase = true) == true -> "网络连接失败，请检查网络"
            errorBody?.contains("authentication", ignoreCase = true) == true -> "认证失败，请检查API Key"
            errorBody != null && errorBody.length < 100 -> errorBody
            else -> "生成失败，请重试"
        }
    }
    
    /**
     * 测试API连接（同步版本，用于设置页面）
     */
    fun testConnection(
        apiKey: String,
        endpoint: String,
        model: String,
        onResult: (Boolean, String) -> Unit
    ) {
        Thread {
            try {
                val url = URL("$endpoint/chat/completions")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Authorization", "Bearer $apiKey")
                connection.doOutput = true
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                
                val requestBody = """
                    {
                        "model": "$model",
                        "messages": [{"role": "user", "content": "hi"}],
                        "max_tokens": 5
                    }
                """.trimIndent()
                
                connection.outputStream.use { os ->
                    os.write(requestBody.toByteArray())
                }
                
                val responseCode = connection.responseCode
                val message = when {
                    responseCode in 200..299 -> "连接成功"
                    responseCode == 401 -> "API Key无效"
                    responseCode == 403 -> "访问被拒绝"
                    responseCode == 429 -> "请求过于频繁"
                    else -> "HTTP $responseCode"
                }
                
                onResult(responseCode in 200..299, message)
                
            } catch (e: Exception) {
                val message = when {
                    e.message?.contains("timeout", ignoreCase = true) == true -> "连接超时"
                    e.message?.contains("connection", ignoreCase = true) == true -> "连接失败"
                    else -> "连接失败: ${e.javaClass.simpleName}"
                }
                onResult(false, message)
            }
        }.start()
    }
    
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
