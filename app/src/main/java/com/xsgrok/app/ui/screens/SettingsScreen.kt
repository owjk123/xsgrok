package com.xsgrok.app.ui.screens

import android.util.Base64
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.xsgrok.app.R
import com.xsgrok.app.data.model.GenerationPresets
import com.xsgrok.app.data.remote.ApiService
import com.xsgrok.app.ui.XSGrokViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * 设置页面 - 增强版
 * - 测试连接按钮
 * - 模型选择
 * - 外观设置
 * - 关于信息
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: XSGrokViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val currentPreset by viewModel.currentPreset.collectAsState()
    val apiConfig = uiState.apiConfig
    
    var apiKey by remember(apiConfig) { mutableStateOf(apiConfig.apiKey) }
    var endpoint by remember(apiConfig) { mutableStateOf(apiConfig.endpoint) }
    var model by remember(apiConfig) { mutableStateOf(apiConfig.model) }
    var showApiKey by remember { mutableStateOf(false) }
    
    // 测试连接状态
    var isTesting by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    
    // 模型列表
    val models = listOf("GLM-5.1", "gpt-4o", "gpt-4o-mini", "claude-3-sonnet")
    
    // 导出格式选项
    var exportFormatExpanded by remember { mutableStateOf(false) }
    var selectedExportFormat by remember { mutableStateOf("TXT") }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // API配置卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.api_configuration),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = {
                        apiKey = it
                        viewModel.updateApiKey(it)
                    },
                    label = { Text(stringResource(R.string.api_key)) },
                    placeholder = { Text(stringResource(R.string.enter_api_key)) },
                    visualTransformation = if (showApiKey) VisualTransformation.None 
                        else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showApiKey = !showApiKey }) {
                            Icon(
                                if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = stringResource(R.string.toggle_visibility)
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = endpoint,
                    onValueChange = {
                        endpoint = it
                        viewModel.updateEndpoint(it)
                    },
                    label = { Text(stringResource(R.string.api_endpoint)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // 模型选择
                ExposedDropdownMenuBox(
                    expanded = false,
                    onExpandedChange = { }
                ) {
                    OutlinedTextField(
                        value = model,
                        onValueChange = {
                            model = it
                            viewModel.updateModel(it)
                        },
                        label = { Text(stringResource(R.string.model)) },
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = false) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 测试连接按钮
                OutlinedButton(
                    onClick = {
                        isTesting = true
                        testResult = null
                        testApiConnection(apiKey, endpoint, model) { success, message ->
                            isTesting = false
                            testResult = Pair(success, message)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = apiKey.isNotBlank() && !isTesting
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("测试中...")
                    } else {
                        Icon(Icons.Default.NetworkCheck, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("测试连接")
                    }
                }
                
                // 测试结果
                testResult?.let { (success, message) ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (success) 
                                MaterialTheme.colorScheme.secondaryContainer 
                            else 
                                MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (success) Icons.Default.CheckCircle else Icons.Default.Error,
                                contentDescription = null,
                                tint = if (success) 
                                    MaterialTheme.colorScheme.secondary 
                                else 
                                    MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = if (success) "连接成功" else "连接失败",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 生成模式选择
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "生成模式",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                
                GenerationPresets.getAll().forEach { preset ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentPreset.id == preset.id,
                            onClick = { viewModel.setGenerationPreset(preset.id) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = preset.name,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = preset.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 外观设置
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Palette, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.appearance),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (apiConfig.isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(stringResource(R.string.dark_mode))
                    }
                    Switch(
                        checked = apiConfig.isDarkMode,
                        onCheckedChange = { viewModel.toggleDarkMode() }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 关于
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.about),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "XSGrok v3.1.0",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "AI小说生成器 - 文学色系版",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.app_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

/**
 * 测试API连接
 */
private fun testApiConnection(
    apiKey: String,
    endpoint: String,
    model: String,
    onResult: (Boolean, String) -> Unit
) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val url = URL("$endpoint/chat/completions")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Authorization", "Bearer $apiKey")
            connection.doOutput = true
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            
            // 发送最小请求体
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
            val response = if (responseCode in 200..299) {
                "响应正常 (HTTP $responseCode)"
            } else {
                val errorStream = connection.errorStream
                if (errorStream != null) {
                    val errorBody = errorStream.bufferedReader().readText()
                    // 隐藏敏感信息
                    val sanitizedError = errorBody
                        .replace(Regex("sk-[a-zA-Z0-9]+"), "sk-***")
                        .take(200)
                    "HTTP $responseCode: $sanitizedError"
                } else {
                    "HTTP $responseCode"
                }
            }
            
            withContext(Dispatchers.Main) {
                onResult(responseCode in 200..299, response)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                // 隐藏敏感错误信息
                val message = when {
                    e.message?.contains("authentication", ignoreCase = true) == true -> 
                        "认证失败，请检查API Key"
                    e.message?.contains("connection", ignoreCase = true) == true -> 
                        "连接失败，请检查网络和Endpoint"
                    e.message?.contains("timeout", ignoreCase = true) == true -> 
                        "连接超时"
                    else -> 
                        "连接错误: ${e.javaClass.simpleName}"
                }
                onResult(false, message)
            }
        }
    }
}
