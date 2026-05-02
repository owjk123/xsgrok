package com.xsgrok.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xsgrok.app.R
import com.xsgrok.app.data.model.AutoModeState
import com.xsgrok.app.data.model.GenerationPresets
import com.xsgrok.app.data.model.Novel
import com.xsgrok.app.ui.Screen
import com.xsgrok.app.ui.XSGrokViewModel

/**
 * 创作页面 - 首屏
 * 合并了原 HomeScreen + AutoModeScreen 的核心功能
 * 一句话创作入口 + 最近作品
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateScreen(viewModel: XSGrokViewModel) {
    val novels by viewModel.novels.collectAsState()
    val currentNovel by viewModel.currentNovel.collectAsState()
    val streamingContent by viewModel.streamingContent.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val autoModeState by viewModel.autoModeState.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val currentPreset by viewModel.currentPreset.collectAsState()
    val currentFoundation by viewModel.currentFoundation.collectAsState()
    val apiConfig by viewModel.uiState.collectAsState()

    var userPrompt by remember { mutableStateOf("") }
    var nextChapterGuide by remember { mutableStateOf("") }
    var selectedPresetId by remember { mutableStateOf("balanced") }

    // 根据autoModeState切换内容
    Column(modifier = Modifier.fillMaxSize()) {
        when (autoModeState) {
            AutoModeState.IDLE -> {
                // === 首页：一句话创作 + 最近作品 ===
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
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
                                Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Spacer(Modifier.width(8.dp))
                                Text(error, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.weight(1f))
                                IconButton(onClick = { viewModel.clearError() }) {
                                    Icon(Icons.Default.Close, contentDescription = "关闭", tint = MaterialTheme.colorScheme.onErrorContainer)
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }

                    // 一句话创作卡片（核心入口）
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "一句话开始创作",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(
                                value = userPrompt,
                                onValueChange = { userPrompt = it },
                                placeholder = { Text("例如：一个现代程序员穿越到修仙世界...") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2,
                                maxLines = 4,
                                textStyle = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(Modifier.height(12.dp))

                            // 生成模式
                            Text("生成模式", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Spacer(Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                GenerationPresets.getAll().forEach { preset ->
                                    FilterChip(
                                        selected = selectedPresetId == preset.id,
                                        onClick = {
                                            selectedPresetId = preset.id
                                            viewModel.setGenerationPreset(preset.id)
                                        },
                                        label = { Text(preset.name, style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }
                            Spacer(Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    if (userPrompt.isNotBlank()) {
                                        viewModel.startAutoMode(userPrompt)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = userPrompt.isNotBlank() && !isGenerating && apiConfig.apiConfig.apiKey.isNotBlank()
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("开始创作")
                            }

                            if (apiConfig.apiConfig.apiKey.isBlank()) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "请先在「设置」中配置API Key",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    // 最近作品
                    if (novels.isNotEmpty()) {
                        Spacer(Modifier.height(24.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("最近作品", style = MaterialTheme.typography.titleMedium)
                            TextButton(onClick = { viewModel.navigateTo(Screen.Bookshelf) }) {
                                Text("查看全部")
                            }
                        }
                        Spacer(Modifier.height(8.dp))

                        novels.sortedByDescending { it.updatedAt }.take(5).forEach { novel ->
                            RecentNovelCard(
                                novel = novel,
                                onClick = { viewModel.selectNovel(novel.id) },
                                onContinue = { viewModel.continueNovel(novel.id) }
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }

            AutoModeState.GENERATING_FOUNDATION -> {
                // 生成基础设定中
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Card(modifier = Modifier.fillMaxWidth().weight(1f)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("AI 正在分析创意，生成基础设定...", style = MaterialTheme.typography.titleSmall)
                            }
                            Spacer(Modifier.height(8.dp))
                            Text("这可能需要 10-30 秒，请稍候", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(16.dp))
                            HorizontalDivider()
                            Spacer(Modifier.height(16.dp))
                            Column(
                                modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())
                            ) {
                                Text(streamingContent.ifBlank { "正在思考中..." }, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    if (isGenerating) {
                        OutlinedButton(onClick = { viewModel.stopGeneration() }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Stop, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("停止生成")
                        }
                    }
                }
            }

            AutoModeState.REVIEW_FOUNDATION -> {
                // 审阅基础设定
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("《${currentNovel?.title ?: "新小说"}》", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(4.dp))
                            Text("请审阅以下6大基础设定，可直接编辑修改，确认后开始写作", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }
                    Spacer(Modifier.height(12.dp))

                    FoundationEditCard("① 角色设定", currentFoundation.characterSettings, { viewModel.updateFoundationField("characterSettings", it) }, "描述主角和配角的性格、外貌、背景等...")
                    FoundationEditCard("② 人物关系", currentFoundation.characterRelationships, { viewModel.updateFoundationField("characterRelationships", it) }, "描述角色之间的关系、矛盾、合作等...")
                    FoundationEditCard("③ 时间线", currentFoundation.timeline, { viewModel.updateFoundationField("timeline", it) }, "描述故事的时间跨度、重要节点等...")
                    FoundationEditCard("④ 章节主要剧情走向", currentFoundation.chapterPlotDirection, { viewModel.updateFoundationField("chapterPlotDirection", it) }, "描述1-10章的主要剧情脉络...", 3)
                    FoundationEditCard("⑤ 写作风格", currentFoundation.writingStyle, { viewModel.updateFoundationField("writingStyle", it) }, "描述文风特点：热血/悬疑/轻松等...")
                    FoundationEditCard("⑥ 目前为止的章节摘要", currentFoundation.chapterSummaries, { viewModel.updateFoundationField("chapterSummaries", it) }, "新作品暂无，写完章节后会自动更新", readOnly = true)

                    Spacer(Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { viewModel.resetAutoMode() }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("重新开始")
                        }
                        OutlinedButton(onClick = { viewModel.regenerateFoundation() }, modifier = Modifier.weight(1f), enabled = !isGenerating) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("重新生成")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.confirmFoundationAndStartWriting() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isGenerating && currentFoundation.characterSettings.isNotBlank()
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("确认，开始写作")
                    }
                }
            }

            AutoModeState.GENERATING_CHAPTER -> {
                // 生成章节中
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Card(modifier = Modifier.fillMaxWidth().weight(1f)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                    Spacer(Modifier.width(8.dp))
                                    Text("正在生成章节内容...", style = MaterialTheme.typography.titleSmall)
                                }
                                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                                    Text(
                                        "已输出 ${streamingContent.length} 字",
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                            HorizontalDivider()
                            Spacer(Modifier.height(16.dp))
                            Column(
                                modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())
                            ) {
                                Text(streamingContent.ifBlank { "正在思考中..." }, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    if (isGenerating) {
                        OutlinedButton(onClick = { viewModel.stopGeneration() }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Stop, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("停止生成")
                        }
                    }
                }
            }

            AutoModeState.COMPLETED -> {
                // 完成
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(32.dp))
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(16.dp))
                    Text("章节生成完成！", style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(8.dp))
                    currentNovel?.let { novel ->
                        Text("《${novel.title}》- 共${novel.chapters.size}章", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(24.dp))

                    Button(onClick = { viewModel.navigateTo(Screen.Reading) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.MenuBook, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("开始阅读")
                    }
                    Spacer(Modifier.height(8.dp))

                    // 继续写作
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("继续写作", style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = nextChapterGuide,
                                onValueChange = { nextChapterGuide = it },
                                placeholder = { Text("引导下一章走向（可选）...") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2
                            )
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = { viewModel.continueAutoMode(nextChapterGuide) },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isGenerating
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("续写下一章")
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))

                    OutlinedButton(onClick = { nextChapterGuide = ""; viewModel.resetAutoMode() }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("创作新小说")
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentNovelCard(
    novel: Novel,
    onClick: () -> Unit,
    onContinue: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(novel.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Row {
                    AssistChip(onClick = {}, label = { Text(novel.genre, style = MaterialTheme.typography.labelSmall) }, modifier = Modifier.padding(end = 4.dp))
                    if (novel.style.isNotBlank()) AssistChip(onClick = {}, label = { Text(novel.style, style = MaterialTheme.typography.labelSmall) })
                }
                Spacer(Modifier.height(2.dp))
                Text("${novel.chapters.size}章", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onContinue) {
                Icon(Icons.Default.EditNote, contentDescription = "继续写作", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun FoundationEditCard(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    minLines: Int = 2,
    readOnly: Boolean = false
) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
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
