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
import com.xsgrok.app.ui.XSGrokViewModel
import kotlinx.coroutines.delay

@Composable
fun AutoModeScreen(viewModel: XSGrokViewModel) {
    val currentNovel by viewModel.currentNovel.collectAsState()
    val streamingContent by viewModel.streamingContent.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val autoModeState by viewModel.autoModeState.collectAsState()
    
    var userPrompt by remember { mutableStateOf("") }
    var nextChapterGuide by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 模式指示器
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
        
        when (autoModeState) {
            AutoModeState.IDLE -> {
                // 初始状态：输入一句话
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
            }
            
            AutoModeState.GENERATING_OUTLINE -> {
                // 生成大纲中
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
                            text = "AI正在创作小说大纲...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            
            AutoModeState.GENERATING_CHAPTER -> {
                // 生成章节中
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
                    
                    OutlinedButton(
                        onClick = { viewModel.finishAutoMode() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.done))
                    }
                }
            }
            
            AutoModeState.COMPLETED -> {
                // 完成
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
                            text = "小说创作完成！",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { viewModel.navigateTo(com.xsgrok.app.ui.Screen.NovelDetail) }
                        ) {
                            Text("查看小说")
                        }
                    }
                }
            }
        }
    }
}

enum class AutoModeState {
    IDLE,
    GENERATING_OUTLINE,
    GENERATING_CHAPTER,
    COMPLETED
}
