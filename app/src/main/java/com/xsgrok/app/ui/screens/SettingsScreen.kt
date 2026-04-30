package com.xsgrok.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.xsgrok.app.R
import com.xsgrok.app.data.model.GenerationPresets
import com.xsgrok.app.ui.XSGrokViewModel

/**
 * 简化版设置界面 - 第一性原理优化
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: XSGrokViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val currentPreset by viewModel.currentPreset.collectAsState()
    val apiConfig = uiState.apiConfig
    
    var apiKey by remember(apiConfig) { mutableStateOf(apiConfig.apiKey) }
    var endpoint by remember(apiConfig) { mutableStateOf(apiConfig.endpoint) }
    var showApiKey by remember { mutableStateOf(false) }
    
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
                Text(
                    text = stringResource(R.string.api_configuration),
                    style = MaterialTheme.typography.titleMedium
                )
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
                
                Spacer(modifier = Modifier.height(16.dp))
                
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
                Text(
                    text = stringResource(R.string.appearance),
                    style = MaterialTheme.typography.titleMedium
                )
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
                Text(
                    text = stringResource(R.string.about),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "XSGrok v2.5.0",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "AI小说生成器 - 第一性原理优化版",
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
    }
}
