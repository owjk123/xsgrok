package com.xsgrok.app.ui.screens

import android.content.Context
import android.content.Intent
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
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.xsgrok.app.R
import com.xsgrok.app.ui.XSGrokViewModel
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun ChapterGenerationScreen(viewModel: XSGrokViewModel) {
    val currentNovel by viewModel.currentNovel.collectAsState()
    val streamingContent by viewModel.streamingContent.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val context = LocalContext.current
    
    var chapterTitle by remember { mutableStateOf("") }
    var showContinueOption by remember { mutableStateOf(false) }
    
    LaunchedEffect(currentNovel) {
        if (currentNovel?.chapters?.isNotEmpty() == true) {
            showContinueOption = true
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        currentNovel?.let { novel ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.generate_chapter),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.novel_prefix) + novel.title,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (!isGenerating && streamingContent.isBlank()) {
                OutlinedTextField(
                    value = chapterTitle,
                    onValueChange = { chapterTitle = it },
                    label = { Text(stringResource(R.string.chapter_title)) },
                    placeholder = { Text(stringResource(R.string.enter_chapter_title)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (chapterTitle.isNotBlank()) {
                                viewModel.generateChapter(novel.id, chapterTitle)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = chapterTitle.isNotBlank()
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.generate))
                    }
                    
                    if (showContinueOption) {
                        FilledTonalButton(
                            onClick = { viewModel.continueChapter(novel.id, null) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.continue_label))
                        }
                    }
                }
            }
            
            if (isGenerating || streamingContent.isNotBlank()) {
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
                                text = if (isGenerating) stringResource(R.string.generating) else stringResource(R.string.generated_content),
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
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isGenerating) {
                        OutlinedButton(
                            onClick = { viewModel.stopGeneration() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.stop))
                        }
                    }
                    
                    FilledTonalButton(
                        onClick = {
                            exportToTxt(context, novel.title, streamingContent)
                        },
                        modifier = Modifier.weight(1f),
                        enabled = streamingContent.isNotBlank()
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.export_txt))
                    }
                    
                    FilledTonalButton(
                        onClick = { viewModel.navigateTo(com.xsgrok.app.ui.Screen.NovelDetail) },
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
    
    errorMessage?.let { error ->
        LaunchedEffect(error) {
            delay(3000)
            viewModel.clearError()
        }
        Snackbar(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(error)
        }
    }
}

private fun exportToTxt(context: Context, title: String, content: String) {
    try {
        val fileName = "${title.replace(" ", "_")}.txt"
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
