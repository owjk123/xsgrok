package com.xsgrok.app.ui.screens

import android.content.Context
import android.content.Intent
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.xsgrok.app.R
import com.xsgrok.app.data.model.Novel
import com.xsgrok.app.ui.Screen
import com.xsgrok.app.ui.XSGrokViewModel
import java.io.File

/**
 * 阅读页面 - 优化版
 * - LazyColumn替代verticalScroll
 * - 字体大小调节
 * - 行间距调节
 * - 沉浸阅读模式
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingScreen(viewModel: XSGrokViewModel) {
    val currentNovel by viewModel.currentNovel.collectAsState()
    val context = LocalContext.current
    val activity = context as? android.app.Activity
    
    // 阅读设置状态
    var fontSize by remember { mutableStateOf(FontSize.MEDIUM) }
    var lineSpacing by remember { mutableStateOf(LineSpacing.MEDIUM) }
    var isImmersiveMode by remember { mutableStateOf(false) }
    var showChapterList by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var currentChapterIndex by remember { mutableIntStateOf(0) }
    
    // 流式字数统计
    val streamingContent by viewModel.streamingContent.collectAsState()
    
    currentNovel?.let { novel ->
        val sortedChapters = novel.chapters.sortedBy { it.order }
        
        if (sortedChapters.isEmpty()) {
            EmptyChaptersView()
            return
        }
        
        val currentChapter = sortedChapters.getOrNull(currentChapterIndex) ?: sortedChapters.first()
        
        // 沉浸模式控制
        DisposableEffect(isImmersiveMode, activity) {
            if (isImmersiveMode && activity != null) {
                val window = activity.window
                WindowCompat.setDecorFitsSystemWindows(window, false)
                WindowInsetsControllerCompat(window, activity.window.decorView).let { controller ->
                    controller.hide(WindowInsetsCompat.Type.systemBars())
                    controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            } else if (activity != null) {
                val window = activity.window
                WindowCompat.setDecorFitsSystemWindows(window, true)
                WindowInsetsControllerCompat(window, activity.window.decorView).show(WindowInsetsCompat.Type.systemBars())
            }
            
            onDispose {
                // 退出时恢复状态栏
                activity?.let { act ->
                    val window = act.window
                    WindowCompat.setDecorFitsSystemWindows(window, true)
                    WindowInsetsControllerCompat(window, act.window.decorView).show(WindowInsetsCompat.Type.systemBars())
                }
            }
        }
        
        Column(modifier = Modifier.fillMaxSize()) {
            // 顶部操作栏（非沉浸模式下显示）
            if (!isImmersiveMode) {
                TopAppBar(
                    title = { 
                        Column {
                            Text(
                                text = novel.title,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = currentChapter.title,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.navigateTo(Screen.Bookshelf) }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    },
                    actions = {
                        IconButton(onClick = { showChapterList = true }) {
                            Icon(Icons.Default.List, contentDescription = stringResource(R.string.chapter_list))
                        }
                        IconButton(onClick = { showSettings = !showSettings }) {
                            Icon(Icons.Default.Settings, contentDescription = "阅读设置")
                        }
                        IconButton(onClick = { exportNovel(context, novel) }) {
                            Icon(Icons.Default.Share, contentDescription = stringResource(R.string.export))
                        }
                    }
                )
            }
            
            // 阅读设置面板
            if (showSettings && !isImmersiveMode) {
                ReadingSettingsPanel(
                    fontSize = fontSize,
                    onFontSizeChange = { fontSize = it },
                    lineSpacing = lineSpacing,
                    onLineSpacingChange = { lineSpacing = it },
                    isImmersiveMode = isImmersiveMode,
                    onImmersiveModeToggle = { isImmersiveMode = it }
                )
            }
            
            // 阅读内容区 - 使用LazyColumn
            val listState = rememberLazyListState()
            
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(lineSpacing.spacing.dp)
            ) {
                // 章节标题
                item {
                    ChapterTitle(
                        title = currentChapter.title,
                        fontSize = fontSize,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
                
                // 章节内容
                item {
                    ChapterContent(
                        content = currentChapter.content,
                        fontSize = fontSize,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                }
                
                // 章节字数统计
                item {
                    WordCountIndicator(
                        wordCount = currentChapter.wordCount,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
                
                // 翻页按钮
                item {
                    ChapterNavigation(
                        currentIndex = currentChapterIndex,
                        totalChapters = sortedChapters.size,
                        onPrev = { if (currentChapterIndex > 0) currentChapterIndex-- },
                        onNext = { if (currentChapterIndex < sortedChapters.size - 1) currentChapterIndex++ },
                        modifier = Modifier.padding(bottom = 32.dp)
                    )
                }
            }
            
            // 沉浸模式下显示的迷你控制条
            if (isImmersiveMode) {
                ImmersiveControlBar(
                    onSettingsClick = { showSettings = !showSettings },
                    onExitClick = { isImmersiveMode = false },
                    onChapterListClick = { showChapterList = true },
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
        
        // 章节目录Sheet
        if (showChapterList) {
            ChapterListSheet(
                novel = novel,
                currentIndex = currentChapterIndex,
                onSelect = { index ->
                    currentChapterIndex = index
                    showChapterList = false
                },
                onDismiss = { showChapterList = false }
            )
        }
    } ?: run {
        // 无小说时显示
        EmptyNovelView()
    }
}

@Composable
private fun ChapterTitle(
    title: String,
    fontSize: FontSize,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall.copy(
            fontSize = (fontSize.size + 8).sp,
            fontWeight = FontWeight.Bold,
            lineHeight = (fontSize.size + 14).sp
        ),
        modifier = modifier
    )
}

@Composable
private fun ChapterContent(
    content: String,
    fontSize: FontSize,
    modifier: Modifier = Modifier
) {
    Text(
        text = content,
        style = MaterialTheme.typography.bodyLarge.copy(
            fontSize = fontSize.size.sp,
            lineHeight = (fontSize.size * lineHeightMultiplier(fontSize)).sp
        ),
        modifier = modifier
    )
}

@Composable
private fun WordCountIndicator(
    wordCount: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Text(
                text = "本章 $wordCount 字",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun ChapterNavigation(
    currentIndex: Int,
    totalChapters: Int,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (currentIndex > 0) {
            OutlinedButton(onClick = onPrev) {
                Icon(Icons.Default.ArrowBack, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("上一章")
            }
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
        
        Text(
            text = "${currentIndex + 1} / $totalChapters",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.align(Alignment.CenterVertically)
        )
        
        if (currentIndex < totalChapters - 1) {
            Button(onClick = onNext) {
                Text("下一章")
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Default.ArrowForward, contentDescription = null)
            }
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun ReadingSettingsPanel(
    fontSize: FontSize,
    onFontSizeChange: (FontSize) -> Unit,
    lineSpacing: LineSpacing,
    onLineSpacingChange: (LineSpacing) -> Unit,
    isImmersiveMode: Boolean,
    onImmersiveModeToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "阅读设置",
                style = MaterialTheme.typography.titleSmall
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 字体大小
            Text(
                text = "字体大小",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FontSize.entries.forEach { size ->
                    FilterChip(
                        selected = fontSize == size,
                        onClick = { onFontSizeChange(size) },
                        label = { Text(size.label) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 行间距
            Text(
                text = "行间距",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LineSpacing.entries.forEach { spacing ->
                    FilterChip(
                        selected = lineSpacing == spacing,
                        onClick = { onLineSpacingChange(spacing) },
                        label = { Text(spacing.label) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 沉浸模式
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Fullscreen, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("沉浸阅读模式")
                }
                Switch(
                    checked = isImmersiveMode,
                    onCheckedChange = onImmersiveModeToggle
                )
            }
        }
    }
}

@Composable
private fun ImmersiveControlBar(
    onSettingsClick: () -> Unit,
    onExitClick: () -> Unit,
    onChapterListClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        IconButton(onClick = onSettingsClick) {
            Icon(Icons.Default.Settings, contentDescription = "设置")
        }
        IconButton(onClick = onChapterListClick) {
            Icon(Icons.Default.List, contentDescription = "目录")
        }
        IconButton(onClick = onExitClick) {
            Icon(Icons.Default.FullscreenExit, contentDescription = "退出沉浸")
        }
    }
}

@Composable
private fun EmptyChaptersView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.MenuBook,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                stringResource(R.string.no_chapters),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun EmptyNovelView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("请先选择一本小说")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterListSheet(
    novel: Novel,
    currentIndex: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val sortedChapters = novel.chapters.sortedBy { it.order }
    
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.chapter_list),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
            ) {
                itemsIndexed(sortedChapters) { index, chapter ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onSelect(index) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (index == currentIndex) 
                                MaterialTheme.colorScheme.primaryContainer 
                            else 
                                MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${index + 1}. ${chapter.title}",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = stringResource(R.string.words_count, chapter.wordCount),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun exportNovel(context: Context, novel: Novel) {
    try {
        val content = buildString {
            appendLine("《${novel.title}》")
            appendLine()
            appendLine("类型：${novel.genre}  风格：${novel.style}")
            appendLine()
            appendLine("=" .repeat(50))
            appendLine()
            novel.chapters.sortedBy { it.order }.forEachIndexed { index, chapter ->
                appendLine(chapter.title)
                appendLine()
                appendLine(chapter.content)
                appendLine()
                appendLine("-".repeat(30))
                appendLine()
            }
            appendLine()
            appendLine("=" .repeat(50))
            appendLine("字数统计：${novel.chapters.sumOf { it.wordCount }}")
        }
        
        val fileName = "${novel.title.replace(" ", "_").replace(Regex("[^\\w\\u4e00-\\u9fa5]"), "")}.txt"
        val file = File(context.cacheDir, fileName)
        file.writeText(content)
        
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
        
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.export_novel)))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/**
 * 字体大小枚举
 */
enum class FontSize(val size: Int, val label: String) {
    SMALL(14, "小"),
    MEDIUM(16, "中"),
    LARGE(18, "大"),
    EXTRA_LARGE(20, "特大")
}

/**
 * 行间距枚举
 */
enum class LineSpacing(val spacing: Int, val label: String) {
    TIGHT(8, "紧凑"),
    MEDIUM(16, "适中"),
    LOOSE(24, "宽松")
}

/**
 * 计算行高倍数
 */
private fun lineHeightMultiplier(fontSize: FontSize): Float {
    return when (fontSize) {
        FontSize.SMALL -> 1.6f
        FontSize.MEDIUM -> 1.8f
        FontSize.LARGE -> 2.0f
        FontSize.EXTRA_LARGE -> 2.2f
    }
}
