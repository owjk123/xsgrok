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
import com.xsgrok.app.data.model.AutoModeState
import com.xsgrok.app.data.model.GenerationPresets
import com.xsgrok.app.ui.Screen
import com.xsgrok.app.ui.XSGrokViewModel

/**
 * 简化版自动模式界面 - 第一性原理优化
 * 核心流程：输入想法 → 一键生成 → 阅读/续写
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoModeScreen(viewModel: XSGrokViewModel) {
    val currentNovel by viewModel.currentNovel.collectAsState()
    val streamingContent by viewModel.streamingContent.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val autoModeState by viewModel.autoModeState.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val currentPreset by viewModel.currentPreset.collectAsState()
    
    var userPrompt by remember { mutableStateOf("") }
    var nextChapterGuide by remember { mutableStateOf("") }
    
    // 模式选择
    var selectedPresetId by remember { mutableStateOf("balanced") }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 标题
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
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 错误提示
        errorMessage?.let { error ->
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
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        when (autoModeState) {
            AutoModeState.IDLE -> {
                // 简洁的输入界面
                OutlinedTextField(
                    value = userPrompt,
                    onValueChange = { userPrompt = it },
                    label = { Text(stringResource(R.string.enter_one_sentence)) },
                    placeholder = { Text("例如：一个都市女孩追寻梦想的故事") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 简洁的模式选择
                Text(
                    text = "生成模式",
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GenerationPresets.getAll().forEach { preset ->
                        FilterChip(
                            selected = selectedPresetId == preset.id,
                            onClick = {
                                selectedPresetId = preset.id
                                viewModel.setGenerationPreset(preset.id)
                            },
                            label = { Text(preset.name) }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // 一键生成按钮
                Button(
                    onClick = {
                        if (userPrompt.isNotBlank()) {
                            viewModel.startAutoMode(userPrompt)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = userPrompt.isNotBlank() && !isGenerating
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.auto_generating))
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.start_generate))
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 提示文字
                Text(
                    text = "输入一句话创意，点击生成，即可获得完整章节",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            AutoModeState.GENERATING -> {
                // 生成中界面
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.auto_generating),
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // 流式显示生成内容
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 300.dp)
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
            }
            
            AutoModeState.REVIEW -> {
                // 简化版审阅（可选）
                Text(
                    text = "审阅章节",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = streamingContent,
                    onValueChange = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 200.dp),
                    readOnly = true
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.resetAutoMode() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("重新开始")
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
            
            AutoModeState.COMPLETED -> {
                // 完成界面
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
                        
                        currentNovel?.let { novel ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "《${novel.title}》${stringResource(R.string.chapters_count, novel.chapters.size)}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.navigateTo(Screen.Reading) }
                            ) {
                                Icon(Icons.Default.MenuBook, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("阅读")
                            }
                            
                            OutlinedButton(
                                onClick = {
                                    nextChapterGuide = ""
                                    viewModel.resetAutoMode()
                                }
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("继续写")
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 继续生成下一章
                OutlinedTextField(
                    value = nextChapterGuide,
                    onValueChange = { nextChapterGuide = it },
                    label = { Text("下一章引导（可选）") },
                    placeholder = { Text("例如：主角遇到危机") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Button(
                    onClick = { viewModel.continueAutoMode(nextChapterGuide) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.NavigateNext, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("生成下一章")
                }
            }
        }
    }
}
