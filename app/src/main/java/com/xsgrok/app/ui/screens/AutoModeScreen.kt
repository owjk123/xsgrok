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
 * 自动模式界面 - 重构版
 * 新流程：输入一句话 → AI生成6大基础设定 → 用户审阅编辑 → 确认后生成章节
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
    val currentFoundation by viewModel.currentFoundation.collectAsState()
    
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
                // ========== 阶段1：输入界面 ==========
                IdleContent(
                    userPrompt = userPrompt,
                    onUserPromptChange = { userPrompt = it },
                    selectedPresetId = selectedPresetId,
                    onPresetSelected = {
                        selectedPresetId = it
                        viewModel.setGenerationPreset(it)
                    },
                    isGenerating = isGenerating,
                    onStartGeneration = {
                        if (userPrompt.isNotBlank()) {
                            viewModel.startAutoMode(userPrompt)
                        }
                    }
                )
            }
            
            AutoModeState.GENERATING_FOUNDATION -> {
                // ========== 阶段2：正在生成基础设定 ==========
                GeneratingFoundationContent(
                    streamingContent = streamingContent,
                    isGenerating = isGenerating,
                    onStop = { viewModel.stopGeneration() }
                )
            }
            
            AutoModeState.REVIEW_FOUNDATION -> {
                // ========== 阶段3：审阅基础设定（可编辑） ==========
                ReviewFoundationContent(
                    foundation = currentFoundation,
                    novelTitle = currentNovel?.title ?: "新小说",
                    onFoundationUpdate = { field, value ->
                        viewModel.updateFoundationField(field, value)
                    },
                    onRegenerate = { viewModel.regenerateFoundation() },
                    onConfirm = { viewModel.confirmFoundationAndStartWriting() },
                    onReset = { viewModel.resetAutoMode() },
                    isGenerating = isGenerating
                )
            }
            
            AutoModeState.GENERATING_CHAPTER -> {
                // ========== 阶段4：正在生成章节 ==========
                GeneratingChapterContent(
                    streamingContent = streamingContent,
                    isGenerating = isGenerating,
                    onStop = { viewModel.stopGeneration() }
                )
            }
            
            AutoModeState.COMPLETED -> {
                // ========== 阶段5：完成界面 ==========
                CompletedContent(
                    currentNovel = currentNovel,
                    nextChapterGuide = nextChapterGuide,
                    onNextGuideChange = { nextChapterGuide = it },
                    onNavigateToReading = { viewModel.navigateTo(Screen.Reading) },
                    onContinueWriting = { viewModel.continueAutoMode(nextChapterGuide) },
                    onReset = { 
                        nextChapterGuide = ""
                        viewModel.resetAutoMode() 
                    }
                )
            }
        }
    }
}

/**
 * IDLE 状态：输入界面
 */
