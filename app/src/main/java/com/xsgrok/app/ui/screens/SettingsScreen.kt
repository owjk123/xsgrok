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
import androidx.compose.ui.unit.dp
import com.xsgrok.app.R
import com.xsgrok.app.data.model.*
import com.xsgrok.app.data.remote.ApiEndpoints
import com.xsgrok.app.ui.XSGrokViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: XSGrokViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val currentNovel by viewModel.currentNovel.collectAsState()
    val apiConfig = uiState.apiConfig
    
    var apiKey by remember(apiConfig) { mutableStateOf(apiConfig.apiKey) }
    var endpoint by remember(apiConfig) { mutableStateOf(apiConfig.endpoint) }
    var selectedModel by remember(apiConfig) { mutableStateOf(apiConfig.model) }
    var showApiKey by remember { mutableStateOf(false) }
    var modelExpanded by remember { mutableStateOf(false) }
    var endpointExpanded by remember { mutableStateOf(false) }
    
    // P4: 新增状态
    var descriptionDensity by remember { mutableStateOf(5f) }
    var selectedTabooLevel by remember { mutableStateOf(TabooLevel.MODERATE) }
    var selectedRhythmPreference by remember { mutableStateOf(RhythmPreference.BALANCED) }
    var selectedPerspectiveMode by remember { mutableStateOf(PerspectiveMode.THIRD_PERSON) }
    var autoEnhanceEnabled by remember { mutableStateOf(true) }
    var consistencyCheckEnabled by remember { mutableStateOf(true) }
    
    // 初始化当前小说的配置
    LaunchedEffect(currentNovel) {
        currentNovel?.let { novel ->
            descriptionDensity = novel.sensoryProfile.descriptionDensity.toFloat()
            selectedTabooLevel = novel.sensoryProfile.tabooLevel
            selectedRhythmPreference = novel.generationConfig.rhythmPreference
            selectedPerspectiveMode = novel.generationConfig.perspectiveMode
            autoEnhanceEnabled = novel.generationConfig.autoEnhanceIntimate
            consistencyCheckEnabled = novel.generationConfig.checkBodyConsistency
        }
    }
    
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
                    visualTransformation = if (showApiKey) androidx.compose.ui.text.input.VisualTransformation.None 
                        else androidx.compose.ui.text.input.PasswordVisualTransformation(),
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
                
                ExposedDropdownMenuBox(
                    expanded = endpointExpanded,
                    onExpandedChange = { endpointExpanded = !endpointExpanded }
                ) {
                    OutlinedTextField(
                        value = endpoint,
                        onValueChange = {
                            endpoint = it
                            viewModel.updateEndpoint(it)
                        },
                        label = { Text(stringResource(R.string.api_endpoint)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = endpointExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        singleLine = true
                    )
                    ExposedDropdownMenu(
                        expanded = endpointExpanded,
                        onDismissRequest = { endpointExpanded = false }
                    ) {
                        listOf(ApiEndpoints.PRIMARY, ApiEndpoints.BACKUP1, ApiEndpoints.BACKUP2).forEach { ep ->
                            DropdownMenuItem(
                                text = { Text(ep) },
                                onClick = {
                                    endpoint = ep
                                    viewModel.updateEndpoint(ep)
                                    endpointExpanded = false
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                ExposedDropdownMenuBox(
                    expanded = modelExpanded,
                    onExpandedChange = { modelExpanded = !modelExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedModel,
                        onValueChange = {},
                        label = { Text(stringResource(R.string.model)) },
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = modelExpanded,
                        onDismissRequest = { modelExpanded = false }
                    ) {
                        ApiEndpoints.MODELS.forEach { model ->
                            DropdownMenuItem(
                                text = { Text(model) },
                                onClick = {
                                    selectedModel = model
                                    viewModel.updateModel(model)
                                    modelExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // P4: 感官描写设置卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Style, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "感官描写设置",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 描写密度滑块
                Text(
                    text = "描写密度：${descriptionDensity.toInt()}/10",
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = descriptionDensity,
                    onValueChange = { descriptionDensity = it },
                    onValueChangeFinished = {
                        currentNovel?.let { novel ->
                            val newProfile = novel.sensoryProfile.copy(
                                descriptionDensity = descriptionDensity.toInt()
                            )
                            viewModel.updateSensoryProfile(novel, newProfile)
                        }
                    },
                    valueRange = 1f..10f,
                    steps = 8,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 禁忌等级选择
                Text(
                    text = "禁忌等级",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TabooLevel.entries.forEach { level ->
                        FilterChip(
                            selected = selectedTabooLevel == level,
                            onClick = {
                                selectedTabooLevel = level
                                currentNovel?.let { novel ->
                                    val newProfile = novel.sensoryProfile.copy(tabooLevel = level)
                                    viewModel.updateSensoryProfile(novel, newProfile)
                                }
                            },
                            label = { Text(level.displayName, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = selectedTabooLevel.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // P4: 生成配置卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Settings, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "生成配置",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 节奏偏好
                Text(
                    text = "节奏偏好",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Column {
                    RhythmPreference.entries.forEach { preference ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedRhythmPreference == preference,
                                onClick = {
                                    selectedRhythmPreference = preference
                                    currentNovel?.let { novel ->
                                        val newConfig = novel.generationConfig.copy(
                                            rhythmPreference = preference
                                        )
                                        viewModel.updateGenerationConfig(novel, newConfig)
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = preference.displayName,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = preference.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                HorizontalDivider()
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 视角模式
                Text(
                    text = "视角模式",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PerspectiveMode.entries.forEach { mode ->
                        FilterChip(
                            selected = selectedPerspectiveMode == mode,
                            onClick = {
                                selectedPerspectiveMode = mode
                                currentNovel?.let { novel ->
                                    val newConfig = novel.generationConfig.copy(
                                        perspectiveMode = mode
                                    )
                                    viewModel.updateGenerationConfig(novel, newConfig)
                                }
                            },
                            label = { Text(mode.displayName) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // P4: 一致性检查卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ManageSearch, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "一致性检查",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 自动增强亲密场景
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "自动增强亲密场景",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "检测到亲密场景时自动注入增强指令",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = autoEnhanceEnabled,
                        onCheckedChange = { 
                            autoEnhanceEnabled = it
                            currentNovel?.let { novel ->
                                val newConfig = novel.generationConfig.copy(
                                    autoEnhanceIntimate = it
                                )
                                viewModel.updateGenerationConfig(novel, newConfig)
                            }
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 身体一致性检查
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "身体描写一致性",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "保持角色身体特征的一致性",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = consistencyCheckEnabled,
                        onCheckedChange = { 
                            consistencyCheckEnabled = it
                            currentNovel?.let { novel ->
                                val newConfig = novel.generationConfig.copy(
                                    checkBodyConsistency = it
                                )
                                viewModel.updateGenerationConfig(novel, newConfig)
                            }
                        }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 外观设置卡片
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
        
        // 关于卡片
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
                    text = "XSGrok v2.0.0",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "高品质长篇叙事文学专用生成引擎",
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
