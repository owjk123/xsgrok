package com.xsgrok.app.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.xsgrok.app.R
import com.xsgrok.app.data.model.Novel
import com.xsgrok.app.ui.Screen
import com.xsgrok.app.ui.XSGrokViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingScreen(viewModel: XSGrokViewModel) {
    val currentNovel by viewModel.currentNovel.collectAsState()
    var currentChapterIndex by remember { mutableStateOf(0) }
    var showChapterList by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    currentNovel?.let { novel ->
        val sortedChapters = novel.chapters.sortedBy { it.order }
        
        if (sortedChapters.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.no_chapters))
            }
            return
        }
        
        val currentChapter = sortedChapters.getOrNull(currentChapterIndex) ?: sortedChapters.first()
        
        Column(modifier = Modifier.fillMaxSize()) {
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
                    IconButton(onClick = { 
                        viewModel.selectNovel(novel.id)
                        viewModel.navigateTo(Screen.WorldBuilding) 
                    }) {
                        Icon(Icons.Default.Public, contentDescription = stringResource(R.string.world_building))
                    }
                    IconButton(onClick = { exportNovel(context, novel) }) {
                        Icon(Icons.Default.Share, contentDescription = stringResource(R.string.export))
                    }
                }
            )
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Text(
                    text = currentChapter.content,
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = MaterialTheme.typography.bodyLarge.lineHeight
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (currentChapterIndex > 0) {
                        OutlinedButton(onClick = { currentChapterIndex-- }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.prev_chapter))
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    
                    if (currentChapterIndex < sortedChapters.size - 1) {
                        Button(onClick = { currentChapterIndex++ }) {
                            Text(stringResource(R.string.next_chapter))
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Default.ArrowForward, contentDescription = null)
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        
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
    
    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
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
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                sortedChapters.forEachIndexed { index, chapter ->
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
            appendLine("---")
            appendLine()
            novel.chapters.sortedBy { it.order }.forEach { chapter ->
                appendLine(chapter.title)
                appendLine()
                appendLine(chapter.content)
                appendLine()
                appendLine("---")
                appendLine()
            }
        }
        
        val fileName = "${novel.title.replace(" ", "_")}.txt"
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