@Composable
private fun IdleContent(
    userPrompt: String,
    onUserPromptChange: (String) -> Unit,
    selectedPresetId: String,
    onPresetSelected: (String) -> Unit,
    isGenerating: Boolean,
    onStartGeneration: () -> Unit
) {
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState())
    ) {
        // 说明文字
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "新功能：AI 自动分解创意",
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "输入一句话创意，AI 会自动生成角色设定、人物关系、时间线、剧情走向等6大基础设定，您可以审阅编辑后再开始写作。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 输入框
        OutlinedTextField(
            value = userPrompt,
            onValueChange = onUserPromptChange,
            label = { Text(stringResource(R.string.enter_one_sentence)) },
            placeholder = { Text("例如：都市女孩从零开始创业，最终成为商业女强人") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 4,
            maxLines = 6
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 模式选择
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
                    onClick = { onPresetSelected(preset.id) },
                    label = { Text(preset.name) }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 开始生成按钮
        Button(
            onClick = onStartGeneration,
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
            text = "AI 将自动分析您的创意，生成完整的基础设定，您可以在下一步审阅编辑",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * GENERATING_FOUNDATION 状态：正在生成基础设定
 */
@Composable
private fun ColumnScope.GeneratingFoundationContent(
    streamingContent: String,
    isGenerating: Boolean,
    onStop: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
    ) {
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
                        text = "正在分析创意，生成基础设定...",
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "这可能需要 10-30 秒，请稍候",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                HorizontalDivider()
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 流式显示生成内容
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = streamingContent.ifBlank { "正在思考中..." },
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (isGenerating) {
            OutlinedButton(
                onClick = onStop,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Stop, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("停止生成")
            }
        }
    }
}

/**
 * REVIEW_FOUNDATION 状态：审阅基础设定（可编辑）
 */
@Composable
private fun ColumnScope.ReviewFoundationContent(
    foundation: com.xsgrok.app.data.model.NovelFoundation,
    novelTitle: String,
    onFoundationUpdate: (String, String) -> Unit,
    onRegenerate: () -> Unit,
    onConfirm: () -> Unit,
    onReset: () -> Unit,
    isGenerating: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .verticalScroll(rememberScrollState())
    ) {
        // 标题和说明
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "《$novelTitle》",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "请审阅以下6大基础设定，可直接编辑修改，确认后开始写作",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // 1. 角色设定
        FoundationCard(
            title = "① 角色设定",
            value = foundation.characterSettings,
            onValueChange = { onFoundationUpdate("characterSettings", it) },
            placeholder = "描述主角和配角的性格、外貌、背景等..."
        )
        
        // 2. 人物关系
        FoundationCard(
            title = "② 人物关系",
            value = foundation.characterRelationships,
            onValueChange = { onFoundationUpdate("characterRelationships", it) },
            placeholder = "描述角色之间的关系、矛盾、合作等..."
        )
        
        // 3. 时间线
        FoundationCard(
            title = "③ 时间线",
            value = foundation.timeline,
            onValueChange = { onFoundationUpdate("timeline", it) },
            placeholder = "描述故事的时间跨度、重要节点等..."
        )
        
        // 4. 章节主要剧情走向
        FoundationCard(
            title = "④ 章节主要剧情走向",
            value = foundation.chapterPlotDirection,
            onValueChange = { onFoundationUpdate("chapterPlotDirection", it) },
            placeholder = "描述1-10章的主要剧情脉络...",
            minLines = 3
        )
        
        // 5. 写作风格
        FoundationCard(
            title = "⑤ 写作风格",
            value = foundation.writingStyle,
            onValueChange = { onFoundationUpdate("writingStyle", it) },
            placeholder = "描述文风特点：热血/悬疑/轻松等..."
        )
        
        // 6. 章节摘要（初始为空或暂无）
        FoundationCard(
            title = "⑥ 目前为止的章节摘要",
            value = foundation.chapterSummaries,
            onValueChange = { onFoundationUpdate("chapterSummaries", it) },
            placeholder = "新作品暂无，写完章节后会自动更新",
            readOnly = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 底部按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onReset,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("重新开始")
            }
            
            OutlinedButton(
                onClick = onRegenerate,
                modifier = Modifier.weight(1f),
                enabled = !isGenerating
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                }
                Spacer(Modifier.width(4.dp))
                Text("重新生成")
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Button(
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isGenerating && foundation.characterSettings.isNotBlank()
        ) {
            Icon(Icons.Default.Check, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("确认，开始写作")
        }
    }
}

/**
 * 基础设定卡片组件
 */
@Composable
private fun FoundationCard(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    minLines: Int = 2,
    readOnly: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(placeholder, style = MaterialTheme.typography.bodySmall) },
                minLines = minLines,
                maxLines = if (minLines > 3) 6 else minLines + 2,
                readOnly = readOnly,
                textStyle = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/**
 * GENERATING_CHAPTER 状态：正在生成章节
 */
@Composable
private fun ColumnScope.GeneratingChapterContent(
    streamingContent: String,
    isGenerating: Boolean,
    onStop: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
    ) {
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
                        text = "正在生成章节内容...",
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                HorizontalDivider()
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 流式显示章节内容
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = streamingContent.ifBlank { "正在创作中..." },
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (isGenerating) {
            OutlinedButton(
                onClick = onStop,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Stop, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("停止生成")
            }
        }
    }
}

/**
 * COMPLETED 状态：完成界面
 */
@Composable
private fun CompletedContent(
    currentNovel: com.xsgrok.app.data.model.Novel?,
    nextChapterGuide: String,
    onNextGuideChange: (String) -> Unit,
    onNavigateToReading: () -> Unit,
    onContinueWriting: () -> Unit,
    onReset: () -> Unit
) {
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState())
    ) {
        // 完成卡片
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
                        text = "《${novel.title}》已完成 ${novel.chapters.size} 章",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(onClick = onNavigateToReading) {
                        Icon(Icons.Default.MenuBook, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("阅读")
                    }
                    
                    OutlinedButton(onClick = onReset) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("新作品")
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 继续生成下一章
        Text(
            text = "继续创作",
            style = MaterialTheme.typography.titleSmall
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        OutlinedTextField(
            value = nextChapterGuide,
            onValueChange = onNextGuideChange,
            label = { Text("下一章引导（可选）") },
            placeholder = { Text("例如：主角遇到危机，展开反击") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Button(
            onClick = onContinueWriting,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.NavigateNext, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("生成下一章")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 章节摘要预览
        currentNovel?.foundation?.chapterSummaries?.let { summary ->
            if (summary.isNotBlank()) {
                Text(
                    text = "故事摘要",
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}
