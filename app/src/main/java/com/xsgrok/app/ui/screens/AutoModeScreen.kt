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
import com.xsgrok.app.ui.Screen
import com.xsgrok.app.ui.XSGrokViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoModeScreen(viewModel: XSGrokViewModel) {
    val currentNovel by viewModel.currentNovel.collectAsState()
    val autoModeNovel by viewModel.autoModeNovel.collectAsState()
    val streamingContent by viewModel.streamingContent.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val autoModeState by viewModel.autoModeState.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    
    var userPrompt by remember { mutableStateOf("") }
    var nextChapterGuide by remember { mutableStateOf("") }
    
    // P4: 审阅阶段的新编辑状态
    var editTitle by remember { mutableStateOf("") }
    var editType by remember { mutableStateOf("") }
    var editStyle by remember { mutableStateOf("") }
    var editMainCharacter by remember { mutableStateOf("") }
    var editOutline by remember { mutableStateOf("") }
    var editWorldBackground by remember { mutableStateOf("") }
    var editPowerSystem by remember { mutableStateOf("") }
    var editWorldRules by remember { mutableStateOf("") }
    var editKeyCharacters by remember { mutableStateOf("") }
    
    // P4: 新增设置状态
    var selectedTabooLevel by remember { mutableStateOf(TabooLevel.MODERATE) }
    var descriptionDensity by remember { mutableStateOf(5f) }
    var selectedRhythmPreference by remember { mutableStateOf(RhythmPreference.BALANCED) }
    var showAdvancedSettings by remember { mutableStateOf(false) }
    
    // 当进入审阅状态时，初始化编辑字段
    LaunchedEffect(autoModeNovel) {
        autoModeNovel?.let { novel ->
            editTitle = novel.title
            editType = novel.type
            editStyle = novel.style
            editMainCharacter = novel.mainCharacter
            editOutline = novel.outline
            editWorldBackground = novel.worldBuilding.worldBackground
            editPowerSystem = novel.worldBuilding.powerSystem
            editWorldRules = novel.worldBuilding.rules
            editKeyCharacters = novel.characters.joinToString("\n") { char ->
                if (char.personality.isNotBlank()) "${char.name} - ${char.role} - ${char.description} - ${char.personality}"
                else "${char.name} - ${char.role} - ${char.description}"
            }
            
            // P4: 初始化新设置
            selectedTabooLevel = novel.sensoryProfile.tabooLevel
            descriptionDensity = novel.sensoryProfile.descriptionDensity.toFloat()
            selectedRhythmPreference = novel.generationConfig.rhythmPreference
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 标题卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.auto_mode),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        
        // P4: 显示当前设置状态
        if (autoModeState == AutoModeState.IDLE && autoModeNovel != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "当前强度：${selectedTabooLevel.displayName} | 密度：${descriptionDensity.toInt()}/10",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    IconButton(onClick = { showAdvancedSettings = !showAdvancedSettings }) {
                        Icon(
                            if (showAdvancedSettings) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "高级设置",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                
                // P4: 高级设置展开
                if (showAdvancedSettings) {
                    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                        Text(
                            text = "节奏偏好：${selectedRhythmPreference.displayName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }
        
        // 错误提示
        errorMessage?.let { error ->
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { viewModel.clearError() }) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        when (autoModeState) {
            AutoModeState.IDLE -> {
                OutlinedTextField(
                    value = userPrompt,
                    onValueChange = { userPrompt = it },
                    label = { Text(stringResource(R.string.enter_one_sentence)) },
                    placeholder = { Text("例如：一个都市女孩追寻梦想的故事") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = { 
                        if (userPrompt.isNotBlank()) {
                            viewModel.startAutoMode(userPrompt)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = userPrompt.isNotBlank()
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.start_auto))
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // P4: 模式说明卡片
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.auto_mode_guide_title),
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.auto_mode_guide_content),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // P4: 强度等级说明
                        Text(
                            text = "【强度等级】",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        TabooLevel.entries.forEach { level ->
                            Text(
                                text = "• ${level.displayName}：${level.description}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            AutoModeState.GENERATING_OUTLINE -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "正在生成基础资料...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = stringResource(R.string.please_wait),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = { 
                                viewModel.stopGeneration()
                            }
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("取消")
                        }
                    }
                }
            }
            
            AutoModeState.REVIEW -> {
                // P4: 审阅界面增强
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "审阅并确认小说资料",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 基础信息卡片
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            OutlinedTextField(
                                value = editTitle,
                                onValueChange = { 
                                    editTitle = it
                                    viewModel.updateAutoModeNovel(title = it)
                                },
                                label = { Text("小说标题") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = editType,
                                    onValueChange = { 
                                        editType = it
                                        viewModel.updateAutoModeNovel(type = it)
                                    },
                                    label = { Text("类型") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                
                                OutlinedTextField(
                                    value = editStyle,
                                    onValueChange = { 
                                        editStyle = it
                                        viewModel.updateAutoModeNovel(style = it)
                                    },
                                    label = { Text("风格") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            OutlinedTextField(
                                value = editMainCharacter,
                                onValueChange = { 
                                    editMainCharacter = it
                                    viewModel.updateAutoModeNovel(mainCharacter = it)
                                },
                                label = { Text("主角简介") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2,
                                maxLines = 3
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // P4: 强度设置卡片
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Style,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "感官描写强度",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // 禁忌等级选择
                            Text(
                                text = "禁忌等级",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                TabooLevel.entries.forEach { level ->
                                    FilterChip(
                                        selected = selectedTabooLevel == level,
                                        onClick = {
                                            selectedTabooLevel = level
                                            viewModel.updateAutoModeTabooLevel(level)
                                        },
                                        label = { 
                                            Text(
                                                level.displayName, 
                                                style = MaterialTheme.typography.labelSmall
                                            ) 
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Text(
                                text = selectedTabooLevel.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // 描写密度
                            Text(
                                text = "描写密度：${descriptionDensity.toInt()}/10",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Slider(
                                value = descriptionDensity,
                                onValueChange = { descriptionDensity = it },
                                onValueChangeFinished = {
                                    viewModel.updateAutoModeDescriptionDensity(descriptionDensity.toInt())
                                },
                                valueRange = 1f..10f,
                                steps = 8,
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // 节奏偏好
                            Text(
                                text = "节奏偏好",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
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
                                                viewModel.updateAutoModeRhythmPreference(preference)
                                            }
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "${preference.displayName}（${preference.description}）",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 大纲卡片
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            OutlinedTextField(
                                value = editOutline,
                                onValueChange = { 
                                    editOutline = it
                                    viewModel.updateAutoModeNovel(outline = it)
                                },
                                label = { Text("小说大纲") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 4,
                                maxLines = 12
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 世界设定卡片
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Public,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "世界设定",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            OutlinedTextField(
                                value = editWorldBackground,
                                onValueChange = { 
                                    editWorldBackground = it
                                    viewModel.updateAutoModeNovel(worldBackground = it)
                                },
                                label = { Text("世界背景") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2,
                                maxLines = 5
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            OutlinedTextField(
                                value = editPowerSystem,
                                onValueChange = { 
                                    editPowerSystem = it
                                    viewModel.updateAutoModeNovel(powerSystem = it)
                                },
                                label = { Text("力量体系（非战斗类可留空）") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2,
                                maxLines = 4
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            OutlinedTextField(
                                value = editWorldRules,
                                onValueChange = { 
                                    editWorldRules = it
                                    viewModel.updateAutoModeNovel(worldRules = it)
                                },
                                label = { Text("世界规则") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2,
                                maxLines = 4
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 关键人物卡片
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.People,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "关键人物",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = "每行一个角色，格式：姓名 - 身份/角色 - 简介 - 性格关键词",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            OutlinedTextField(
                                value = editKeyCharacters,
                                onValueChange = { 
                                    editKeyCharacters = it
                                    viewModel.updateAutoModeKeyCharacters(it)
                                },
                                placeholder = { Text("林晓 - 主角 - 普通大学生 - 好奇心强\n苏老师 - 配角 - 神秘导师 - 冷静睿智") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 4,
                                maxLines = 10
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 确认按钮 - Bug1修复：区分新建和继续写作
                    if (autoModeNovel?.chapters?.isNotEmpty() == true) {
                        Button(
                            onClick = { viewModel.confirmAndStartWriting() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("继续写下一章")
                        }
                    } else {
                        Button(
                            onClick = { viewModel.confirmAndStartWriting() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("确认并开始写作")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedButton(
                        onClick = { viewModel.resetAutoMode() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("重新开始")
                    }
                }
            }
            
            AutoModeState.GENERATING_CHAPTER -> {
                // 生成中的界面
                currentNovel?.let { novel ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = novel.title,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Row(
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                AssistChip(
                                    onClick = {},
                                    label = { Text(novel.type, style = MaterialTheme.typography.labelSmall) }
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                AssistChip(
                                    onClick = {},
                                    label = { Text(novel.style, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                            Text(
                                text = stringResource(R.string.chapters_count, novel.chapters.size),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.auto_generating),
                                    style = MaterialTheme.typography.titleSmall
                                )
                                if (isGenerating) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Text(
                                    text = streamingContent.ifBlank { stringResource(R.string.waiting_content) },
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (isGenerating) {
                        OutlinedButton(
                            onClick = { viewModel.stopGeneration() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("停止生成")
                        }
                    }
                    
                    if (!isGenerating && streamingContent.isBlank() && errorMessage != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.resetAutoMode() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("重新开始")
                            }
                            Button(
                                onClick = { viewModel.retryAutoMode() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("重试生成")
                            }
                        }
                    }
                    
                    if (!isGenerating && streamingContent.isNotBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = nextChapterGuide,
                                onValueChange = { nextChapterGuide = it },
                                placeholder = { Text(stringResource(R.string.guide_next)) },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            
                            Button(
                                onClick = {
                                    viewModel.continueAutoMode(nextChapterGuide)
                                    nextChapterGuide = ""
                                }
                            ) {
                                Text(stringResource(R.string.next_chapter))
                            }
                        }
                        
                        // 用户实时指令注入
                        Spacer(modifier = Modifier.height(8.dp))
                        var userCommand by remember { mutableStateOf("") }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = userCommand,
                                onValueChange = { userCommand = it },
                                placeholder = { Text("⚡ 注入剧情指令（最高优先级）") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                supportingText = { Text("例：让主角发现真相 / 角色必须受伤", style = MaterialTheme.typography.labelSmall) }
                            )
                            
                            OutlinedButton(
                                onClick = {
                                    if (userCommand.isNotBlank()) {
                                        viewModel.injectUserCommand(userCommand)
                                        userCommand = ""
                                    }
                                },
                                enabled = userCommand.isNotBlank()
                            ) {
                                Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("注入")
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { 
                                    currentNovel?.let { viewModel.selectNovel(it.id) }
                                    viewModel.navigateTo(Screen.Reading) 
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.MenuBook, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.read))
                            }
                            
                            Button(
                                onClick = { viewModel.finishAutoMode() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.done))
                            }
                        }
                    }
                }
            }
            
            AutoModeState.COMPLETED -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.novel_completed),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        currentNovel?.let { novel ->
                            Text(
                                text = "《${novel.title}》${stringResource(R.string.chapters_count, novel.chapters.size)}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            
                            // P4: 显示生成统计
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "【生成统计】",
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                    Text(
                                        text = "强度等级：${novel.sensoryProfile.tabooLevel.displayName}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = "描写密度：${novel.sensoryProfile.descriptionDensity}/10",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = "节奏偏好：${novel.generationConfig.rhythmPreference.displayName}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(onClick = { viewModel.navigateTo(Screen.Reading) }) {
                                Icon(Icons.Default.MenuBook, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.read))
                            }
                            OutlinedButton(onClick = { viewModel.navigateTo(Screen.WorldBuilding) }) {
                                Icon(Icons.Default.Edit, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.edit_settings))
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedButton(
                    onClick = { 
                        viewModel.resetAutoMode()
                        viewModel.navigateTo(Screen.AutoMode)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.create_new_novel))
                }
            }
        }
    }
}
