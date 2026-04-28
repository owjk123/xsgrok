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
    
    // 审阅阶段的编辑状态
    var editTitle by remember { mutableStateOf("") }
    var editType by remember { mutableStateOf("") }
    var editStyle by remember { mutableStateOf("") }
    var editMainCharacter by remember { mutableStateOf("") }
    var editOutline by remember { mutableStateOf("") }
    var editWorldBackground by remember { mutableStateOf("") }
    var editPowerSystem by remember { mutableStateOf("") }
    
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
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
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
                    placeholder = { Text("例如：一个修仙者在末世求生的故事") },
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
                                viewModel.resetAutoMode()
                            }
                        ) {
                            Icon(Icons.Default.Cancel, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("取消")
                        }
                    }
                }
            }
            
            AutoModeState.REVIEW -> {
                // 审阅基础资料界面
                autoModeNovel?.let { novel ->
                    Text(
                        text = "基础资料审阅",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "请审阅并修改以下资料，确认后开始创作",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = editTitle,
                            onValueChange = { editTitle = it },
                            label = { Text("小说标题") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = editType,
                                onValueChange = { editType = it },
                                label = { Text("类型") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = editStyle,
                                onValueChange = { editStyle = it },
                                label = { Text("风格") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                        
                        OutlinedTextField(
                            value = editMainCharacter,
                            onValueChange = { editMainCharacter = it },
                            label = { Text("主角设定") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            maxLines = 4
                        )
                        
                        OutlinedTextField(
                            value = editOutline,
                            onValueChange = { editOutline = it },
                            label = { Text("故事大纲") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 4,
                            maxLines = 8
                        )
                        
                        OutlinedTextField(
                            value = editWorldBackground,
                            onValueChange = { editWorldBackground = it },
                            label = { Text("世界背景") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 6
                        )
                        
                        OutlinedTextField(
                            value = editPowerSystem,
                            onValueChange = { editPowerSystem = it },
                            label = { Text("力量体系") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 6
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 按钮区域
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
                            Text("重新生成")
                        }
                        Button(
                            onClick = {
                                // 更新资料并开始写作
                                viewModel.updateAutoModeNovel(
                                    title = editTitle,
                                    type = editType,
                                    style = editStyle,
                                    mainCharacter = editMainCharacter,
                                    outline = editOutline,
                                    worldBackground = editWorldBackground,
                                    powerSystem = editPowerSystem
                                )
                                viewModel.confirmAndStartWriting()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("开始写作")
                        }
                    }
                }
            }
            
            AutoModeState.GENERATING_CHAPTER -> {
                currentNovel?.let { novel ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { 
                                viewModel.selectNovel(novel.id)
                                viewModel.navigateTo(Screen.WorldBuilding)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.world_building), maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = { 
                                viewModel.selectNovel(novel.id)
                                viewModel.navigateTo(Screen.Characters)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.characters), maxLines = 1)
                        }
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
